package com.eqnitymusic.damn

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class EqnityMusicApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
    
    companion object {
        lateinit var instance: EqnityMusicApplication
            private set
        
        fun getInstance(): EqnityMusicApplication = instance
    }
}