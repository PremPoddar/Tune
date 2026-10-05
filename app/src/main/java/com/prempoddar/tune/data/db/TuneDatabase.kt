package com.prempoddar.tune.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProfileEntity::class, RouteEntity::class], version = 1)
abstract class TuneDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        fun build(context: Context): TuneDatabase =
            Room.databaseBuilder(context, TuneDatabase::class.java, "tune.db").build()
    }
}
