package com.ducatus

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.adapter.ExpenseHistoryAdapter
import com.ducatus.data.*
import com.ducatus.databinding.ActivityBudgetDetailBinding
import com.ducatus.interfaces.ExpenseHistoryInterface
import com.ducatus.viewmodel.BudgetViewModel
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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class BudgetDetailActivity : AppCompatActivity(), ExpenseHistoryInterface {
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityBudgetDetailBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var viewImageDialog: Dialog
    private lateinit var selectedBudget: Budget
    private var firebaseUser: FirebaseUser? = null
    private var totalExpenseAmount = 0.0
    private val budgetViewModel: BudgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        loadData()

        binding.tbBudgetDetail.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbBudgetDetail.inflateMenu(R.menu.edit_delete_menu)
        binding.tbBudgetDetail.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.edit -> {
                    firebaseUser?.let {
                        val bundle = Bundle()
                        bundle.putString("budget", Gson().toJson(selectedBudget))

                        val fragmentManager = supportFragmentManager
                        val newFragment = BudgetEditDialogFragment()
                        newFragment.arguments = bundle
                        newFragment.show(fragmentManager, "dialog")
                    }

                    true
                }
                R.id.delete -> {
                    firebaseUser?.let {
                        confirmDelete(
                            it.uid,
                            sharedPreferences.accountId.toString(),
                            selectedBudget.id!!
                        )
                    }
                    true
                }
                else -> false
            }
        }

        budgetViewModel.budget.observe(this) { budget ->
            budget.getContentIfNotHandled()?.let { content ->
                firebaseUser?.let {
                    selectedBudget = content
                    loadBudget(selectedBudget)
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun getActivityInterface(): Activity {
        return this
    }

    override fun viewImage(imagePath: String) {
        viewImageDialog.show()
        storageReference.child(imagePath).downloadUrl
            .addOnSuccessListener { uri ->
                Picasso.get()
                    .load(uri)
                    .into(viewImageDialog.findViewById<ImageView>(R.id.ivViewImage))
            }
            .addOnFailureListener {
                viewImageDialog.dismiss()
                Snackbar
                    .make(binding.clBudgetDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            viewImageDialog = Dialog(this)
            viewImageDialog.setContentView(R.layout.fragment_view_image_dialog)

            sharedPreferences = SharedPreferences(this)
            val currentAccountId = sharedPreferences.accountId.toString()

            val strBudget = intent.getStringExtra("budget")
            selectedBudget = Gson().fromJson(strBudget, Budget::class.java)

            database = Firebase.database
            storage = FirebaseStorage.getInstance()
            storageReference =
                storage.getReference("transactions")
                    .child(firebaseUser!!.uid)

            loadBudget(selectedBudget)
            loadTransactions(firebaseUser!!.uid, currentAccountId, selectedBudget.id!!)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadBudget(budget: Budget) {
        showProgressDialog()
        val iconColor = resources.getIdentifier(
            budget.categoryColor,
            "color",
            packageName
        )

        binding.flBudgetDetailCategoryIcon.backgroundTintList =
            ContextCompat.getColorStateList(this, iconColor)

        val icon = resources.getIdentifier(
            budget.categoryIcon,
            "drawable",
            packageName
        )

        binding.ivBudgetDetailCategoryIcon.setImageResource(icon)
        binding.ivBudgetDetailCategoryIcon.setColorFilter(
            ResourcesCompat.getColor(
                resources,
                R.color.white,
                null
            )
        )

        binding.tvBudgetDetailCategory.text = budget.categoryName

        val budgetTotal = budget.amountTotal.toString().toDouble()
        val budgetSpent = budget.amountSpent.toString().toDouble()
        val budgetLeft = budgetTotal.minus(budgetSpent)

        val spentText = "₱" + String.format("%,.2f", budgetSpent)
        binding.tvBudgetDetailSpent.text = spentText
        binding.tvBudgetDetailSpent.setTextColor(
            ContextCompat.getColor(this, iconColor)
        )

        val budgetLeftText = "₱" + String.format("%,.2f", budgetLeft)
        binding.tvBudgetDetailLeft.text = budgetLeftText
        binding.tvBudgetDetailLeft.setTextColor(
            ContextCompat.getColor(this, iconColor)
        )

        val budgetTotalText = "₱" + String.format("%,.2f", budgetTotal)
        binding.tvBudgetDetailLimit.text = budgetTotalText
        binding.tvBudgetDetailLimit.setTextColor(
            ContextCompat.getColor(this, iconColor)
        )

        binding.pbBudgetDetailStatus.progress = ((budgetSpent / budgetTotal) * 100).toInt()
        binding.pbBudgetDetailStatus.setIndicatorColor(ContextCompat.getColor(this, iconColor))

        // determine icon and text to display
        var statusIcon = ""
        var statusText = ""

        when (binding.pbBudgetDetailStatus.progress) {
            in 0..59 -> {
                statusIcon = "ic_budget_status_1"
                statusText = "Your budget is on track"
            }
            in 60..99 -> {
                statusIcon = "ic_budget_status_2"
                statusText = "You have almost reached your budget limit"
            }
            100 -> {
                statusIcon = "ic_budget_status_3"
                statusText = "You have reached your budget limit"
            }
        }

        val statusIconRes = resources.getIdentifier(
            statusIcon,
            "drawable",
            packageName
        )

        binding.ivBudgetDetailStatus.setImageResource(statusIconRes)
        binding.tvBudgetDetailStatus.text = statusText
    }

    private fun loadTransactions(uid: String, accountId: String, categoryId: String) {
        showProgressDialog()
        databaseReference = database.getReference("transactions").child(uid).child(accountId)
        val query = databaseReference.orderByChild("categoryId").equalTo(categoryId)
        query.get()
            .addOnSuccessListener { snapshot ->
                val expensesHistory = mutableListOf<ExpenseHistory>()
                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null) {
                        when (transaction.type) {
                            0 -> totalExpenseAmount += transaction.amount
                            1 -> totalExpenseAmount -= transaction.amount
                        }
                        expensesHistory.add(
                            ExpenseHistory(
                                transaction.name,
                                transaction.amount,
                                transaction.date,
                                'T',
                                transaction.type == 0,
                                transaction.paymentType,
                                transaction.imagePath
                            )
                        )
                    }
                }

                loadSubscriptions(uid, accountId, categoryId, expensesHistory)
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clBudgetDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun loadSubscriptions(uid: String, accountId: String, categoryId: String, expensesHistory: MutableList<ExpenseHistory>) {
        databaseReference = database.getReference("subscriptions").child(uid).child(accountId)
        val query = databaseReference.orderByChild("categoryId").equalTo(categoryId)
        query.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val subscription = child.getValue<Subscription>()
                    if (subscription != null) {
                        totalExpenseAmount += subscription.amount
                        expensesHistory.add(
                            ExpenseHistory(
                                subscription.name,
                                subscription.amount,
                                subscription.dueDate,
                                'S',
                                true,
                                subscription.paymentType,
                                null,
                            )
                        )
                    }
                }

                // sort by latest date
                expensesHistory.sortByDescending { it.date!! }
                adaptExpenseHistory(expensesHistory)
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clBudgetDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun adaptExpenseHistory(expensesHistory: MutableList<ExpenseHistory>) {
        val expenseHistoryAdapter = ExpenseHistoryAdapter(mutableListOf(), this)
        binding.rvExpenseHistory.adapter = expenseHistoryAdapter
        binding.rvExpenseHistory.layoutManager = LinearLayoutManager(this)

        for (expenseHistory in expensesHistory) {
            expenseHistoryAdapter.addExpenseHistory(expenseHistory)
        }

        hideProgressDialog()
    }

    private fun confirmDelete(uid: String, accountId: String, budgetId: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.delete_budget))
            .setMessage(resources.getString(R.string.delete_budget_confirm))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ -> deleteBudget(uid, accountId, budgetId) }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    private fun deleteBudget(uid: String, accountId: String, budgetId: String) {
        showProgressDialogDelete()
        databaseReference =
            database
                .getReference("budgets")
                .child(uid)
                .child(accountId)
                .child(budgetId)

        databaseReference.removeValue()
            .addOnSuccessListener {
                unallocateCategory(uid, accountId, budgetId)
            }
            .addOnFailureListener {
                hideProgressDialogDelete()
                Snackbar
                    .make(binding.clBudgetDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun unallocateCategory(uid: String, accountId: String, categoryId: String) {
        databaseReference =
            database.getReference("categories")
                .child(uid)
                .child(accountId)
                .child(categoryId)

        databaseReference.child("allocated").setValue(false)
            .addOnSuccessListener {
                getCategories(uid, accountId, categoryId)
            }
            .addOnFailureListener {
                hideProgressDialogDelete()
                Snackbar
                    .make(binding.clBudgetDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun getCategories(uid: String, accountId: String, categoryId: String) {
        databaseReference = database.getReference("categories").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                var allocatedCategories = 0
                for (child in snapshot.children) {
                    if (child.child("allocated").value.toString() == "true") {
                        allocatedCategories++
                    }
                }

                // cancel all future expense notifications if no categories are allocated after
                // deleting current budget
                if (allocatedCategories == 0) {
                    cancelNotifications(this)
                }

                deleteTransactions(uid, accountId, categoryId)
            }
            .addOnFailureListener {
                hideProgressDialogDelete()
                Snackbar
                    .make(binding.clBudgetDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun cancelNotifications(context: Context) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.expensesChannelId)
        if (notificationChannel != null) {
            val notificationIntent = Intent(context, NotificationReceiver::class.java)
            notificationIntent.action = "com.ducatus.EXPENSE"

            val zdt = ZonedDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            for (i in 0 until 14) {
                val notificationId = zdt.dayOfYear.plus(i)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.cancel(pendingIntent)
            }
        }
    }

    private fun deleteTransactions(uid: String, accountId: String, categoryId: String) {
        databaseReference = database.getReference("transactions").child(uid).child(accountId)
        val query = databaseReference.orderByChild("categoryId").equalTo(categoryId)
        query.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null) {
                        databaseReference.child(transaction.id!!).removeValue()
                            .addOnFailureListener {
                                hideProgressDialogDelete()
                                Snackbar
                                    .make(binding.clBudgetDetail, it.localizedMessage!!,5000)
                                    .show()
                            }
                    }
                }

                deleteSubscriptions(uid, accountId, categoryId)
            }
            .addOnFailureListener {
                hideProgressDialogDelete()
                Snackbar
                    .make(binding.clBudgetDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun deleteSubscriptions(uid: String, accountId: String, categoryId: String) {
        databaseReference = database.getReference("subscriptions").child(uid).child(accountId)
        val query = databaseReference.orderByChild("categoryId").equalTo(categoryId)
        query.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val subscription = child.getValue<Subscription>()
                    if (subscription != null) {
                        databaseReference =
                            database.getReference("subscriptions")
                                .child(uid)
                                .child(accountId)

                        databaseReference.child(subscription.id!!).removeValue()
                            .addOnSuccessListener {
                                deleteSubscriptionHistory(uid, accountId, subscription.id!!)
                            }
                            .addOnFailureListener {
                                hideProgressDialogDelete()
                                Snackbar
                                    .make(binding.clBudgetDetail, it.localizedMessage!!,5000)
                                    .show()
                            }
                    }
                }

                updateAccount(uid, accountId)
            }
            .addOnFailureListener {
                hideProgressDialogDelete()
                Snackbar
                    .make(binding.clBudgetDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun deleteSubscriptionHistory(uid: String, accountId: String, subscriptionId: String) {
        databaseReference =
            database.getReference("subscriptionHistory")
                .child(uid)
                .child(accountId)
                .child(subscriptionId)

        databaseReference.removeValue()
            .addOnFailureListener {
                hideProgressDialogDelete()
                Snackbar
                    .make(binding.clBudgetDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun updateAccount(uid: String, accountId: String) {
        databaseReference = database.getReference("accounts").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val account = snapshot.getValue<Account>()
                if (account != null) {
                    account.remainingBalance += totalExpenseAmount
                    account.remainingBudget += selectedBudget.amountTotal

                    databaseReference.setValue(account)
                        .addOnSuccessListener {
                            hideProgressDialogDelete()
                            onBackPressed()
                        }
                        .addOnFailureListener {
                            hideProgressDialogDelete()
                            Snackbar
                                .make(binding.clBudgetDetail, it.localizedMessage!!, 5000)
                                .show()
                        }
                }
            }
            .addOnFailureListener {
                hideProgressDialogDelete()
                Snackbar
                    .make(binding.clBudgetDetail, it.localizedMessage!!,5000)
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
        binding.pbBudgetDetail.visibility = View.VISIBLE
        binding.llBudgetDetail.visibility = View.GONE
        binding.rvExpenseHistory.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbBudgetDetail.visibility = View.INVISIBLE
        binding.llBudgetDetail.visibility = View.VISIBLE
        binding.rvExpenseHistory.visibility = View.VISIBLE
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