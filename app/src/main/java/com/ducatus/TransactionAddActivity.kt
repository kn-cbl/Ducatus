package com.ducatus

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import com.ducatus.data.*
import com.ducatus.databinding.ActivityTransactionAddBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import com.yalantis.ucrop.UCrop
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class TransactionAddActivity : AppCompatActivity() {
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityTransactionAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var currentAccountId: String
    private lateinit var selectedCategory: Category
    private val requestPickImage = 1
    private var firebaseUser: FirebaseUser? = null
    private var selectedSubcategory: Subcategory? = null
    private var selectedCategoryWithTag: CategoryWithTag? = null
    private var remainingBudget: Double = 0.0
    private var transactionType = 0
    private var imageUri: Uri? = null
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
                    binding.rbTransactionExpense.setTextColor(ContextCompat.getColor(this, R.color.off_white))
                    binding.rbTransactionIncome.setTextColor(ContextCompat.getColor(this, R.color.bright_blue))
                    transactionType = 0
                }
                R.id.rbTransactionIncome -> {
                    binding.rbTransactionIncome.setTextColor(ContextCompat.getColor(this, R.color.off_white))
                    binding.rbTransactionExpense.setTextColor(ContextCompat.getColor(this, R.color.bright_blue))
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
                selectedCategoryWithTag = category

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

        binding.tfAddTransactionImage.editText?.setOnClickListener {
            selectImage()
        }

        binding.tfAddTransactionImage.setEndIconOnClickListener {
            selectImage()
        }

        binding.fabScanImage.setOnClickListener {
            selectImage()
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
            database = Firebase.database
            sharedPreferences = SharedPreferences(this)
            currentAccountId = sharedPreferences.accountId.toString()
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
                    .make(binding.clAddTransaction, getString(R.string.load_categories_error), 5000)
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

                    if (selectedCategoryWithTag != null) {
                        selectedCategory = selectedCategoryWithTag!!.category
                        spinner?.setText(selectedCategoryWithTag.toString(), false)
                    }
                    else {
                        spinner?.setText(categories.first().toString(), false)
                    }
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
                    selectedSubcategory = null // make sure no subcategory
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

                    val adapter = ArrayAdapter(applicationContext, R.layout.list_item, subcategories)
                    val spinner = (binding.tfAddTransactionSubcategory.editText as? AutoCompleteTextView)
                    spinner?.setAdapter(adapter)
                }

                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clAddTransaction, getString(R.string.load_subcategories_error), 5000)
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
        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )

        val janThisYear = ZonedDateTime.of(zdtToday.year, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())
        val lastTwentyYears = janThisYear.minusYears(20)

        val startDate = lastTwentyYears.toInstant().toEpochMilli()
        val endDate = zdtToday.toInstant().toEpochMilli()

        val constraintsBuilder =
            CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
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
            val startOfDay = zdt.with(LocalTime.MIN)
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val formattedDate = dtf.format(startOfDay)

            binding.tfAddTransactionDate.editText?.setText(formattedDate)
            dateTimeMap["date"] = startOfDay.toInstant().toEpochMilli()
        }

        val timePicker = MaterialTimePicker.Builder()
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

            val milliseconds: Long = 1000
            val msHour: Long = timePicker.hour * milliseconds * 60 * 60
            val msMinute: Long = timePicker.minute * milliseconds * 60

            dateTimeMap["hour"] = msHour
            dateTimeMap["minute"] = msMinute
        }

        binding.tfAddTransactionDate.editText?.setOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "tag")
            }
        }

        binding.tfAddTransactionDate.setEndIconOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "tag")
            }
        }

        binding.tfAddTransactionTime.editText?.setOnClickListener {
            if (!timePicker.isAdded) {
                timePicker.show(supportFragmentManager, "tag")
            }
        }

        binding.tfAddTransactionTime.setEndIconOnClickListener {
            if (!timePicker.isAdded) {
                timePicker.show(supportFragmentManager, "tag")
            }
        }
    }

    private fun selectImage() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, requestPickImage)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestPickImage && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                var imageName = "transaction_receipt.jpg"
                contentResolver.query(uri, null, null, null, null)
                    ?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        imageName = cursor.getString(nameIndex)
                    }

                val options = UCrop.Options()
                options.setHideBottomControls(false)
                options.setFreeStyleCropEnabled(true)

                UCrop.of(uri, Uri.fromFile(File(cacheDir, imageName)))
                    .withMaxResultSize(480, 480)
                    .withOptions(options)
                    .start(this)
            }
        }
        else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            data?.let { returnIntent ->
                val resultUri = UCrop.getOutput(returnIntent)
                resultUri?.let { uri ->
                    val imageName = uri.path?.let { File(it).name }
                    binding.tfAddTransactionImage.editText?.setText(imageName)

                    imageUri = uri
                    Picasso.get()
                        .load(uri)
                        .into(binding.ivAddtransactionImage)

//                    contentResolver.query(uri, null, null, null, null)
//                        ?.use { cursor ->
//
//
//                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
//                            cursor.moveToFirst()
//                            binding.tfAddTransactionAttachment.editText?.setText(cursor.getString(nameIndex))
//                        }
                }
            }
        }
        else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                val error = UCrop.getError(data)
                Snackbar
                    .make(binding.clAddTransaction, error.toString(), 5000)
                    .show()
            }
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

        binding.tfAddTransactionName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddTransactionName.error = getString(R.string.transaction_name_empty)
            }
            else {
                binding.tfAddTransactionName.error = null
            }
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

        val name = binding.tfAddTransactionName.editText?.text.toString().trim { it <= ' ' }
        val amount = binding.tfAddTransactionAmount.editText?.text.toString().trim { it <= ' ' }
        val date = binding.tfAddTransactionDate.editText?.text.toString().trim { it <= ' ' }
        val time = binding.tfAddTransactionTime.editText?.text.toString().trim { it <= ' ' }
        val category = binding.tfAddTransactionCategory.editText?.text.toString().trim { it <= ' ' }
        val paymentType = binding.tfAddTransactionPaymentType.editText?.text.toString().trim { it <= ' ' }
        var notes: String? = binding.tfAddTransactionNotes.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (TextUtils.isEmpty(name)) {
            binding.tfAddTransactionName.error = getString(R.string.transaction_name_empty)
            errors++
        }

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
            firebaseUser?.let {
                showProgressDialogAdd()

                val totalDate = dateTimeMap["date"]!! + dateTimeMap["hour"]!! + dateTimeMap["minute"]!!
                val transaction = Transaction(
                    null,
                    name,
                    name.lowercase(),
                    amount.toDouble(),
                    transactionType,
                    paymentType,
                    notes,
                    null,
                    totalDate,
                    totalDate.toString(),
                    selectedCategory.id!!,
                    selectedCategory.name,
                    selectedCategory.nameLower,
                    selectedCategory.color,
                    selectedCategory.icon,
                    selectedSubcategory?.id,
                    selectedSubcategory?.name,
                    selectedSubcategory?.nameLower,
                    selectedSubcategory?.color,
                    selectedSubcategory?.icon,
                )

                if (imageUri != null) {
                    storeImage(it.uid, currentAccountId, imageUri!!, transaction)
                }
                else {
                    decreaseBudget(it.uid, currentAccountId, transaction)
                }
            }
        }
    }

    private fun storeImage(uid: String, accountId: String, uri: Uri, transaction: Transaction) {
//            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(uri.toString()))
//            val outputStream = ByteArrayOutputStream()
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
//            val data = outputStream.toByteArray()

        // generate unique id
        val imageId = UUID.randomUUID().toString()
        transaction.imagePath = imageId

        storage = FirebaseStorage.getInstance()
        storageReference = storage.getReference("transactions").child(uid)
        storageReference.child(imageId).putFile(uri)
            .addOnSuccessListener {
                decreaseBudget(uid, accountId, transaction)
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clAddTransaction, getString(R.string.store_image_error), 5000)
                    .show()
            }
    }

    private fun decreaseBudget(uid: String, accountId: String, transaction: Transaction) {
        databaseReference =
            database.getReference("budgets")
                .child(uid)
                .child(accountId)
                .child(transaction.categoryId!!)

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val budget = snapshot.getValue<Budget>()
                if (budget != null) {
                    val newAmountSpent = when (transaction.type) {
                        0 -> budget.amountSpent + transaction.amount
                        else -> budget.amountSpent - transaction.amount
                    }

                    budget.amountSpent = newAmountSpent

                    val zdt = ZonedDateTime.ofInstant(
                        Instant.now(),
                        ZoneId.systemDefault()
                    )

                    budget.updatedAt = zdt.toInstant().toEpochMilli()

                    databaseReference.setValue(budget)
                        .addOnSuccessListener {
                            decreaseAccountBalance(uid, accountId, transaction)
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

    private fun decreaseAccountBalance(uid: String, accountId: String, transaction: Transaction) {
        databaseReference =
            database.getReference("accounts")
                .child(uid)
                .child(accountId)
                .child("remainingBalance")

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val remainingBalance = snapshot.value.toString().toDouble()
                val newRemainingBalance = when (transaction.type) {
                    0 -> remainingBalance - transaction.amount
                    else -> remainingBalance + transaction.amount
                }

                databaseReference.setValue(newRemainingBalance)
                    .addOnSuccessListener {
                        addTransaction(uid, accountId, transaction)
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

    private fun addTransaction(uid: String, accountId: String, transaction: Transaction) {
        databaseReference = database.getReference("transactions").child(uid).child(accountId)

        val key = databaseReference.push().key
        transaction.id = key!!

        databaseReference.child(key).setValue(transaction)
            .addOnSuccessListener {
                cancelNotification(this, accountId)
                hideProgressDialogAdd()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialogAdd()
                Snackbar
                    .make(binding.clAddTransaction, getString(R.string.add_transaction_error), 5000)
                    .show()
            }
    }

    private fun cancelNotification(context: Context, accountId: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.expensesChannelId)
        if (notificationChannel != null) {
            val notificationIntent = Intent(context, NotificationReceiver::class.java)
            notificationIntent.action = "com.ducatus.EXPENSE"

            val zdt = ZonedDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            )
            val notificationId = zdt.dayOfYear

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)

            // schedule future notifications
            if (notificationChannel.importance != NotificationManager.IMPORTANCE_NONE) {
                scheduleNotifications(context, accountId)
            }
        }
    }

    private fun enableReceiver(context: Context) {
        val receiver = ComponentName(context, NotificationReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun scheduleNotifications(context: Context, accountId: String) {
        enableReceiver(context)

        // pass to broadcast receiver
        val notificationIntent = Intent(context, NotificationReceiver::class.java)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val zdt = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )
        val eightPm = zdt.with(LocalTime.MIN).plusHours(20)

        val title = "Record your expenses for today"
        val message = "Tap here to open Ducatus."

        for (i in 0 until 14) {
            val notificationId = zdt.dayOfYear.plus(i)

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

            // set notifications for 2 weeks
            val day = eightPm.plusDays(i.toLong()).toInstant().toEpochMilli()
            alarmManager.set(AlarmManager.RTC_WAKEUP, day, pendingIntent)
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
        binding.pbAddTransactionMain.visibility = View.VISIBLE
        binding.svTransactionAdd.visibility = View.GONE
        binding.fabScanImage.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbAddTransactionMain.visibility = View.INVISIBLE
        binding.svTransactionAdd.visibility = View.VISIBLE
        binding.fabScanImage.visibility = View.VISIBLE
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