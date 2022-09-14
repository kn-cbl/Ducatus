package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.ducatus.databinding.ActivitySignupBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class Signup : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySignupBinding
    private lateinit var crypto: Crypto
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var gso: GoogleSignInOptions
    private lateinit var gsc: GoogleSignInClient
    private var emailRegex = "^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*(\\.\\w{2,3})+\$"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        gsc = GoogleSignIn.getClient(this, gso)

        binding = ActivitySignupBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        inputObserver()

        binding.tvLoginLink.setOnClickListener {
            clearErrors()
            startActivity(Intent(this, Login::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.btnSignup.setOnClickListener {
            // validate credentials -> check if user data exists -> create user
            validateCredentials()
        }

        binding.flSignupGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun inputObserver() {
        binding.tfSignupEmail.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfSignupEmail.error = getString(R.string.email_empty)
            else if (!emailRegex.toRegex().matches(text!!)) binding.tfSignupEmail.error = getString(R.string.email_invalid)
            else binding.tfSignupEmail.error = null
        }
        binding.tfSignupUsername.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfSignupUsername.error = getString(R.string.username_empty)
            else binding.tfSignupUsername.error = null
        }
        binding.tfSignupPassword.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfSignupPassword.error = getString(R.string.password_empty)
            else if (text?.length!! < 8)  binding.tfSignupPassword.error = getString(R.string.password_complexity)
            else binding.tfSignupPassword.error = null
        }
    }

    // User Manual Sign In
    private fun validateCredentials() {
        clearErrors()

        val username = binding.tfSignupUsername.editText?.text.toString().trim {it <= ' '}
        val email = binding.tfSignupEmail.editText?.text.toString().trim {it <= ' '}
        val password = binding.tfSignupPassword.editText?.text.toString().trim {it <= ' '}

        if (emailRegex.toRegex().matches(email) && password.length >= 8) {
            disableWindow()
            usernameExists(username, email, password)
        }
        else {
            if (!emailRegex.toRegex().matches(email)) binding.tfSignupEmail.error = getString(R.string.email_invalid)
            if (password.length < 8) binding.tfSignupPassword.error = getString(R.string.password_complexity)
            if (TextUtils.isEmpty(username)) binding.tfSignupUsername.error = getString(R.string.username_empty)
            if (TextUtils.isEmpty(email)) binding.tfSignupEmail.error = getString(R.string.email_empty)
            if (TextUtils.isEmpty(password)) binding.tfSignupPassword.error = getString(R.string.password_empty)
        }
    }

    private fun usernameExists(username: String, email: String, password: String) {
        database = Firebase.database
        databaseReference = database.getReference("users")
        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var usernameKey = false

                for (child in snapshot.children) {
                    if(username == child.child("username").value.toString()) {
                        usernameKey = true
                        break
                    }
                }

                if (!usernameKey) {
                    createUser(username, email, password)
                }
                else {
                    enableWindow()
                    binding.tfSignupUsername.error = getString(R.string.username_exists)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                enableWindow()
                Log.e("databaseReference", error.toString())
                binding.tvSignupErrorAuth.text = error.message
            }
        })
    }

    private fun createUser(username: String, email: String, password: String) {
        auth = Firebase.auth
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser: FirebaseUser = task.result!!.user!!
                    storeData(firebaseUser.uid, email, password, username)

                    val intent = Intent(this, VerifyEmail::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                }
                else {
                    enableWindow()
                    Log.e("createUser", task.exception?.localizedMessage.toString())
                    binding.tvSignupErrorAuth.text = task.exception?.localizedMessage
//                    try {
//                        throw task.exception!!
//                    }
//                    catch(e: FirebaseAuthInvalidCredentialsException) {
//                        binding.tvSignupErrorAuth.setText(R.string.email_invalid)
//                    }
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
            binding.tvSignupErrorAuth.text = e.localizedMessage
        }
    }

    // check if signed in user has existing data in database
    private fun userExists(account: GoogleSignInAccount) {
        auth = Firebase.auth
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(account.email.toString())
            .addOnCompleteListener { task ->
                if (task.result.signInMethods?.isEmpty()!!) {
                    storeData(account.id.toString(), account.email.toString(), null, account.displayName.toString())
                }
            }
    }

    private fun storeData(uid: String, email: String, password: String?, username: String) {
        crypto = Crypto()
        database = Firebase.database
        databaseReference = database.getReference("users")

        val user = User(uid, email, crypto.encrypt(password!!), username)
        databaseReference.child(uid).setValue(user)
    }

    private fun enableWindow() {
        binding.btnSignup.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        binding.pbSignup.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun disableWindow() {
        binding.btnSignup.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.light_gray_text)
        binding.pbSignup.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun clearErrors() {
        binding.tvSignupErrorAuth.text = ""
        binding.tfSignupUsername.error = null
        binding.tfSignupEmail.error = null
        binding.tfSignupPassword.error = null
    }
}