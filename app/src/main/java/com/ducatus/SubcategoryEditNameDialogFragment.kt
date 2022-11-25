package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
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
import com.ducatus.databinding.FragmentSubcategoryEditNameDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SubcategoryEditNameDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentSubcategoryEditNameDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private var updated: Boolean = false
    private val args: SubcategoryEditNameDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        binding = FragmentSubcategoryEditNameDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inputObserver()

        binding.tfEditSubcategoryName.editText?.setText(args.subcategoryName)

        binding.btnEditSubcategoryNameCancel.setOnClickListener {
            dismiss()
        }

        binding.btnEditSubcategoryNameSave.setOnClickListener {
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
        binding.tfEditSubcategoryName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfEditSubcategoryName.error = getString(R.string.subcategory_name_empty)
            else binding.tfEditSubcategoryName.error = null
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

        val subcategoryName = binding.tfEditSubcategoryName.editText?.text.toString().trim {it <= ' '}
        if (subcategoryName.lowercase() == args.subcategoryName.lowercase()) {
            // no changes were made
            dismiss()
        }
        else if (TextUtils.isEmpty(subcategoryName)) {
            binding.tfEditSubcategoryName.error = getString(R.string.subcategory_name_empty)
        }
        else {
            auth = Firebase.auth
            val firebaseUser: FirebaseUser? = auth.currentUser
            if (firebaseUser != null) {
                val sharedPreferences = SharedPreferences(activity)
                val currentAccountId = sharedPreferences.accountId.toString()
                subcategoryExists(firebaseUser.uid, currentAccountId, subcategoryName)
            }
            else {
                sessionExpired()
            }
        }
    }

    private fun subcategoryExists(uid: String, accountId: String, subcategoryName: String) {
        showProgressDialog()
        database = Firebase.database
        databaseReference = database.getReference("subcategories").child(uid).child(accountId).child(args.categoryId)
        val query = databaseReference.orderByChild("nameLower").equalTo(subcategoryName.lowercase())
        query.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    saveChanges(subcategoryName)
                }
                else {
                    hideProgressDialog()
                    binding.tfEditSubcategoryName.error = getString(R.string.subcategory_name_exists)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun saveChanges(subcategoryName: String) {
        databaseReference = databaseReference.child(args.subcategoryId)
        databaseReference.child("name").setValue(subcategoryName)
            .addOnSuccessListener {
                databaseReference.child("nameLower").setValue(subcategoryName.lowercase())
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
        binding.pbEditSubcategoryName.visibility = View.VISIBLE
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }
    private fun hideProgressDialog() {
        binding.pbEditSubcategoryName.visibility = View.INVISIBLE
        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}