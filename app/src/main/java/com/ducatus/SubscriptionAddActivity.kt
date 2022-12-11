package com.ducatus

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import com.ducatus.data.*
import com.ducatus.databinding.ActivitySubscriptionAddBinding
import com.ducatus.viewmodel.SubscriptionRecurrenceViewModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
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
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class SubscriptionAddActivity : AppCompatActivity() {
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySubscriptionAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var selectedCategory: Category
    private var firebaseUser: FirebaseUser? = null
    private var remainingBudget: Double = 0.0
    private var selectedSubcategory: Subcategory? = null
    private var selectedFrequency = 0
    private var selectedDate: Long = 0
    private var selectedNotification = 0
    private var selectedRecurrence = 0
    private val recurrenceViewModel: SubscriptionRecurrenceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionAddBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadData()
        setAmountPresetClickListener()
        setDatePicker()
        setFrequencies()
        setNotifications()
        inputObserver()

        binding.tbAddSubscription.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbAddSubscription.inflateMenu(R.menu.check_menu)
        binding.tbAddSubscription.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.done -> {
                    validateData()
                    true
                }
                else -> false
            }
        }

        val spCategory = (binding.tfAddSubscriptionCategory.editText as? AutoCompleteTextView)
        spCategory?.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val category = parent?.getItemAtPosition(position) as CategoryWithTag

                // store data of selected category
                selectedCategory = category.category

                val categoryId = category.category.id!!
                firebaseUser?.let {
                    getCategoryRemainingBudget(it.uid, sharedPreferences.accountId!!, categoryId)
                    loadSubcategories(it.uid, sharedPreferences.accountId!!, categoryId)
                }
            }

        val spSubcategory = (binding.tfAddSubscriptionSubcategory.editText as? AutoCompleteTextView)
        spSubcategory?.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val subcategory = parent?.getItemAtPosition(position) as SubcategoryWithTag

                // store data of selected subcategory
                selectedSubcategory = subcategory.subcategory
            }

        val spFrequencies = (binding.tfAddSubscriptionFrequency.editText as? AutoCompleteTextView)
        spFrequencies?.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                selectedFrequency = position
                if (position == 0) {
                    binding.tfAddSubscriptionDate.hint = getString(R.string.due_date)
                    binding.tfAddSubscriptionRecurrence.visibility = View.GONE
                    selectedRecurrence = 0
                }
                else {
                    binding.tfAddSubscriptionDate.hint = getString(R.string.start_date)
                    binding.tfAddSubscriptionRecurrence.visibility = View.VISIBLE
                }
            }

        val spNotifications = (binding.tfAddSubscriptionNotifications.editText as? AutoCompleteTextView)
        spNotifications?.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                selectedNotification = position
            }

        binding.tfAddSubscriptionRecurrence.editText?.setOnClickListener {
            val fragmentManager = supportFragmentManager
            val newFragment = SubscriptionRecurrenceDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        binding.tfAddSubscriptionRecurrence.setEndIconOnClickListener {
            val fragmentManager = supportFragmentManager
            val newFragment = SubscriptionRecurrenceDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        recurrenceViewModel.recurrence.observe(this) { recurrence ->
            selectedRecurrence = recurrence

            val text = "$recurrence month/s"
            binding.tfAddSubscriptionRecurrence.editText?.setText(text)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onResume() {
        super.onResume()

        firebaseUser?.let {
            hasSetBudget(it.uid, sharedPreferences.accountId!!)
            loadCategories(it.uid, sharedPreferences.accountId!!)
        }
    }

    private fun loadData() {
        showProgressDialog()
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            database = Firebase.database
            sharedPreferences = SharedPreferences(this)
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
                    .make(binding.clAddSubscription, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun setMonthlyBudget() {
        val intent = Intent(this, AccountsActivity::class.java)
        intent.putExtra("setBudget", "set")
        intent.putExtra("accountId", sharedPreferences.accountId!!)
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
                        .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                            startActivity(Intent(this, BudgetAddActivity::class.java))
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        }
                        .setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
                        .show()
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clAddSubscription, it.localizedMessage!!, 5000)
                    .show()
            }
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
                    .make(binding.clAddSubscription, getString(R.string.load_categories_error), 5000)
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
                    val spinner = (binding.tfAddSubscriptionCategory.editText as? AutoCompleteTextView)
                    spinner?.setAdapter(adapter)
                    spinner?.setText(categories.first().toString(), false)
                }
                else {
                    binding.tfAddSubscriptionCategory.error = getString(R.string.categories_remaining_budget_empty)
                    hideProgressDialog()
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clAddSubscription, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun getCategoryRemainingBudget(uid: String, accountId: String, categoryId: String) {
        databaseReference =
            database.getReference("budgets")
                .child(uid)
                .child(accountId)
                .child(categoryId)

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val budget = snapshot.getValue<Budget>()
                if (budget != null) {
                    remainingBudget = budget.amountTotal - budget.amountSpent
                    val text = "Remaining budget: ₱" + String.format("%,.2f", remainingBudget)
                    binding.tfAddSubscriptionCategory.helperText = text
                }
                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clAddSubscription, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun loadSubcategories(uid: String, accountId: String, categoryId: String) {
        database.getReference("subcategories")
            .child(uid)
            .child(accountId)
            .child(categoryId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    selectedSubcategory = null // make sure no subcategory
                    binding.tfAddSubscriptionSubcategory.visibility = View.GONE
                }
                else {
                    binding.tfAddSubscriptionSubcategory.visibility = View.VISIBLE
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

                    val adapter = ArrayAdapter(applicationContext, R.layout.list_item, subcategories)
                    val spinner = (binding.tfAddSubscriptionSubcategory.editText as? AutoCompleteTextView)
                    spinner?.setAdapter(adapter)
                }

                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clAddSubscription, getString(R.string.load_subcategories_error), 5000)
                    .show()
            }
    }

    private fun setAmountPresetClickListener() {
        val gridLayout = findViewById<GridLayout>(R.id.glAmountPreset)
        for (i in 0 until gridLayout.childCount) {
            gridLayout.getChildAt(i).setOnClickListener { item ->
                val amount = item.tag.toString()
                binding.tfAddSubscriptionAmount.editText?.setText(amount)
            }
        }
    }

    private fun setDatePicker() {
        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )

        val nextFiveYears = zdtToday.plusYears(5)
        val startDate = zdtToday.toInstant().toEpochMilli()
        val endDate = nextFiveYears.toInstant().toEpochMilli()

        val constraintsBuilder =
            CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .setStart(startDate)
                .setEnd(endDate)

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { date ->
            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(date),
                ZoneId.systemDefault()
            )
            val endOfDay = zdt.with(LocalTime.MAX)
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val formattedDate = dtf.format(endOfDay)

            binding.tfAddSubscriptionDate.editText?.setText(formattedDate)
            selectedDate = endOfDay.toInstant().toEpochMilli()
        }

        binding.tfAddSubscriptionDate.editText?.setOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "tag")
            }
        }

        binding.tfAddSubscriptionDate.setEndIconOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "tag")
            }
        }
    }

    private fun setFrequencies() {
        val frequencies = listOf("One Time", "Recurrent")
        val adapter = ArrayAdapter(this, R.layout.list_item, frequencies)
        (binding.tfAddSubscriptionFrequency.editText as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun setNotifications() {
        val notifications = listOf("None", "On due date", "1 day before", "3 days before", "1 week before")
        val adapter = ArrayAdapter(this, R.layout.list_item, notifications)
        (binding.tfAddSubscriptionNotifications.editText as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun inputObserver() {
        binding.tfAddSubscriptionAmount.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddSubscriptionAmount.error = getString(R.string.amount_empty)
            }
            else if (text.toString().toDouble() > remainingBudget) {
                binding.tfAddSubscriptionAmount.error = getString(R.string.amount_overflow)
            }
            else {
                binding.tfAddSubscriptionAmount.error = null
            }
        }

        binding.tfAddSubscriptionAmount.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) text?.clear()
        }

        binding.tfAddSubscriptionName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddSubscriptionName.error = getString(R.string.subscription_name_empty)
            }
            else {
                binding.tfAddSubscriptionName.error = null
            }
        }

        binding.tfAddSubscriptionCategory.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddSubscriptionCategory.error = null
        }

        binding.tfAddSubscriptionSubcategory.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddSubscriptionSubcategory.error = null
        }

        binding.tfAddSubscriptionPaymentType.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddSubscriptionPaymentType.error = getString(R.string.payment_type_empty)
            }
            else {
                binding.tfAddSubscriptionPaymentType.error = null
            }
        }

        binding.tfAddSubscriptionFrequency.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddSubscriptionFrequency.error = null
        }

        binding.tfAddSubscriptionRecurrence.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddSubscriptionRecurrence.error = null
        }

        binding.tfAddSubscriptionDate.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddSubscriptionDate.error = null
        }

        binding.tfAddSubscriptionNotifications.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddSubscriptionNotifications.error = null
        }
    }

    private fun validateData() {
        // hide keyboard
        try {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        val amount = binding.tfAddSubscriptionAmount.editText?.text.toString().trim { it <= ' ' }
        val name = binding.tfAddSubscriptionName.editText?.text.toString().trim { it <= ' ' }
        val category = binding.tfAddSubscriptionCategory.editText?.text.toString().trim { it <= ' ' }
        val paymentType = binding.tfAddSubscriptionPaymentType.editText?.text.toString().trim { it <= ' ' }
        val frequency = binding.tfAddSubscriptionFrequency.editText?.text.toString().trim { it <= ' ' }
        val recurrence = binding.tfAddSubscriptionRecurrence.editText?.text.toString().trim { it <= ' ' }
        val date = binding.tfAddSubscriptionDate.editText?.text.toString().trim { it <= ' ' }
        val notifications = binding.tfAddSubscriptionNotifications.editText?.text.toString().trim { it <= ' ' }
        var notes: String? = binding.tfAddSubscriptionNotes.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (TextUtils.isEmpty(name)) {
            binding.tfAddSubscriptionName.error = getString(R.string.subscription_name_empty)
            errors++
        }

        if (TextUtils.isEmpty(category)) {
            binding.tfAddSubscriptionCategory.error = getString(R.string.category_empty)
            errors++
        }

        if (TextUtils.isEmpty(paymentType)) {
            binding.tfAddSubscriptionPaymentType.error = getString(R.string.payment_type_empty)
            errors++
        }

        if (TextUtils.isEmpty(frequency)) {
            binding.tfAddSubscriptionFrequency.error = getString(R.string.frequency_empty)
            errors++
        }

        if (selectedFrequency == 1 && TextUtils.isEmpty(recurrence)) {
            binding.tfAddSubscriptionRecurrence.error = getString(R.string.select_recurrence)
            errors++
        }

        if (TextUtils.isEmpty(date)) {
            binding.tfAddSubscriptionDate.error = getString(R.string.date_empty)
            errors++
        }

        if (TextUtils.isEmpty(notifications)) {
            binding.tfAddSubscriptionNotifications.error = getString(R.string.notification_empty)
            errors++
        }

        if (TextUtils.isEmpty(notes)) {
            notes = null
        }

        if (TextUtils.isEmpty(amount)) {
            binding.tfAddSubscriptionAmount.error = getString(R.string.amount_empty)
            errors++
        }
        else {
            if (amount.startsWith("0")) {
                binding.tfAddSubscriptionAmount.error = getString(R.string.amount_starts_0)
                errors++
            }
            else if (amount.toDouble() > remainingBudget) {
                binding.tfAddSubscriptionAmount.error = getString(R.string.amount_overflow)
                errors++
            }
        }

        if (errors == 0) {
            firebaseUser?.let {
                showProgressDialogAdd()
                val zdtToday = ZonedDateTime.ofInstant(
                    Instant.now(),
                    ZoneId.systemDefault()
                ).toInstant().toEpochMilli()

                val renewsAt = getRenewalDate(selectedFrequency, selectedRecurrence)

                val subscription = Subscription(
                    null,
                    name,
                    name.lowercase(),
                    amount.toDouble(),
                    paymentType,
                    selectedFrequency,
                    zdtToday.toString(),
                    zdtToday,
                    selectedDate,
                    selectedNotification,
                    selectedRecurrence,
                    renewsAt,
                    null,
                    notes,
                    selectedCategory.id,
                    selectedCategory.name,
                    selectedCategory.nameLower,
                    selectedCategory.color,
                    selectedCategory.icon,
                    selectedSubcategory?.id,
                    selectedSubcategory?.name,
                    selectedSubcategory?.nameLower,
                    selectedSubcategory?.color,
                    selectedSubcategory?.icon
                )

                decreaseBudget(it.uid, sharedPreferences.accountId!!, subscription)
            }
        }
    }

    private fun getRenewalDate(frequency: Int, recurrence: Int): Long? {
        val date = when (frequency) {
            0 -> {
                null
            }
            else -> {
                val zdt = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(selectedDate),
                    ZoneId.systemDefault()
                )

                zdt.with(LocalTime.MAX).plusMonths(recurrence.toLong()).toInstant().toEpochMilli()
            }
        }

        return date
    }

    private fun decreaseBudget(uid: String, accountId: String, subscription: Subscription) {
        val budgetsReference =
            database.getReference("budgets")
                .child(uid)
                .child(accountId)
                .child(subscription.categoryId!!)

        budgetsReference.get()
            .addOnSuccessListener { snapshot ->
                val budget = snapshot.getValue<Budget>()
                if (budget != null) {
                    val zdt = ZonedDateTime.ofInstant(
                        Instant.now(),
                        ZoneId.systemDefault()
                    )

                    budget.amountSpent += subscription.amount
                    budget.updatedAt = zdt.toInstant().toEpochMilli()

                    budgetsReference.setValue(budget)
                        .addOnSuccessListener {
                            decreaseAccountBalance(uid, accountId, subscription)
                        }
                        .addOnFailureListener {
                            hideProgressDialogAdd()
                            Snackbar
                                .make(binding.clAddSubscription, it.localizedMessage!!, 5000)
                                .show()
                        }
                }
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clAddSubscription, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun decreaseAccountBalance(uid: String, accountId: String, subscription: Subscription) {
        val accountsReference =
            database.getReference("accounts")
                .child(uid)
                .child(accountId)
                .child("remainingBalance")

        accountsReference.get()
            .addOnSuccessListener { snapshot ->
                val remainingBalance = snapshot.value.toString().toDouble()
                val newRemainingBalance = remainingBalance - subscription.amount

                accountsReference.setValue(newRemainingBalance)
                    .addOnSuccessListener {
                        addSubscription(uid, accountId, subscription)
                    }
                    .addOnFailureListener {
                        hideProgressDialogAdd()
                        Snackbar
                            .make(binding.clAddSubscription, it.localizedMessage!!, 5000)
                            .show()
                    }
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clAddSubscription, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun addSubscription(uid: String, accountId: String, subscription: Subscription) {
        val subscriptionsReference = database.getReference("subscriptions").child(uid).child(accountId)
        val key = subscriptionsReference.push().key
        subscription.id = key!!

        subscriptionsReference.child(key).setValue(subscription)
            .addOnSuccessListener {
                addSubscriptionHistory(uid, accountId, subscription)
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clAddSubscription, getString(R.string.add_subscription_error), 5000)
                    .show()
            }
    }

    private fun addSubscriptionHistory(uid: String, accountId: String, subscription: Subscription) {
        val subscriptionHistoryReference =
            database.getReference("subscriptionHistory")
                .child(uid)
                .child(accountId)
                .child(subscription.id!!)

        val key = subscriptionHistoryReference.push().key!!
        val subscriptionHistory = SubscriptionHistory(
            key,
            subscription.amount,
            null,
            null,
            subscription.id,
            System.currentTimeMillis().toInt()
        )

        // 0 for one time, 1 for recurring
        when (subscription.frequency) {
            0 -> subscriptionHistory.dueAt = subscription.dueDate
            1 -> subscriptionHistory.dueAt = subscription.renewsAt
        }

        subscriptionHistoryReference.child(key).setValue(subscriptionHistory)
            .addOnSuccessListener {
                if (subscription.notification != 0) {
                    when (subscription.notification) {
                        1 -> scheduleNotification(this, 0, accountId, subscription, subscriptionHistory)
                        2 -> scheduleNotification(this, 1, accountId, subscription, subscriptionHistory)
                        3 -> scheduleNotification(this, 3, accountId, subscription, subscriptionHistory)
                        4 -> scheduleNotification(this, 7, accountId, subscription, subscriptionHistory)
                    }
                }

                hideProgressDialogAdd()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clAddSubscription, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun createNotificationChannel() {
        val name = "Subscriptions"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(sharedPreferences.subscriptionsChannelId, name, importance)

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

    private fun scheduleNotification(
        context: Context,
        delay: Long,
        accountId: String,
        subscription: Subscription,
        subscriptionHistory: SubscriptionHistory
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.subscriptionsChannelId)
        if (notificationChannel == null) {
            createNotificationChannel()
            notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.subscriptionsChannelId)
        }

        // create notification if channel is enabled
        // else do not create
        if (notificationChannel.importance != NotificationManager.IMPORTANCE_NONE) {
            enableReceiver(context)

            // pass to broadcast receiver
            val notificationIntent = Intent(context, NotificationReceiver::class.java)

            val dtf = DateTimeFormatter.ofPattern("MMM dd")
            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(subscriptionHistory.dueAt!!),
                ZoneId.systemDefault()
            ).with(LocalTime.MIN)

            val date = getElapsedTime(subscriptionHistory.dueAt!!)
            val formattedDate = dtf.format(zdt)
            val formattedAmount = "₱" + String.format("%,.2f", subscription.amount)

            val title = "Payment for ${subscription.name} due $date"
            val message = "Confirm your payment of $formattedAmount on or before $formattedDate."
            val notificationId = subscriptionHistory.notificationId!!

            notificationIntent.action = "com.ducatus.SUBSCRIPTION"
            notificationIntent.putExtra(titleExtra, title)
            notificationIntent.putExtra(messageExtra, message)
            notificationIntent.putExtra(notificationIdExtra, notificationId)
            notificationIntent.putExtra(itemIdExtra, subscription.id)
            notificationIntent.putExtra(accountIdExtra, accountId)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationDate = zdt.minusDays(delay).toInstant().toEpochMilli()

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.RTC_WAKEUP, notificationDate, pendingIntent)
        }
    }

    private fun getElapsedTime(date: Long): String {
        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )
        val zdt = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(date),
            ZoneId.systemDefault()
        )

        val startDate = zdtToday.toInstant()
        val endDate = zdt.toInstant()

        val elapsedDays = ChronoUnit.DAYS.between(startDate, endDate)
        val dateText =
            if (elapsedDays > 0) {
                if (elapsedDays.toInt() == 1) {
                    "in $elapsedDays day"
                }
                else {
                    "in $elapsedDays days"
                }
            }
            else if (elapsedDays.toInt() == 0){
                "today"
            }
            else if (elapsedDays < 1){
                "${elapsedDays * -1} days ago"
            }
            else {
                "${elapsedDays * -1} day ago"
            }

        return dateText
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
        binding.pbSubscriptionMain.visibility = View.VISIBLE
        binding.svSubscriptionAdd.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbSubscriptionMain.visibility = View.INVISIBLE
        binding.svSubscriptionAdd.visibility = View.VISIBLE
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