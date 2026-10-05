package com.prempoddar.tune.data.db

import androidx.room.Entity
import com.prempoddar.tune.core.AppProfile
import com.prempoddar.tune.core.Levels

@Entity(tableName = "profiles", primaryKeys = ["pkg", "route"])
data class ProfileEntity(
    val pkg: String,
    val route: String,
    val media: Int,
    val ring: Int,
    val notification: Int,
    val alarm: Int,
    val pinned: Boolean,
    val updatedAt: Long,
) {
    fun toModel() = AppProfile(pkg, route, Levels(media, ring, notification, alarm), pinned, updatedAt)

    companion object {
        fun from(p: AppProfile) = ProfileEntity(
            p.pkg, p.route, p.levels.media, p.levels.ring, p.levels.notification, p.levels.alarm,
            p.pinned, p.updatedAt,
        )
    }
}
