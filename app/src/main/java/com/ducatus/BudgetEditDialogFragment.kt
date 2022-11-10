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
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.ducatus.data.Account
import com.ducatus.data.Budget
import com.ducatus.data.Category
import com.ducatus.databinding.FragmentBudgetEditDialogBinding
import com.ducatus.viewmodel.AmountViewModel
import com.ducatus.viewmodel.BudgetViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class BudgetEditDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentBudgetEditDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var rootLayout: LinearLayout
    private lateinit var currentAccountId: String
    private lateinit var selectedCategory: Category
    private var accountBudget = mutableMapOf(
        "monthly" to 0.0,
        "remaining" to 0.0,
    )

    private var natureCount = mutableListOf(0, 0, 0)
    private val args: BudgetEditDialogFragmentArgs by navArgs()
    private val amountViewModel: AmountViewModel by activityViewModels()
    private val viewModel: BudgetViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llBudgetDetailRoot)
        binding = FragmentBudgetEditDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        inputObserver()

        // disable editing of budget category and budget total when user has already added
        // a transaction record for the budget
//        if (args.budgetSpent.toDouble() > 0) {
//            binding.tfEditBudgetAmount.editText?.isEnabled = false
//            binding.tfEditBudgetAmount.editText?.setTextColor(ContextCompat.getColor(activity, R.color.gray))
//            editable = false
//        }
//        else {
//
//        }

        amountViewModel.amount.observe(viewLifecycleOwner) { amount ->
            binding.tfEditBudgetAmount.editText?.setText(amount)
        }

        binding.tfEditBudgetAmount.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = AmountDialogFragment()

            val bundle = Bundle()
            bundle.putString("budget", "budget")
            newFragment.arguments = bundle

            newFragment.show(fragmentManager, "dialog")
        }

        binding.tfEditBudgetName.editText?.setText(args.budgetName)
        binding.tfEditBudgetAmount.editText?.setText(args.budgetAmount.toDouble().toInt().toString())

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

                        val text = "Remaining budget: ₱" + String.format("%,.2f", accountBudget["monthly"])
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

                        if (category.id!! == args.budgetId) {
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
        binding.tfEditBudgetName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfEditBudgetName.error = getString(R.string.budget_name_empty)
            }
            else {
                binding.tfEditBudgetName.error = null
            }
        }
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

        val budgetName =  binding.tfEditBudgetName.editText?.text.toString().trim { it <= ' ' }
        val budgetAmount = binding.tfEditBudgetAmount.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (TextUtils.isEmpty(budgetName)) {
            binding.tfEditBudgetName.error = getString(R.string.budget_name_empty)
            errors++
        }
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
            if (budgetName == args.budgetName && selectedCategory.id == args.budgetId && budgetAmount.toDouble() == args.budgetAmount.toDouble()) {
                dismiss()
            }
            else {
                updateAccountRemainingBudget(firebaseUser.uid, currentAccountId, budgetName, selectedCategory, budgetAmount.toDouble())
            }
        }
    }

    private fun updateAccountRemainingBudget(uid: String, accountId: String, budgetName: String, category: Category, newBudgetAmount: Double) {
        showProgressDialogEdit()
        databaseReference = database.getReference("accounts").child(uid).child(accountId)
        databaseReference.child("remainingBudget").get()
            .addOnSuccessListener { snapshot ->
                val remainingBudget = snapshot.value.toString().toDouble()
                val reallocatedBudget = remainingBudget + args.budgetAmount.toDouble() - newBudgetAmount

                databaseReference.setValue(reallocatedBudget)
                    .addOnSuccessListener {
                        updateBudget(uid, accountId, budgetName, category, newBudgetAmount)
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

    private fun updateBudget(uid: String, accountId: String, budgetName: String, category: Category, newBudgetAmount: Double) {
        databaseReference = database.getReference("budgets").child(uid).child(accountId).child(args.budgetId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val currentBudget = snapshot.getValue<Budget>()
                if (currentBudget != null) {
                    val newBudget = Budget(
                        currentBudget.id,
                        budgetName,
                        budgetName.lowercase(),
                        newBudgetAmount,
                        0.0,
                        currentBudget.createdAt,
                        category.name,
                        category.color,
                        category.icon
                    )

                    databaseReference.setValue(newBudget)
                        .addOnSuccessListener {
                            hideProgressDialogEdit()
                            viewModel.update(true)
                            dismiss()
                        }
                        .addOnFailureListener {
                            hideProgressDialog()
                            Toast
                                .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                                .show()
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
                try {
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity.finish()
                }
                catch (e: Exception) {}
            }
        }.start()
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
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialogEdit() {
        binding.pbEditBudget.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}