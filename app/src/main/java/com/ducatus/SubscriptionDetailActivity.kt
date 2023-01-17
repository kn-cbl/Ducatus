package com.ducatus

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.adapter.SubscriptionHistoryAdapter
import com.ducatus.common.AppResources
import com.ducatus.data.Account
import com.ducatus.data.Subscription
import com.ducatus.data.SubscriptionHistory
import com.ducatus.databinding.ActivitySubscriptionDetailBinding
import com.ducatus.interfaces.SubscriptionHistoryInterface
import com.ducatus.viewmodel.SubscriptionViewModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class SubscriptionDetailActivity : AppCompatActivity(), SubscriptionHistoryInterface {
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySubscriptionDetailBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var currentAccountId: String
    private lateinit var sharedPreferences: SharedPreferences
    private var selectedSubscription: Subscription? = null
    private var mutableSubscriptionHistory = mutableListOf<SubscriptionHistory>()
    private var nextDueDate: Long? = null
    private var firebaseUser: FirebaseUser? = null
    private val subscriptionViewModel: SubscriptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        loadData()

        binding.tbSubscriptionDetail.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbSubscriptionDetail.inflateMenu(R.menu.edit_delete_menu)
        binding.tbSubscriptionDetail.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.edit -> {
                    firebaseUser?.let {
                        val bundle = Bundle()
                        bundle.putSerializable("subscription", Gson().toJson(selectedSubscription))

                        val fragmentManager = supportFragmentManager
                        val newFragment = SubscriptionEditDialogFragment()
                        newFragment.arguments = bundle
                        newFragment.show(fragmentManager, "dialog")

//                        val transaction = fragmentManager.beginTransaction()
//                        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//                        transaction
//                            .add(android.R.id.content, newFragment)
//                            .commit()
                    }
                    true
                }
                R.id.delete -> {
                    firebaseUser?.let {
                        selectedSubscription?.let { subscription ->
                            confirmDelete(
                                it.uid,
                                currentAccountId,
                                subscription
                            )
                        }
                    }
                    true
                }
                else -> false
            }
        }

        subscriptionViewModel.subscription.observe(this) { subscription ->
            subscription.getContentIfNotHandled()?.let { content ->
                firebaseUser?.let {
                    selectedSubscription = content
                    loadSubscription(it.uid, sharedPreferences.accountId!!, selectedSubscription!!.id!!)
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun confirmPayment(subscriptionHistory: SubscriptionHistory) {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.confirm_payment_title))
            .setPositiveButton(resources.getString(R.string.confirm)) { _, _ ->
                firebaseUser?.let {
                    when (selectedSubscription!!.frequency) {
                        0 -> { // one time payment
                            savePayment(
                                it.uid,
                                sharedPreferences.accountId.toString(),
                                selectedSubscription!!,
                                subscriptionHistory,
                                null
                            )
                        }
                        1 -> { // recurring payment
                            showNextDueDateDialog(
                                it.uid,
                                sharedPreferences.accountId.toString(),
                                selectedSubscription!!,
                                subscriptionHistory
                            )
                        }
                    }
                }
            }
            .setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
            .show()
    }

    private fun loadData() {
        showProgressDialog()
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            database = Firebase.database
            val subscriptionId = intent.getStringExtra("subscriptionId")!!

            sharedPreferences = SharedPreferences(this)
            currentAccountId = sharedPreferences.accountId.toString()
            val accountId = intent.getStringExtra("accountId")

            // check if accountId from the notification is the same as current accountId
            // this prevents checking an item designated for a different account
            if (accountId != null && currentAccountId != accountId) {
                selectAccount(firebaseUser!!.uid, accountId, subscriptionId)
            }
            else {
                loadSubscription(firebaseUser!!.uid, currentAccountId, subscriptionId)
            }

        }
        else {
            sessionExpired()
        }
    }

    private fun selectAccount(uid: String, accountId: String, subscriptionId: String) {
        databaseReference = database.getReference("accounts").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    subscriptionDoesNotExist()
                }
                else {
                    val account = snapshot.getValue<Account>()
                    account?.let {
                        sharedPreferences.accountId = it.id
                        sharedPreferences.accountName = it.name
                        sharedPreferences.accountColor = it.color

                        currentAccountId = sharedPreferences.accountId.toString()
                        loadSubscription(uid, currentAccountId, subscriptionId)
                    }
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.clSubscriptionDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun subscriptionDoesNotExist() {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.subscription_empty_title))
            .setPositiveButton(resources.getString(R.string.go_back)) { _, _ -> onBackPressed() }
            .setOnDismissListener { onBackPressed() }
            .show()
    }

    private fun loadSubscription(uid: String, accountId: String, subscriptionId: String) {
        showProgressDialog()
        databaseReference =
            database.getReference("subscriptions")
                .child(uid)
                .child(accountId)
                .child(subscriptionId)

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    subscriptionDoesNotExist()
                }
                else {
                    val subscription = snapshot.getValue<Subscription>()
                    if (subscription != null) {
                        loadSubscriptionHistory(uid, accountId, subscriptionId)
                        selectedSubscription = subscription

                        binding.tbSubscriptionDetail.title = subscription.name

                        val iconColor = resources.getIdentifier(
                            subscription.categoryColor,
                            "color",
                            applicationContext.packageName
                        )

                        binding.flSubscriptionDetailCategoryIcon.backgroundTintList =
                            ContextCompat.getColorStateList(applicationContext, iconColor)

                        val icon = resources.getIdentifier(
                            subscription.categoryIcon,
                            "drawable",
                            applicationContext.packageName
                        )

                        binding.ivSubscriptionDetailCategoryIcon.setImageResource(icon)
                        binding.ivSubscriptionDetailCategoryIcon.setColorFilter(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.white,
                                null
                            )
                        )

                        binding.tvSubscriptionDetailCategory.text = subscription.categoryName

                        val paymentTypes = AppResources().getPaymentTypes()
                        val paymentType =
                            if (subscription.paymentType != 4) paymentTypes[subscription.paymentType]
                            else subscription.paymentTypeOthers

                        binding.tvSubscriptionDetailPaymentType.text = paymentType

                        when (subscription.frequency) {
                            0 -> {
                                binding.tvSubscriptionDetailRecurrence.text = getString(R.string.one_time)
                            }
                            1 -> {
                                val frequencyText =
                                    if (subscription.recurrence == 1) "Every ${subscription.recurrence} month"
                                    else "Every ${subscription.recurrence} months"

                                binding.tvSubscriptionDetailRecurrence.text = frequencyText
                            }
                        }

                        val amountText = "₱" + String.format("%,.2f", subscription.amount)
                        binding.tvSubscriptionDetailAmount.text = amountText

                        binding.tvSubscriptionDetailNotes.text =
                            if (subscription.notes == null) "No notes"
                            else subscription.notes
                    }
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clSubscriptionDetail, getString(R.string.load_subscription_error),5000)
                    .show()
            }
    }

    private fun loadSubscriptionHistory(uid: String, accountId: String, subscriptionId: String) {
        databaseReference =
            database.getReference("subscriptionHistory")
                .child(uid)
                .child(accountId)
                .child(subscriptionId)

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val subscriptionsHistory = mutableListOf<SubscriptionHistory>()
                for (child in snapshot.children) {
                    val subscriptionHistory = child.getValue<SubscriptionHistory>()
                    if (subscriptionHistory != null) {
                        subscriptionsHistory.add(subscriptionHistory)
                    }
                }

                subscriptionsHistory.sortByDescending { it.dueAt }

                val subscriptionHistoryAdapter = SubscriptionHistoryAdapter(mutableListOf(), this@SubscriptionDetailActivity)
                binding.rvSubscriptionHistory.adapter = subscriptionHistoryAdapter
                binding.rvSubscriptionHistory.layoutManager = LinearLayoutManager(applicationContext)

                subscriptionsHistory.forEach {
                    subscriptionHistoryAdapter.addSubscriptionHistory(it)
                    mutableSubscriptionHistory.add(it)
                }

                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clSubscriptionDetail, getString(R.string.load_subscription_history_error),5000)
                    .show()
            }
    }

    private fun showNextDueDateDialog(
        uid: String,
        accountId: String,
        subscription: Subscription,
        subscriptionHistory: SubscriptionHistory
    ) {
        val nextDueDateDialogFragment = Dialog(this).apply {
            setCancelable(true)
            setContentView(R.layout.fragment_next_due_date_dialog)
        }

        val tfNextDueDate = nextDueDateDialogFragment.findViewById<TextInputLayout>(R.id.tfNextDueDate)
        val btnNextDueDateCancel = nextDueDateDialogFragment.findViewById<Button>(R.id.btnNextDueDateCancel)
        val btnNextDueDateOK = nextDueDateDialogFragment.findViewById<Button>(R.id.btnNextDueDateOK)

        setDatePicker(tfNextDueDate, subscription)

        btnNextDueDateCancel.setOnClickListener {
            nextDueDateDialogFragment.dismiss()
        }

        btnNextDueDateOK.setOnClickListener {
            val isValidDueDate = validateNextDueDate(tfNextDueDate)
            if (isValidDueDate) {
                nextDueDateDialogFragment.dismiss()
                savePayment(uid, accountId, subscription, subscriptionHistory, nextDueDate)
            }
            else {
                tfNextDueDate.error = getString(R.string.select_next_due_date_error)
            }
        }

        nextDueDateDialogFragment.show()
    }

    private fun validateNextDueDate(textInputLayout: TextInputLayout): Boolean {
        textInputLayout.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null) {
                textInputLayout.error = getString(R.string.select_next_due_date_error)
            }
            else {
                textInputLayout.error = null
            }
        }

        return nextDueDate != null
    }

    private fun setDatePicker(textInputLayout: TextInputLayout, subscription: Subscription) {
        val recurringDate = subscription.dueDate!! + subscription.recurrence!!
        val zdtRecurringDate = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(recurringDate),
            ZoneId.systemDefault()
        )

        val startDate = zdtRecurringDate.toInstant().toEpochMilli()
        val endDate = zdtRecurringDate.plusMonths(1).toInstant().toEpochMilli()

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

            textInputLayout.editText?.setText(formattedDate)
            nextDueDate = endOfDay.toInstant().toEpochMilli()
        }

        textInputLayout.editText?.setOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "tag")
            }
        }

        textInputLayout.setEndIconOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "tag")
            }
        }
    }

    private fun savePayment(
        uid: String,
        accountId: String,
        subscription: Subscription,
        subscriptionHistory: SubscriptionHistory,
        nextDueDate: Long?
    ) {
        showProgressDialogAction(getString(R.string.saving))
        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )
        // set paid at current time
        subscriptionHistory.paidAt = zdtToday.toInstant().toEpochMilli()

        databaseReference =
            database.getReference("subscriptionHistory")
                .child(uid)
                .child(accountId)
                .child(subscription.id!!)
                .child(subscriptionHistory.id!!)

        databaseReference.setValue(subscriptionHistory)
            .addOnSuccessListener {
                updateSubscription(uid, accountId, subscription, subscriptionHistory, nextDueDate)
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clSubscriptionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun updateSubscription(
        uid: String,
        accountId: String,
        subscription: Subscription,
        subscriptionHistory: SubscriptionHistory,
        nextDueDate: Long?
    ) {
        var isCompleted = false
        when (subscription.frequency) {
            0 -> {
                // set subscription to paid
                subscription.paidAt = subscriptionHistory.paidAt
                isCompleted = true
            }
            1 -> {
                subscription.dueDate = nextDueDate
            }
        }

        databaseReference =
            database.getReference("subscriptions")
                .child(uid)
                .child(accountId)
                .child(subscription.id!!)

        databaseReference.setValue(subscription)
            .addOnSuccessListener {
                if (!isCompleted) {
                    createNewHistory(uid, accountId, subscription)
                }
                else {
                    cancelNotification(this, subscriptionHistory.notificationId!!)
                    hideProgressDialogAction()
                    loadSubscriptionHistory(uid, accountId, subscription.id!!)
                }
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clSubscriptionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun createNewHistory(uid: String, accountId: String, subscription: Subscription) {
        databaseReference =
            database.getReference("subscriptionHistory")
                .child(uid)
                .child(accountId)
                .child(subscription.id!!)

        val key = databaseReference.push().key!!
        val subscriptionHistory = SubscriptionHistory(
            key,
            subscription.amount,
            subscription.dueDate,
            null,
            subscription.id,
            System.currentTimeMillis().toInt()
        )

        databaseReference.child(key).setValue(subscriptionHistory)
            .addOnSuccessListener {
                when (subscription.notification) {
                    1 -> scheduleNotification(this, 0, accountId, subscription, subscriptionHistory)
                    2 -> scheduleNotification(this, 1, accountId, subscription, subscriptionHistory)
                    3 -> scheduleNotification(this, 3, accountId, subscription, subscriptionHistory)
                    4 -> scheduleNotification(this, 7, accountId, subscription, subscriptionHistory)
                }

                hideProgressDialogAction()
                loadSubscriptionHistory(uid, accountId, subscription.id!!)
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clSubscriptionDetail, it.localizedMessage!!, 5000)
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

            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(subscription.dueDate!!),
                ZoneId.systemDefault()
            ).with(LocalTime.MIN)

            val dtf = DateTimeFormatter.ofPattern("MMM dd, h:mm a")
            val formattedDate = dtf.format(zdt)
            val formattedAmount = "₱" + String.format("%,.2f", subscription.amount)
            val dueDate = getElapsedTime(subscription.dueDate!!)

            val title = "Payment for ${subscription.name} due $dueDate"
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

    private fun confirmDelete(uid: String, accountId: String, subscription: Subscription) {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.delete_subscription_title))
            .setMessage(resources.getString(R.string.delete_subscription_message))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ ->
                deleteSubscription(uid, accountId, subscription)
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> } // do nothing
            .show()
    }

    private fun deleteSubscription(uid: String, accountId: String, subscription: Subscription) {
        showProgressDialogAction(getString(R.string.deleting))
        databaseReference =
            database.getReference("subscriptions")
                .child(uid)
                .child(accountId)
                .child(subscription.id!!)

        databaseReference.removeValue()
            .addOnSuccessListener {
                mutableSubscriptionHistory.forEach {
                    cancelNotification(this, it.notificationId!!)
                }
                updateBudget(uid, accountId, subscription)
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clSubscriptionDetail, getString(R.string.delete_loan_error),5000)
                    .show()
            }
    }

    private fun updateBudget(uid: String, accountId: String, subscription: Subscription) {
        databaseReference =
            database.getReference("budgets")
                .child(uid)
                .child(accountId)
                .child(subscription.categoryId!!)
                .child("amountSpent")

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val amountSpent = snapshot.value.toString().toDouble()
                var newAmount = amountSpent - subscription.amount

                if (newAmount < 0.0) newAmount = 0.0
                databaseReference.setValue(newAmount)
                    .addOnSuccessListener {
                        updateAccount(uid, accountId, subscription)
                    }
                    .addOnFailureListener {
                        hideProgressDialogAction()
                        Snackbar
                            .make(binding.clSubscriptionDetail, it.localizedMessage!!, 5000)
                            .show()
                    }
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clSubscriptionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun updateAccount(uid: String, accountId: String, subscription: Subscription) {
        databaseReference =
            database.getReference("accounts")
                .child(uid)
                .child(accountId)
                .child("remainingBalance")

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val remainingBalance = snapshot.value.toString().toDouble()
                val newBalance = remainingBalance + subscription.amount

                databaseReference.setValue(newBalance)
                    .addOnSuccessListener {
                        deleteSubscriptionHistory(uid, accountId, subscription.id!!)
                    }
                    .addOnFailureListener {
                        hideProgressDialogAction()
                        Snackbar
                            .make(binding.clSubscriptionDetail, it.localizedMessage!!, 5000)
                            .show()
                    }
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clSubscriptionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun deleteSubscriptionHistory(uid: String, accountId: String, subscriptionId: String) {
        databaseReference =
            database
                .getReference("subscriptionHistory")
                .child(uid)
                .child(accountId)
                .child(subscriptionId)

        databaseReference.removeValue()
            .addOnSuccessListener {
                hideProgressDialogAction()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clSubscriptionDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.subscriptionsChannelId)
        if (notificationChannel != null) {
            val notificationIntent = Intent(context, NotificationReceiver::class.java)
            notificationIntent.action = "com.ducatus.SUBSCRIPTION"

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
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
        binding.pbSubscriptionDetail.visibility = View.VISIBLE
        binding.rlSubscriptionDetail.visibility = View.GONE
        binding.llSubscriptionHistory.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbSubscriptionDetail.visibility = View.INVISIBLE
        binding.rlSubscriptionDetail.visibility = View.VISIBLE
        binding.llSubscriptionHistory.visibility = View.VISIBLE
    }

    private fun showProgressDialogAction(title: String) {
        val bundle = Bundle()
        bundle.putString("title", title)

        actionDialog = ActionDialogFragment()
        actionDialog.arguments = bundle
        actionDialog.show(supportFragmentManager, "dialog")
    }

    private fun hideProgressDialogAction() {
        actionDialog.dismiss()
    }
}