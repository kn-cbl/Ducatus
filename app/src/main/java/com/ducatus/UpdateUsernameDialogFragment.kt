package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentUpdateUsernameDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class UpdateUsernameDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentUpdateUsernameDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private val args: UpdateUsernameDialogFragmentArgs by navArgs()
    private var updated: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llUserProfile)

        binding = FragmentUpdateUsernameDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inputObserver()

        binding.tfUpdateUsername.editText?.setText(args.username)

        binding.btnUpdateUsernameCancel.setOnClickListener {
            dismiss()
        }

        binding.btnUpdateUsernameConfirm.setOnClickListener {
            validateData()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (updated) {
            val fragment = parentFragmentManager.findFragmentById(R.id.fcUserProfile)
            if (fragment is DialogInterface.OnDismissListener) {
                (fragment as DialogInterface.OnDismissListener?)?.onDismiss(dialog)
            }
        }
    }

    private fun inputObserver() {
        binding.tfUpdateUsername.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfUpdateUsername.error = getString(R.string.username_empty)
            else binding.tfUpdateUsername.error = null
        }
    }

    private fun validateData() {
        // hide keyboard
        try {
            val windowToken: View = dialog!!.window!!.decorView.rootView
            val imm: InputMethodManager = dialog!!.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken.windowToken, 0)
        }
        catch (e: Exception){}

        val username = binding.tfUpdateUsername.editText?.text.toString().trim {it <= ' '}
        if (username == args.username) {
            // no changes were made
            dismiss()
        }
        else if (TextUtils.isEmpty(username)) {
            binding.tfUpdateUsername.error = getString(R.string.username_empty)
        }
        else {
            auth = Firebase.auth
            val firebaseUser: FirebaseUser? = auth.currentUser
            if (firebaseUser != null) {
                usernameExists(firebaseUser, username)
            }
            else {
                sessionExpired()
            }
        }
    }

    private fun usernameExists(firebaseUser: FirebaseUser, username: String) {
        showProgressDialog()
        database = Firebase.database
        databaseReference = database.getReference("users")

        val query = databaseReference.orderByChild("username").equalTo(username)
        query.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    updateDB(firebaseUser, username)
                }
                else {
                    hideProgressDialog()
                    binding.tfUpdateUsername.error = getString(R.string.username_exists)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun updateDB(firebaseUser: FirebaseUser, username: String) {
        databaseReference = database.getReference("users").child(firebaseUser.uid).child("username")
        databaseReference.setValue(username)
            .addOnSuccessListener {
                updateProfile(firebaseUser, username)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun updateProfile(firebaseUser: FirebaseUser, username: String) {
        val updates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .setPhotoUri(firebaseUser.photoUrl)
            .build()

        firebaseUser.updateProfile(updates)
            .addOnSuccessListener {
                hideProgressDialog()
                updated = true
                dismiss()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun sessionExpired() {
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.session_expired))
            .setPositiveButton(resources.getString(R.string.log_in)) { _, _ -> }

        dialog.setOnDismissListener {
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity.finish()
        }

        dialog.show()
    }

    private fun showProgressDialog() {
        binding.pbUpdateUsername.visibility = View.VISIBLE
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialog() {
        binding.pbUpdateUsername.visibility = View.INVISIBLE
        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

}