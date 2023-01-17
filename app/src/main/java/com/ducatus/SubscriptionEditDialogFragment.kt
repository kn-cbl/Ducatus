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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.ducatus.common.AppResources
import com.ducatus.data.*
import com.ducatus.databinding.FragmentSubscriptionEditDialogBinding
import com.ducatus.viewmodel.AmountViewModel
import com.ducatus.viewmodel.SubscriptionRecurrenceViewModel
import com.ducatus.viewmodel.SubscriptionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

class SubscriptionEditDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentSubscriptionEditDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var selectedSubscription: Subscription
    private var firebaseUser: FirebaseUser? = null
    private var remainingBudget: Double = 0.0
    private var selectedPaymentType: Int = 0
    private var selectedDate: Long = 0
    private var selectedNotification = 0
    private var selectedRecurrence = 0
    private val amountViewModel: AmountViewModel by activityViewModels()
    private val recurrenceViewModel: SubscriptionRecurrenceViewModel by activityViewModels()
    private val subscriptionViewModel: SubscriptionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.clSubscriptionDetail)
        binding = FragmentSubscriptionEditDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        setPaymentTypes()
        inputObserver()

        amountViewModel.amount.observe(viewLifecycleOwner) { amount ->
            amount.getContentIfNotHandled()?.let { content ->
                binding.tfSubscriptionEditAmount.editText?.setText(content)
            }
        }

        val spNotifications = (binding.tfSubscriptionEditNotifications.editText as? AutoCompleteTextView)
        spNotifications?.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                selectedNotification = position
            }

        binding.btnSubscriptionEditCancel.setOnClickListener {
            dismiss()
        }

        binding.tfSubscriptionEditAmount.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = AmountDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        binding.tfSubscriptionEditRecurrence.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = SubscriptionRecurrenceDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        binding.tfSubscriptionEditRecurrence.setEndIconOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = SubscriptionRecurrenceDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        recurrenceViewModel.recurrence.observe(this) { recurrence ->
            selectedRecurrence = recurrence

            val text = "Every $recurrence month/s"
            binding.tfSubscriptionEditRecurrence.editText?.setText(text)
        }

        binding.btnSubscriptionEditSave.setOnClickListener {
            validateData()
        }
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            database = Firebase.database
            sharedPreferences = SharedPreferences(activity)

            val strSubscription = arguments?.getString("subscription")
            selectedSubscription = Gson().fromJson(strSubscription, Subscription::class.java)
            loadSubscription(selectedSubscription)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadSubscription(subscription: Subscription) {
        selectedDate = subscription.dueDate!!
        selectedNotification = subscription.notification

        if (subscription.paidAt != null) {
            binding.tfSubscriptionEditAmount.editText?.apply {
                visibility = View.GONE
            }
            binding.tfSubscriptionEditNotifications.editText?.apply {
                visibility = View.GONE
            }
        }

        if (subscription.frequency == 1) {
            selectedRecurrence = subscription.recurrence!!

            val text = "Every $selectedRecurrence month/s"
            binding.tfSubscriptionEditRecurrence.editText?.setText(text)
            binding.tfSubscriptionEditRecurrence.visibility = View.VISIBLE
        }

        getCategoryRemainingBudget(firebaseUser!!.uid, sharedPreferences.accountId!!, subscription.categoryId!!)
        binding.tfSubscriptionEditName.editText?.setText(subscription.name)
        binding.tfSubscriptionEditAmount.editText?.setText(subscription.amount.toInt().toString())

        selectedPaymentType = subscription.paymentType
        val paymentTypes = AppResources().getPaymentTypes()
        val paymentType = paymentTypes[subscription.paymentType]
        val spPaymentType = (binding.tfSubscriptionEditPaymentType.editText as? AutoCompleteTextView)
        spPaymentType?.setText(paymentType, false)

        if (subscription.paymentType == 4) {
            binding.tfSubscriptionEditPaymentTypeOthers.apply {
                editText?.setText(subscription.paymentTypeOthers)
                visibility = View.VISIBLE
            }
        }

        setNotifications(subscription.notification)

        binding.tfSubscriptionEditNotes.editText?.setText(subscription.notes)
    }

    private fun getCategoryRemainingBudget(uid: String, accountId: String, budgetId: String) {
        databaseReference =
            database.getReference("budgets")
                .child(uid)
                .child(accountId)
                .child(budgetId)

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val budget = snapshot.getValue<Budget>()
                budget?.let {
                    val amountLeft = budget.amountTotal - budget.amountSpent
                    remainingBudget = amountLeft + selectedSubscription.amount

                    val formattedAmount = "Remaining category budget: ₱" + String.format("%,.2f", amountLeft)
                    binding.tvSubscriptionEditRemainingBudget.text = formattedAmount
                }
            }
            .addOnFailureListener {
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun setPaymentTypes() {
        val paymentTypes = AppResources().getPaymentTypes()
        val adapter = ArrayAdapter(activity, R.layout.list_item, paymentTypes)

        val spPaymentType = (binding.tfSubscriptionEditPaymentType.editText as? AutoCompleteTextView)
        spPaymentType?.setAdapter(adapter)
        spPaymentType?.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                selectedPaymentType = position
                if (position == 4) {
                    binding.tfSubscriptionEditPaymentTypeOthers.visibility = View.VISIBLE
                }
                else {
                    binding.tfSubscriptionEditPaymentTypeOthers.visibility = View.GONE
                }
            }
    }

    private fun setNotifications(type: Int) {
        val notifications = AppResources().getSubscriptionNotifications()
        val adapter = ArrayAdapter(activity, R.layout.list_item, notifications)
        val spinner = (binding.tfSubscriptionEditNotifications.editText as? AutoCompleteTextView)
        spinner?.setAdapter(adapter)

        when (type) {
            0 -> spinner?.setText(getString(R.string.none), false)
            1 -> spinner?.setText(getString(R.string.on_due_date), false)
            2 -> spinner?.setText(getString(R.string._1_day_before), false)
            3 -> spinner?.setText(getString(R.string._3_day_before), false)
            4 -> spinner?.setText(getString(R.string._1_week_before), false)
        }
    }

    private fun inputObserver() {
        binding.tfSubscriptionEditAmount.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfSubscriptionEditAmount.error = getString(R.string.amount_empty)
            }
            else if (text.toString().toDouble() > remainingBudget) {
                binding.tfSubscriptionEditAmount.error = getString(R.string.amount_overflow)
            }
            else {
                binding.tfSubscriptionEditAmount.error = null
            }
        }

        binding.tfSubscriptionEditAmount.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) text?.clear()
        }

        binding.tfSubscriptionEditName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfSubscriptionEditName.error = getString(R.string.subscription_name_empty)
            }
            else {
                binding.tfSubscriptionEditName.error = null
            }
        }

        binding.tfSubscriptionEditPaymentType.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfSubscriptionEditPaymentType.error = null
        }

        binding.tfSubscriptionEditPaymentTypeOthers.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfSubscriptionEditPaymentTypeOthers.error = getString(R.string.payment_type_empty_2)
            }
            else {
                binding.tfSubscriptionEditPaymentTypeOthers.error = null
            }
        }

        binding.tfSubscriptionEditRecurrence.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfSubscriptionEditRecurrence.error = null
        }

        binding.tfSubscriptionEditNotifications.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfSubscriptionEditNotifications.error = null
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

        val amount = binding.tfSubscriptionEditAmount.editText?.text.toString().trim { it <= ' ' }
        val name = binding.tfSubscriptionEditName.editText?.text.toString().trim { it <= ' ' }
        val paymentType = binding.tfSubscriptionEditPaymentType.editText?.text.toString().trim { it <= ' ' }
        var paymentTypeOthers: String? = binding.tfSubscriptionEditPaymentTypeOthers.editText?.text.toString().trim { it <= ' ' }
        val recurrence = binding.tfSubscriptionEditRecurrence.editText?.text.toString().trim { it <= ' ' }
        val notifications = binding.tfSubscriptionEditNotifications.editText?.text.toString().trim { it <= ' ' }
        var notes: String? = binding.tfSubscriptionEditNotes.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (TextUtils.isEmpty(name)) {
            binding.tfSubscriptionEditName.error = getString(R.string.subscription_name_empty)
            errors++
        }

        if (TextUtils.isEmpty(paymentType)) {
            binding.tfSubscriptionEditPaymentType.error = getString(R.string.payment_type_empty)
            errors++
        }
        else {
            if (selectedPaymentType == 4) {
                if (TextUtils.isEmpty(paymentTypeOthers)) {
                    binding.tfSubscriptionEditPaymentTypeOthers.error = getString(R.string.payment_type_empty_2)
                    errors++
                }
            }
            else {
                paymentTypeOthers = null
            }
        }

        if (selectedSubscription.frequency == 1 && TextUtils.isEmpty(recurrence)) {
            binding.tfSubscriptionEditRecurrence.error = getString(R.string.select_recurrence)
            errors++
        }

        if (TextUtils.isEmpty(notifications)) {
            binding.tfSubscriptionEditNotifications.error = getString(R.string.notification_empty)
            errors++
        }

        if (TextUtils.isEmpty(notes)) {
            notes = null
        }

        if (TextUtils.isEmpty(amount)) {
            binding.tfSubscriptionEditAmount.error = getString(R.string.amount_empty)
            errors++
        }
        else {
            if (amount.startsWith("0")) {
                binding.tfSubscriptionEditAmount.error = getString(R.string.amount_starts_0)
                errors++
            }
            else if (amount.toDouble() > remainingBudget) {
                binding.tfSubscriptionEditAmount.error = getString(R.string.amount_overflow)
                errors++
            }
        }

        if (errors == 0) {
            var changes = 0
            if (name.lowercase() != selectedSubscription.nameLower) changes++
            if (amount.toDouble() != selectedSubscription.amount) changes++
            if (selectedPaymentType != selectedSubscription.paymentType) changes++
            if (paymentTypeOthers != selectedSubscription.paymentTypeOthers) changes++
            if (selectedRecurrence != selectedSubscription.recurrence) changes++
            if (selectedDate != selectedSubscription.dueDate) changes++
            if (selectedNotification != selectedSubscription.notification) changes++
            if (notes != selectedSubscription.notes) changes++

            if (changes == 0) {
                dismiss()
            }
            else {
                firebaseUser?.let {
                    showProgressDialog()

                    selectedSubscription.name = name
                    selectedSubscription.paymentType = selectedPaymentType
                    selectedSubscription.paymentTypeOthers = paymentTypeOthers
                    selectedSubscription.recurrence = selectedRecurrence
                    selectedSubscription.dueDate = selectedDate
                    selectedSubscription.notification = selectedNotification
                    selectedSubscription.notes = notes

                    decreaseBudget(it.uid, sharedPreferences.accountId!!, selectedSubscription, amount.toDouble())
                }
            }
        }
    }

    private fun decreaseBudget(uid: String, accountId: String, subscription: Subscription, newAmount: Double) {
        databaseReference =
            database.getReference("budgets")
                .child(uid)
                .child(accountId)
                .child(subscription.categoryId!!)

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val budget = snapshot.getValue<Budget>()
                if (budget != null) {
                    val zdt = ZonedDateTime.ofInstant(
                        Instant.now(),
                        ZoneId.systemDefault()
                    )

                    budget.amountSpent -= subscription.amount
                    budget.amountSpent += newAmount
                    budget.updatedAt = zdt.toInstant().toEpochMilli()

                    databaseReference.setValue(budget)
                        .addOnSuccessListener {
                            decreaseAccountBalance(uid, accountId, subscription, newAmount)
                        }
                        .addOnFailureListener {
                            hideProgressDialog()
                            Toast
                                .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                                .show()
                        }
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun decreaseAccountBalance(uid: String, accountId: String, subscription: Subscription, newAmount: Double) {
        databaseReference =
            database.getReference("accounts")
                .child(uid)
                .child(accountId)
                .child("remainingBalance")

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val remainingBalance = snapshot.value.toString().toDouble()
                var newRemainingBalance = remainingBalance + subscription.amount
                newRemainingBalance -= newAmount

                databaseReference.setValue(newRemainingBalance)
                    .addOnSuccessListener {
                        selectedSubscription.amount = newAmount
                        updateSubscription(uid, accountId, subscription)
                    }
                    .addOnFailureListener {
                        hideProgressDialog()
                        Toast
                            .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                            .show()
                    }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun updateSubscription(uid: String, accountId: String, subscription: Subscription) {
        databaseReference =
            database.getReference("subscriptions")
                .child(uid)
                .child(accountId)
                .child(subscription.id!!)

        databaseReference.setValue(subscription)
            .addOnSuccessListener {
                updateSubscriptionHistory(uid, accountId, subscription)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun updateSubscriptionHistory(uid: String, accountId: String, subscription: Subscription) {
        databaseReference =
            database.getReference("subscriptionHistory")
                .child(uid)
                .child(accountId)
                .child(subscription.id!!)

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val subscriptionHistory = child.getValue<SubscriptionHistory>()
                    if (subscriptionHistory != null && subscriptionHistory.paidAt == null) {
                        when (subscription.frequency) {
                            0 -> { // one time
                                // due date for one time subscription is the same for the history
                                val zdt = ZonedDateTime.ofInstant(
                                    Instant.ofEpochMilli(subscription.dueDate!!),
                                    ZoneId.systemDefault()
                                ).with(LocalTime.MAX)
                                subscriptionHistory.dueAt = zdt.toInstant().toEpochMilli()
                            }
                            1 -> { // recurring
                                val zdt = ZonedDateTime.ofInstant(
                                    Instant.ofEpochMilli(subscriptionHistory.dueAt!!),
                                    ZoneId.systemDefault()
                                ).with(LocalTime.MAX)

                                val startDate = zdt.minusMonths(subscription.recurrence!!.toLong())
                                val newDate = startDate.plusMonths(selectedRecurrence.toLong())
                                subscriptionHistory.dueAt = newDate.toInstant().toEpochMilli()
                            }
                        }

                        databaseReference.child(subscriptionHistory.id!!).setValue(subscriptionHistory)
                            .addOnSuccessListener {
                                if (subscription.notification != 0) {
                                    when (subscription.notification) {
                                        1 -> scheduleNotification(activity, 0, accountId, subscription, subscriptionHistory)
                                        2 -> scheduleNotification(activity, 1, accountId, subscription, subscriptionHistory)
                                        3 -> scheduleNotification(activity, 3, accountId, subscription, subscriptionHistory)
                                        4 -> scheduleNotification(activity, 7, accountId, subscription, subscriptionHistory)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                hideProgressDialog()
                                Toast
                                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                                    .show()
                            }
                    }
                }

                subscriptionViewModel.setSubscription(subscription)
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
        val name = "Subscriptions"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(sharedPreferences.subscriptionsChannelId, name, importance)

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

    private fun scheduleNotification(
        context: Context,
        delay: Long,
        accountId: String,
        subscription: Subscription,
        subscriptionHistory: SubscriptionHistory
    ) {
        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
        binding.pbEditSubscription.visibility = View.VISIBLE
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialog() {
        binding.pbEditSubscription.visibility = View.INVISIBLE
        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}