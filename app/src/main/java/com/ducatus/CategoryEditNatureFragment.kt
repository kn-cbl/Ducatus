package com.ducatus

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentCategoryEditNatureBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class CategoryEditNatureFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentCategoryEditNatureBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private val args: CategoryEditNatureFragmentArgs by navArgs()
    private var updated: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        binding = FragmentCategoryEditNatureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadNatures()
        inputObserver()

        binding.btnEditCategoryNatureCancel.setOnClickListener {
            dismiss()
        }

        binding.btnEditCategoryNatureSave.setOnClickListener {
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

    private fun loadNatures() {
        val natures = listOf("Essentials", "Wants", "Savings")
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, natures)
        (binding.tfEditCategoryNature.editText as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun inputObserver() {
        binding.tfEditCategoryNature.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfEditCategoryNature.error = getString(R.string.category_nature_empty)
            else binding.tfEditCategoryNature.error = null
        }
    }

    private fun validateInput() {
        val categoryNature = binding.tfEditCategoryNature.editText?.text.toString().trim {it <= ' '}
        if (categoryNature == args.categoryNature) {
            // no changes were made
            dismiss()
        }
        else if (TextUtils.isEmpty(categoryNature)) {
            binding.tfEditCategoryNature.error = getString(R.string.category_name_empty)
        }
        else {
            // parse selected nature
            val nature = when (categoryNature) {
                "Essentials" -> 0
                "Wants" -> 1
                "Savings" -> 2
                else -> 3
            }

            saveChanges(nature)
        }
    }

    private fun saveChanges(categoryNature: Int) {
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val sharedPreferences = SharedPreferences(activity)
            val currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            databaseReference = database.getReference("categories").child(firebaseUser.uid).child(currentAccountId).child(args.categoryId).child("category_nature")
            databaseReference.setValue(categoryNature)
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
                        .setAction(getString(R.string.retry)) { saveChanges(categoryNature) }
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