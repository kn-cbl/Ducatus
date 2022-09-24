package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.ducatus.databinding.ActivityLoginBinding
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

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var gso: GoogleSignInOptions
    private lateinit var gsc: GoogleSignInClient
    private var emailRegex = "^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*(\\.\\w{2,3})+\$"

    override fun onCreate(savedInstanceState: Bundle?) {
        isUserLoggedIn()

        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        inputObserver()

        binding.tvForgotPassword.setOnClickListener {
            clearErrors()
            startActivity(Intent(this, ResetPasswordEmailActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.tvSignupLink.setOnClickListener {
            clearErrors()
            startActivity(Intent(this, SignupActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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

    private fun inputObserver() {
        binding.tfLoginEmailUsername.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfLoginEmailUsername.error = getString(R.string.email_username_empty)
            else binding.tfLoginEmailUsername.error = null
        }
        binding.tfLoginPassword.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfLoginPassword.error = getString(R.string.password_empty)
            else binding.tfLoginPassword.error = null
        }
    }

    private fun isUserLoggedIn() {
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            if (firebaseUser.isEmailVerified) {
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            else {
                startActivity(Intent(this, VerifyEmailActivity::class.java))
            }
        }
    }

    private fun validateCredentials() {
        clearErrors()
        val emailUsername = binding.tfLoginEmailUsername.editText?.text.toString().trim {it <= ' '}
        val password = binding.tfLoginPassword.editText?.text.toString().trim {it <= ' '}

        if (TextUtils.isEmpty(emailUsername) || TextUtils.isEmpty(password)) {
            if (TextUtils.isEmpty(emailUsername)) binding.tfLoginEmailUsername.error = getString(R.string.email_username_empty)
            if (TextUtils.isEmpty(password)) binding.tfLoginPassword.error = getString(R.string.password_empty)
        }
        else {
            showProgressDialog()
            // check if input is an email or username
            if (!emailUsername.contains("@")) {
                usernameExists(emailUsername, password)
            }
            else {
                if (emailRegex.toRegex().matches(emailUsername)) {
                    login(emailUsername, password)
                }
                else {
                    hideProgressDialog()
                    binding.tfLoginEmailUsername.error = getString(R.string.email_invalid)
                }
            }
        }
    }

    private fun usernameExists(username: String, password: String) {
        database = Firebase.database
        databaseReference = database.getReference("users")
        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var email: String? = null

                for (child in snapshot.children) {
                    if(username == child.child("username").value.toString()) {
                        email = child.child("email").value.toString()
                        break
                    }
                }

                if (email != null) {
                    login(email, password)
                }
                else {
                    hideProgressDialog()
                    binding.tvLoginErrorAuth.text = getString(R.string.user_does_not_exist)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                hideProgressDialog()
                binding.tvLoginErrorAuth.text = error.message
            }
        })
    }

    private fun login(email: String, password: String) {
        auth = Firebase.auth
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val firebaseUser: FirebaseUser? = it.user
                if (firebaseUser != null) {
                    isEmailVerified(firebaseUser)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvLoginErrorAuth.text = it.localizedMessage
            }
    }

    private fun storeData(firebaseUser: FirebaseUser, googleSignInAccount: GoogleSignInAccount) {
        database = Firebase.database
        databaseReference = database.getReference("users")
        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.child(firebaseUser.uid).exists()) {
                    val user = User(firebaseUser.uid, firebaseUser.email, null, firebaseUser.displayName)
                    databaseReference.child(firebaseUser.uid).setValue(user)
                        .addOnSuccessListener {
                            isEmailVerified(firebaseUser)
                        }
                        .addOnFailureListener {
                            Snackbar
                                .make(binding.clLogin, "Failed to store user data", Snackbar.LENGTH_INDEFINITE)
                                .setAction("Retry") { storeData(firebaseUser, googleSignInAccount) }
                                .show()
                        }
                }
                else {
                    val username = snapshot.child(firebaseUser.uid).child("username").value.toString()
                    if (username != firebaseUser.displayName) {
                        updateProfile(firebaseUser, googleSignInAccount, username)
                    }
                    else {
                        isEmailVerified(firebaseUser)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                hideProgressDialog()
                binding.tvLoginErrorAuth.text = error.message
            }
        })
    }

    private fun updateProfile(firebaseUser: FirebaseUser, googleSignInAccount: GoogleSignInAccount, username: String) {
        val updates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .setPhotoUri(googleSignInAccount.photoUrl)
            .build()

        firebaseUser.updateProfile(updates)
            .addOnSuccessListener {
                isEmailVerified(firebaseUser)
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clLogin, "Failed to store user data", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") { updateProfile(firebaseUser, googleSignInAccount, username) }
                    .show()
            }
    }

    private fun isEmailVerified(firebaseUser: FirebaseUser) {
        showProgressDialog()
        if (firebaseUser.isEmailVerified) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
        else {
            startActivity(Intent(this, VerifyEmailActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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
                binding.tvLoginErrorAuth.text = getString(R.string.google_sign_in_failed)
            }
        }
    }

    private fun firebaseAuthWithGoogle(googleSignInAccount: GoogleSignInAccount) {
        val firebaseCredential = GoogleAuthProvider.getCredential(googleSignInAccount.idToken, null)
        auth.signInWithCredential(firebaseCredential)
            .addOnSuccessListener {
                val firebaseUser: FirebaseUser? = it.user
                if (firebaseUser != null) {
                    storeData(firebaseUser, googleSignInAccount)
                }
            }
            .addOnFailureListener {
                gsc.signOut()
                binding.tvLoginErrorAuth.text = getString(R.string.google_sign_in_failed)
            }
    }

    private fun showProgressDialog() {
        binding.btnLogin.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.light_gray_text)
        binding.pbLogin.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.btnLogin.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        binding.pbLogin.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun clearErrors() {
        binding.tvLoginErrorAuth.text = ""
        binding.tfLoginEmailUsername.error = null
        binding.tfLoginPassword.error = null
    }
}