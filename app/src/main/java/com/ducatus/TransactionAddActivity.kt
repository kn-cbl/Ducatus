package com.ducatus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import com.ducatus.data.Budget
import com.ducatus.data.Category
import com.ducatus.data.Subcategory
import com.ducatus.data.Transaction
import com.ducatus.databinding.ActivityTransactionAddBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.text.DateFormat
import java.util.*


class TransactionAddActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityTransactionAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var datePicker: MaterialDatePicker<Long>
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var timePicker: MaterialTimePicker
    private lateinit var currentAccountId: String
    private lateinit var selectedCategory: String
    private var selectedSubcategory: String? = null
    private var selectedDate: Long? = null
    private var selectedHour: Long? = null
    private var selectedMinute: Long? = null
    private var transactionType = 0
    private val milliseconds: Long = 60 * 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionAddBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadData()
        setAmountPresetClickListener()
        setDateTimePicker()
        inputObserver()

        binding.tbAddTransaction.inflateMenu(R.menu.check_menu)
        binding.tbAddTransaction.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbAddTransaction.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.done -> {
                    validateData()
                    true
                }
                else -> false
            }
        }

        binding.tfAddTransactionDate.editText?.setOnClickListener {
            try {
                datePicker.show(supportFragmentManager, "tag")
            }
            catch (e: Exception) {}
        }

        binding.tfAddTransactionDate.setEndIconOnClickListener {
            try {
                datePicker.show(supportFragmentManager, "tag")
            }
            catch (e: Exception) {}
        }

        binding.tfAddTransactionTime.editText?.setOnClickListener {
            try {
                timePicker.show(supportFragmentManager, "tag")
            }
            catch (e: Exception) {}
        }

        binding.tfAddTransactionTime.setEndIconOnClickListener {
            try {
                timePicker.show(supportFragmentManager, "tag")
            }
            catch (e: Exception) {}
        }

        val spCategory = (binding.tfAddTransactionCategory.editText as? AutoCompleteTextView)
        spCategory?.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val string: StringWithTag = parent?.getItemAtPosition(position) as StringWithTag

                // store id of selected category
                selectedCategory = string.tag
                getCategoryRemainingBudget(firebaseUser.uid, currentAccountId, selectedCategory)
                loadSubcategories(firebaseUser.uid, currentAccountId, selectedCategory)
            }

        val spSubcategory = (binding.tfAddTransactionSubcategory.editText as? AutoCompleteTextView)
        spSubcategory?.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val string: StringWithTag = parent?.getItemAtPosition(position) as StringWithTag

                // store id of selected category
                selectedSubcategory = string.tag
            }

        // determine if transaction is expense or income
        binding.rgAddTransaction.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbTransactionExpense -> {
                    transactionType = 0
                }
                R.id.rbTransactionIncome -> {
                    transactionType = 1
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onResume() {
        super.onResume()

        hasSetBudget(firebaseUser.uid, currentAccountId)
        loadCategories(firebaseUser.uid, currentAccountId)
    }

    private fun loadData() {
        showProgressDialog()
        auth = Firebase.auth
        if (auth.currentUser != null) {
            firebaseUser = auth.currentUser!!

            val sharedPreferences = SharedPreferences(this)
            currentAccountId = sharedPreferences.accountId.toString()
            database = Firebase.database
        }
        else {
            sessionExpired()
        }
    }

    private fun hasSetBudget(uid: String, accountId: String) {
        databaseReference = database.getReference("accounts").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener {
                val monthlyBudget = it.child("account_monthly_budget").value.toString().toDouble()
                if (monthlyBudget <= 0) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(resources.getString(R.string.set_monthly_budget))
                        .setMessage(resources.getString(R.string.set_monthly_budget_mark))
                        .setPositiveButton(resources.getString(R.string.yes)) { _, _ -> setMonthlyBudget(accountId) }
                        .setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
                        .show()
                }
                else {
                    hasAllocatedBudget(uid, accountId)
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clAddTransaction, it.localizedMessage!!, 5000)
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

    private fun hasAllocatedBudget(uid: String, accountId: String) {
        databaseReference = database.getReference("budgets").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(resources.getString(R.string.allocate_budgets))
                        .setMessage(resources.getString(R.string.allocate_budgets_message))
                        .setPositiveButton(resources.getString(R.string.yes)) { _, _ -> allocateBudget() }
                        .setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
                        .show()
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clAddTransaction, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun allocateBudget() {
        startActivity(Intent(this, BudgetAddActivity::class.java))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun loadCategories(uid: String, accountId: String) {
        databaseReference = database.getReference("categories").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    hasNoCategories()
                }
                else {
                    val categories = mutableListOf<StringWithTag>()
                    for (child in snapshot.children) {
                        val category = child.getValue<Category>()
                        if (category != null) {
                            if (category.category_allocated) {
                                categories.add(
                                    StringWithTag(
                                        category.category_name!!,
                                        category.category_id!!,
                                        null,
                                        null
                                    )
                                )
                            }
                        }
                    }

                    if (categories.isNotEmpty()) {
                        // sort categories by name
                        categories.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.string })

                        // store category id
                        selectedCategory = categories.first().tag
                        getCategoryRemainingBudget(uid, accountId, selectedCategory)
                        loadSubcategories(firebaseUser.uid, currentAccountId, selectedCategory)

                        val adapter = ArrayAdapter(applicationContext, R.layout.list_item, categories)
                        val spinner = (binding.tfAddTransactionCategory.editText as? AutoCompleteTextView)
                        spinner?.setAdapter(adapter)
                        spinner?.setText(categories.first().toString(), false)
                    }
                }

            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clAddTransaction, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun hasNoCategories() {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.allocate_budgets))
            .setMessage(resources.getString(R.string.allocate_budgets_message))
            .setPositiveButton(resources.getString(R.string.yes)) { _, _ -> addCategories() }
            .setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
            .show()
    }

    private fun addCategories() {
        startActivity(Intent(this, CategoriesActivity::class.java))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun loadSubcategories(uid: String, accountId: String, categoryId: String) {
        databaseReference = database.getReference("subcategories").child(uid).child(accountId).child(categoryId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    binding.tfAddTransactionSubcategory.visibility = View.GONE
                }
                else {
                    binding.tfAddTransactionSubcategory.visibility = View.VISIBLE
                    val subcategories = mutableListOf<StringWithTag>()
                    for (child in snapshot.children) {
                        val subcategory = child.getValue<Subcategory>()
                        if (subcategory != null) {
                            subcategories.add(
                                StringWithTag(
                                    subcategory.subcategory_name.toString(),
                                    subcategory.subcategory_id.toString(),
                                    null,
                                    null
                                )
                            )
                        }
                    }

                    // sort categories by name
                    subcategories.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.string })

                    // store category id
                    selectedSubcategory = subcategories.first().tag

                    val adapter = ArrayAdapter(applicationContext, R.layout.list_item, subcategories)
                    val spinner = (binding.tfAddTransactionSubcategory.editText as? AutoCompleteTextView)
                    spinner?.setAdapter(adapter)
                    spinner?.setText(subcategories.first().toString(), false)
                }

                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clAddTransaction, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun getCategoryRemainingBudget(uid: String, accountId: String, categoryId: String) {
        databaseReference = database.getReference("budgets").child(uid).child(accountId).child(categoryId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val budget = snapshot.getValue<Budget>()
                if (budget != null) {
                    val remainingBudget = budget.budget_amount_total - budget.budget_amount_spent
                    val text = "Remaining budget: ₱" + String.format("%,.2f", remainingBudget)
                    binding.tfAddTransactionCategory.helperText = text
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clAddTransaction, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun setAmountPresetClickListener() {
        val gridLayout = findViewById<GridLayout>(R.id.glAmountPreset)
        for (i in 0 until gridLayout.childCount) {
            gridLayout.getChildAt(i).setOnClickListener { item ->
                val amount = item.tag.toString()
                binding.tfAddTransactionAmount.editText?.setText(amount)
            }
        }
    }

    private fun setDateTimePicker() {
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        calendar.timeInMillis = today
        calendar[Calendar.MONTH] = Calendar.JANUARY
        val janThisYear = calendar.timeInMillis

        val constraintsBuilder =
            CalendarConstraints.Builder()
                .setStart(janThisYear)
                .setEnd(today)

        datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { date ->
            val formattedDate =
                DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
                    .format(Date(date))

            binding.tfAddTransactionDate.editText?.setText(formattedDate)
            selectedDate = date
        }

        timePicker = MaterialTimePicker.Builder()
            .setTitleText("Select time")
            .setHour(12)
            .setMinute(0)
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val meridian: String
            var hour = timePicker.hour

            if (hour > 12) {
                hour = timePicker.hour - 12
                meridian = "PM"
            }
            else if (timePicker.hour == 12) {
                hour = timePicker.hour
                meridian = "PM"
            }
            else if (timePicker.hour == 0) {
                hour = timePicker.hour + 12
                meridian = "AM"
            }
            else { // < 12
                hour = timePicker.hour
                meridian = "AM"
            }

            val minute =
                if (timePicker.minute > 9) timePicker.minute
                else "0${timePicker.minute}"

            val time = "$hour:$minute $meridian"
            binding.tfAddTransactionTime.editText?.setText(time)

            val msHour: Long = timePicker.hour * milliseconds
            val msMinute: Long = timePicker.minute * milliseconds

            selectedHour = msHour
            selectedMinute = msMinute
        }
    }

    private fun inputObserver() {
        binding.tfAddTransactionAmount.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddTransactionAmount.error = getString(R.string.amount_empty)
            }
            else {
                binding.tfAddTransactionAmount.error = null
            }
        }

        binding.tfAddTransactionAmount.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) text?.clear()
        }

        binding.tfAddTransactionDate.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddTransactionDate.error = null
        }

        binding.tfAddTransactionTime.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddTransactionTime.error = null
        }

        binding.tfAddTransactionCategory.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddTransactionCategory.error = null
        }

        binding.tfAddTransactionSubcategory.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddTransactionSubcategory.error = null
        }

        binding.tfAddTransactionPaymentType.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddTransactionPaymentType.error = getString(R.string.payment_type_empty)
            }
            else {
                binding.tfAddTransactionPaymentType.error = null
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

        val amount = binding.tfAddTransactionAmount.editText?.text.toString().trim { it <= ' ' }
        val date = binding.tfAddTransactionDate.editText?.text.toString().trim { it <= ' ' }
        val time = binding.tfAddTransactionTime.editText?.text.toString().trim { it <= ' ' }
        val category = binding.tfAddTransactionCategory.editText?.text.toString().trim { it <= ' ' }
        val paymentType = binding.tfAddTransactionPaymentType.editText?.text.toString().trim { it <= ' ' }
        val notes = binding.tfAddTransactionNotes.editText?.text.toString().trim { it <= ' ' }
        val image: Long
        var errors = 0

        if (TextUtils.isEmpty(date)) {
            binding.tfAddTransactionDate.error = getString(R.string.date_empty)
            errors++
        }

        if (TextUtils.isEmpty(time)) {
            binding.tfAddTransactionTime.error = getString(R.string.time_empty)
            errors++
        }

        if (TextUtils.isEmpty(category)) {
            binding.tfAddTransactionCategory.error = getString(R.string.category_empty)
            errors++
        }

        if (TextUtils.isEmpty(paymentType)) {
            binding.tfAddTransactionPaymentType.error = getString(R.string.payment_type_empty)
            errors++
        }

        if (TextUtils.isEmpty(amount)) {
            binding.tfAddTransactionAmount.error = getString(R.string.amount_empty)
            errors++
        }

        else {
            if (amount.startsWith("0")) {
                binding.tfAddTransactionAmount.error = getString(R.string.budget_amount_0)
                errors++
            }
        }

        if (errors == 0) {
            deductAmount(amount.toDouble(), paymentType, notes, null)
        }
    }

    private fun storeImage() {

    }

    private fun deductAmount(amount: Double, paymentType: String, notes: String, imageUrl: Uri?) {
        databaseReference = database.getReference("budgets").child(firebaseUser.uid).child(currentAccountId).child(selectedCategory)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val budget = snapshot.getValue<Budget>()
                if (budget != null) {
                    val amountSpent = when (transactionType) {
                        0 -> budget.budget_amount_spent + amount
                        else -> budget.budget_amount_spent - amount
                    }

                    databaseReference.child("budget_amount_spent").setValue(amountSpent)
                        .addOnSuccessListener {
                            val categoryData = mapOf(
                                "id" to budget.category_id.toString(),
                                "name" to budget.category_name.toString(),
                                "color" to budget.category_color.toString(),
                                "icon" to budget.category_icon.toString()
                            )

                            if (selectedSubcategory != null) {
                                getSubcategory(amount, paymentType, notes, imageUrl, categoryData)
                            }
                            else {
                                addTransaction(amount, paymentType, notes, imageUrl, categoryData, null)
                            }
                        }
                        .addOnFailureListener {
                            hideProgressDialogAdd()
                            Snackbar
                                .make(binding.clAddTransaction, it.localizedMessage!!, 5000)
                                .show()
                        }
                }
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clAddTransaction, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun getSubcategory(amount: Double, paymentType: String, notes: String, imageUrl: Uri?, categoryData: Map<String, String>) {
        showProgressDialogAdd()
        databaseReference = database.getReference("subcategories").child(firebaseUser.uid).child(currentAccountId).child(selectedCategory).child(selectedSubcategory!!)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val subcategory = snapshot.getValue<Subcategory>()
                if (subcategory != null) {
                    val subcategoryData = mapOf(
                        "id" to subcategory.subcategory_id.toString(),
                        "name" to subcategory.subcategory_name.toString(),
                        "color" to subcategory.subcategory_color.toString(),
                        "icon" to subcategory.subcategory_icon.toString()
                    )

                    addTransaction(amount, paymentType, notes, imageUrl, categoryData, subcategoryData)
                }
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clAddTransaction, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun addTransaction(
        amount: Double,
        paymentType: String,
        notes: String,
        imageUrl: Uri?,
        category: Map<String, String>,
        subcategory: Map<String, String>?
    ) {
        showProgressDialogAdd()
        databaseReference = database.getReference("transactions").child(firebaseUser.uid).child(currentAccountId)
        val key = databaseReference.push().key
        val transaction = Transaction(
            key,
            amount,
            transactionType,
            paymentType,
            notes,
            null,
            selectedDate,
            selectedHour,
            selectedMinute,
            category["id"],
            category["name"],
            category["color"],
            category["icon"],
            subcategory?.get("id"),
            subcategory?.get("name"),
            subcategory?.get("color"),
            subcategory?.get("icon"),
        )

        databaseReference.child(key!!).setValue(transaction)
            .addOnSuccessListener {
                hideProgressDialogAdd()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clAddTransaction, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun sessionExpired() {
        Snackbar
            .make(
                binding.clAddTransaction,
                getString(R.string.session_expired),
                Snackbar.LENGTH_LONG
            )
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
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.pbTransactionAdd.visibility = View.VISIBLE
        binding.svTransactionAdd.visibility = View.GONE
        binding.ibScanImage.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbTransactionAdd.visibility = View.INVISIBLE
        binding.svTransactionAdd.visibility = View.VISIBLE
        binding.ibScanImage.visibility = View.VISIBLE
    }

    private fun showProgressDialogAdd() {
        binding.pbAddTransaction.visibility = View.VISIBLE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialogAdd() {
        binding.pbAddTransaction.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}