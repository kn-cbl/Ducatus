package com.ducatus

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.ducatus.data.Loan
import com.ducatus.data.LoanHistory
import com.ducatus.databinding.FragmentLoanBorrowDialogBinding
import com.ducatus.viewmodel.AmountViewModel
import com.ducatus.viewmodel.LoanViewModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class LoanBorrowDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentLoanBorrowDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var selectedLoan: Loan
    private val amountViewModel: AmountViewModel by activityViewModels()
    private val loanViewModel: LoanViewModel by activityViewModels()
    private var firebaseUser: FirebaseUser? = null
    private var isPaid: Boolean = true
    private var dateTimeMap: MutableMap<String, Long> =
        mutableMapOf("date" to 0, "hour" to 0, "minute" to 0)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.clLoanDetail)
        binding = FragmentLoanBorrowDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        setDateTimePicker()
        inputObserver()

        amountViewModel.amount.observe(viewLifecycleOwner) { amount ->
            amount.getContentIfNotHandled()?.let { content ->
                binding.tfLoanBorrowAmount.editText?.setText(content)
            }
        }

        binding.tfLoanBorrowAmount.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = AmountDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        binding.btnLoanBorrowCancel.setOnClickListener {
            dismiss()
        }

        binding.btnLoanBorrowSave.setOnClickListener {
            validateData()
        }
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            sessionExpired()
        }

        val strLoan = arguments?.getString("loan")
        selectedLoan = Gson().fromJson(strLoan, Loan::class.java)

        if (selectedLoan.paidAt == null) {
            isPaid = false
            binding.tfLoanBorrowDate.hint = getString(R.string.date)

            // change button text from borrow to receive if loan is of type lend
            if (selectedLoan.type == "L") {
                binding.tvLoanBorrowTitle.text = getString(R.string.receive_payment)
            }

            val remainingLoanText = getString(R.string.remaining_loan_2) + " ₱" + String.format("%,.2f", selectedLoan.amount)
            binding.tvLoanBorrowRemaining.apply { 
                text = remainingLoanText
                visibility = View.VISIBLE
            }

            binding.tfLoanBorrowAmount.editText?.doOnTextChanged { text, _, _, _ ->
                if (text != null && text.toString().toDouble() > selectedLoan.amount) {
                    binding.tvLoanBorrowRemainingNotice.visibility = View.VISIBLE
                }
                else {
                    binding.tvLoanBorrowRemainingNotice.visibility = View.GONE
                }
            }

            // set date and time to current date and time
            val zdt = ZonedDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            )
            val startOfDay = zdt.with(LocalTime.MIN)
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val formattedDate = dtf.format(startOfDay)

            binding.tfLoanBorrowDate.visibility = View.GONE
            binding.tfLoanBorrowDate.isEnabled = false
            binding.tfLoanBorrowDate.editText?.setText(formattedDate)
            dateTimeMap["date"] = startOfDay.toInstant().toEpochMilli()

            val dtf2 = DateTimeFormatter.ofPattern("h:mm a")
            val formattedTime = dtf2.format(zdt)

            val milliseconds: Long = 1000
            val msHour: Long = zdt.hour * milliseconds * 60 * 60
            val msMinute: Long = zdt.minute * milliseconds * 60

            binding.tfLoanBorrowTime.visibility = View.GONE
            binding.tfLoanBorrowTime.isEnabled = false
            binding.tfLoanBorrowTime.editText?.setText(formattedTime)
            dateTimeMap["hour"] = msHour
            dateTimeMap["minute"] = msMinute
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
            val formattedDate = dtf.format(startOfDay)

            binding.tfLoanBorrowDate.editText?.setText(formattedDate)
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
            binding.tfLoanBorrowTime.editText?.setText(time)

            val milliseconds: Long = 1000
            val msHour: Long = timePicker.hour * milliseconds * 60 * 60
            val msMinute: Long = timePicker.minute * milliseconds * 60

            dateTimeMap["hour"] = msHour
            dateTimeMap["minute"] = msMinute
        }

        binding.tfLoanBorrowDate.editText?.setOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(childFragmentManager, "tag")
            }
        }

        binding.tfLoanBorrowDate.setEndIconOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(childFragmentManager, "tag")
            }
        }

        binding.tfLoanBorrowTime.editText?.setOnClickListener {
            if (!timePicker.isAdded) {
                timePicker.show(childFragmentManager, "tag")
            }
        }

        binding.tfLoanBorrowTime.setEndIconOnClickListener {
            if (!timePicker.isAdded) {
                timePicker.show(childFragmentManager, "tag")
            }
        }
    }

    private fun inputObserver() {
        binding.tfLoanBorrowAmount.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfLoanBorrowAmount.error = getString(R.string.amount_empty)
            }
            else {
                binding.tfLoanBorrowAmount.error = null
            }
        }

        binding.tfLoanBorrowAmount.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) {
                text?.clear()
            }
        }

        binding.tfLoanBorrowDate.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfLoanBorrowDate.error = null
        }

        binding.tfLoanBorrowTime.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfLoanBorrowTime.error = null
        }
    }

    private fun validateData() {
        // hide keyboard
        try {
            val windowToken: View = dialog!!.window!!.decorView.rootView
            val imm: InputMethodManager = dialog!!.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken.windowToken, 0)
        }
        catch (e: Exception){}

        val amount = binding.tfLoanBorrowAmount.editText?.text.toString().trim { it <= ' ' }
        val date = binding.tfLoanBorrowDate.editText?.text.toString().trim { it <= ' ' }
        val time = binding.tfLoanBorrowTime.editText?.text.toString().trim { it <= ' ' }
        var notes: String? = binding.tfLoanBorrowNotes.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (TextUtils.isEmpty(date)) {
            binding.tfLoanBorrowDate.error = getString(R.string.date_empty)
            errors++
        }

        if (TextUtils.isEmpty(time)) {
            binding.tfLoanBorrowTime.error = getString(R.string.time_empty)
            errors++
        }

        if (TextUtils.isEmpty(notes)) {
            notes = null
        }

        if (TextUtils.isEmpty(amount)) {
            binding.tfLoanBorrowAmount.error = getString(R.string.amount_empty)
            errors++
        }

        else {
            if (amount.startsWith("0")) {
                binding.tfLoanBorrowAmount.error = getString(R.string.amount_starts_0)
                errors++
            }
        }

        if (errors == 0) {
            firebaseUser?.let {
                showProgressDialog()
                sharedPreferences = SharedPreferences(activity)
                val currentAccountId = sharedPreferences.accountId.toString()
                val totalDate = dateTimeMap["date"]!! + dateTimeMap["hour"]!! + dateTimeMap["minute"]!!

                val loanHistory = LoanHistory(
                    null,
                    amount.toDouble(),
                    "B",
                    totalDate,
                    notes,
                    selectedLoan.id
                )

                val loanNewAmount = selectedLoan.amount - loanHistory.amount
                when (selectedLoan.paidAt) {
                    null -> { // paid loan / progressing loan
                        if (loanNewAmount <= 0.0) { // paid
                            // set paid at date today
                            val zdt = ZonedDateTime.ofInstant(
                                Instant.now(),
                                ZoneId.systemDefault()
                            )
                            selectedLoan.paidAt = zdt.toInstant().toEpochMilli()
                            selectedLoan.amount = 0.0
                            cancelNotification(activity, selectedLoan.notificationId!!)
                        }
                        else { // progressing loan
                            selectedLoan.amount = loanNewAmount
                        }
                    }
                    else -> { // adding new record; activating loan
                        selectedLoan.amount = loanHistory.amount
                        selectedLoan.type = "B"
                        selectedLoan.dueDate = loanHistory.date
                        selectedLoan.paidAt = null
                        selectedLoan.notificationId = System.currentTimeMillis().toInt()
                    }
                }

                database = Firebase.database
                updateLoan(it.uid, currentAccountId, selectedLoan, loanHistory)
            }
        }
    }

    private fun updateLoan(uid: String, accountId: String, loan: Loan, loanHistory: LoanHistory) {
        databaseReference =
            database.getReference("loans")
                .child(uid)
                .child(accountId)
                .child(loan.id!!)

        databaseReference.setValue(loan)
            .addOnSuccessListener {
                addLoanHistory(uid, accountId, loan, loanHistory)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun addLoanHistory(uid: String, accountId: String, loan: Loan, loanHistory: LoanHistory) {
        databaseReference = database
            .getReference("loanHistory")
            .child(uid)
            .child(accountId)
            .child(loanHistory.loanId!!)

        val key = databaseReference.push().key!!
        loanHistory.id = key

        databaseReference.child(key).setValue(loanHistory)
            .addOnSuccessListener {
                // check if loan was already paid before updating loan
                // if previous value was paid and updated value is not paid
                // schedule notification
                if (isPaid && loan.paidAt == null) {
                    scheduleNotification(activity, loan, accountId)
                }

                loanViewModel.setLoan(loan)
                hideProgressDialog()
                dismiss()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun createNotificationChannel() {
        val name = "Loans"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(sharedPreferences.loansChannelId, name, importance)

        val notificationManager = activity.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
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
        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(loan.dueDate!!),
                ZoneId.systemDefault()
            )

            val dtf = DateTimeFormatter.ofPattern("MMM dd, h:mm a")
            val formattedDate = dtf.format(zdt)
            val formattedAmount = "₱" + String.format("%,.2f", loan.amount)
            val elapsedTime = getElapsedTime(loan.dueDate!!)

            val title = "Loan payment for ${loan.name} due in $elapsedTime"
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

            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.RTC_WAKEUP, notificationDate, pendingIntent)
        }
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.loansChannelId)
        if (notificationChannel != null) {
            val notificationIntent = Intent(context, NotificationReceiver::class.java)
            notificationIntent.action = "com.ducatus.LOAN"

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
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
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.session_expired))
            .setPositiveButton(resources.getString(R.string.log_in)) { _, _ -> }

        dialog.setOnDismissListener {
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity.finish()
        }

        dialog.show()
    }

    private fun showProgressDialog() {
        binding.pbLoanBorrow.visibility = View.VISIBLE
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialog() {
        binding.pbLoanBorrow.visibility = View.INVISIBLE
        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}