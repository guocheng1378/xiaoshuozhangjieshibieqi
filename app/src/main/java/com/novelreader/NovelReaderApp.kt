package com.novelreader

import android.app.Application
import com.novelreader.BuildConfig

class NovelReaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NovelReaderApp
            private set

        val versionName: String
            get() = BuildConfig.VERSION_NAME
    }
}
