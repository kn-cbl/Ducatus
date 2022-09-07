package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.ducatus.databinding.ActivityHomescreenBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class Homescreen : AppCompatActivity() {
    private lateinit var binding: ActivityHomescreenBinding
    private lateinit var gso: GoogleSignInOptions
    private lateinit var gsc: GoogleSignInClient
    private var loginMethod: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homescreen)

        binding = ActivityHomescreenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loginMethod = intent.getIntExtra("loginMethod", 0)
        if (loginMethod == 1) {
            val firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
            if(firebaseUser != null) {
                binding.tv1.text = firebaseUser.uid
                binding.tv2.text = firebaseUser.displayName
                binding.tv3.text = firebaseUser.email
            }
        }
        else if (loginMethod == 2) {
            gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

            gsc = GoogleSignIn.getClient(this, gso)

            val account = GoogleSignIn.getLastSignedInAccount(this)!!
            binding.tv1.text = account.id
            binding.tv2.text = account.displayName
            binding.tv3.text = account.email
        }

        binding.btnLogout.setOnClickListener {
            if (loginMethod == 1) {
                logout()
            }
            else if (loginMethod == 2) {
                logoutGoogle()
            }
        }
    }

    private fun logoutGoogle() {
        gsc.signOut()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, Login::class.java))
                    finish()
                }
                else {
                    Toast.makeText(this, task.exception!!.message.toString(), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, Login::class.java))
        finish()
    }
}