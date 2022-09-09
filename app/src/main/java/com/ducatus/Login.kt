package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.ducatus.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var gso: GoogleSignInOptions
    private lateinit var gsc: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        isUserLoggedIn()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.tvForgotPassword.setOnClickListener {
            forgotPasswordLink()
        }

        binding.tvSignupLink.setOnClickListener {
            signupLink()
        }

        binding.btnLogin.setOnClickListener {
            // validate credentials -> login -> check if user is email verified
            validateCredentials()
        }

        binding.flLoginGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun forgotPasswordLink() {
        startActivity(Intent(this, ResetPasswordEmail::class.java))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun signupLink() {
        intent = Intent(this, Signup::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun isUserLoggedIn() {
        auth = Firebase.auth
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) {
            val intent = Intent(this, Homescreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        gsc = GoogleSignIn.getClient(this, gso)

        val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (googleSignInAccount != null) {
            val intent = Intent(this, Homescreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    private fun validateCredentials() {
        binding.tvLoginErrorAuth.text = ""
        binding.tvLoginErrorEmailUsername.text = ""
        binding.tvLoginErrorPassword.text = ""

        val emailUsername = binding.etLoginEmailUsername.text.toString().trim {it <= ' '}
        val password = binding.etLoginPassword.text.toString().trim {it <= ' '}

        if (TextUtils.isEmpty(emailUsername) || TextUtils.isEmpty(password)) {
            if (TextUtils.isEmpty(emailUsername)) binding.tvLoginErrorEmailUsername.text = "Please enter email / username"
            if (TextUtils.isEmpty(password)) binding.tvLoginErrorPassword.text = "Please enter password"
        }
        else {
            disableWindow()

            // check if input is an email or username
            if (!emailUsername.contains("@")) {
                database = Firebase.database
                databaseReference = database.getReference("users")
                databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var email: String? = null

                        for (child in snapshot.children) {
                            if(emailUsername == child.child("username").value.toString()) {
                                email = child.child("email").value.toString()
                                break
                            }
                        }

                        if (email != null) {
                            login(email, password)
                        }
                        else {
                            enableWindow()
                            binding.tvLoginErrorAuth.text = "User does not exist"
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        enableWindow()
                        Log.e("databaseError", error.message)
                        binding.tvLoginErrorAuth.text = error.message
                    }
                })
            }
            else {
                login(emailUsername, password)
            }
        }
    }

    private fun login(email: String, password: String) {
        auth = Firebase.auth
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser: FirebaseUser = task.result!!.user!!
                    isEmailVerified(firebaseUser)
                }
                else {
                    enableWindow()
                    Log.e("login", task.exception!!.message.toString())
                    binding.tvLoginErrorAuth.text = task.exception!!.message
                }
            }
    }

    private fun storeDataToRTDB(uid: String, email: String, username: String) {
        database = Firebase.database
        databaseReference = database.getReference("users")

        val user = User(uid, email, username, null)
        databaseReference.child(uid).setValue(user)
    }

    // Google Sign In
    private fun signInWithGoogle() {
        val signInIntent: Intent = gsc.signInIntent
        startActivityForResult(signInIntent, 100)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount? = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                disableWindow()
                userExists(account)

                val intent = Intent(this, Homescreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
            }
        }
        catch (e: ApiException) {
            Log.e("signup", e.statusCode.toString())
        }
    }

    private fun userExists(account: GoogleSignInAccount) {
        auth = Firebase.auth
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(account.email.toString())
            .addOnCompleteListener { task ->
                if(task.result.signInMethods?.isEmpty()!!) {
                    storeDataToRTDB(account.id.toString(), account.email.toString(), account.displayName.toString())
                }
            }
    }

    private fun isEmailVerified(firebaseUser: FirebaseUser) {
        disableWindow()

        if (firebaseUser.isEmailVerified) {
            val intent = Intent(this, Homescreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
        else {
            startActivity(Intent(this, VerifyEmail::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun enableWindow() {
        binding.btnLogin.setBackgroundResource(R.drawable.green_button)
        binding.pbLogin.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun disableWindow() {
        binding.btnLogin.setBackgroundResource(R.drawable.btn_disabled)
        binding.pbLogin.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}