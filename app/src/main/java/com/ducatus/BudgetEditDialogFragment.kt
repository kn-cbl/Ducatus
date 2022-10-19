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
import com.ducatus.databinding.FragmentBudgetEditDialogBinding
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
    private lateinit var rootLayout: LinearLayout
    private lateinit var selectedCategory: String
    private lateinit var currentAccountId: String
    private var firebaseUser: FirebaseUser? = null
    private var accountMonthlyBudget: Double = 0.0
    private var accountRemainingBudget: Double = 0.0
    private var essentialCategories = 0
    private var wantCategories = 0
    private var savingCategories = 0
    private var editable: Boolean = true
    private val args: BudgetEditDialogFragmentArgs by navArgs()
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
        loadAccount(firebaseUser!!.uid, currentAccountId)

        // disable editing of budget category and budget total when user has already added
        // a transaction record for the budget
        if (args.budgetSpent.toDouble() > 0) {
            binding.tfEditBudgetCategory.editText?.setText(args.categoryName)
            binding.tfEditBudgetCategory.editText?.setTextColor(ContextCompat.getColor(activity, R.color.gray))
            binding.tfEditBudgetCategory.editText?.isEnabled = false
            binding.tfEditBudgetAmount.editText?.isEnabled = false
            binding.tfEditBudgetAmount.editText?.setTextColor(ContextCompat.getColor(activity, R.color.gray))
            selectedCategory = args.categoryId
            editable = false
        }
        else {
            loadCategories(firebaseUser!!.uid, currentAccountId)

            val spCategory = (binding.tfEditBudgetCategory.editText as? AutoCompleteTextView)
            spCategory?.onItemClickListener =
                AdapterView.OnItemClickListener { parent, _, position, _ ->
                    val string: StringWithTag = parent?.getItemAtPosition(position) as StringWithTag

                    // store id of selected category
                    selectedCategory = string.tag

                    // determine budget based on selected category
                    determineRecommendedBudget(string.tag2)
                }
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
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val sharedPreferences = SharedPreferences(activity)
            currentAccountId = sharedPreferences.accountId.toString()
        }
        else {
            sessionExpired()
        }
    }

    private fun loadAccount(uid: String, accountId: String) {
        showProgressDialog()
        database = Firebase.database
        databaseReference = database.getReference("accounts").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener {
                val account = it.getValue<Account>()
                if (account != null) {
                    // set remaining budget
                    accountMonthlyBudget = account.account_monthly_budget
                    accountRemainingBudget = account.account_remaining_budget
                    val text = "Remaining budget: ₱" + String.format("%,.2f", accountRemainingBudget)

                    binding.tfEditBudgetAccount.helperText = text
                    binding.tfEditBudgetAccount.editText?.setText(account.account_name)
                }
                inputObserver()
                hideProgressDialog()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage!!.toString(), Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun loadCategories(uid: String, accountId: String) {
        showProgressDialog()
        databaseReference = database.getReference("categories").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener {
                essentialCategories = 0
                wantCategories = 0
                savingCategories = 0

                var selectedCategoryTags: StringWithTag? = null

                val categories = mutableListOf<StringWithTag>()
                for (child in it.children) {
                    val category = child.getValue<Category>()
                    if (category != null) {
                        // count total number of each category nature to be used in determining
                        // recommended budget
                        when (category.category_nature) {
                            0 -> essentialCategories++
                            1 -> wantCategories++
                            2 -> savingCategories++
                        }

                        if (category.category_id.toString() == args.categoryId) {
                            selectedCategoryTags = StringWithTag(
                                category.category_name.toString(),
                                category.category_id.toString(),
                                category.category_nature.toString(),
                                null
                            )
                        }

                        categories.add(
                            StringWithTag(
                                category.category_name.toString(),
                                category.category_id.toString(),
                                category.category_nature.toString(),
                                null
                            )
                        )
                    }
                }

                // store category id
                selectedCategory = selectedCategoryTags!!.tag
                determineRecommendedBudget(selectedCategoryTags.tag2)

                val adapter = ArrayAdapter(activity, R.layout.list_item, categories)
                val spinner = (binding.tfEditBudgetCategory.editText as? AutoCompleteTextView)
                spinner?.setAdapter(adapter)
                spinner?.setText(selectedCategoryTags.toString(), false)
                hideProgressDialog()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage!!.toString(), Toast.LENGTH_LONG)
                    .show()
            }
    }

    // determine budget based on selected category using 50:30:20 rule
    private fun determineRecommendedBudget(categoryNature: String) {
        var recommendedBudget = 0.0
        when (categoryNature) {
            "0" -> recommendedBudget = (accountMonthlyBudget * 0.50) / essentialCategories
            "1" -> recommendedBudget = (accountMonthlyBudget * 0.30) / wantCategories
            "2" -> recommendedBudget = (accountMonthlyBudget * 0.20) / savingCategories
        }

        val text = "Recommended budget for the selected category: ₱" + String.format("%,.2f", recommendedBudget)
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
                binding.tfEditBudgetAmount.error = getString(R.string.budget_amount_empty)
            }
            else if (text.toString().toDouble() > accountRemainingBudget) {
                Toast.makeText(activity, accountRemainingBudget.toString(), Toast.LENGTH_LONG).show()
                binding.tfEditBudgetAmount.error = getString(R.string.budget_amount_overflow)
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
        binding.tfEditBudgetCategory.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) {
                binding.tfEditBudgetCategory.error = null
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
        val budgetCategory = binding.tfEditBudgetCategory.editText?.text.toString().trim { it <= ' ' }
        val budgetAmount = binding.tfEditBudgetAmount.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (TextUtils.isEmpty(budgetName)) {
            binding.tfEditBudgetName.error = getString(R.string.budget_name_empty)
            errors++
        }
        if (TextUtils.isEmpty(budgetCategory)) {
            binding.tfEditBudgetCategory.error = getString(R.string.budget_category_empty)
            errors++
        }
        if (TextUtils.isEmpty(budgetAmount)) {
            binding.tfEditBudgetAmount.error = getString(R.string.budget_amount_empty)
            errors++
        }
        else {
            if (budgetAmount.startsWith("0")) {
                binding.tfEditBudgetAmount.error = getString(R.string.budget_amount_0)
                errors++
            }
            if (budgetAmount.toDouble() > accountRemainingBudget) {
                binding.tfEditBudgetAmount.error = getString(R.string.budget_amount_overflow)
                errors++
            }
        }

        if (errors == 0) {
            if (budgetName == args.budgetName && selectedCategory == args.categoryId && budgetAmount.toDouble() == args.budgetAmount.toDouble()) {
                dismiss()
            }
            else {
                getAccount(firebaseUser!!.uid, currentAccountId, budgetName, selectedCategory, budgetAmount.toDouble())
            }
        }
    }

    private fun getAccount(uid: String, accountId: String, budgetName: String, categoryId: String, newBudgetAmount: Double) {
        showProgressDialogEdit()
        databaseReference = database.getReference("accounts").child(uid).child(accountId).child("account_remaining_budget")
        databaseReference.get()
            .addOnSuccessListener {
                val remainingBudget = it.value.toString().toDouble()
                val reallocatedBudget = remainingBudget + args.budgetAmount.toDouble() - newBudgetAmount
                setAccountBudget(uid, accountId, budgetName, categoryId, reallocatedBudget, newBudgetAmount)
            }
            .addOnFailureListener {
                hideProgressDialogEdit()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun setAccountBudget(uid: String, accountId: String, budgetName: String, categoryId: String, reallocatedBudget: Double, newBudgetAmount: Double) {
        databaseReference.setValue(reallocatedBudget)
            .addOnSuccessListener {
                getCategory(uid, accountId, budgetName, categoryId, newBudgetAmount)
            }
            .addOnFailureListener {
                hideProgressDialogEdit()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun getCategory(uid: String, accountId: String, budgetName: String, categoryId: String, newBudgetAmount: Double) {
        databaseReference = database.getReference("categories").child(uid).child(accountId).child(categoryId)
        databaseReference.get()
            .addOnSuccessListener {
                val category = it.getValue<Category>()
                if (category != null) {
                    getBudget(uid, accountId, budgetName, category, newBudgetAmount)
                }
            }
            .addOnFailureListener {
                hideProgressDialogEdit()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun getBudget(uid: String, accountId: String, budgetName: String, category: Category, newBudgetAmount: Double) {
        databaseReference = database.getReference("budgets").child(uid).child(accountId).child(args.budgetId)
        databaseReference.get()
            .addOnSuccessListener {
                val currentBudget = it.getValue<Budget>()
                if (currentBudget != null) {
                    val newBudget = Budget(
                        currentBudget.budget_id, budgetName, newBudgetAmount, 0.0,
                        currentBudget.budget_created_at, category.category_id, category.category_name,
                        category.category_color, category.category_icon
                    )

                    updateBudget(uid, accountId, newBudget)
                }
            }
            .addOnFailureListener {
                hideProgressDialogEdit()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun updateBudget(uid: String, accountId: String, budget: Budget) {
        databaseReference = database.getReference("budgets").child(uid).child(accountId)
        databaseReference.child(budget.budget_id.toString()).setValue(budget)
            .addOnSuccessListener {
                hideProgressDialogEdit()
                viewModel.update(true)
                dismiss()
            }
            .addOnFailureListener {
                hideProgressDialogEdit()
                Toast
                    .makeText(activity, it.localizedMessage!!.toString(), Toast.LENGTH_LONG)
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