package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentUpdateUsernameBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class UpdateUsernameFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentUpdateUsernameBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private val args: UpdateUsernameFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llUserProfile)
        binding = FragmentUpdateUsernameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inputObserver()

        binding.tfUpdateUsername.editText?.setText(args.username)

        binding.btnUpdateUsernameCancel.setOnClickListener {
            dismiss()
        }

        binding.btnUpdateUsernameSave.setOnClickListener {
            validateInput()
        }
    }

    private fun inputObserver() {
        binding.tfUpdateUsername.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfUpdateUsername.error = getString(R.string.username_empty)
            else binding.tfUpdateUsername.error = null
        }
    }

    private fun validateInput() {
        // hide keyboard
        try {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val username = binding.tfUpdateUsername.editText?.text.toString().trim {it <= ' '}

            if (username == args.username) {
                // no changes were made
                dismiss()
            }
            else if (TextUtils.isEmpty(username)) {
                binding.tfUpdateUsername.error = getString(R.string.username_empty)
            }
            else {
                usernameExists(firebaseUser, username)
            }
        }
        else {
            sessionExpired()
        }
    }

    private fun usernameExists(firebaseUser: FirebaseUser, username: String) {
        showProgressDialog()
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
                    updateDB(firebaseUser, username)
                }
                else {
                    hideProgressDialog()
                    binding.tfUpdateUsername.error = getString(R.string.username_exists)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to update username, ${error.message}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { usernameExists(firebaseUser, username )}
                    .show()
            }
        })
    }

    private fun updateDB(firebaseUser: FirebaseUser, username: String) {
        showProgressDialog()
        databaseReference = database.getReference("users").child(firebaseUser.uid).child("username")
        databaseReference.setValue(username)
            .addOnSuccessListener {
                updateProfile(firebaseUser, username)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to update username, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { updateDB(firebaseUser, username )}
                    .show()
            }
    }

    private fun updateProfile(firebaseUser: FirebaseUser, username: String) {
        showProgressDialog()
        val updates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .setPhotoUri(firebaseUser.photoUrl)
            .build()

        firebaseUser.updateProfile(updates)
            .addOnSuccessListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Successfully saved changes", Snackbar.LENGTH_LONG)
                    .show()

                dismiss()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to save changes, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { updateProfile(firebaseUser, username) }
                    .show()
            }
    }

    private fun sessionExpired() {
        Snackbar
            .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        // add 3 second delay
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // do nothing
            }
            override fun onFinish() {
                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity.finish()
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.pbUpdateUsername.visibility = View.VISIBLE
        binding.llUpdateUsernameButtons.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbUpdateUsername.visibility = View.GONE
        binding.llUpdateUsernameButtons.visibility = View.VISIBLE
    }

}