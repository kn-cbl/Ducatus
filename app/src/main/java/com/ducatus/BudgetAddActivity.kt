package com.ducatus

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import com.ducatus.data.Account
import com.ducatus.data.Budget
import com.ducatus.data.Category
import com.ducatus.data.CategoryWithTag
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
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class BudgetAddActivity : AppCompatActivity() {
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityBudgetAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var currentAccountId: String
    private lateinit var selectedCategory: Category
    private var firebaseUser: FirebaseUser? = null
    private var allocatedCategories = 0
    private var natureCount = mutableListOf(0, 0, 0)
    private var accountBudget = mutableMapOf(
        "monthly" to 0.0,
        "remaining" to 0.0,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetAddBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadData()
        inputObserver()
        setAmountPresetClickListener()

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

        val spCategory = (binding.tfAddBudgetCategory.editText as? AutoCompleteTextView)
        spCategory?.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val category = parent?.getItemAtPosition(position) as CategoryWithTag

                // store data of selected category
                selectedCategory = category.category

                // determine budget based on selected category
                determineRecommendedBudget(category.category.nature)
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onResume() {
        super.onResume()

        firebaseUser?.let {
            hasSetBudget(it.uid, currentAccountId)
            loadAccounts(it.uid, currentAccountId)
            loadCategories(it.uid, currentAccountId)
        }
    }

    private fun loadData() {
        auth = Firebase.auth
        if (auth.currentUser != null) {
            database = Firebase.database
            firebaseUser = auth.currentUser!!
            sharedPreferences = SharedPreferences(this)
            currentAccountId = sharedPreferences.accountId.toString()
        }
        else {
            sessionExpired()
        }
    }

    private fun loadAccounts(uid: String, accountId: String) {
        showProgressDialog()
        databaseReference = database.getReference("accounts").child(uid)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                // get all accounts
                for (child in snapshot.children) {
                    val account = child.getValue<Account>()
                    if (account != null) {
                        if (account.id == accountId) {
                            // set remaining budget
                            accountBudget["monthly"] = account.monthlyBudget
                            accountBudget["remaining"] = account.remainingBudget
                        }
                    }
                }

                val text = "Remaining budget: ₱" + String.format("%,.2f", accountBudget["remaining"])
                binding.tvAddBudgetRemainingBudget.text = text
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clBudgetAdd, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun hasSetBudget(uid: String, accountId: String) {
        databaseReference = database.getReference("accounts").child(uid).child(accountId)
        databaseReference.child("monthlyBudget").get()
            .addOnSuccessListener { snapshot ->
                val monthlyBudget = snapshot.value.toString().toDouble()
                if (monthlyBudget <= 0.0) {
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
                natureCount = mutableListOf(0, 0, 0)

                val categories = mutableListOf<CategoryWithTag>()
                for (child in snapshot.children) {
                    val category = child.getValue<Category>()
                    if (category != null) {
                        // count total number of each category nature to be used in determining
                        // recommended budget
                        when (category.nature) {
                            0 -> natureCount[0]++
                            1 -> natureCount[1]++
                            2 -> natureCount[2]++
                        }

                        // only add categories that have not been budgeted yet
                        if (category.allocated.toString() == "false") {
                            categories.add(
                                CategoryWithTag(
                                    category.name!!,
                                    category
                                )
                            )
                        }
                        else {
                            allocatedCategories++
                        }
                    }
                }

                // sort categories by name
                categories.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

                // store category data
                selectedCategory = categories.first().category
                determineRecommendedBudget(categories.first().category.nature)

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
    private fun determineRecommendedBudget(categoryNature: Int) {
        var recommendedBudget = 0.0
        when (categoryNature) {
            0 -> recommendedBudget = (accountBudget["monthly"]!! * 0.50) / natureCount[0]
            1 -> recommendedBudget = (accountBudget["monthly"]!! * 0.30) / natureCount[1]
            2 -> recommendedBudget = (accountBudget["monthly"]!! * 0.20) / natureCount[2]
        }

        val text = "Recommended budget for the selected category: ₱" + String.format("%,.2f", recommendedBudget)
        binding.tfAddBudgetAmount.helperText = text

        binding.tvAddBudgetRecommendedBudget.text = recommendedBudget.toString()
        binding.tvAddBudgetRecommendedBudget.tag = recommendedBudget
    }

    private fun setAmountPresetClickListener() {
        val amountList = listOf(
            "500", "1000", "1500",
            "2000", "3000", "4000",
            "5000", "7500", "10000"
        )

        val gridLayout = findViewById<GridLayout>(R.id.glAmountPreset)
        for (i in 0 until gridLayout.childCount) {
            val gridItem = gridLayout.getChildAt(i) as TextView
            gridItem.text = amountList[i]
            gridItem.tag = amountList[i]

            gridLayout.getChildAt(i).setOnClickListener { item ->
                val amount = item.tag.toString()
                binding.tfAddBudgetAmount.editText?.setText(amount)
            }
        }

        binding.tvAddBudgetRecommendedBudget.setOnClickListener {
            val amount = it.tag.toString()
            binding.tfAddBudgetAmount.editText?.setText(amount)
        }
    }

    private fun inputObserver() {
        binding.tfAddBudgetAmount.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddBudgetAmount.error = getString(R.string.amount_empty)
            }
            else if (text.toString().toDouble() > accountBudget["remaining"]!!) {
                binding.tfAddBudgetAmount.error = getString(R.string.amount_overflow)
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

        val budgetCategory = binding.tfAddBudgetCategory.editText?.text.toString().trim { it <= ' ' }
        val budgetAmount = binding.tfAddBudgetAmount.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (accountBudget["monthly"]!! <= 0.0) {
            firebaseUser?.let { hasSetBudget(it.uid, currentAccountId) }
            errors++
        }

        if (TextUtils.isEmpty(budgetCategory)) {
            binding.tfAddBudgetCategory.error = getString(R.string.category_empty)
            errors++
        }

        if (TextUtils.isEmpty(budgetAmount)) {
            binding.tfAddBudgetAmount.error = getString(R.string.amount_empty)
            errors++
        }

        else {
            if (budgetAmount.startsWith("0")) {
                binding.tfAddBudgetAmount.error = getString(R.string.amount_starts_0)
                errors++
            }

            if (budgetAmount.toDouble() > accountBudget["remaining"]!!) {
                binding.tfAddBudgetAmount.error = getString(R.string.amount_overflow)
                errors++
            }
        }

        if (errors == 0) {
            firebaseUser?.let {
                showProgressDialogAdd()

                // epoch time
                val timestamp = (System.currentTimeMillis() / 1000)
                val budget = Budget(
                    selectedCategory.id,
                    budgetAmount.toDouble(),
                    0.0,
                    timestamp,
                    selectedCategory.name,
                    selectedCategory.nameLower,
                    selectedCategory.color,
                    selectedCategory.icon
                )

                val remainingBudget = accountBudget["remaining"]!! - budget.amountTotal
                setAllocated(it.uid, currentAccountId, budget, remainingBudget, allocatedCategories)
            }
        }
    }

    private fun setAllocated(
        uid: String,
        accountId: String,
        budget: Budget,
        remainingBudget: Double,
        allocatedCategories: Int
    ) {
        val categoriesReference =
            database.getReference("categories")
                .child(uid)
                .child(accountId)
                .child(budget.id!!)

        categoriesReference.child("allocated").setValue(true)
            .addOnSuccessListener {
                decreaseRemainingBudget(uid, accountId, budget, remainingBudget, allocatedCategories)
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clBudgetAdd, getString(R.string.add_budget_error),5000)
                    .show()
            }
    }

    private fun decreaseRemainingBudget(
        uid: String,
        accountId: String,
        budget: Budget,
        remainingBudget: Double,
        allocatedCategories: Int
    ) {
        val accountsReference = database.getReference("accounts").child(uid).child(accountId)
        accountsReference.child("remainingBudget").setValue(remainingBudget)
            .addOnSuccessListener {
                addBudget(uid, accountId, budget, allocatedCategories)
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clBudgetAdd, getString(R.string.add_budget_error),5000)
                    .show()
            }
    }

    private fun addBudget(uid: String, accountId: String, budget: Budget, allocatedCategories: Int) {
        val budgetsReference = database.getReference("budgets").child(uid).child(accountId)
        budgetsReference.child(budget.id!!).setValue(budget)
            .addOnSuccessListener {
                if (allocatedCategories == 0) {
                    scheduleNotification(this, accountId)
                }

                hideProgressDialogAdd()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clBudgetAdd, getString(R.string.add_budget_error),5000)
                    .show()
            }
    }

    private fun createNotificationChannel() {
        val name = "Expenses"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(sharedPreferences.expensesChannelId, name, importance)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun enableReceiver(context: Context) {
        val receiver = ComponentName(context, NotificationReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun scheduleNotification(context: Context, accountId: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.expensesChannelId)
        if (notificationChannel == null) {
            createNotificationChannel()
            notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.expensesChannelId)
        }

        // create notification if channel is enabled
        // else do not create
        if (notificationChannel.importance != NotificationManager.IMPORTANCE_NONE) {
            enableReceiver(context)

            // pass to broadcast receiver
            val notificationIntent = Intent(context, NotificationReceiver::class.java)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val title = "Record your expenses for today"
            val message = "Tap here to open Ducatus."

            val notificationId = 28800000
            notificationIntent.action = "com.ducatus.EXPENSE"
            notificationIntent.putExtra(titleExtra, title)
            notificationIntent.putExtra(messageExtra, message)
            notificationIntent.putExtra(notificationIdExtra, notificationId)
            notificationIntent.putExtra(accountIdExtra, accountId)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // six pm every day
            val zdtNow = ZonedDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            ).with(LocalTime.MIN).plusHours(18).toInstant().toEpochMilli()

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                zdtNow,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    private fun sessionExpired() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.session_expired))
            .setPositiveButton(resources.getString(R.string.log_in)) { _, _ -> }

        dialog.setOnDismissListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        dialog.show()
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
        val bundle = Bundle()
        bundle.putString("title", getString(R.string.adding))

        actionDialog = ActionDialogFragment()
        actionDialog.arguments = bundle
        actionDialog.show(supportFragmentManager, "dialog")
    }

    private fun hideProgressDialogAdd() {
        actionDialog.dismiss()
    }
}