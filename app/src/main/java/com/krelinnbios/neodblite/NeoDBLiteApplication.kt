package com.krelinnbios.neodblite

import android.app.Application
import com.krelinnbios.neodblite.global.App

class NeoDBLiteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        App.init(this)
    }
}
