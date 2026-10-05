package com.prempoddar.tune

import android.app.Application
import com.prempoddar.tune.audio.MixEngine
import com.prempoddar.tune.data.AppInfoCache
import com.prempoddar.tune.data.ProfileRepository
import com.prempoddar.tune.data.SettingsRepository
import com.prempoddar.tune.data.db.TuneDatabase

/** The app's long-lived objects, wired by hand. */
object AppGraph {
    private lateinit var app: Application

    fun init(application: Application) {
        app = application
    }

    val database by lazy { TuneDatabase.build(app) }
    val profiles by lazy { ProfileRepository(database.profileDao()) }
    val settings by lazy { SettingsRepository(app) }
    val appInfo by lazy { AppInfoCache(app) }
    val engine by lazy { MixEngine(app, profiles, settings) }
}
