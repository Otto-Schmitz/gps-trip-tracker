package com.gearhead.redline

import android.app.Application
import com.gearhead.redline.di.ServiceLocator

class RedlineApp : Application() {

    lateinit var serviceLocator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        serviceLocator = ServiceLocator(this)
    }
}
