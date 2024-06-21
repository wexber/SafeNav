package com.example.safenav.ui.theme

import android.app.Application
import android.util.Log

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

//        // Configura el CustomExceptionHandler
//        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
//        Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler(this, defaultHandler))
//
//        Log.d("MyApplication", "CustomExceptionHandler configurado")
    }
}