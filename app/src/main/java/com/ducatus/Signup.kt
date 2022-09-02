package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
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
import kotlinx.android.synthetic.main.activity_signup.*

class Signup : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var crypto: Crypto
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var gso: GoogleSignInOptions
    private lateinit var gsc: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        gsc = GoogleSignIn.getClient(this, gso)

        tvLoginLink.setOnClickListener {
            loginLink()
        }

        btnSignup.setOnClickListener {
            validateCredentials()
        }

        flSignupGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun loginLink() {
        startActivity(Intent(this, Login::class.java))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    // User Manual Sign In
    private fun validateCredentials() {
        tvSignupErrorAuth.text = ""
        tvSignupErrorUsername.visibility = View.INVISIBLE
        tvSignupErrorEmail.visibility = View.INVISIBLE
        tvSignupErrorPassword.visibility = View.INVISIBLE

        val username = etSignupUsername.text.toString().trim {it <= ' '}
        val email = etSignupEmail.text.toString().trim {it <= ' '}
        val password = etSignupPassword.text.toString().trim {it <= ' '}

        when {
            TextUtils.isEmpty(username) -> {
                pbSignup.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                tvSignupErrorUsername.visibility = View.VISIBLE
            }

            TextUtils.isEmpty(email) -> {
                pbSignup.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                tvSignupErrorEmail.visibility = View.VISIBLE
            }

            TextUtils.isEmpty(password) -> {
                pbSignup.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                tvSignupErrorPassword.visibility = View.VISIBLE
            }

            else -> {
                pbSignup.visibility = View.VISIBLE
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                database = Firebase.database
                databaseReference = database.getReference("users")

                databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var usernameExists = false

                        for (child in snapshot.children) {
                            if(username == child.child("username").value.toString()) {
                                usernameExists = true
                                break
                            }
                        }

                        if (!usernameExists) {
                            createUser(username, email, password)
                        }
                        else {
                            pbSignup.visibility = View.INVISIBLE
                            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                            Log.e("databaseReference", "username already exists")
                            tvSignupErrorAuth.text = "Username already exists"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        pbSignup.visibility = View.INVISIBLE
                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                        Log.e("databaseReference", error.toString())
                        tvSignupErrorAuth.text = error.message
                    }
                })
            }
        }
    }

    private fun createUser(username: String, email: String, password: String) {
        auth = Firebase.auth
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser: FirebaseUser = task.result!!.user!!
                    storeDataToRTDB(firebaseUser.uid, email, password, username)

                    pbSignup.visibility = View.INVISIBLE
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                    val intent = Intent(this, VerifyEmail::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                }
                else {
                    pbSignup.visibility = View.INVISIBLE
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                    Log.e("createUser", task.exception!!.message.toString())
                    tvSignupErrorAuth.text = task.exception!!.message
                }
            }
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
                pbSignup.visibility = View.VISIBLE
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                userExists(account)

                pbSignup.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                val intent = Intent(this, Homescreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                intent.putExtra("loginMethod", 2)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
            }
        }
        catch (e: ApiException) {
            Log.e("signup", e.statusCode.toString())
        }
    }

    // Check if signed in user has data in database
    private fun userExists(account: GoogleSignInAccount) {
        auth = Firebase.auth
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(account.email.toString())
            .addOnCompleteListener { task ->
                if (task.result.signInMethods?.isEmpty()!!) {
                    storeDataToRTDB(account.id.toString(), account.email.toString(), null, account.displayName.toString())
                }
            }
    }

    private fun storeDataToRTDB(uid: String, email: String, password: String?, username: String) {
        crypto = Crypto()
        database = Firebase.database
        databaseReference = database.getReference("users")

        val user = User(uid, email, crypto.encrypt(password!!), username)
        databaseReference.child(uid).setValue(user)
    }
}