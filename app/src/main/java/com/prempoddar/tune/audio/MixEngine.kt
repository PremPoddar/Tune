package com.prempoddar.tune.audio

import android.content.Context
import android.os.SystemClock
import com.prempoddar.tune.core.AppProfile
import com.prempoddar.tune.core.DEFAULT_PKG
import com.prempoddar.tune.core.LearnDebouncer
import com.prempoddar.tune.core.LevelSource
import com.prempoddar.tune.core.Levels
import com.prempoddar.tune.core.MixInput
import com.prempoddar.tune.core.MixPlan
import com.prempoddar.tune.core.MixPlanner
import com.prempoddar.tune.core.Player
import com.prempoddar.tune.core.ProfileResolver
import com.prempoddar.tune.core.Resolved
import com.prempoddar.tune.core.RouteInfo
import com.prempoddar.tune.core.RouteKeys
import com.prempoddar.tune.core.SelfChangeGuard
import com.prempoddar.tune.core.StreamKind
import com.prempoddar.tune.core.VolumeMath
import com.prempoddar.tune.data.ProfileRepository
import com.prempoddar.tune.data.SettingsRepository
import com.prempoddar.tune.data.TuneSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A level set in Tune that is applied right away but not yet visible in the database. */
data class OverrideKey(val pkg: String, val route: String, val kind: StreamKind)

data class EngineState(
    /** True while the accessibility service is running the engine. */
    val running: Boolean = false,
    val enabled: Boolean = true,
    val route: RouteInfo = RouteKeys.SPEAKER,
    val foreground: String? = null,
    val players: List<Player> = emptyList(),
    val plan: MixPlan? = null,
    val profiles: List<AppProfile> = emptyList(),
    val hasDump: Boolean = false,
    /** Android refused a ring/notification change: Do Not Disturb access is missing. */
    val ringBlocked: Boolean = false,
    val overrides: Map<OverrideKey, Int> = emptyMap(),
) {
    fun resolved(pkg: String, routeKey: String = route.key): Resolved {
        val base = ProfileResolver.resolve(pkg, routeKey, profiles)
        var levels = base.levels
        overrides.forEach { (k, v) -> if (k.pkg == pkg && k.route == routeKey) levels = levels.with(k.kind, v) }
        return base.copy(levels = levels)
    }

    fun levelsFor(pkg: String): Levels = resolved(pkg).levels

    /** The app the volume keys move right now. */
    val target: String get() = plan?.target ?: foreground ?: DEFAULT_PKG
}

/**
 * Runs Tune: watches who is on screen and who is playing, plans the mix, applies it, and turns
 * the user's volume changes into saved levels.
 */
class MixEngine(
    context: Context,
    private val repo: ProfileRepository,
    private val settingsRepo: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val guard = SelfChangeGuard()
    private val streams = StreamController(context, guard)
    private val routes = RouteWatcher(context)
    private val sessions = SessionTracker(context)
    private val media = MediaSessionWatcher(context)
    private val effects = EffectController()
    val foreground = ForegroundTracker(context)

    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state.asStateFlow()

    var settings = TuneSettings()
        private set
    private val firstSeen = mutableMapOf<String, Long>()
    private var lastApplied: MixPlan? = null
    private var runJob: Job? = null

    private val debouncer = LearnDebouncer(scope) { pkg, kind, pct ->
        val key = pkg to kind
        val route = pendingRoute[key] ?: return@LearnDebouncer
        repo.setLevel(pkg, route, kind, pct, ProfileResolver.resolve(pkg, route, _state.value.profiles).levels)
    }
    private val pendingRoute = mutableMapOf<Pair<String, StreamKind>, String>()

    init {
        // Profiles and settings are followed for the app's whole life so the UI always has them.
        scope.launch {
            repo.profiles.collect { profiles ->
                _state.update { s -> s.copy(profiles = profiles, overrides = pruneSaved(s.overrides, profiles)) }
            }
        }
        scope.launch {
            settingsRepo.settings.collect { s ->
                settings = s
                _state.update { it.copy(enabled = s.enabled) }
            }
        }
    }

    fun start() {
        if (runJob != null) return
        streams.start()
        routes.start()
        media.start()
        runJob = scope.launch {
            sessions.start(this)
            launch { streams.userChanges.collect { (kind, pct) -> onSystemChange(kind, pct) } }
            launch {
                routes.route.collect { r ->
                    repo.seenRoute(r)
                    lastApplied = null // a new output has its own levels; apply everything again
                }
            }
            launch {
                combine(
                    foreground.foreground,
                    media.playing,
                    sessions.dumpStarted,
                    sessions.sessions,
                    effects.failed,
                ) { fg, playing, dumpStarted, known, failed ->
                    val now = SystemClock.elapsedRealtime()
                    val active = playing.keys + dumpStarted
                    firstSeen.keys.retainAll(active)
                    val players = active.map { pkg -> Player(pkg, playing[pkg] ?: firstSeen.getOrPut(pkg) { now }) }
                    Triple(fg, players, known.filterKeys { it !in failed })
                }.combine(routes.route) { t, route -> t to route }
                    .collect { (t, route) ->
                        val (fg, players, known) = t
                        _state.update {
                            it.copy(running = true, foreground = fg, players = players, route = route, hasDump = sessions.hasDump)
                        }
                        knownSessions = known
                        replan()
                    }
            }
            launch {
                // Re-plan whenever stored levels, overrides or the on/off switch change.
                _state.distinctUntilChangedBy { Triple(it.profiles, it.overrides, it.enabled) }
                    .collect { replan() }
            }
        }
    }

    fun stop() {
        runJob?.cancel()
        runJob = null
        sessions.stop()
        media.stop()
        routes.stop()
        streams.stop()
        effects.releaseAll()
        lastApplied = null
        _state.update { it.copy(running = false, foreground = null, players = emptyList(), plan = null) }
    }

    fun onWindowChanged(pkg: String?) = foreground.onWindow(pkg)

    /**
     * Sets a level from Tune's own UI. Saved even for pinned apps: pinning only stops the volume
     * keys and other apps from changing a level.
     */
    fun setLevel(pkg: String, kind: StreamKind, pct: Int, routeKey: String = _state.value.route.key) {
        override(pkg, routeKey, kind, pct)
        learn(pkg, routeKey, kind, pct)
    }

    /** Volume key press: move the target app's media level one step. */
    fun step(up: Boolean) {
        val s = _state.value
        val target = s.target
        val current = s.levelsFor(target).media
        val step = settings.stepPct.coerceAtLeast(1)
        val next = if (up) (current / step + 1) * step else ((current + step - 1) / step - 1) * step
        changeFromOutside(target, StreamKind.MEDIA, next.coerceIn(0, 100))
    }

    private var knownSessions: Map<Int, String> = emptyMap()

    private fun onSystemChange(kind: StreamKind, pct: Int) {
        val s = _state.value
        val pkg = if (kind == StreamKind.MEDIA) s.target else s.foreground ?: DEFAULT_PKG
        changeFromOutside(pkg, kind, pct)
    }

    /** A change from the volume keys or the system: learned unless the app is pinned. */
    private fun changeFromOutside(pkg: String, kind: StreamKind, pct: Int) {
        val s = _state.value
        override(pkg, s.route.key, kind, pct)
        if (pkg != DEFAULT_PKG && !s.resolved(pkg).pinned) learn(pkg, s.route.key, kind, pct)
    }

    private fun override(pkg: String, routeKey: String, kind: StreamKind, pct: Int) {
        _state.update { it.copy(overrides = it.overrides + (OverrideKey(pkg, routeKey, kind) to pct.coerceIn(0, 100))) }
    }

    private fun learn(pkg: String, routeKey: String, kind: StreamKind, pct: Int) {
        pendingRoute[pkg to kind] = routeKey
        debouncer.onUserChange(pkg, kind, pct)
    }

    private fun pruneSaved(overrides: Map<OverrideKey, Int>, profiles: List<AppProfile>): Map<OverrideKey, Int> =
        overrides.filterNot { (k, v) ->
            val stored = ProfileResolver.resolve(k.pkg, k.route, profiles)
            stored.source == LevelSource.EXACT && stored.levels[k.kind] == v ||
                k.pkg == DEFAULT_PKG && stored.levels[k.kind] == v
        }

    private fun replan() {
        val s = _state.value
        if (!s.running) return
        // Pinned apps forget temporary changes once they stop being used.
        val inUse = s.players.map { it.pkg }.toSet() + listOfNotNull(s.foreground)
        val stale = s.overrides.keys.filter { it.pkg !in inUse && s.resolved(it.pkg).pinned }
        if (stale.isNotEmpty()) {
            _state.update { it.copy(overrides = it.overrides - stale.toSet()) }
            return // the update re-enters replan
        }
        val plan = MixPlanner.plan(
            MixInput(
                foreground = s.foreground,
                players = s.players,
                sessions = knownSessions,
                levelsFor = s::levelsFor,
                mediaMaxIndex = streams.maxIndex(StreamKind.MEDIA),
            ),
        )
        _state.update { it.copy(plan = plan) }
        if (!s.enabled) {
            effects.releaseAll()
            lastApplied = null
            return
        }
        val last = lastApplied
        if (last == null || plan.mediaIndex != last.mediaIndex) streams.apply(StreamKind.MEDIA, plan.mediaIndex)
        var blocked = false
        fun other(kind: StreamKind, pct: Int, lastPct: Int?) {
            if (lastPct == pct) return
            if (!streams.apply(kind, VolumeMath.pctToIndex(pct, streams.maxIndex(kind)))) blocked = true
        }
        other(StreamKind.RING, plan.ring, last?.ring)
        other(StreamKind.NOTIFICATION, plan.notification, last?.notification)
        other(StreamKind.ALARM, plan.alarm, last?.alarm)
        effects.apply(plan.sessionGains)
        lastApplied = plan
        if (blocked != s.ringBlocked) _state.update { it.copy(ringBlocked = blocked) }
    }
}
