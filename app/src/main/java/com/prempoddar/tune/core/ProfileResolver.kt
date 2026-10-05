package com.prempoddar.tune.core

enum class LevelSource { EXACT, INHERITED, DEFAULT }

data class Resolved(val levels: Levels, val pinned: Boolean, val source: LevelSource)

/** Decides which stored levels apply to an app on an output route. */
object ProfileResolver {

    fun resolve(pkg: String, route: String, all: List<AppProfile>): Resolved {
        if (pkg != DEFAULT_PKG) {
            val own = all.filter { it.pkg == pkg }
            own.firstOrNull { it.route == route }?.let {
                return Resolved(it.levels, it.pinned, LevelSource.EXACT)
            }
            // First use on a new route starts from where the app was last used.
            own.maxByOrNull { it.updatedAt }?.let {
                return Resolved(it.levels, it.pinned, LevelSource.INHERITED)
            }
        }
        return Resolved(defaultFor(route, all), pinned = false, source = LevelSource.DEFAULT)
    }

    fun defaultFor(route: String, all: List<AppProfile>): Levels {
        val defaults = all.filter { it.pkg == DEFAULT_PKG }
        return defaults.firstOrNull { it.route == route }?.levels
            ?: defaults.firstOrNull { it.route == SPEAKER_ROUTE }?.levels
            ?: Levels.FALLBACK
    }
}
