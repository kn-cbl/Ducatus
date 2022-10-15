package com.ducatus

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.ducatus.databinding.ActivityUpdatePasswordBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class UpdatePasswordActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityUpdatePasswordBinding
    private lateinit var crypto: Crypto
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_password)

        binding = ActivityUpdatePasswordBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        inputObserver()

        binding.tbUpdatePassword.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.btnUpdatePassword.setOnClickListener {
            // validate password -> reauthenticate user -> update password
            validatePassword()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun inputObserver() {
        val currentPassword = binding.tfUpdatePasswordCurrent
        val newPassword = binding.tfUpdatePasswordNew
        val confirmPassword = binding.tfUpdatePasswordConfirm

        currentPassword.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) currentPassword.error = getString(R.string.current_password_empty)
            else  currentPassword.error = null
        }
        newPassword.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) newPassword.error = getString(R.string.new_password_empty)
            else if (text.length < 8) newPassword.error = getString(R.string.password_complexity)
            else  newPassword.error = null
        }
        confirmPassword.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) confirmPassword.error = getString(R.string.confirm_password_empty)
            else if (text.toString() != newPassword.editText?.text.toString()) confirmPassword.error = getString(R.string.password_match_error)
            else  confirmPassword.error = null
        }
    }

    private fun validatePassword() {
        // hide keyboard
        try {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        binding.tfUpdatePasswordCurrent.error = null
        binding.tfUpdatePasswordNew.error = null
        binding.tfUpdatePasswordConfirm.error = null

        val currentPassword = binding.tfUpdatePasswordCurrent.editText?.text.toString().trim {it <= ' '}
        val newPassword = binding.tfUpdatePasswordNew.editText?.text.toString().trim {it <= ' '}
        val confirmPassword = binding.tfUpdatePasswordConfirm.editText?.text.toString().trim {it <= ' '}

        if (!TextUtils.isEmpty(currentPassword) && newPassword.length >= 8 && newPassword == confirmPassword) {
            confirmUpdate(newPassword, currentPassword)
        }
        else {
            if (newPassword.length < 8) binding.tfUpdatePasswordNew.error = getString(R.string.password_complexity)
            if (newPassword != confirmPassword) binding.tfUpdatePasswordConfirm.error = getString(R.string.password_match_error)
            if (TextUtils.isEmpty(currentPassword)) binding.tfUpdatePasswordCurrent.error = getString(R.string.current_password_empty)
            if (TextUtils.isEmpty(newPassword)) binding.tfUpdatePasswordNew.error = getString(R.string.new_password_empty)
            if (TextUtils.isEmpty(confirmPassword)) binding.tfUpdatePasswordConfirm.error = getString(R.string.confirm_password_empty)
        }
    }

    private fun confirmUpdate(newPassword: String, currentPassword: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.update_password_mark))
            .setPositiveButton(resources.getString(R.string.change)) { _, _ -> reauthenticateUser(newPassword, currentPassword) }
            .setNegativeButton(resources.getString(R.string.no)) { _, _ -> } // do nothing
            .show()
    }

    private fun reauthenticateUser(newPassword: String, currentPassword: String) {
        showProgressDialog()
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val credential = EmailAuthProvider.getCredential(firebaseUser.email.toString(), currentPassword)
            firebaseUser.reauthenticate(credential)
                .addOnSuccessListener {
                    updatePassword(firebaseUser, newPassword)
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    Snackbar
                        .make(binding.llUpdatePassword, it.localizedMessage!!, Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry)) { reauthenticateUser(newPassword, currentPassword) }
                        .show()
                }
        }
        else {
            hideProgressDialog()
            Snackbar
                .make(binding.llUpdatePassword, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
                .show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    private fun updatePassword(firebaseUser: FirebaseUser, newPassword: String) {
        showProgressDialog()
        firebaseUser.updatePassword(newPassword)
            .addOnSuccessListener {
                crypto = Crypto()
                database = Firebase.database
                databaseReference = database.getReference("users").child(firebaseUser.uid).child("password")
                databaseReference.setValue(crypto.encrypt(newPassword).toString())

                hideProgressDialog()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.llUpdatePassword, "Unable to update password", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { updatePassword(firebaseUser, newPassword) }
                    .show()
            }
    }

    private fun showProgressDialog() {
        binding.pbUpdatePassword.visibility = View.VISIBLE
        binding.btnUpdatePassword.text = null
        binding.btnUpdatePassword.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.gray)
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbUpdatePassword.visibility = View.INVISIBLE
        binding.btnUpdatePassword.text = getString(R.string.update_password_small)
        binding.btnUpdatePassword.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}