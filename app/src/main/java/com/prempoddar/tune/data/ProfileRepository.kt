package com.prempoddar.tune.data

import com.prempoddar.tune.core.AppProfile
import com.prempoddar.tune.core.Levels
import com.prempoddar.tune.core.RouteInfo
import com.prempoddar.tune.core.StreamKind
import com.prempoddar.tune.data.db.ProfileDao
import com.prempoddar.tune.data.db.ProfileEntity
import com.prempoddar.tune.data.db.RouteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(
    private val dao: ProfileDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val profiles: Flow<List<AppProfile>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    val routes: Flow<List<RouteInfo>> = dao.observeRoutes().map { rows -> rows.map { RouteInfo(it.key, it.name) } }

    /** Stores one stream level, starting from [base] when the app has no row for [route] yet. */
    suspend fun setLevel(pkg: String, route: String, kind: StreamKind, pct: Int, base: Levels) {
        val existing = dao.forApp(pkg)
        val row = existing.firstOrNull { it.route == route }?.toModel()
            ?: AppProfile(pkg, route, base, existing.any { it.pinned }, 0)
        dao.upsert(ProfileEntity.from(row.copy(levels = row.levels.with(kind, pct), updatedAt = clock())))
    }

    /** Pins or unpins an app on every route, creating the [route] row from [base] if needed. */
    suspend fun setPinned(pkg: String, route: String, pinned: Boolean, base: Levels) {
        if (dao.forApp(pkg).none { it.route == route }) {
            dao.upsert(ProfileEntity.from(AppProfile(pkg, route, base, pinned, clock())))
        }
        dao.setPinned(pkg, pinned)
    }

    suspend fun forget(pkg: String) = dao.deleteApp(pkg)

    suspend fun seenRoute(route: RouteInfo) = dao.upsertRoute(RouteEntity(route.key, route.name, clock()))
}
