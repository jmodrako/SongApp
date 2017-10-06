package com.tooploox.songapp

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class SongApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}