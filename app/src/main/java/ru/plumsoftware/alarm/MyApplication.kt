package ru.plumsoftware.alarm

import android.app.Application
import ru.plumsoftware.alarm.data.AdsConfig

// В Application.kt или в onCreate() MainActivity
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }

    companion object {
        val adsConfig = AdsConfig.HuaweiAppGallery()
    }
}