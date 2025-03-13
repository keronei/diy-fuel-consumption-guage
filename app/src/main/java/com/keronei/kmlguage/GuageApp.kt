package com.keronei.kmlguage

import android.app.Application
import com.google.firebase.FirebaseApp

class GuageApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
