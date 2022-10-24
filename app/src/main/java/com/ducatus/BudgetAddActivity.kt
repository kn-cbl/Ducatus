package com.ducatus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import com.ducatus.data.Account
import com.ducatus.data.Budget
import com.ducatus.data.Category
import com.ducatus.databinding.ActivityBudgetAddBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class BudgetAddActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityBudgetAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var selectedAccount: String
    private lateinit var selectedCategory: String
    private var accountMonthlyBudget: Double = 0.0
    private var accountRemainingBudget: Double = 0.0
    private var essentialCategories = 0
    private var wantCategories = 0
    private var savingCategories = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetAddBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadData()
        inputObserver()

        val spAccount = (binding.tfAddBudgetAccount.editText as? AutoCompleteTextView)
        spAccount?.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val string: StringWithTag = parent?.getItemAtPosition(position) as StringWithTag

                // set remaining budget
                accountRemainingBudget = string.tag2!!.toDouble()
                accountMonthlyBudget = string.tag3!!.toDouble()

                val text = "Remaining budget: ₱" + String.format("%,.2f", accountRemainingBudget)
                binding.tfAddBudgetAccount.helperText = text

                // store id of selected account
                selectedAccount = string.tag
                hasSetBudget(firebaseUser.uid, selectedAccount)
                loadCategories(firebaseUser.uid, selectedAccount)
            }

        val spCategory = (binding.tfAddBudgetCategory.editText as? AutoCompleteTextView)
        spCategory?.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val string: StringWithTag = parent?.getItemAtPosition(position) as StringWithTag

                // store id of selected category
                selectedCategory = string.tag

                // determine budget based on selected category
                determineRecommendedBudget(string.tag2!!)
            }

        binding.tbAddBudget.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbAddBudget.inflateMenu(R.menu.check_menu)
        binding.tbAddBudget.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.done -> {
                    validateData()
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onResume() {
        super.onResume()

        hasSetBudget(firebaseUser.uid, selectedAccount)
        loadAccounts(firebaseUser.uid, selectedAccount)
        loadCategories(firebaseUser.uid, selectedAccount)
    }

    private fun loadData() {
        auth = Firebase.auth
        if (auth.currentUser != null) {
            firebaseUser = auth.currentUser!!
            val sharedPreferences = SharedPreferences(this)
            val currentAccountId = sharedPreferences.accountId.toString()

            selectedAccount = currentAccountId
            database = Firebase.database
        }
        else {
            sessionExpired()
        }
    }

    private fun loadAccounts(uid: String, accountId: String) {
        showProgressDialog()
        databaseReference = database.getReference("accounts").child(uid)
        databaseReference.get()
            .addOnSuccessListener {
                var currentAccount: String? = null

                // get all accounts
                val accounts = mutableListOf<StringWithTag>()
                for (child in it.children) {
                    val account = child.getValue<Account>()
                    if (account != null) {
                        if (account.account_id == accountId) {
                            // set remaining budget
                            accountRemainingBudget = account.account_remaining_budget
                            accountMonthlyBudget = account.account_monthly_budget
                            currentAccount = account.account_name!!
                        }
                        accounts.add(
                            StringWithTag(
                                account.account_name!!,
                                account.account_id!!,
                                account.account_remaining_budget.toString(),
                                account.account_monthly_budget.toString()
                            )
                        )
                    }
                }

                val text = "Remaining budget: ₱" + String.format("%,.2f", accountRemainingBudget)
                binding.tfAddBudgetAccount.helperText = text

                val adapter = ArrayAdapter(applicationContext, R.layout.list_item, accounts)
                val spinner = (binding.tfAddBudgetAccount.editText as? AutoCompleteTextView)
                spinner?.setAdapter(adapter)
                spinner?.setText(currentAccount, false)
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clBudgetAdd, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun hasSetBudget(uid: String, accountId: String) {
        databaseReference = database.getReference("accounts").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener {
                val monthlyBudget = it.child("account_monthly_budget").value.toString().toDouble()
                if (monthlyBudget <= 0) {
                    MaterialAlertDialogBuilder(this@BudgetAddActivity)
                        .setTitle(resources.getString(R.string.set_monthly_budget))
                        .setMessage(resources.getString(R.string.set_monthly_budget_mark))
                        .setPositiveButton(resources.getString(R.string.yes)) { _, _ -> setMonthlyBudget(accountId) }
                        .setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
                        .show()
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clBudgetAdd, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun setMonthlyBudget(accountId: String) {
        val intent = Intent(this, AccountsActivity::class.java)
        intent.putExtra("setBudget", "set")
        intent.putExtra("accountId", accountId)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun loadCategories(uid: String, accountId: String) {
        showProgressDialog()
        databaseReference = database.getReference("categories").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                essentialCategories = 0
                wantCategories = 0
                savingCategories = 0

                val categories = mutableListOf<StringWithTag>()
                for (child in snapshot.children) {
                    val category = child.getValue<Category>()
                    if (category != null) {
                        // count total number of each category nature to be used in determining
                        // recommended budget
                        when (category.category_nature) {
                            0 -> essentialCategories++
                            1 -> wantCategories++
                            2 -> savingCategories++
                        }

                        // only add categories that have not been budgeted yet
                        if (category.category_allocated.toString() == "false") {
                            categories.add(
                                StringWithTag(
                                    category.category_name!!,
                                    category.category_id!!,
                                    category.category_nature.toString(),
                                    null
                                )
                            )
                        }
                    }
                }

                // sort categories by name
                categories.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.string })

                // store category id
                selectedCategory = categories.first().tag
                determineRecommendedBudget(categories.first().tag2!!)

                val adapter = ArrayAdapter(applicationContext, R.layout.list_item, categories)
                val spinner = (binding.tfAddBudgetCategory.editText as? AutoCompleteTextView)
                spinner?.setAdapter(adapter)
                spinner?.setText(categories.first().toString(), false)
                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clBudgetAdd, it.localizedMessage!!, 5000)
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
        binding.tfAddBudgetAmount.helperText = text
    }

    private fun inputObserver() {
        binding.tfAddBudgetName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddBudgetName.error = getString(R.string.budget_name_empty)
            }
            else {
                binding.tfAddBudgetName.error = null
            }
        }
        binding.tfAddBudgetAmount.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddBudgetAmount.error = getString(R.string.budget_amount_empty)
            }
            else if (text.toString().toDouble() > accountRemainingBudget) {
                binding.tfAddBudgetAmount.error = getString(R.string.budget_amount_overflow)
            }
            else {
                binding.tfAddBudgetAmount.error = null
            }
        }
        binding.tfAddBudgetAmount.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) {
                text?.clear()
            }
        }
        binding.tfAddBudgetCategory.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) {
                binding.tfAddBudgetCategory.error = null
            }
        }
    }

    private fun validateData() {
        // hide keyboard
        try {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        val budgetName =  binding.tfAddBudgetName.editText?.text.toString().trim { it <= ' ' }
        val budgetCategory = binding.tfAddBudgetCategory.editText?.text.toString().trim { it <= ' ' }
        val budgetAmount = binding.tfAddBudgetAmount.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (accountMonthlyBudget <= 0.0) {
            hasSetBudget(firebaseUser.uid, selectedAccount)
            errors++
        }

        if (TextUtils.isEmpty(budgetName)) {
            binding.tfAddBudgetName.error = getString(R.string.budget_name_empty)
            errors++
        }

        if (TextUtils.isEmpty(budgetCategory)) {
            binding.tfAddBudgetCategory.error = getString(R.string.category_empty)
            errors++
        }

        if (TextUtils.isEmpty(budgetAmount)) {
            binding.tfAddBudgetAmount.error = getString(R.string.budget_amount_empty)
            errors++
        }

        else {
            if (budgetAmount.startsWith("0")) {
                binding.tfAddBudgetAmount.error = getString(R.string.budget_amount_0)
                errors++
            }

            if (budgetAmount.toDouble() > accountRemainingBudget) {
                binding.tfAddBudgetAmount.error = getString(R.string.budget_amount_overflow)
                errors++
            }
        }

        if (errors == 0) {
            budgetExists(firebaseUser.uid, budgetName, selectedCategory, selectedAccount, budgetAmount.toDouble())
        }
    }

    private fun budgetExists(uid: String, budgetName: String, categoryId: String, accountId: String, budgetAmount: Double) {
        showProgressDialogAdd()
        databaseReference = database.getReference("budgets").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener {
                var nameKey = false
                for (child in it.children) {
                    if (budgetName == child.child("budget_name").value.toString()) {
                        nameKey = true
                        break
                    }
                }

                if (!nameKey) {
                    getCategory(categoryId, uid, budgetName, categoryId, accountId, budgetAmount)
                }
                else {
                    hideProgressDialog()
                    binding.tfAddBudgetName.error = getString(R.string.budget_name_exists)
                }
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clBudgetAdd, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun getCategory(id: String, uid: String, budgetName: String, categoryId: String, accountId: String, budgetAmount: Double) {
        databaseReference = database.getReference("categories").child(uid).child(accountId).child(categoryId)
        databaseReference.get()
            .addOnSuccessListener {
                val categoryName = it.child("category_name").value.toString()
                val categoryColor = it.child("category_color").value.toString()
                val categoryIcon = it.child("category_icon").value.toString()

                // epoch time
                val timestamp = (System.currentTimeMillis() / 1000)
                val budget = Budget(
                    id, budgetName, budgetAmount, 0.0, timestamp,
                    categoryId, categoryName, categoryColor, categoryIcon
                )

                setAllocated(uid, accountId, budget)
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clBudgetAdd, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun setAllocated(uid: String, accountId: String, budget: Budget) {
        databaseReference.child("category_allocated").setValue(true)
            .addOnSuccessListener {
                decreaseRemainingBudget(uid, accountId, budget)
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clBudgetAdd, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun decreaseRemainingBudget(uid: String, accountId: String, budget: Budget) {
        showProgressDialogAdd()
        val remainingBudget = accountRemainingBudget - budget.budget_amount_total

        databaseReference = database.getReference("accounts").child(uid).child(accountId)
        databaseReference.child("account_remaining_budget").setValue(remainingBudget)
            .addOnSuccessListener {
                addBudget(uid, accountId, budget)
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clBudgetAdd, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun addBudget(uid: String, accountId: String, budget: Budget) {
        databaseReference = database.getReference("budgets").child(uid).child(accountId)
        databaseReference.child(budget.budget_id!!).setValue(budget)
            .addOnSuccessListener {
                hideProgressDialogAdd()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clBudgetAdd, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun sessionExpired() {
        Snackbar
            .make(binding.clBudgetAdd, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        // add 3 second delay
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // do nothing
            }
            override fun onFinish() {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.pbBudgetAdd.visibility = View.VISIBLE
        binding.llBudgetAdd.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbBudgetAdd.visibility = View.INVISIBLE
        binding.llBudgetAdd.visibility = View.VISIBLE
    }

    private fun showProgressDialogAdd() {
        binding.pbAddBudget.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialogAdd() {
        binding.pbAddBudget.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}