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
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentCategoryEditNameDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class CategoryEditNameDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentCategoryEditNameDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private var updated: Boolean = false
    private val args: CategoryEditNameDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        binding = FragmentCategoryEditNameDialogBinding.inflate(inflater, container, false)
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
            validateData()
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

    private fun validateData() {
        // hide keyboard
        try {
            val windowToken: View = dialog!!.window!!.decorView.rootView
            val imm: InputMethodManager = dialog!!.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken.windowToken, 0)
        }
        catch (e: Exception){}

        val categoryName = binding.tfEditCategoryName.editText?.text.toString().trim {it <= ' '}
        if (TextUtils.isEmpty(categoryName)) {
            binding.tfEditCategoryName.error = getString(R.string.category_name_empty)
        }
        else if (categoryName.lowercase() == args.categoryName.lowercase()) {
            // no changes were made
            dismiss()
        }
        else {
            auth = Firebase.auth
            val firebaseUser: FirebaseUser? = auth.currentUser
            if (firebaseUser != null) {
                val sharedPreferences = SharedPreferences(activity)
                val currentAccountId = sharedPreferences.accountId.toString()
                categoryExists(firebaseUser.uid, currentAccountId, categoryName)
            }
            else {
                sessionExpired()
            }
        }
    }

    private fun categoryExists(uid: String, accountId: String, categoryName: String) {
        showProgressDialog()
        database = Firebase.database
        databaseReference = database.getReference("categories").child(uid).child(accountId)
        val query = databaseReference.orderByChild("nameLower").equalTo(categoryName.lowercase())
        query.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    saveChanges(categoryName)
                }
                else {
                    binding.tfEditCategoryName.error = getString(R.string.category_name_exists)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun saveChanges(categoryName: String) {
        databaseReference.child(args.categoryId).child("name").setValue(categoryName)
            .addOnSuccessListener {
                databaseReference.child(args.categoryId).child("nameLower").setValue(categoryName.lowercase())
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
        binding.pbEditCategoryName.visibility = View.VISIBLE
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialog() {
        binding.pbEditCategoryName.visibility = View.INVISIBLE
        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}