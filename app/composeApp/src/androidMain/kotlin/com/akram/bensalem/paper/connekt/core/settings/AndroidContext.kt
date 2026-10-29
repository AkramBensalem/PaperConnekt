package com.akram.bensalem.paper.connekt.core.settings

import android.content.Context

private var applicationContext: Context? = null

fun setAndroidApplicationContext(context: Context) {
    applicationContext = context.applicationContext
}

fun requireAndroidApplicationContext(): Context =
    checkNotNull(applicationContext) { "Android application context has not been initialized." }
