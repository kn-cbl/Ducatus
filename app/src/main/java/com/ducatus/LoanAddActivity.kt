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
import android.os.CountDownTimer
import android.text.TextUtils
import android.widget.GridLayout
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import com.ducatus.data.Loan
import com.ducatus.databinding.ActivityLoanAddBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
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
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class LoanAddActivity : AppCompatActivity() {
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoanAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private var firebaseUser: FirebaseUser? = null
    private var loanType: String = "B"
    private var dateTimeMap: MutableMap<String, Long> =
        mutableMapOf("date" to 0, "hour" to 0, "minute" to 0)

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

        // determine if l loan is lend or borrow
        binding.rgAddLoan.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbLoanLend -> {
                    loanType = "L"
                }
                R.id.rbLoanBorrow -> {
                    loanType = "B"
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
        firebaseUser = auth.currentUser
        if (firebaseUser == null) {
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
            val startOfDay = zdt.with(LocalTime.MIN)
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val formattedDate = dtf.format(zdt)

            binding.tfAddLoanDate.editText?.setText(formattedDate)
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
            binding.tfAddLoanTime.editText?.setText(time)

            val milliseconds: Long = 1000
            val msHour: Long = timePicker.hour * milliseconds * 60 * 60
            val msMinute: Long = timePicker.minute * milliseconds * 60

            dateTimeMap["hour"] = msHour
            dateTimeMap["minute"] = msMinute
        }

        binding.tfAddLoanDate.editText?.setOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "tag")
            }
        }

        binding.tfAddLoanDate.setEndIconOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "tag")
            }
        }

        binding.tfAddLoanTime.editText?.setOnClickListener {
            if (!timePicker.isAdded) {
                timePicker.show(supportFragmentManager, "tag")
            }
        }

        binding.tfAddLoanTime.setEndIconOnClickListener {
            if (!timePicker.isAdded) {
                timePicker.show(supportFragmentManager, "tag")
            }
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
    }

    private fun validateData() {
        val amount = binding.tfAddLoanAmount.editText?.text.toString().trim { it <= ' ' }
        val name = binding.tfAddLoanName.editText?.text.toString().trim { it <= ' ' }
        val date = binding.tfAddLoanDate.editText?.text.toString().trim { it <= ' ' }
        var notes: String? = binding.tfAddLoanNotes.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (TextUtils.isEmpty(name)) {
            binding.tfAddLoanName.error = getString(R.string.loan_name_empty)
            errors++
        }

        if (TextUtils.isEmpty(date)) {
            binding.tfAddLoanDate.error = getString(R.string.date_empty)
            errors++
        }

        if (TextUtils.isEmpty(notes)) {
            notes = null
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
            firebaseUser?.let {
                showProgressDialog()
                sharedPreferences = SharedPreferences(this)
                val currentAccountId = sharedPreferences.accountId.toString()
                val totalDate = dateTimeMap["date"]!! + dateTimeMap["hour"]!! + dateTimeMap["minute"]!!

                val loan = Loan(
                    null,
                    name,
                    name.lowercase(),
                    amount.toDouble(),
                    loanType,
                    totalDate,
                    null,
                    notes,
                    notes?.lowercase(),
                    System.currentTimeMillis().toInt()
                )

                loanExists(it.uid, currentAccountId, loan)
            }
        }
    }

    private fun loanExists(uid: String, accountId: String, loan: Loan) {
        database = Firebase.database
        databaseReference = database.getReference("loans").child(uid).child(accountId)
        val query = databaseReference.orderByChild("nameLower").equalTo(loan.nameLower)
        query.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    addLoan(accountId, loan)
                }
                else {
                    hideProgressDialog()
                    binding.tfAddLoanName.error = getString(R.string.loan_name_exists)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.llAddLoan, getString(R.string.add_loan_error), 5000)
                    .show()
            }
    }

    private fun addLoan(accountId: String, loan: Loan) {
        val key = databaseReference.push().key!!
        loan.id = key

        databaseReference.child(key).setValue(loan)
            .addOnSuccessListener {
                scheduleNotification(this, loan, accountId)
                hideProgressDialog()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.llAddLoan, getString(R.string.add_loan_error), 5000)
                    .show()
            }
    }

    private fun createNotificationChannel() {
        val name = "Loans"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(sharedPreferences.loansChannelId, name, importance)

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

    private fun scheduleNotification(context: Context, loan: Loan, accountId: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.loansChannelId)
        if (notificationChannel == null) {
            createNotificationChannel()
            notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.loansChannelId)
        }

        // create notification if channel is enabled
        // else do not create
        if (notificationChannel.importance != NotificationManager.IMPORTANCE_NONE) {
            enableReceiver(context)

            // pass to broadcast receiver
            val notificationIntent = Intent(context, NotificationReceiver::class.java)

            val dtf = DateTimeFormatter.ofPattern("MMM dd, h:mm a")
            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(loan.dueDate!!),
                ZoneId.systemDefault()
            )

            val formattedDate = dtf.format(zdt)
            val formattedAmount = "₱" + String.format("%,.2f", loan.amount)
            val elapsedTime = getElapsedTime(loan.dueDate!!)

            val title = "Loan payment for ${loan.name} due $elapsedTime"
            val message = "Settle your payment of $formattedAmount on or before $formattedDate."
            val notificationId = loan.notificationId!!

            notificationIntent.action = "com.ducatus.LOAN"
            notificationIntent.putExtra(titleExtra, title)
            notificationIntent.putExtra(messageExtra, message)
            notificationIntent.putExtra(notificationIdExtra, notificationId)
            notificationIntent.putExtra(itemIdExtra, loan.id)
            notificationIntent.putExtra(accountIdExtra, accountId)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationDate = zdt.minusDays(3).toInstant().toEpochMilli()

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
        var elapsedHours = ChronoUnit.HOURS.between(startDate, endDate)
        var elapsedMinutes = ChronoUnit.MINUTES.between(startDate, endDate)
        var elapsedSeconds = ChronoUnit.SECONDS.between(startDate, endDate)

        val dateText =
            if (elapsedDays > 0) {
                if (elapsedDays.toInt() == 1) {
                    "in $elapsedDays day"
                }
                else {
                    "in $elapsedDays days"
                }
            }
            else if (elapsedHours > 0) {
                if (elapsedHours.toInt() == 1) {
                    "in $elapsedHours hour"
                }
                else {
                    "in $elapsedHours hours"
                }
            }
            else if (elapsedMinutes > 0) {
                if (elapsedMinutes.toInt() == 1) {
                    "in $elapsedMinutes minute"
                }
                else {
                    "in $elapsedMinutes minutes"
                }
            }
            else if (elapsedSeconds > 0) {
                if (elapsedSeconds.toInt() == 1) {
                    "in $elapsedSeconds second"
                }
                else {
                    "in $elapsedSeconds seconds"
                }
            }
            else {
                elapsedHours *= -1
                elapsedMinutes *= -1
                elapsedSeconds *= -1

                if (elapsedHours > 0) {
                    "${elapsedHours}h ago"
                }
                else if (elapsedMinutes > 0) {
                    "${elapsedMinutes}m ago"
                }
                else {
                    "${elapsedSeconds}s ago"
                }
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
        val bundle = Bundle()
        bundle.putString("title", getString(R.string.adding))

        actionDialog = ActionDialogFragment()
        actionDialog.arguments = bundle
        actionDialog.show(supportFragmentManager, "dialog")
    }

    private fun hideProgressDialog() {
        actionDialog.dismiss()
    }
}