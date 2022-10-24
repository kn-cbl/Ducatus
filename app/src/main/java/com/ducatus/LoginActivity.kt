package com.ducatus

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.ducatus.data.Account
import com.ducatus.data.User
import com.ducatus.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.math.floor

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
        database = Firebase.database
    }

    private fun inputObserver() {
        binding.tfLoginEmailUsername.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfLoginEmailUsername.error = getString(R.string.email_username_empty)
            else binding.tfLoginEmailUsername.error = null
        }
        binding.tfLoginPassword.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfLoginPassword.error = getString(R.string.password_empty)
            else binding.tfLoginPassword.error = null
        }
    }

    private fun validateCredentials() {
        clearErrors()
        try {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

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
        databaseReference = database.getReference("users")
        databaseReference.get()
            .addOnSuccessListener {
                var email: String? = null

                for (child in it.children) {
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
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvLoginErrorAuth.text = it.localizedMessage
            }
    }

    private fun login(email: String, password: String) {
        auth = Firebase.auth
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser: FirebaseUser? = task.result.user
                    if (firebaseUser != null) {
                        checkSelectedAccount(firebaseUser)
                    }
                }
                else {
                    hideProgressDialog()
                    val exception = task.exception as FirebaseAuthException
                    when (exception.errorCode) {
                        "ERROR_WRONG_PASSWORD" -> binding.tvLoginErrorAuth.text = getString(R.string.password_invalid)
                        "ERROR_USER_NOT_FOUND" -> binding.tvLoginErrorAuth.text = getString(R.string.user_does_not_exist)
                        else -> binding.tvLoginErrorAuth.text = exception.localizedMessage
                    }
                }
            }
    }

    private fun checkSelectedAccount(firebaseUser: FirebaseUser) {
        databaseReference = database.getReference("accounts").child(firebaseUser.uid)
        databaseReference.get()
            .addOnSuccessListener {
                for(child in it.children) {
                    if (child.child("selected").value.toString() == "true") {
                        val sharedPreferences = SharedPreferences(applicationContext)
                        sharedPreferences.accountId = child.child("account_id").value.toString()
                        sharedPreferences.accountName = child.child("account_name").value.toString()
                        sharedPreferences.accountColor = child.child("account_color").value.toString()
                        break
                    }
                }

                verifyEmail(firebaseUser.isEmailVerified)
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvLoginErrorAuth.text = it.localizedMessage
            }
    }

    private fun storeData(firebaseUser: FirebaseUser) {
        showProgressDialog()
        database = Firebase.database
        databaseReference = database.getReference("users").child(firebaseUser.uid)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    val user = User(firebaseUser.email, null, null, null)
                    databaseReference.setValue(user)
                        .addOnSuccessListener {
                            createDefaultAccount(firebaseUser, firebaseUser.displayName.toString())
                        }
                        .addOnFailureListener {
                            hideProgressDialog()
                            binding.tvLoginErrorAuth.text = it.localizedMessage
                        }
                }
                else {
                    createDefaultAccount(firebaseUser, firebaseUser.displayName.toString())
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvLoginErrorAuth.text = it.localizedMessage
            }
    }

    private fun createDefaultAccount(firebaseUser: FirebaseUser, username: String) {
        showProgressDialog()
        databaseReference = database.getReference("accounts").child(firebaseUser.uid)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val key = databaseReference.push().key

                if (!snapshot.exists()) {
                    val randomColor = generateRandomColor()

                    val account = Account(
                        key,
                        username,
                        resources.getResourceEntryName(randomColor),
                        0.0,
                        0.0,
                        true
                    )

                    databaseReference.child(key!!).setValue(account)
                        .addOnSuccessListener {
                            val sharedPreferences = SharedPreferences(applicationContext)
                            sharedPreferences.accountName = account.account_name
                            sharedPreferences.accountColor = account.account_color

                            createDefaultCategories(firebaseUser, key)
                        }
                        .addOnFailureListener {
                            hideProgressDialog()
                            binding.tvLoginErrorAuth.text = it.localizedMessage
                        }
                }
                else {
                    createDefaultCategories(firebaseUser, key!!)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvLoginErrorAuth.text = it.localizedMessage
            }
    }

    private fun createDefaultCategories(firebaseUser: FirebaseUser, accountId: String) {
        showProgressDialog()
        databaseReference = database.getReference("categories").child(firebaseUser.uid)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    val keys = mutableListOf<String>()

                    val size = AppResources().getCategoryItemCount()
                    for (i in 0 until size) {
                        val key = databaseReference.push().key
                        keys.add(key!!)
                    }

                    val categories = AppResources().getCategories(keys)
                    databaseReference.child(accountId).setValue(categories)
                        .addOnSuccessListener {
                            checkSelectedAccount(firebaseUser)
                        }
                        .addOnFailureListener {
                            hideProgressDialog()
                            binding.tvLoginErrorAuth.text = it.localizedMessage
                        }
                }
                else {
                    checkSelectedAccount(firebaseUser)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvLoginErrorAuth.text = it.localizedMessage
            }
    }

    private fun verifyEmail(verified: Boolean) {
        if (verified) {
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
                    storeData(firebaseUser)
                }
            }
            .addOnFailureListener {
                gsc.signOut()
                binding.tvLoginErrorAuth.text = getString(R.string.google_sign_in_failed)
            }
    }

    private fun generateRandomColor(): Int {
        // select random color from list
        val colors = AppResources().getColors()
        val randomIndex = floor(Math.random() * colors.size).toInt()
        return resources.getIdentifier(colors[randomIndex], "color", packageName)
    }

    private fun showProgressDialog() {
        clearErrors()
        binding.pbLogin.visibility = View.VISIBLE
        binding.btnLogin.text = null
        binding.btnLogin.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.gray)
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbLogin.visibility = View.INVISIBLE
        binding.btnLogin.text = getString(R.string.log_in)
        binding.btnLogin.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun clearErrors() {
        binding.tvLoginErrorAuth.text = ""
        binding.tfLoginEmailUsername.error = null
        binding.tfLoginPassword.error = null
    }
}