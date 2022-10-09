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
                    verifyEmail(firebaseUser.isEmailVerified)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvLoginErrorAuth.text = it.localizedMessage
            }
    }

    private fun storeData(firebaseUser: FirebaseUser, googleSignInAccount: GoogleSignInAccount) {
        showProgressDialog()
        database = Firebase.database
        databaseReference = database.getReference("users")
        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.child(firebaseUser.uid).exists()) {
                    val user = User(firebaseUser.email, null, firebaseUser.displayName, null)
                    databaseReference.child(firebaseUser.uid).setValue(user)
                        .addOnSuccessListener {
                            createDefaultAccount(firebaseUser, firebaseUser.displayName.toString())
                        }
                        .addOnFailureListener {
                            hideProgressDialog()
                            Snackbar
                                .make(binding.llLogin, "Failed to store user data, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                                .setAction(getString(R.string.retry)) { storeData(firebaseUser, googleSignInAccount) }
                                .show()
                        }
                }
                else {
                    val username = snapshot.child(firebaseUser.uid).child("username").value.toString()
                    if (username != firebaseUser.displayName) {
                        updateProfile(firebaseUser, googleSignInAccount, username)
                    }
                    else {
                        verifyEmail(firebaseUser.isEmailVerified)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                hideProgressDialog()
                binding.tvLoginErrorAuth.text = error.message
            }
        })
    }

    private fun createDefaultAccount(firebaseUser: FirebaseUser, username: String) {
        showProgressDialog()
        databaseReference = database.getReference("accounts").child(firebaseUser.uid)
        databaseReference.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) { // check if path exists
                    val randomColor = generateRandomColor()
                    val account = Account(0, username, resources.getResourceEntryName(randomColor), 0.0, 0.0)
                    databaseReference.child("0").setValue(account)
                        .addOnSuccessListener {
                            val sharedPreferences = SharedPreferences(applicationContext)
                            sharedPreferences.accountName = account.account_name
                            sharedPreferences.accountColor = account.account_color

                            createDefaultCategories(firebaseUser)
                        }
                        .addOnFailureListener {
                            hideProgressDialog()
                            Snackbar
                                .make(findViewById(R.id.llLogin), "Failed to store user data, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                                .setAction(getString(R.string.retry)) { createDefaultAccount(firebaseUser, username) }
                                .show()
                        }
                }
                else {
                    verifyEmail(firebaseUser.isEmailVerified)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                hideProgressDialog()
                Snackbar
                    .make(findViewById(R.id.llLogin), "Failed to store user data, ${error.message}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { createDefaultAccount(firebaseUser, username) }
                    .show()
            }
        })
    }

    private fun createDefaultCategories(firebaseUser: FirebaseUser) {
        showProgressDialog()
        databaseReference = database.getReference("categories").child(firebaseUser.uid).child("0")
        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) { // check if path exists
                    val categoryNames = listOf(
                        "Electronics", "Financial Expenses", "Food and Drinks",
                        "Housing", "Investments", "Life and Entertainment",
                        "Shopping", "Transportation", "Vehicle",
                        "Others"
                    )

                    val categoryNatures = listOf(
                        1, 0, 0,
                        0, 0, 1,
                        1, 0, 1,
                        1
                    )

                    val categoryColors= listOf(
                        "dark_blue", "dark_yellow", "bright_red",
                        "dark_green", "orange", "cyan",
                        "light_pink", "dark_brown", "purple",
                        "light_gray"
                    )

                    val categoryIcons = listOf(
                        "ic_baseline_devices_24", "ic_baseline_wallet_24", "ic_baseline_fastfood_24",
                        "ic_baseline_home_24", "investment", "ic_baseline_videogame_asset_24",
                        "ic_outline_shopping_bag_24", "ic_baseline_directions_bus_24", "ic_baseline_directions_car_24",
                        "ic_baseline_more_horiz_24"
                    )

                    // loop through items and store values based on index
                    for (i in categoryNames.indices) {
                        val category = Category(i, categoryNames[i], categoryNatures[i], categoryColors[i], categoryIcons[i])
                        databaseReference.child(i.toString()).setValue(category)
                            .addOnSuccessListener {
                                verifyEmail(firebaseUser.isEmailVerified)
                            }
                            .addOnFailureListener {
                                hideProgressDialog()
                                Snackbar
                                    .make(findViewById(R.id.llSignup), "Failed to store user data, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                                    .setAction(getString(R.string.retry)) { createDefaultCategories(firebaseUser) }
                                    .show()
                            }
                    }
                }
                else {
                    verifyEmail(firebaseUser.isEmailVerified)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                hideProgressDialog()
                Snackbar
                    .make(findViewById(R.id.llSignup), "Failed to store user data, ${error.message}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { createDefaultCategories(firebaseUser) }
                    .show()
            }
        })
    }

    private fun updateProfile(firebaseUser: FirebaseUser, googleSignInAccount: GoogleSignInAccount, username: String) {
        showProgressDialog()
        val updates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .setPhotoUri(googleSignInAccount.photoUrl)
            .build()

        firebaseUser.updateProfile(updates)
            .addOnSuccessListener {
                verifyEmail(firebaseUser.isEmailVerified)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.llLogin, "Failed to store user data, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { updateProfile(firebaseUser, googleSignInAccount, username) }
                    .show()
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
                    storeData(firebaseUser, googleSignInAccount)
                }
            }
            .addOnFailureListener {
                gsc.signOut()
                binding.tvLoginErrorAuth.text = getString(R.string.google_sign_in_failed)
            }
    }

    private fun generateRandomColor(): Int {
        // select random color from list
        val colorList = listOf(
            "color_one", "color_two", "color_three", "color_four", "color_five",
            "color_six", "color_seven", "color_eight", "color_nine", "color_ten",
            "color_eleven", "color_twelve", "color_thirteen", "color_fourteen", "color_fifteen",
            "color_sixteen", "color_seventeen", "color_eighteen", "color_nineteen", "color_twenty",
            "color_twenty_one", "color_twenty_two", "color_twenty_three", "color_twenty_four", "color_twenty_five",
        )

        val randomIndex = floor(Math.random() * colorList.size).toInt()
        return resources.getIdentifier(colorList[randomIndex], "color", packageName)
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