package com.ducatus

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.GridLayout
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import com.ducatus.data.Budget
import com.ducatus.data.Category
import com.ducatus.data.Subcategory
import com.ducatus.databinding.ActivityPlannedPaymentAddBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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

class PlannedPaymentAddActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityPlannedPaymentAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var datePicker: MaterialDatePicker<Long>
    private lateinit var currentAccountId: String
    private lateinit var selectedCategory: Category
    private var firebaseUser: FirebaseUser? = null
    private var remainingBudget: Double = 0.0
    private var selectedSubcategory: Subcategory? = null
    private var selectedFrequency = 0
    private var selectedDate: Long? = null
    private var selectedNotification = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlannedPaymentAddBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadData()
        setAmountPresetClickListener()
        setDatePicker()
        setFrequencies()
        setNotifications()
        inputObserver()

        binding.tbAddPlannedPayment.setNavigationOnClickListener {
            onBackPressed()
        }

        val spCategory = (binding.tfAddPlannedPaymentCategory.editText as? AutoCompleteTextView)
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

        val spSubcategory = (binding.tfAddPlannedPaymentSubcategory.editText as? AutoCompleteTextView)
        spSubcategory?.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val subcategory = parent?.getItemAtPosition(position) as SubcategoryWithTag

                // store data of selected subcategory
                selectedSubcategory = subcategory.subcategory
            }

        val spFrequencies = (binding.tfAddPlannedPaymentFrequency.editText as? AutoCompleteTextView)
        spFrequencies?.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                selectedFrequency = position
                if (position == 0) {
                    binding.tfAddPlannedPaymentRecurrence.visibility = View.GONE
                }
                else {
                    binding.tfAddPlannedPaymentRecurrence.visibility = View.VISIBLE
                }
            }

        val spNotifications = (binding.tfAddPlannedPaymentNotifications.editText as? AutoCompleteTextView)
        spNotifications?.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                selectedNotification = position
            }

        binding.tfAddPlannedPaymentRecurrence.editText?.setOnClickListener {
            val fragmentManager = supportFragmentManager
            val newFragment = PlannedPaymentRecurrenceDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        binding.tfAddPlannedPaymentRecurrence.setEndIconOnClickListener {
            val fragmentManager = supportFragmentManager
            val newFragment = PlannedPaymentRecurrenceDialogFragment()
            newFragment.show(fragmentManager, "dialog")
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
                    .make(binding.clAddPlannedPayment, it.localizedMessage!!, 5000)
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
                    .make(binding.clAddPlannedPayment, it.localizedMessage!!, 5000)
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
                    .make(binding.clAddPlannedPayment, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun hasNoCategories() {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.allocate_budgets))
            .setMessage(resources.getString(R.string.allocate_budgets_message))
            .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                startActivity(Intent(this, CategoriesActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            .setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
            .show()
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

                // remove categories that have <= 0 remaining budget
                for (index in indexes) {
                    categories.removeAt(index)
                }

                if (categories.isNotEmpty()) {
                    // sort categories by name
                    categories.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.category.name!! })

                    // store category data
                    selectedCategory = categories.first().category
                    getCategoryRemainingBudget(uid, accountId, selectedCategory.id!!)
                    loadSubcategories(uid, accountId, selectedCategory.id!!)

                    val adapter = ArrayAdapter(applicationContext, R.layout.list_item, categories)
                    val spinner = (binding.tfAddPlannedPaymentCategory.editText as? AutoCompleteTextView)
                    spinner?.setAdapter(adapter)
                    spinner?.setText(categories.first().toString(), false)
                }
                else {
                    binding.tfAddPlannedPaymentCategory.error = getString(R.string.categories_remaining_budget_empty)
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clAddPlannedPayment, it.localizedMessage!!, 5000)
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
                    binding.tfAddPlannedPaymentCategory.helperText = text
                }
                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clAddPlannedPayment, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun loadSubcategories(uid: String, accountId: String, categoryId: String) {
        databaseReference = database.getReference("subcategories").child(uid).child(accountId).child(categoryId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    binding.tfAddPlannedPaymentSubcategory.visibility = View.GONE
                }
                else {
                    binding.tfAddPlannedPaymentSubcategory.visibility = View.VISIBLE
                    val subcategories = mutableListOf<SubcategoryWithTag>()
                    for (child in snapshot.children) {
                        val subcategory = child.getValue<Subcategory>()
                        if (subcategory != null) {
                            subcategories.add(
                                SubcategoryWithTag(
                                    subcategory.name.toString(),
                                    subcategory
                                )
                            )
                        }
                    }

                    // sort categories by name
                    subcategories.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.subcategory.name!! })

                    // store subcategory data
                    selectedSubcategory = subcategories.first().subcategory

                    val adapter = ArrayAdapter(applicationContext, R.layout.list_item, subcategories)
                    val spinner = (binding.tfAddPlannedPaymentSubcategory.editText as? AutoCompleteTextView)
                    spinner?.setAdapter(adapter)
                    spinner?.setText(subcategories.first().toString(), false)
                }

                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clAddPlannedPayment, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun setAmountPresetClickListener() {
        val gridLayout = findViewById<GridLayout>(R.id.glAmountPreset)
        for (i in 0 until gridLayout.childCount) {
            gridLayout.getChildAt(i).setOnClickListener { item ->
                val amount = item.tag.toString()
                binding.tfAddPlannedPaymentAmount.editText?.setText(amount)
            }
        }
    }

    private fun setDatePicker() {
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

            binding.tfAddPlannedPaymentDate.editText?.setText(formattedDate)
            selectedDate = date
        }

        binding.tfAddPlannedPaymentDate.editText?.setOnClickListener {
            try {
                datePicker.show(supportFragmentManager, "tag")
            }
            catch (e: Exception) {}
        }

        binding.tfAddPlannedPaymentDate.setEndIconOnClickListener {
            try {
                datePicker.show(supportFragmentManager, "tag")
            }
            catch (e: Exception) {}
        }
    }

    private fun setFrequencies() {
        val frequencies = listOf("One Time", "Recurrent")
        val adapter = ArrayAdapter(this, R.layout.list_item, frequencies)
        (binding.tfAddPlannedPaymentFrequency.editText as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun setNotifications() {
        val notifications = listOf("None", "On due date", "1 day before", "3 days before", "1 week before")
        val adapter = ArrayAdapter(this, R.layout.list_item, notifications)
        (binding.tfAddPlannedPaymentNotifications.editText as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun inputObserver() {
        binding.tfAddPlannedPaymentAmount.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddPlannedPaymentAmount.error = getString(R.string.amount_empty)
            }
            else if (text.toString().toDouble() > remainingBudget) {
                binding.tfAddPlannedPaymentAmount.error = getString(R.string.amount_overflow)
            }
            else {
                binding.tfAddPlannedPaymentAmount.error = null
            }
        }

        binding.tfAddPlannedPaymentAmount.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) text?.clear()
        }

        binding.tfAddPlannedPaymentDate.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddPlannedPaymentDate.error = null
        }

        binding.tfAddPlannedPaymentCategory.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddPlannedPaymentCategory.error = null
        }

        binding.tfAddPlannedPaymentSubcategory.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddPlannedPaymentSubcategory.error = null
        }

        binding.tfAddPlannedPaymentPaymentType.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddPlannedPaymentPaymentType.error = getString(R.string.payment_type_empty)
            }
            else {
                binding.tfAddPlannedPaymentPaymentType.error = null
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

        val amount = binding.tfAddPlannedPaymentAmount.editText?.text.toString().trim { it <= ' ' }
        val date = binding.tfAddPlannedPaymentDate.editText?.text.toString().trim { it <= ' ' }
        val category = binding.tfAddPlannedPaymentCategory.editText?.text.toString().trim { it <= ' ' }
        val paymentType = binding.tfAddPlannedPaymentPaymentType.editText?.text.toString().trim { it <= ' ' }
        var notes: String? = binding.tfAddPlannedPaymentNotes.editText?.text.toString().trim { it <= ' ' }
        val image: Long
        var errors = 0

        if (TextUtils.isEmpty(date)) {
            binding.tfAddPlannedPaymentDate.error = getString(R.string.date_empty)
            errors++
        }

        if (TextUtils.isEmpty(category)) {
            binding.tfAddPlannedPaymentCategory.error = getString(R.string.category_empty)
            errors++
        }

        if (TextUtils.isEmpty(paymentType)) {
            binding.tfAddPlannedPaymentPaymentType.error = getString(R.string.payment_type_empty)
            errors++
        }

        if (TextUtils.isEmpty(notes)) {
            notes = null
        }

        if (TextUtils.isEmpty(amount)) {
            binding.tfAddPlannedPaymentAmount.error = getString(R.string.amount_empty)
            errors++
        }

        else {
            if (amount.startsWith("0")) {
                binding.tfAddPlannedPaymentAmount.error = getString(R.string.amount_starts_0)
                errors++
            }
            else if (amount.toDouble() > remainingBudget) {
                binding.tfAddPlannedPaymentAmount.error = getString(R.string.amount_overflow)
                errors++
            }
        }

        if (errors == 0) {

        }
    }

    private fun sessionExpired() {
        Snackbar
            .make(
                binding.clAddPlannedPayment,
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
        binding.pbPlannedPaymentMain.visibility = View.VISIBLE
        binding.svPlannedPaymentAdd.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbPlannedPaymentMain.visibility = View.INVISIBLE
        binding.svPlannedPaymentAdd.visibility = View.VISIBLE
    }

    private fun showProgressDialogAdd() {
        binding.pbAddPlannedPaymentAction.visibility = View.VISIBLE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialogAdd() {
        binding.pbAddPlannedPaymentAction.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}