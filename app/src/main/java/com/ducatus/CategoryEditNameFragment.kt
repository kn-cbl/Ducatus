package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentCategoryEditNameBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class CategoryEditNameFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentCategoryEditNameBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private val args: CategoryEditNameFragmentArgs by navArgs()
    private var updated: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        binding = FragmentCategoryEditNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inputObserver()

        binding.tfEditCategoryName.editText?.setText(args.categoryName)

        binding.btnEditCategoryNameCancel.setOnClickListener {
            dismiss()
        }

        binding.btnEditCategoryNameSave.setOnClickListener {
            validateInput()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (updated) {
            val fragment = parentFragmentManager.findFragmentById(R.id.fcCategories)
            if (fragment is DialogInterface.OnDismissListener) {
                (fragment as DialogInterface.OnDismissListener?)?.onDismiss(dialog)
            }
        }
    }

    private fun inputObserver() {
        binding.tfEditCategoryName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfEditCategoryName.error = getString(R.string.category_name_empty)
            else binding.tfEditCategoryName.error = null
        }
    }

    private fun validateInput() {
        // hide keyboard
        try {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        val categoryName = binding.tfEditCategoryName.editText?.text.toString().trim {it <= ' '}
        if (categoryName == args.categoryName) {
            // no changes were made
            dismiss()
        }
        else if (TextUtils.isEmpty(categoryName)) {
            binding.tfEditCategoryName.error = getString(R.string.category_name_empty)
        }
        else {
            saveChanges(categoryName)
        }
    }

    private fun saveChanges(categoryName: String) {
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val sharedPreferences = SharedPreferences(activity)
            val currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            databaseReference = database.getReference("categories").child(firebaseUser.uid).child(currentAccountId).child(args.categoryId).child("category_name")
            databaseReference.setValue(categoryName)
                .addOnSuccessListener {
                    Snackbar
                        .make(rootLayout, "Successfully saved changes", Snackbar.LENGTH_LONG)
                        .show()

                    updated = true
                    dismiss()
                }
                .addOnFailureListener {
                    Snackbar
                        .make(rootLayout, "Unable to save changes, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry)) { saveChanges(categoryName) }
                        .show()
                }
        }
        else {
            sessionExpired()
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

}