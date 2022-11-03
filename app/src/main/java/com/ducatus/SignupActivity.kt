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
import com.ducatus.databinding.ActivitySignupBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.math.floor

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
        auth = Firebase.auth
        database = Firebase.database

        binding.tvLoginLink.setOnClickListener {
            clearErrors()
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.btnSignup.setOnClickListener {
            // validate credentials -> check if user data exists -> create user -> store data -> create default account -> verify email
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
            if (text == null || text.isEmpty()) binding.tfSignupEmail.error = getString(R.string.email_empty)
            else if (!emailRegex.toRegex().matches(text)) binding.tfSignupEmail.error = getString(R.string.email_invalid)
            else binding.tfSignupEmail.error = null
        }
        binding.tfSignupUsername.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfSignupUsername.error = getString(R.string.username_empty)
            else binding.tfSignupUsername.error = null
        }
        binding.tfSignupPassword.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfSignupPassword.error = getString(R.string.password_empty)
            else if (text.length < 8)  binding.tfSignupPassword.error = getString(R.string.password_complexity)
            else binding.tfSignupPassword.error = null
        }
    }

    // User Manual Sign In
    private fun validateCredentials() {
        clearErrors()

        // hide keyboard
        try {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

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
        databaseReference = database.getReference("users")
        val query = databaseReference.orderByChild("username").equalTo(username)
        query.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    createUser(username, email, password)
                }
                else {
                    hideProgressDialog()
                    binding.tfSignupUsername.error = getString(R.string.username_exists)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvSignupErrorAuth.text = it.localizedMessage
            }
    }

    private fun createUser(username: String, email: String, password: String) {
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
            }
    }

    private fun checkSelectedAccount(firebaseUser: FirebaseUser) {
        database = Firebase.database
        databaseReference = database.getReference("accounts").child(firebaseUser.uid)
        databaseReference.get()
            .addOnSuccessListener {
                for(child in it.children) {
                    if (child.child("selected").value.toString() == "true") {
                        val sharedPreferences = SharedPreferences(applicationContext)
                        sharedPreferences.accountId = child.child("id").value.toString()
                        sharedPreferences.accountName = child.child("name").value.toString()
                        sharedPreferences.accountColor = child.child("color").value.toString()
                        break
                    }
                }

                verifyEmail(firebaseUser.isEmailVerified)
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvSignupErrorAuth.text = it.localizedMessage
            }
    }

    private fun storeData(firebaseUser: FirebaseUser, password: String?, username: String) {
        showProgressDialog()
        crypto = Crypto()

        val user = if (password != null) User(firebaseUser.email, crypto.encrypt(password), username, null)
        else User(firebaseUser.email, null, null, null)

        databaseReference = database.getReference("users").child(firebaseUser.uid)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    databaseReference.setValue(user)
                        .addOnSuccessListener {
                            createDefaultAccount(firebaseUser, username)
                        }
                        .addOnFailureListener {
                            hideProgressDialog()
                            binding.tvSignupErrorAuth.text = it.localizedMessage
                        }
                }
                else {
                    createDefaultAccount(firebaseUser, username)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvSignupErrorAuth.text = it.localizedMessage
            }
    }

    private fun createDefaultAccount(firebaseUser: FirebaseUser, username: String) {
        showProgressDialog()
        databaseReference = database.getReference("accounts").child(firebaseUser.uid)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val key = databaseReference.push().key

                if (!snapshot.exists()) { // check if path exists
                    val randomColor = generateRandomColor()

                    val account = Account(
                        key,
                        username,
                        username.lowercase(),
                        resources.getResourceEntryName(randomColor),
                        0.0,
                        0.0,
                        0.0,
                        true
                    )

                    databaseReference.child(key!!).setValue(account)
                        .addOnSuccessListener {
                            val sharedPreferences = SharedPreferences(applicationContext)
                            sharedPreferences.accountName = account.name
                            sharedPreferences.accountColor = account.color

                            createDefaultCategories(firebaseUser, key)
                        }
                        .addOnFailureListener {
                            hideProgressDialog()
                            binding.tvSignupErrorAuth.text = it.localizedMessage
                        }
                }
                else {
                    createDefaultCategories(firebaseUser, key!!)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvSignupErrorAuth.text = it.localizedMessage
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
                            binding.tvSignupErrorAuth.text = it.localizedMessage
                        }
                }
                else {
                    checkSelectedAccount(firebaseUser)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvSignupErrorAuth.text = it.localizedMessage
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
                firebaseAuthWithGoogle(googleSignInAccount)
            }
        }
        catch (e: ApiException) {
            if (e.statusCode == 12500) {
                hideProgressDialog()
                binding.tvSignupErrorAuth.text = getString(R.string.google_sign_in_failed)
            }
        }
    }

    private fun firebaseAuthWithGoogle(googleSignInAccount: GoogleSignInAccount) {
        showProgressDialog()
        val firebaseCredential = GoogleAuthProvider.getCredential(googleSignInAccount.idToken, null)
        auth.signInWithCredential(firebaseCredential)
            .addOnSuccessListener {
                val firebaseUser: FirebaseUser? = it.user
                if (firebaseUser != null) {
                    storeData(firebaseUser, null, googleSignInAccount.displayName.toString())
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
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

    private fun generateRandomColor(): Int {
        // select random color from list
        val colors = AppResources().getColors()
        val randomIndex = floor(Math.random() * colors.size).toInt()
        return resources.getIdentifier(colors[randomIndex], "color", packageName)
    }

    private fun showProgressDialog() {
        clearErrors()
        binding.pbSignup.visibility = View.VISIBLE
        binding.btnSignup.text = null
        binding.btnSignup.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.gray)
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbSignup.visibility = View.INVISIBLE
        binding.btnSignup.text = getString(R.string.create_account)
        binding.btnSignup.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun clearErrors() {
        binding.tvSignupErrorAuth.text = ""
        binding.tfSignupUsername.error = null
        binding.tfSignupEmail.error = null
        binding.tfSignupPassword.error = null
    }
}