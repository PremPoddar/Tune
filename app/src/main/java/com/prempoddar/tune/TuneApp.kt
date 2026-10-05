package com.prempoddar.tune

import android.app.Application

class TuneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
