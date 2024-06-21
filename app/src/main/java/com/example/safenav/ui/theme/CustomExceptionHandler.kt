package com.example.safenav.ui.theme

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.example.safenav.MenuActivity

class CustomExceptionHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
//        Log.e("CustomExceptionHandler", "Excepción no capturada: ", throwable)
//
//        val intent = Intent(context, MenuActivity::class.java).apply {
//            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
//        }
//        context.startActivity(intent)
//
//        SystemClock.sleep(100)
//
//        defaultHandler?.uncaughtException(thread, throwable)
    }
}
