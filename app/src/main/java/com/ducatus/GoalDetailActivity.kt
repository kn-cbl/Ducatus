package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import com.ducatus.adapter.GoalHistoryAdapter
import com.ducatus.data.Goal
import com.ducatus.data.GoalHistory
import com.ducatus.databinding.ActivityGoalDetailBinding
import com.ducatus.viewmodel.GoalViewModel
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
import com.google.gson.Gson
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class GoalDetailActivity : AppCompatActivity() {
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityGoalDetailBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var currentAccountId: String
    private lateinit var selectedGoal: Goal
    private var firebaseUser: FirebaseUser? = null
    private val goalViewModel: GoalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        loadData()

        binding.tbGoalDetail.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbGoalDetail.inflateMenu(R.menu.more_menu)
        binding.tbGoalDetail.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.more -> {
                    showPopup(findViewById(R.id.more))
                    true
                }
                else -> false
            }
        }

        // add goal monitoring on fast progress
        // saving trend for the month

        goalViewModel.goal.observe(this) { goal ->
            goal.getContentIfNotHandled()?.let { content ->
                firebaseUser?.let {
                    selectedGoal = content
                    loadGoal(it.uid, currentAccountId, selectedGoal)
                }
            }
        }

        binding.btnGoalDetailAddSavedAmount.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("goal", Gson().toJson(selectedGoal))

            val fragmentManager = supportFragmentManager
            val newFragment = GoalAddSavedAmountDialogFragment()
            newFragment.arguments = bundle
            newFragment.show(fragmentManager, "dialog")
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun showPopup(view: View) {
        val popup = PopupMenu(this, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.optionPause -> {
                    firebaseUser?.let {
                        confirmPause(it.uid, currentAccountId, selectedGoal)
                    }
                    true
                }
                R.id.optionResume -> {
                    firebaseUser?.let {
                        confirmResume(it.uid, currentAccountId, selectedGoal)
                    }
                    true
                }
                R.id.optionEdit -> {
                    firebaseUser?.let {
                        val bundle = Bundle()
                        bundle.putSerializable("goal", Gson().toJson(selectedGoal))

                        val fragmentManager = supportFragmentManager
                        val newFragment = GoalEditDialogFragment()
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
                R.id.optionDelete -> {
                    firebaseUser?.let {
                        confirmDelete(it.uid, currentAccountId, selectedGoal.id)
                    }
                    true
                }
                else -> false
            }
        }

        // menu to inflate
        when (selectedGoal.status) {
            "A" -> popup.menuInflater.inflate(R.menu.pause_edit_delete_menu, popup.menu)
            "P" -> popup.menuInflater.inflate(R.menu.resume_edit_delete_menu, popup.menu)
            "R" -> popup.menuInflater.inflate(R.menu.edit_options_2_menu, popup.menu)
        }

        popup.show()
    }

    private fun loadData() {
        showProgressDialog()
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        firebaseUser?.run {
            database = Firebase.database
            val strGoal = intent.getStringExtra("goal")
            selectedGoal = Gson().fromJson(strGoal, Goal::class.java)

            val sharedPreferences = SharedPreferences(applicationContext)
            currentAccountId = sharedPreferences.accountId.toString()

            loadGoal(uid, currentAccountId, selectedGoal)
        } ?: sessionExpired()
    }

    private fun loadGoal(uid: String, accountId: String, goal: Goal) {
        showProgressDialog()
        loadGoalHistory(uid, accountId, goal.id)

        val iconColor = resources.getIdentifier(
            goal.color,
            "color",
            packageName
        )

        binding.flGoalDetailIcon.backgroundTintList =
            ContextCompat.getColorStateList(this, iconColor)

        val icon = resources.getIdentifier(
            goal.icon,
            "drawable",
            packageName
        )

        binding.ivGoalDetailIcon.setImageResource(icon)
        binding.ivGoalDetailIcon.setColorFilter(
            ResourcesCompat.getColor(
                resources,
                R.color.white,
                null
            )
        )

        binding.tvGoalDetailName.text = goal.name
        binding.tvGoalDetailNotes.text =
            if (goal.notes == null) "No notes"
            else goal.notes

        val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
        var zdt = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(goal.targetDate),
            ZoneId.systemDefault()
        )
        var formattedDate = dtf.format(zdt)
        var dateText = "Target date: $formattedDate"

        if (goal.status == "R") {
            zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(goal.reachedDate!!),
                ZoneId.systemDefault()
            )
            formattedDate = dtf.format(zdt)
            dateText = "Reached: $formattedDate"
        }

        binding.tvGoalDetailTargetDate.text = dateText

        val goalAmount = goal.targetAmount
        val goalSaved = goal.savedAmount
        val goalRemaining = goalAmount.minus(goalSaved)

        val goalAmountText = "₱" + String.format("%,.2f", goalAmount)
        binding.tvGoalDetailTargetAmount.text = goalAmountText

        val goalSavedText = "₱" + String.format("%,.2f", goalSaved)
        binding.tvGoalDetailSaved.text = goalSavedText

        val goalRemainingText = "₱" + String.format("%,.2f", goalRemaining)
        binding.tvGoalDetailRemaining.text = goalRemainingText

        val goalProgress = ((goalSaved / goalAmount) * 100).toInt()
        binding.pbGoalDetailStatus.progress = goalProgress
        binding.pbGoalDetailStatus.setIndicatorColor(
            ContextCompat.getColor(this, iconColor)
        )

        val progressText = "$goalProgress%"
        binding.tvGoalDetailProgress.text = progressText
        binding.tvGoalDetailProgress.setTextColor(
            ContextCompat.getColor(this, iconColor)
        )

        if (goal.status != "R") {
            val overdueText = isOverdue(goal.targetDate)
            if (overdueText != null) {
                binding.tvGoalDetailOverdue.text = overdueText
            }
            else {
                binding.tvGoalDetailOverdue.visibility = View.GONE
            }
        }

        if (goal.status == "A") {
            if (goal.targetAmount == goal.savedAmount) {
                binding.btnGoalDetailAddSavedAmount.visibility = View.GONE
            }
            else {
                binding.btnGoalDetailAddSavedAmount.visibility = View.VISIBLE
            }
        }
        else {
            binding.btnGoalDetailAddSavedAmount.visibility = View.GONE

            if (goal.status == "R") {
                binding.tvGoalDetailOverdue.visibility = View.GONE
            }
        }
    }

    private fun isOverdue(targetDate: Long): String? {
        var dateText: String? = null

        val startDate = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(targetDate),
            ZoneId.systemDefault()
        )
        val endDate = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )

        val startEpoch = startDate.toInstant().toEpochMilli()
        val endEpoch = endDate.toInstant().toEpochMilli()

        if (startEpoch < endEpoch) {
            val elapsedYears = ChronoUnit.YEARS.between(startDate, endDate)
            val elapsedMonths = ChronoUnit.MONTHS.between(startDate, endDate)
            val elapsedDays = ChronoUnit.DAYS.between(startDate, endDate)
            val elapsedHours = ChronoUnit.HOURS.between(startDate, endDate)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startDate, endDate)
            val elapsedSeconds = ChronoUnit.SECONDS.between(startDate, endDate)

            dateText =
                if (elapsedYears > 0) {
                    if (elapsedYears.toInt() == 1) {
                        "$elapsedYears year overdue"
                    }
                    else {
                        "$elapsedYears years overdue"
                    }
                }
                else if (elapsedMonths > 0) {
                    if (elapsedMonths.toInt() == 1) {
                        "$elapsedMonths month overdue"
                    }
                    else {
                        "$elapsedMonths months overdue"
                    }
                }
                else if (elapsedDays > 0) {
                    if (elapsedDays.toInt() == 1) {
                        "$elapsedDays day overdue"
                    }
                    else {
                        "$elapsedDays days overdue"
                    }
                }
                else if (elapsedHours > 0) {
                    if (elapsedHours.toInt() == 1) {
                        "$elapsedHours hour overdue"
                    }
                    else {
                        "$elapsedHours hours overdue"
                    }
                }
                else if (elapsedMinutes > 0) {
                    if (elapsedMinutes.toInt() == 1) {
                        "$elapsedMinutes minutes overdue"
                    }
                    else {
                        "$elapsedMinutes minute overdue"
                    }
                }
                else {
                    if (elapsedSeconds.toInt() == 1) {
                        "$elapsedSeconds second overdue"
                    }
                    else {
                        "$elapsedSeconds seconds overdue"
                    }
                }
        }

        return dateText
    }

    private fun loadGoalHistory(uid: String, accountId: String, goalId: String) {
        databaseReference =
            database
                .getReference("goalHistory")
                .child(uid)
                .child(accountId)
                .child(goalId)

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val goalHistoryAdapter = GoalHistoryAdapter(mutableListOf())
                binding.rvGoalHistory.adapter = goalHistoryAdapter
                binding.rvGoalHistory.layoutManager = GridLayoutManager(
                    this,
                    2,
                    GridLayoutManager.VERTICAL,
                    false
                )

                val goalsHistory = mutableListOf<GoalHistory>()
                for (child in snapshot.children) {
                    val goalHistory  = child.getValue<GoalHistory>()
                    if (goalHistory != null) {
                        goalsHistory.add(goalHistory)
                    }
                }

                // sort by latest date
                goalsHistory.sortByDescending { it.date }

                for (goalHistory in goalsHistory) {
                    goalHistoryAdapter.addGoalHistory(goalHistory)
                }

                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clGoalDetail, getString(R.string.load_goal_history_error),5000)
                    .show()
            }
    }

    private fun confirmPause(uid: String, accountId: String, goal: Goal) {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.pause_goal_title))
            .setPositiveButton(resources.getString(R.string.pause)) { _, _ ->
                updateGoalStatus(uid, accountId, goal, "P")
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> } // do nothing
            .show()
    }

    private fun confirmResume(uid: String, accountId: String, goal: Goal) {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.resume_goal_title))
            .setPositiveButton(resources.getString(R.string.resume)) { _, _ ->
                updateGoalStatus(uid, accountId, goal, "A")
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> } // do nothing
            .show()
    }

    private fun updateGoalStatus(uid: String, accountId: String, goal: Goal, status: String) {
        val progressTitle = when (status) {
            "P" -> getString(R.string.pausing)
            "A" -> getString(R.string.resuming)
            else -> getString(R.string.updating)
        }
        showProgressDialogAction(progressTitle)

        databaseReference =
            database.getReference("goals")
                .child(uid)
                .child(accountId)
                .child(goal.id)

        goal.status = status
        databaseReference.setValue(goal)
            .addOnSuccessListener {
                hideProgressDialogAction()
                loadGoal(uid, accountId, goal)
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clGoalDetail, getString(R.string.pause_goal_error),5000)
                    .show()
            }
    }

    private fun confirmDelete(uid: String, accountId: String, goalId: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.delete_goal_title))
            .setMessage(resources.getString(R.string.delete_goal_message))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ ->
                deleteGoal(uid, accountId, goalId)
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> } // do nothing
            .show()
    }

    private fun deleteGoal(uid: String, accountId: String, goalId: String) {
        showProgressDialogAction(getString(R.string.deleting))
        databaseReference =
            database.getReference("goals")
                .child(uid)
                .child(accountId)
                .child(goalId)

        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteGoalHistory(uid, accountId, goalId)
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clGoalDetail, getString(R.string.delete_goal_error),5000)
                    .show()
            }
    }

    private fun deleteGoalHistory(uid: String, accountId: String, goalId: String) {
        databaseReference =
            database.getReference("goalHistory")
                .child(uid)
                .child(accountId)
                .child(goalId)

        databaseReference.removeValue()
            .addOnSuccessListener {
                hideProgressDialogAction()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clGoalDetail, it.localizedMessage!!,5000)
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
        binding.pbGoalDetail.visibility = View.VISIBLE
        binding.svGoalDetail.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbGoalDetail.visibility = View.GONE
        binding.svGoalDetail.visibility = View.VISIBLE
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