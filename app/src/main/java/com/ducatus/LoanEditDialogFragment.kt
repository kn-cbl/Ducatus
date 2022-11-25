package com.ducatus

import android.app.Activity
import android.content.Context
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
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.ducatus.data.Loan
import com.ducatus.databinding.FragmentLoanEditDialogBinding
import com.ducatus.viewmodel.AmountViewModel
import com.ducatus.viewmodel.LoanViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class LoanEditDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentLoanEditDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var currentLoanNameLower: String
    private lateinit var selectedLoan: Loan
    private var firebaseUser: FirebaseUser? = null
    private var loanType: String = "L"
    private val amountViewModel: AmountViewModel by activityViewModels()
    private val loanViewModel: LoanViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.clLoanDetail)
        binding = FragmentLoanEditDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        inputObserver()

        amountViewModel.amount.observe(viewLifecycleOwner) { amount ->
            amount.getContentIfNotHandled()?.let { content ->
                binding.tfLoanEditRemainingLoan.editText?.setText(content)
            }
        }

        binding.btnLoanEditCancel.setOnClickListener {
            dismiss()
        }

        binding.tfLoanEditRemainingLoan.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = AmountDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        // determine if loan is lend or borrow
        binding.rgLoanEdit.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbLoanEditLend -> {
                    loanType = "L"
                }
                R.id.rbLoanEditBorrow -> {
                    loanType = "B"
                }
            }
        }

        binding.btnLoanEditSave.setOnClickListener {
            validateData()
        }
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            sessionExpired()
        }

        val strLoan = arguments?.getString("loan")
        selectedLoan = Gson().fromJson(strLoan, Loan::class.java)
        currentLoanNameLower = selectedLoan.nameLower!!
        loanType = selectedLoan.type!!

        // lend or borrow
        when (loanType) {
            "L" -> {
                binding.rbLoanEditBorrow.isChecked = false
                binding.rbLoanEditLend.isChecked = true
            }
            "B" -> {
                binding.rbLoanEditLend.isChecked = false
                binding.rbLoanEditBorrow.isChecked = true
            }
        }

        binding.tfLoanEditName.editText?.setText(selectedLoan.name)
        binding.tfLoanEditRemainingLoan.editText?.setText(selectedLoan.amount.toInt().toString())
        binding.tfLoanEditNotes.editText?.setText(selectedLoan.notes)

        if (selectedLoan.amount <= 0.0) {
            binding.rgLoanEdit.visibility = View.GONE
            binding.tfLoanEditRemainingLoan.visibility = View.GONE
        }
    }

    private fun inputObserver() {
        binding.tfLoanEditName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfLoanEditName.error = getString(R.string.loan_name_empty)
            }
            else {
                binding.tfLoanEditName.error = null
            }
        }

        binding.tfLoanEditRemainingLoan.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfLoanEditRemainingLoan.error = getString(R.string.amount_empty)
            }
            else {
                binding.tfLoanEditRemainingLoan.error = null
            }
        }

        binding.tfLoanEditRemainingLoan.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) {
                text?.clear()
            }
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

        val name = binding.tfLoanEditName.editText?.text.toString().trim { it <= ' ' }
        val amount = binding.tfLoanEditRemainingLoan.editText?.text.toString().trim { it <= ' ' }
        var notes: String? = binding.tfLoanEditNotes.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (TextUtils.isEmpty(name)) {
            binding.tfLoanEditName.error = getString(R.string.loan_name_empty)
            errors++
        }

        if (TextUtils.isEmpty(notes)) {
            notes = null
        }

        if (TextUtils.isEmpty(amount)) {
            binding.tfLoanEditRemainingLoan.error = getString(R.string.amount_empty)
            errors++
        }
        else {
            if (amount.startsWith("0")) {
                binding.tfLoanEditRemainingLoan.error = getString(R.string.amount_starts_0)
                errors++
            }
        }

        if (errors == 0) {
            firebaseUser?.let {
                var changes = 0

                if (loanType != selectedLoan.type) {
                    changes++
                }
                if (name.lowercase() != selectedLoan.nameLower) {
                    changes++
                }
                if (amount.toDouble() != selectedLoan.amount) {
                    changes++
                }
                if (notes != selectedLoan.notes) {
                    changes++
                }

                if (changes == 0) {
                    dismiss()
                }
                else {
                    showProgressDialog()

                    selectedLoan.name = name
                    selectedLoan.nameLower = name.lowercase()
                    selectedLoan.amount = amount.toDouble()
                    selectedLoan.type = loanType
                    selectedLoan.notes = notes

                    if (selectedLoan.amount <= 0.0) {
                        // set paid at date today
                        val zdt = ZonedDateTime.ofInstant(
                            Instant.now(),
                            ZoneId.systemDefault()
                        )
                        selectedLoan.paidAt = zdt.toInstant().toEpochMilli()
                        selectedLoan.amount = 0.0
                    }

                    loanExists(it.uid, selectedLoan)
                }
            }
        }
    }

    private fun loanExists(uid: String, loan: Loan) {
        database = Firebase.database
        val sharedPreferences = SharedPreferences(activity)
        val currentAccountId = sharedPreferences.accountId.toString()

        databaseReference = database.getReference("loans").child(uid).child(currentAccountId)
        val query = databaseReference.orderByChild("nameLower").equalTo(loan.nameLower)
        query.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    updateLoan(loan)
                }
                else {
                    if (currentLoanNameLower == loan.nameLower!!) {
                        updateLoan(loan)
                    }
                    else {
                        hideProgressDialog()
                        binding.tfLoanEditName.error = getString(R.string.loan_name_exists)
                    }
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun updateLoan(loan: Loan) {
        databaseReference.child(loan.id!!).setValue(selectedLoan)
            .addOnSuccessListener {
                loanViewModel.setLoan(loan)
                hideProgressDialog()
                dismiss()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
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
        binding.pbLoanEdit.visibility = View.VISIBLE
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialog() {
        binding.pbLoanEdit.visibility = View.INVISIBLE
        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}