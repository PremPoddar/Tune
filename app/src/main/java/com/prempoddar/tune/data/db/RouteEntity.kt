package com.prempoddar.tune.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** An output route Tune has seen, kept so its name can be shown when it is disconnected. */
@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val key: String,
    val name: String,
    val lastSeen: Long,
)
