package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.ducatus.databinding.ActivityUpdatePasswordBinding
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
    private lateinit var alertDialog: AlertDialog
    private lateinit var binding: ActivityUpdatePasswordBinding
    private lateinit var builder: AlertDialog.Builder
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
            if (text?.length == 0) currentPassword.error = getString(R.string.current_password_empty)
            else  currentPassword.error = null
        }
        newPassword.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) newPassword.error = getString(R.string.new_password_empty)
            else if (text?.length!! < 8) newPassword.error = getString(R.string.password_complexity)
            else  newPassword.error = null
        }
        confirmPassword.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) confirmPassword.error = getString(R.string.confirm_password_empty)
            else if (text.toString() != newPassword.editText?.text.toString()) confirmPassword.error = getString(R.string.password_match_error)
            else  confirmPassword.error = null
        }
    }

    private fun validatePassword() {
        binding.tvUpdatePasswordErrorAuth.text = ""
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
        builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.update_password)
        builder.setMessage(R.string.update_password_confirm)
        builder.setIcon(R.drawable.lock)
        builder.setPositiveButton("Update") { _, _ -> reauthenticateUser(newPassword, currentPassword) }
        builder.setNegativeButton("No") { _, _ -> }

        alertDialog = builder.create()
        alertDialog.show()
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
                        .make(binding.clUpdatePassword, "Failed to reauthenticate user", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry") { reauthenticateUser(newPassword, currentPassword) }
                        .show()
                    binding.tvUpdatePasswordErrorAuth.text = it.message
                }
        }
        else {
            hideProgressDialog()
            Snackbar
                .make(binding.clUpdatePassword, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
                .show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    private fun updatePassword(firebaseUser: FirebaseUser, newPassword: String) {
        firebaseUser.updatePassword(newPassword).addOnCompleteListener { updateTask ->
            if (updateTask.isSuccessful) {
                crypto = Crypto()
                database = Firebase.database
                databaseReference = database.getReference("users/" + firebaseUser.uid + "/password")
                databaseReference.setValue(crypto.encrypt(newPassword).toString())

                hideProgressDialog()
                Toast.makeText(this, "Successfully changed password", Toast.LENGTH_SHORT).show()
            }
            else {
                hideProgressDialog()
                Log.d("updatePassword", updateTask.exception!!.message.toString())
                binding.tvUpdatePasswordErrorAuth.text = updateTask.exception!!.message
            }
        }
    }

    private fun showProgressDialog() {
        binding.btnUpdatePassword.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.light_gray_text)
        binding.pbUpdatePassword.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.btnUpdatePassword.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        binding.pbUpdatePassword.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}