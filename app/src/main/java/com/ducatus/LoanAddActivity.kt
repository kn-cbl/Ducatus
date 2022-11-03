package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.GridLayout
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import com.ducatus.data.Loan
import com.ducatus.databinding.ActivityLoanAddBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.DateFormat
import java.util.*

class LoanAddActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoanAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var datePicker: MaterialDatePicker<Long>
    private lateinit var timePicker: MaterialTimePicker
    private lateinit var dateTimeMap: MutableMap<String, Long>
    private val milliseconds: Long = 60 * 1000
    private var loanType: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoanAddBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadData()
        setAmountPresetClickListener()
        setDateTimePicker()
        inputObserver()

        binding.tbAddLoan.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbAddLoan.inflateMenu(R.menu.check_menu)
        binding.tbAddLoan.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.done -> {
                    validateData()
                    true
                }
                else -> false
            }
        }

        // determine if transaction is expense or income
        binding.rgAddLoan.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbLoanLend -> {
                    loanType = 0
                }
                R.id.rbLoanBorrow -> {
                    loanType = 1
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun loadData() {
        auth = Firebase.auth
        if (auth.currentUser != null) {
            database = Firebase.database
        }
        else {
            sessionExpired()
        }
    }

    private fun setAmountPresetClickListener() {
        val gridLayout = findViewById<GridLayout>(R.id.glAmountPreset)
        for (i in 0 until gridLayout.childCount) {
            gridLayout.getChildAt(i).setOnClickListener { item ->
                val amount = item.tag.toString()
                binding.tfAddLoanAmount.editText?.setText(amount)
            }
        }
    }

    private fun setDateTimePicker() {
        dateTimeMap = mutableMapOf(
            "date" to 0,
            "hour" to 0,
            "minute" to 0,
        )

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

            binding.tfAddLoanDate.editText?.setText(formattedDate)
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
            binding.tfAddLoanTime.editText?.setText(time)

            val msHour: Long = timePicker.hour * milliseconds
            val msMinute: Long = timePicker.minute * milliseconds

            dateTimeMap["hour"] = msHour
            dateTimeMap["minute"] = msMinute
        }

        binding.tfAddLoanDate.editText?.setOnClickListener {
            try {
                datePicker.show(supportFragmentManager, "tag")
            }
            catch (e: Exception) {}
        }

        binding.tfAddLoanDate.setEndIconOnClickListener {
            try {
                datePicker.show(supportFragmentManager, "tag")
            }
            catch (e: Exception) {}
        }

        binding.tfAddLoanTime.editText?.setOnClickListener {
            try {
                timePicker.show(supportFragmentManager, "tag")
            }
            catch (e: Exception) {}
        }

        binding.tfAddLoanTime.setEndIconOnClickListener {
            try {
                timePicker.show(supportFragmentManager, "tag")
            }
            catch (e: Exception) {}
        }
    }

    private fun inputObserver() {
        binding.tfAddLoanAmount.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddLoanAmount.error = getString(R.string.amount_empty)
            }
            else {
                binding.tfAddLoanAmount.error = null
            }
        }

        binding.tfAddLoanAmount.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) text?.clear()
        }

        binding.tfAddLoanName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfAddLoanName.error = getString(R.string.loan_name_empty)
            }
            else {
                binding.tfAddLoanName.error = null
            }
        }

        binding.tfAddLoanDate.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddLoanDate.error = null
        }

        binding.tfAddLoanTime.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfAddLoanTime.error = null
        }
    }

    private fun validateData() {
        val amount = binding.tfAddLoanAmount.editText?.text.toString().trim { it <= ' ' }
        val name = binding.tfAddLoanName.editText?.text.toString().trim { it <= ' ' }
        val date = binding.tfAddLoanDate.editText?.text.toString().trim { it <= ' ' }
        val time = binding.tfAddLoanTime.editText?.text.toString().trim { it <= ' ' }
        val notes = binding.tfAddLoanNotes.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (TextUtils.isEmpty(name)) {
            binding.tfAddLoanName.error = getString(R.string.loan_name_empty)
            errors++
        }

        if (TextUtils.isEmpty(date)) {
            binding.tfAddLoanDate.error = getString(R.string.date_empty)
            errors++
        }

        if (TextUtils.isEmpty(time)) {
            binding.tfAddLoanTime.error = getString(R.string.time_empty)
            errors++
        }

        if (TextUtils.isEmpty(amount)) {
            binding.tfAddLoanAmount.error = getString(R.string.amount_empty)
            errors++
        }
        else {
            if (amount.startsWith("0")) {
                binding.tfAddLoanAmount.error = getString(R.string.amount_starts_0)
                errors++
            }
        }

        if (errors == 0) {
            showProgressDialog()
            val sharedPreferences = SharedPreferences(this)
            val currentAccountId = sharedPreferences.accountId.toString()
            val loanData = mapOf(
                "name" to name,
                "amount" to amount,
                "type" to loanType.toString(),
                "date" to dateTimeMap["date"].toString(),
                "hour" to dateTimeMap["hour"].toString(),
                "minute" to dateTimeMap["minute"].toString(),
                "notes" to notes,
            )

            addLoan(auth.currentUser!!.uid, currentAccountId, loanData)
        }
    }

    private fun addLoan(uid: String, accountId: String, loanData: Map<String, String>) {
        databaseReference = database.getReference("loans").child(uid).child(accountId)
        val key = databaseReference.push().key
        val loan = Loan(
            key,
            loanData["name"],
            loanData["amount"]!!.toDouble(),
            loanData["type"]!!.toInt(),
            loanData["date"]!!.toLong(),
            loanData["hour"]!!.toLong(),
            loanData["minute"]!!.toLong(),
            loanData["notes"]
        )

        databaseReference.child(key!!).setValue(loan)
            .addOnSuccessListener {
                hideProgressDialog()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.llAddLoan, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun sessionExpired() {
        Snackbar
            .make(binding.llAddLoan, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
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
        binding.pbAddLoan.visibility = View.VISIBLE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialog() {
        binding.pbAddLoan.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}