package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.ducatus.data.Account
import com.ducatus.data.Budget
import com.ducatus.data.Category
import com.ducatus.databinding.FragmentBudgetEditDialogBinding
import com.ducatus.viewmodel.AmountViewModel
import com.ducatus.viewmodel.BudgetViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson

class BudgetEditDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentBudgetEditDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var currentAccountId: String
    private lateinit var selectedBudget: Budget
    private lateinit var selectedCategory: Category
    private var accountBudget = mutableMapOf(
        "monthly" to 0.0,
        "remaining" to 0.0,
    )

    private var natureCount = mutableListOf(0, 0, 0)
    private val amountViewModel: AmountViewModel by activityViewModels()
    private val budgetViewModel: BudgetViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.clBudgetDetail)
        binding = FragmentBudgetEditDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        inputObserver()

        amountViewModel.amount.observe(viewLifecycleOwner) { amount ->
            amount.getContentIfNotHandled()?.let { content ->
                binding.tfEditBudgetAmount.editText?.setText(content)
            }
        }

        binding.tfEditBudgetAmount.editText?.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("budget", "budget")

            val fragmentManager = childFragmentManager
            val newFragment = AmountDialogFragment()
            newFragment.arguments = bundle
            newFragment.show(fragmentManager, "dialog")
        }

        binding.tfEditBudgetAmount.editText?.setText(selectedBudget.amountTotal.toInt().toString())

        binding.btnEditBudgetCancel.setOnClickListener {
            dismiss()
        }

        binding.btnEditBudgetSave.setOnClickListener {
            validateData()
        }
    }

    private fun loadData() {
        auth = Firebase.auth
        if (auth.currentUser != null) {
            firebaseUser = auth.currentUser!!
            database = Firebase.database

            val sharedPreferences = SharedPreferences(activity)
            currentAccountId = sharedPreferences.accountId.toString()

            val strBudget = arguments?.getString("budget")
            selectedBudget = Gson().fromJson(strBudget, Budget::class.java)

            getAccountRemainingBudget(firebaseUser.uid, currentAccountId)
            getCategories(firebaseUser.uid, currentAccountId)
        }
        else {
            sessionExpired()
        }
    }

    private fun getAccountRemainingBudget(uid: String, accountId: String) {
        showProgressDialog()
        databaseReference = database.getReference("accounts").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val account = snapshot.getValue<Account>()
                if (account != null) {
                    if (account.id == accountId) {
                        accountBudget["monthly"] = account.monthlyBudget
                        accountBudget["remaining"] = account.remainingBudget

                        val text = "Remaining budget: ₱" + String.format("%,.2f", accountBudget["remaining"])
                        binding.tvEditBudgetRemainingBudget.text = text
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

    private fun getCategories(uid: String, accountId: String) {
        showProgressDialog()
        databaseReference = database.getReference("categories").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener {
                for (child in it.children) {
                    val category = child.getValue<Category>()
                    if (category != null) {
                        // count total number of each category nature to be used in determining
                        // recommended budget
                        when (category.nature) {
                            0 -> natureCount[0]++
                            1 -> natureCount[1]++
                            2 -> natureCount[2]++
                        }

                        if (category.id!! == selectedBudget.id!!) {
                            selectedCategory = category
                        }
                    }
                }

                determineRecommendedBudget(selectedCategory.nature)
                hideProgressDialog()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    // determine budget based on selected category using 50:30:20 rule
    private fun determineRecommendedBudget(categoryNature: Int) {
        var recommendedBudget = 0.0
        when (categoryNature) {
            0 -> recommendedBudget = (accountBudget["monthly"]!! * 0.50) / natureCount[0]
            1 -> recommendedBudget = (accountBudget["monthly"]!! * 0.30) / natureCount[1]
            2 -> recommendedBudget = (accountBudget["monthly"]!! * 0.20) / natureCount[2]
        }

        val text = "Recommended budget: ₱" + String.format("%,.2f", recommendedBudget)
        binding.tfEditBudgetAmount.helperText = text
    }

    private fun inputObserver() {
        binding.tfEditBudgetAmount.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfEditBudgetAmount.error = getString(R.string.amount_empty)
            }
            else if (text.toString().toDouble() > accountBudget["remaining"]!!) {
                binding.tfEditBudgetAmount.error = getString(R.string.amount_overflow)
            }
            else {
                binding.tfEditBudgetAmount.error = null
            }
        }
        binding.tfEditBudgetAmount.editText?.doAfterTextChanged { text ->
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

        val budgetAmount = binding.tfEditBudgetAmount.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (TextUtils.isEmpty(budgetAmount)) {
            binding.tfEditBudgetAmount.error = getString(R.string.amount_empty)
            errors++
        }
        else {
            if (budgetAmount.startsWith("0")) {
                binding.tfEditBudgetAmount.error = getString(R.string.amount_starts_0)
                errors++
            }
            if (budgetAmount.toDouble() > accountBudget["remaining"]!!) {
                binding.tfEditBudgetAmount.error = getString(R.string.amount_overflow)
                errors++
            }
        }

        if (errors == 0) {
            if (budgetAmount.toDouble() == selectedBudget.amountTotal) {
                dismiss()
            }
            else {
                updateAccountRemainingBudget(
                    firebaseUser.uid,
                    currentAccountId,
                    selectedBudget,
                    budgetAmount.toDouble()
                )
            }
        }
    }

    private fun updateAccountRemainingBudget(uid: String, accountId: String, budget: Budget, newBudgetAmount: Double) {
        showProgressDialogEdit()
        databaseReference = 
            database.getReference("accounts")
                .child(uid)
                .child(accountId)
                .child("remainingBudget")
                
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val remainingBudget = snapshot.value.toString().toDouble()
                val reallocatedBudget = remainingBudget + budget.amountTotal - newBudgetAmount

                databaseReference.setValue(reallocatedBudget)
                    .addOnSuccessListener {
                        budget.amountTotal = newBudgetAmount
                        updateBudget(uid, accountId, budget)
                    }
                    .addOnFailureListener {
                        hideProgressDialog()
                        Toast
                            .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                            .show()
                    }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun updateBudget(uid: String, accountId: String, budget: Budget) {
        databaseReference =
            database.getReference("budgets")
                .child(uid)
                .child(accountId)
                .child(budget.id!!)

        databaseReference.setValue(budget)
            .addOnSuccessListener {
                budgetViewModel.setBudget(budget)
                hideProgressDialogEdit()
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
        binding.pbBudgetEdit.visibility = View.VISIBLE
        binding.llBudgetEdit.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbBudgetEdit.visibility = View.INVISIBLE
        binding.llBudgetEdit.visibility = View.VISIBLE
    }

    private fun showProgressDialogEdit() {
        binding.pbEditBudget.visibility = View.VISIBLE
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialogEdit() {
        binding.pbEditBudget.visibility = View.INVISIBLE
        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}
