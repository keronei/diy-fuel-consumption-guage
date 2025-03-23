package com.keronei.kmlguage

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.FirebaseApp

class GuageApp : Application() {
    lateinit var eventViewModel: EventViewModel

    override fun onCreate() {
        super.onCreate()

        eventViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(this)
            .create(EventViewModel::class.java)

        FirebaseApp.initializeApp(this)
    }
}
