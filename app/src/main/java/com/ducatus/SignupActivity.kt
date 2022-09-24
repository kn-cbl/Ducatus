package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {
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
        binding = ActivitySignupBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        inputObserver()

        binding.tvLoginLink.setOnClickListener {
            clearErrors()
            startActivity(Intent(this, LoginActivity::class.java))
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
            showProgressDialog()
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
                    hideProgressDialog()
                    binding.tfSignupUsername.error = getString(R.string.username_exists)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                hideProgressDialog()
                binding.tvSignupErrorAuth.text = error.message
            }
        })
    }

    private fun createUser(username: String, email: String, password: String) {
        auth = Firebase.auth
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val firebaseUser: FirebaseUser? = it.user
                if (firebaseUser != null) {
                    firebaseUser.updateProfile(UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .setPhotoUri(null)
                        .build())

                    storeData(firebaseUser, password, username)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvSignupErrorAuth.text = it.localizedMessage
//                    try {
//                        throw task.exception!!
//                    }
//                    catch(e: FirebaseAuthInvalidCredentialsException) {
//                        binding.tvSignupErrorAuth.setText(R.string.email_invalid)
//                    }
            }
    }

    private fun storeData(firebaseUser: FirebaseUser, password: String?, username: String) {
        crypto = Crypto()
        database = Firebase.database
        databaseReference = database.getReference("users")
        var user: User? = null

        user = if (password != null) User(firebaseUser.uid, firebaseUser.email, crypto.encrypt(password), username)
        else User(firebaseUser.uid, firebaseUser.email, null, username)

        databaseReference.child(firebaseUser.uid).setValue(user)
            .addOnSuccessListener {
                verifyEmail(firebaseUser.isEmailVerified)
            }
            .addOnFailureListener {
                Snackbar
                    .make(findViewById(R.id.clSignup), "Failed to store user data", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") { storeData(firebaseUser, password, username) }
                    .show()
            }
    }

    // Google Sign In
    private fun signInWithGoogle() {
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        gsc = GoogleSignIn.getClient(this, gso)

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

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val googleSignInAccount: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            if (googleSignInAccount != null) {
                showProgressDialog()
                firebaseAuthWithGoogle(googleSignInAccount)
            }
        }
        catch (e: ApiException) {
            if (e.statusCode == 12500) {
                binding.tvSignupErrorAuth.text = getString(R.string.google_sign_in_failed)
            }
        }
    }

    private fun firebaseAuthWithGoogle(googleSignInAccount: GoogleSignInAccount) {
        val firebaseCredential = GoogleAuthProvider.getCredential(googleSignInAccount.idToken, null)
        auth = Firebase.auth
        auth.signInWithCredential(firebaseCredential)
            .addOnSuccessListener {
                val firebaseUser: FirebaseUser? = it.user
                if (firebaseUser != null) {
                    storeData(firebaseUser, null, googleSignInAccount.displayName.toString())
                }
            }
            .addOnFailureListener {
                gsc.signOut()
                binding.tvSignupErrorAuth.text = getString(R.string.google_sign_in_failed)
            }
    }

    private fun verifyEmail(isEmailVerified: Boolean) {
        if (isEmailVerified) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
        else {
            val intent = Intent(this, VerifyEmailActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    private fun showProgressDialog() {
        binding.btnSignup.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.light_gray_text)
        binding.pbSignup.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.btnSignup.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        binding.pbSignup.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun clearErrors() {
        binding.tvSignupErrorAuth.text = ""
        binding.tfSignupUsername.error = null
        binding.tfSignupEmail.error = null
        binding.tfSignupPassword.error = null
    }
}