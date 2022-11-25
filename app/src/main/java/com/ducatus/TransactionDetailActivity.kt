package com.ducatus

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import androidx.core.widget.doOnTextChanged
import com.ducatus.data.Transaction
import com.ducatus.databinding.ActivityTransactionDetailBinding
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
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import com.yalantis.ucrop.UCrop
import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

class TransactionDetailActivity : AppCompatActivity() {
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityTransactionDetailBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var viewImageDialog: Dialog
    private lateinit var currentAccountId: String
    private lateinit var selectedTransaction: Transaction
    private val requestPickImage = 1
    private var firebaseUser: FirebaseUser? = null
    private var imageUri: Uri? = null
    private var dateTimeMap: MutableMap<String, Long> =
        mutableMapOf("date" to 0, "hour" to 0, "minute" to 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setDateTimePicker()
        inputObserver()
        loadData()

        binding.tbTransactionDetail.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbTransactionDetail.inflateMenu(R.menu.edit_done_menu)
        binding.tbTransactionDetail.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.delete -> {
                    firebaseUser?.let { confirmDelete(selectedTransaction) }
                    true
                }
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

            binding.tfTransactionDetailDate.editText?.setText(formattedDate)
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
            binding.tfTransactionDetailTime.editText?.setText(time)

            val milliseconds: Long = 1000
            val msHour: Long = timePicker.hour * milliseconds * 60 * 60
            val msMinute: Long = timePicker.minute * milliseconds * 60

            dateTimeMap["hour"] = msHour
            dateTimeMap["minute"] = msMinute
        }

        binding.tfTransactionDetailDate.editText?.setOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "tag")
            }
        }

        binding.tfTransactionDetailDate.setEndIconOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "tag")
            }
        }

        binding.tfTransactionDetailTime.editText?.setOnClickListener {
            if (!timePicker.isAdded) {
                timePicker.show(supportFragmentManager, "tag")
            }
        }

        binding.tfTransactionDetailTime.setEndIconOnClickListener {
            if (!timePicker.isAdded) {
                timePicker.show(supportFragmentManager, "tag")
            }
        }
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            viewImageDialog = Dialog(this)
            viewImageDialog.setContentView(R.layout.fragment_view_image_dialog)

            val sharedPreferences = SharedPreferences(this)
            currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            storage = FirebaseStorage.getInstance()
            storageReference = storage.getReference("transactions")

            val stringTransaction = intent.getStringExtra("transaction")
            selectedTransaction = Gson().fromJson(stringTransaction, Transaction::class.java)
            loadTransaction(firebaseUser!!.uid, selectedTransaction)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadTransaction(uid: String, transaction: Transaction) {
        when (transaction.type) {
            0 -> {
                binding.rbTransactionIncome.isChecked = false
                binding.rbTransactionExpense.isChecked = true
            }
            else -> {
                binding.rbTransactionExpense.isChecked = false
                binding.rbTransactionIncome.isChecked = true
            }
        }

        val zdt = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(transaction.date!!),
            ZoneId.systemDefault()
        )
        val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
        val formattedDate = dtf.format(zdt)

        val dtf2 = DateTimeFormatter.ofPattern("h:mm a")
        val formattedTime = dtf2.format(zdt)

        val milliseconds: Long = 1000
        val msHour: Long = zdt.hour * milliseconds * 60 * 60
        val msMinute: Long = zdt.minute * milliseconds * 60
        val startOfDay = zdt.with(LocalTime.MIN).toInstant().toEpochMilli()

        dateTimeMap["date"] = startOfDay
        dateTimeMap["hour"] = msHour
        dateTimeMap["minute"] = msMinute

        binding.tfTransactionDetailDate.editText?.setText(formattedDate)
        binding.tfTransactionDetailTime.editText?.setText(formattedTime)

        binding.tfTransactionDetailName.editText?.setText(transaction.name)
        binding.tfTransactionDetailCategory.editText?.setText(transaction.categoryName)

        if (transaction.subcategoryName != null) {
            binding.tfTransactionDetailSubcategory.editText?.setText(transaction.subcategoryName)
        }
        else {
            binding.tfTransactionDetailSubcategory.visibility = View.GONE
        }

        binding.tfTransactionDetailAmount.editText?.setText(transaction.amount.toInt().toString())

        binding.tfTransactionDetailPaymentType.editText?.setText(transaction.paymentType)
        binding.tfTransactionDetailNotes.editText?.setText(transaction.notes)

        if (transaction.imagePath == null) {
            binding.tfTransactionDetailImage.editText?.setOnClickListener {
                selectImage()
            }

            binding.tfTransactionDetailImage.setEndIconOnClickListener {
                selectImage()
            }
        }
        else {
            binding.tfTransactionDetailImage.editText?.setOnClickListener {
                viewImage(uid, transaction.imagePath!!)
            }
            binding.tfTransactionDetailImage.setEndIconOnClickListener {
                viewImage(uid, transaction.imagePath!!)
            }

            binding.tfTransactionDetailImage.editText?.setText(getString(R.string.preview_attached_image))
            binding.tfTransactionDetailImage.helperText = null
        }
    }

    private fun viewImage(uid: String, imagePath: String) {
        viewImageDialog.show()
        storageReference.child(uid).child(imagePath).downloadUrl
            .addOnSuccessListener { uri ->
                Picasso.get()
                    .load(uri)
                    .into(viewImageDialog.findViewById<ImageView>(R.id.ivViewImage))
            }
            .addOnFailureListener {
                viewImageDialog.dismiss()
                Snackbar
                    .make(binding.clTransactionDetail, it.localizedMessage!!,5000)
                    .show()
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
                    imageUri = uri
                    val imageName = uri.path?.let { File(it).name }
                    binding.tfTransactionDetailImage.editText?.setText(imageName)
                }
            }
        }
        else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                val error = UCrop.getError(data)
                Snackbar
                    .make(binding.clTransactionDetail, error.toString(), 5000)
                    .show()
            }
        }
    }

    private fun inputObserver() {
        binding.tfTransactionDetailName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfTransactionDetailName.error = getString(R.string.transaction_name_empty)
            }
            else {
                binding.tfTransactionDetailName.error = null
            }
        }
        binding.tfTransactionDetailPaymentType.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfTransactionDetailPaymentType.error = getString(R.string.payment_type_empty)
            }
            else {
                binding.tfTransactionDetailPaymentType.error = null
            }
        }
    }

    private fun validateData() {
        val name = binding.tfTransactionDetailName.editText?.text.toString().trim { it <= ' ' }
        val paymentType = binding.tfTransactionDetailPaymentType.editText?.text.toString().trim { it <= ' ' }
        var notes: String? = binding.tfTransactionDetailNotes.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (TextUtils.isEmpty(name)) {
            binding.tfTransactionDetailName.error = getString(R.string.transaction_name_empty)
            errors++
        }
        if (TextUtils.isEmpty(paymentType)) {
            binding.tfTransactionDetailPaymentType.error = getString(R.string.payment_type_empty)
            errors++
        }
        if (TextUtils.isEmpty(notes)) {
            notes = null
        }

        if (errors == 0) {
            firebaseUser?.let {
                val zdt = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(selectedTransaction.date!!),
                    ZoneId.systemDefault()
                )

                val milliseconds: Long = 1000
                val msHour: Long = zdt.hour * milliseconds * 60 * 60
                val msMinute: Long = zdt.minute * milliseconds * 60
                val startOfDay = zdt.with(LocalTime.MIN).toInstant().toEpochMilli()

                var changes = 0
                if (name.lowercase() != selectedTransaction.nameLower) changes++
                if (dateTimeMap["date"] != startOfDay) changes++
                if (dateTimeMap["hour"] != msHour) changes++
                if (dateTimeMap["minute"] != msMinute) changes++
                if (paymentType != selectedTransaction.paymentType) changes++
                if (notes != selectedTransaction.notes) changes++

                if (changes == 0) {
                    onBackPressed()
                }
                else {
                    showProgressDialog()

                    selectedTransaction.name = name
                    selectedTransaction.nameLower = name.lowercase()
                    selectedTransaction.date = dateTimeMap["date"]
                    selectedTransaction.paymentType = paymentType
                    selectedTransaction.notes = notes

                    if (imageUri != null) {
                        storeImage(it.uid, currentAccountId, imageUri!!, selectedTransaction)
                    }
                    else {
                        updateTransaction(it.uid, currentAccountId, selectedTransaction)
                    }
                }
            }
        }
    }

    private fun storeImage(uid: String, accountId: String, uri: Uri, transaction: Transaction) {
        val imageId = UUID.randomUUID().toString()
        transaction.imagePath = imageId
        storageReference.child(uid).child(imageId).putFile(uri)
            .addOnSuccessListener {
                updateTransaction(uid, accountId, transaction)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.clTransactionDetail, getString(R.string.store_image_error), 5000)
                    .show()
            }
    }

    private fun updateTransaction(uid: String, accountId: String, transaction: Transaction) {
        databaseReference =
            database
                .getReference("transactions")
                .child(uid)
                .child(accountId)
                .child(transaction.id!!)

        databaseReference.setValue(selectedTransaction)
            .addOnSuccessListener {
                hideProgressDialog()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun confirmDelete(transaction: Transaction) {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.delete_transaction))
            .setMessage(resources.getString(R.string.delete_transaction_message))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ ->
                firebaseUser?.let { deleteTransaction(it.uid, currentAccountId, transaction) }
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    private fun deleteTransaction(uid: String, accountId: String, transaction: Transaction) {
        showProgressDialogDelete()
        databaseReference = database.getReference("transactions").child(uid).child(accountId).child(transaction.id!!)
        databaseReference.removeValue()
            .addOnSuccessListener {
                if (transaction.imagePath != null) {
                    deleteImage(uid, accountId, transaction)
                }
                else {
                    updateBudget(uid, accountId, transaction)
                }
            }
            .addOnFailureListener {
                hideProgressDialogDelete()
                Snackbar
                    .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun deleteImage(uid: String, accountId: String, transaction: Transaction) {
        storageReference.child(uid).child(transaction.imagePath!!).delete()
            .addOnSuccessListener {
                updateBudget(uid, accountId, transaction)
            }
            .addOnFailureListener {
                hideProgressDialogDelete()
                Snackbar
                    .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun updateBudget(uid: String, accountId: String, transaction: Transaction) {
        databaseReference =
            database.getReference("budgets")
                .child(uid)
                .child(accountId)
                .child(transaction.categoryId!!)
                .child("amountSpent")

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val amountSpent = snapshot.value.toString().toDouble()
                var newAmount = when (transaction.type) {
                    0 -> amountSpent - transaction.amount
                    else -> amountSpent + transaction.amount
                }

                if (newAmount < 0.0) newAmount = 0.0
                databaseReference.setValue(newAmount)
                    .addOnSuccessListener {
                        updateAccount(uid, accountId, transaction)
                    }
                    .addOnFailureListener {
                        hideProgressDialogDelete()
                        Snackbar
                            .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                            .show()
                    }
            }
            .addOnFailureListener {
                hideProgressDialogDelete()
                Snackbar
                    .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun updateAccount(uid: String, accountId: String, transaction: Transaction) {
        databaseReference =
            database.getReference("accounts")
                .child(uid)
                .child(accountId)
                .child("remainingBalance")

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val remainingBalance = snapshot.value.toString().toDouble()
                val newBalance = when (transaction.type) {
                    0 -> remainingBalance + transaction.amount
                    else -> remainingBalance - transaction.amount
                }

                databaseReference.setValue(newBalance)
                    .addOnSuccessListener {
                        hideProgressDialogDelete()
                        onBackPressed()
                    }
                    .addOnFailureListener {
                        hideProgressDialogDelete()
                        Snackbar
                            .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                            .show()
                    }
            }
            .addOnFailureListener {
                hideProgressDialogDelete()
                Snackbar
                    .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                    .show()
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
        val bundle = Bundle()
        bundle.putString("title", getString(R.string.saving))

        actionDialog = ActionDialogFragment()
        actionDialog.arguments = bundle
        actionDialog.show(supportFragmentManager, "dialog")
    }

    private fun hideProgressDialog() {
        actionDialog.dismiss()
    }

    private fun showProgressDialogDelete() {
        val bundle = Bundle()
        bundle.putString("title", getString(R.string.deleting))

        actionDialog = ActionDialogFragment()
        actionDialog.arguments = bundle
        actionDialog.show(supportFragmentManager, "dialog")
    }

    private fun hideProgressDialogDelete() {
        actionDialog.dismiss()
    }
}