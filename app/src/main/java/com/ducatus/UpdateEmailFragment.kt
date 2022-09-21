package com.ducatus

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.ducatus.databinding.FragmentUpdateEmailBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class UpdateEmailFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var activity: Activity
    private lateinit var alertDialog: AlertDialog
    private lateinit var binding: FragmentUpdateEmailBinding
    private lateinit var builder: AlertDialog.Builder
    private var emailRegex = "^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*(\\.\\w{2,3})+\$"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentUpdateEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inputObserver()

        binding.btnUpdateEmail.setOnClickListener {
            validateCredentials()
        }
    }

    private fun inputObserver() {
        binding.tfUpdateEmail.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfUpdateEmail.error = getString(R.string.new_email_empty)
            else if (!emailRegex.toRegex().matches(text!!)) binding.tfUpdateEmail.error = getString(R.string.email_invalid)
            else binding.tfUpdateEmail.error = null
        }

        binding.tfUpdateEmailReauthenticate.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfUpdateEmailReauthenticate.error = getString(R.string.password_empty)
            else binding.tfUpdateEmailReauthenticate.error = null
        }
    }

    private fun validateCredentials() {
        clearErrors()
        val email = binding.tfUpdateEmail.editText?.text.toString().trim {it <= ' '}
        val password = binding.tfUpdateEmailReauthenticate.editText?.text.toString().trim {it <= ' '}

        auth = Firebase.auth
        val authUser = auth.currentUser

        if (authUser != null) {
            if (authUser.email != email && emailRegex.toRegex().matches(email) && !TextUtils.isEmpty(password)) {
                confirmUpdate(email, password)
            }
            else {
                if (!emailRegex.toRegex().matches(email)) binding.tfUpdateEmail.error = getString(R.string.email_invalid)
                if (authUser.email == email) binding.tfUpdateEmail.error = getString(R.string.new_email_same)
                if (TextUtils.isEmpty(email)) binding.tfUpdateEmail.error = getString(R.string.new_email_empty)
                if (TextUtils.isEmpty(password)) binding.tfUpdateEmailReauthenticate.error = getString(R.string.password_empty)
            }
        }
        else {
            Toast.makeText(activity, "null", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmUpdate(email: String, password: String) {
        builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.update_email)
        builder.setMessage(R.string.update_email_confirm)
        builder.setIcon(R.drawable.lock)
        builder.setPositiveButton("Update") { _, _ -> reauthenticateUser(email, password) }
        builder.setNegativeButton("No") { _, _ -> }

        alertDialog = builder.create()
        alertDialog.show()
    }

    private fun reauthenticateUser(email: String, password: String) {
        disableWindow()
        val authUser = auth.currentUser
        val credential = EmailAuthProvider.getCredential(authUser!!.email.toString(), password)
        authUser.reauthenticate(credential).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                updateEmail(authUser, email)
            }
            else {
                enableWindow()
                binding.tfUpdateEmailReauthenticate.error = authTask.exception!!.localizedMessage
            }
        }
    }

    private fun updateEmail(authUser: FirebaseUser, email: String) {
        authUser.updateEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                enableWindow()
                Toast.makeText(activity, "success", Toast.LENGTH_SHORT).show()
            }
            else {
                enableWindow()
                Toast.makeText(activity, task.exception?.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyEmail() {

    }

    private fun enableWindow() {
        binding.btnUpdateEmail.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.green_primary)
        binding.pbUpdateEmail.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun disableWindow() {
        binding.btnUpdateEmail.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.light_gray_text)
        binding.pbUpdateEmail.visibility = View.VISIBLE
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun clearErrors() {
        binding.tfUpdateEmail.error = null
        binding.tfUpdateEmailReauthenticate.error = null
    }
}