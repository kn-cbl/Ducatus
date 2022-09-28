package com.ducatus

import android.app.Activity
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AuthObserver {
    private lateinit var auth: FirebaseAuth

    fun isUserLoggedIn(activity: Activity, context: Context): Boolean {
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        return firebaseUser != null
    }
}