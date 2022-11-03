package com.ducatus

import android.content.Context
import android.content.Intent
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
    private lateinit var timePicker: MaterialTimePicker
    private lateinit var currentAccountId: String
    private lateinit var selectedCategory: Category
    private var firebaseUser: FirebaseUser? = null
    private val milliseconds: Long = 60 * 1000
    private var selectedSubcategory: Subcategory? = null
    private var remainingBudget: Double = 0.0
    private var transactionType = 0
    private var dateTimeMap: MutableMap<String, Long> =
        mutableMapOf("date" to 0, "hour" to 0, "minute" to 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionAddBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadData()
        setAmountPresetClickListener()
        setDateTimePicker()
        inputObserver()

        binding.tbAddTransaction.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbAddTransaction.inflateMenu(R.menu.check_menu)
        binding.tbAddTransaction.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.done -> {
                    validateData()
                    true
                }
                else -> false
            }
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

        val spCategory = (binding.tfAddTransactionCategory.editText as? AutoCompleteTextView)
        spCategory?.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val category = parent?.getItemAtPosition(position) as CategoryWithTag

                // store data of selected category
                selectedCategory = category.category

                val categoryId = category.category.id!!
                firebaseUser?.let {
                    getCategoryRemainingBudget(it.uid, currentAccountId, categoryId)
                    loadSubcategories(it.uid, currentAccountId, categoryId)
                }
            }

        val spSubcategory = (binding.tfAddTransactionSubcategory.editText as? AutoCompleteTextView)
        spSubcategory?.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val subcategory = parent?.getItemAtPosition(position) as SubcategoryWithTag

                // store data of selected subcategory
                selectedSubcategory = subcategory.subcategory
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
            loadCategories(it.uid, currentAccountId)
        }
    }

    private fun loadData() {
        showProgressDialog()
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
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
            .addOnSuccessListener { snapshot ->
                val monthlyBudget = snapshot.child("monthlyBudget").value.toString().toDouble()
                if (monthlyBudget <= 0) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(resources.getString(R.string.set_monthly_budget))
                        .setMessage(resources.getString(R.string.set_monthly_budget_mark))
                        .setPositiveButton(resources.getString(R.string.yes)) { _, _ -> setMonthlyBudget() }
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

    private fun setMonthlyBudget() {
        val intent = Intent(this, AccountsActivity::class.java)
        intent.putExtra("setBudget", "set")
        intent.putExtra("accountId", currentAccountId)
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
                    val categories = mutableListOf<CategoryWithTag>()
                    for (child in snapshot.children) {
                        val category = child.getValue<Category>()
                        if (category != null) {
                            if (category.allocated) {
                                categories.add(
                                    CategoryWithTag(
                                        category.name!!,
                                        category
                                    )
                                )
                            }
                        }
                    }

                    // check categories that still have remaining budget
                    hasRemainingBudget(uid, accountId, categories)
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

    private fun hasRemainingBudget(uid: String, accountId: String, categories: MutableList<CategoryWithTag>) {
        databaseReference = database.getReference("budgets").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val indexes = mutableListOf<Int>()
                for (i in 0 until categories.size) {
                    val budget = snapshot.child(categories[i].category.id!!).getValue<Budget>()
                    if (budget != null) {
                        remainingBudget = budget.amountTotal - budget.amountSpent
                        if (remainingBudget <= 0) {
                            // store index of category to be removed later
                            // if category is removed now, loop will be out of bounds
                            indexes.add(i)
                        }
                    }
                }

                // remove categories that has <= 0 remaining budget
                for (index in indexes) {
                    categories.removeAt(index)
                }

                if (categories.isNotEmpty()) {
                    // sort categories by name
                    categories.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

                    // store category data
                    selectedCategory = categories.first().category

                    val categoryId = categories.first().category.id!!
                    getCategoryRemainingBudget(uid, accountId, categoryId)
                    loadSubcategories(uid, accountId, categoryId)

                    val adapter = ArrayAdapter(applicationContext, R.layout.list_item, categories)
                    val spinner = (binding.tfAddTransactionCategory.editText as? AutoCompleteTextView)
                    spinner?.setAdapter(adapter)
                    spinner?.setText(categories.first().toString(), false)
                }
                else {
                    binding.tfAddTransactionCategory.error = getString(R.string.categories_remaining_budget_empty)
                }
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
                    remainingBudget = budget.amountTotal - budget.amountSpent
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

    private fun loadSubcategories(uid: String, accountId: String, categoryId: String) {
        databaseReference = database.getReference("subcategories").child(uid).child(accountId).child(categoryId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    binding.tfAddTransactionSubcategory.visibility = View.GONE
                }
                else {
                    binding.tfAddTransactionSubcategory.visibility = View.VISIBLE
                    val subcategories = mutableListOf<SubcategoryWithTag>()
                    for (child in snapshot.children) {
                        val subcategory = child.getValue<Subcategory>()
                        if (subcategory != null) {
                            subcategories.add(
                                SubcategoryWithTag(
                                    subcategory.name!!,
                                    subcategory,
                                )
                            )
                        }
                    }

                    // sort categories by name
                    subcategories.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

                    // store category data
                    selectedSubcategory = subcategories.first().subcategory

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
            dateTimeMap["date"] = date
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

            dateTimeMap["hour"] = msHour
            dateTimeMap["minute"] = msMinute
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
    }

    private fun inputObserver() {
        binding.tfAddTransactionAmount.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddTransactionAmount.error = getString(R.string.amount_empty)
            }
            else if (text.toString().toDouble() > remainingBudget) {
                binding.tfAddTransactionAmount.error = getString(R.string.amount_overflow)
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
        var notes: String? = binding.tfAddTransactionNotes.editText?.text.toString().trim { it <= ' ' }
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

        if (TextUtils.isEmpty(notes)) {
            notes = null
        }

        if (TextUtils.isEmpty(amount)) {
            binding.tfAddTransactionAmount.error = getString(R.string.amount_empty)
            errors++
        }

        else {
            if (amount.startsWith("0")) {
                binding.tfAddTransactionAmount.error = getString(R.string.amount_starts_0)
                errors++
            }
            else if (amount.toDouble() > remainingBudget) {
                binding.tfAddTransactionAmount.error = getString(R.string.amount_overflow)
                errors++
            }
        }

        if (errors == 0) {
            val transactionData = mapOf(
                "amount" to amount,
                "paymentType" to paymentType,
                "notes" to notes,
//                "imageUri" to null,
                "date" to dateTimeMap["date"].toString(),
                "hour" to dateTimeMap["hour"].toString(),
                "minute" to dateTimeMap["minute"].toString(),
            )

            firebaseUser?.let { decreaseBudget(it.uid, currentAccountId, transactionData) }
        }
    }

    private fun storeImage() {

    }

    private fun decreaseBudget(uid: String, accountId: String, transactionData: Map<String, String?>) {
        showProgressDialogAdd()
        databaseReference = database.getReference("budgets")
            .child(uid).child(accountId).child(selectedCategory.id!!).child("amountSpent")

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val amountSpent = snapshot.value.toString().toDouble()
                val newAmountSpent = when (transactionType) {
                    0 -> amountSpent + transactionData["amount"]!!.toDouble()
                    else -> amountSpent - transactionData["amount"]!!.toDouble()
                }

                databaseReference.setValue(newAmountSpent)
                    .addOnSuccessListener {
                        decreaseAccountBalance(uid, accountId, transactionData)
                    }
                    .addOnFailureListener {
                        hideProgressDialogAdd()
                        Snackbar
                            .make(binding.clAddTransaction, it.localizedMessage!!, 5000)
                            .show()
                    }
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clAddTransaction, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun decreaseAccountBalance(uid: String, accountId: String, transactionData: Map<String, String?>) {
        databaseReference = database.getReference("accounts")
            .child(uid).child(accountId).child("remainingBalance")

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val remainingBalance = snapshot.value.toString().toDouble()
                val newRemainingBalance = when (transactionType) {
                    0 -> remainingBalance - transactionData["amount"]!!.toDouble()
                    else -> remainingBalance + transactionData["amount"]!!.toDouble()
                }

                databaseReference.setValue(newRemainingBalance)
                    .addOnSuccessListener {
                        addTransaction(uid, accountId, transactionData, selectedCategory, selectedSubcategory)
                    }
                    .addOnFailureListener {
                        hideProgressDialogAdd()
                        Snackbar
                            .make(binding.clAddTransaction, it.localizedMessage!!, 5000)
                            .show()
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
        uid: String,
        accountId: String,
        transactionData: Map<String, String?>,
        category: Category,
        subcategory: Subcategory?
    ) {
        databaseReference = database.getReference("transactions").child(uid).child(accountId)
        val key = databaseReference.push().key
        val transaction = Transaction(
            key!!,
            transactionData["amount"]!!.toDouble(),
            transactionType,
            transactionData["paymentType"],
            transactionData["notes"],
            null,
            transactionData["date"]!!.toLong(),
            transactionData["hour"]!!.toLong(),
            transactionData["minute"]!!.toLong(),
            category.id!!,
            category.name,
            category.name!!.lowercase(),
            category.color,
            category.icon,
            subcategory?.id,
            subcategory?.name,
            subcategory?.name?.lowercase(),
            subcategory?.color,
            subcategory?.icon,
        )

        databaseReference.child(key).setValue(transaction)
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
        binding.pbAddTransactionMain.visibility = View.VISIBLE
        binding.svTransactionAdd.visibility = View.GONE
        binding.ibScanImage.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbAddTransactionMain.visibility = View.INVISIBLE
        binding.svTransactionAdd.visibility = View.VISIBLE
        binding.ibScanImage.visibility = View.VISIBLE
    }

    private fun showProgressDialogAdd() {
        binding.pbAddTransactionAction.visibility = View.VISIBLE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialogAdd() {
        binding.pbAddTransactionAction.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}