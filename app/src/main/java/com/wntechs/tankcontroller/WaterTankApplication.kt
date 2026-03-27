package com.wntechs.tankcontroller

import android.app.Application
import com.wntechs.tankcontroller.data.AppContainer

class WaterTankApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
