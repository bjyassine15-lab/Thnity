package com.example

import android.app.Application
import com.google.firebase.FirebaseApp

class TransitApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // FirebaseInitProvider normally performs this automatically, but the
        // explicit guard keeps startup deterministic when using Firebase APIs
        // from repositories created during the first Activity launch.
        if (FirebaseApp.getApps(this).isEmpty()) {
            checkNotNull(FirebaseApp.initializeApp(this)) {
                "Firebase initialization failed. Check app/google-services.json and the applicationId."
            }
        }
    }
}
