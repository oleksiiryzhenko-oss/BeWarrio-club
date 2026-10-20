package com.katadojo.app

import android.app.Application
import com.katadojo.app.data.AppContainer

class KataDojoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}
