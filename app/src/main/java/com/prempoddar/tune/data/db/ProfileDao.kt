package com.prempoddar.tune.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE pkg = :pkg")
    suspend fun forApp(pkg: String): List<ProfileEntity>

    @Upsert
    suspend fun upsert(entity: ProfileEntity)

    @Query("UPDATE profiles SET pinned = :pinned WHERE pkg = :pkg")
    suspend fun setPinned(pkg: String, pinned: Boolean)

    @Query("DELETE FROM profiles WHERE pkg = :pkg")
    suspend fun deleteApp(pkg: String)

    @Query("SELECT * FROM routes ORDER BY lastSeen DESC")
    fun observeRoutes(): Flow<List<RouteEntity>>

    @Upsert
    suspend fun upsertRoute(route: RouteEntity)
}
