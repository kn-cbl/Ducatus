package com.ducatus

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import com.ducatus.data.Goal
import com.ducatus.data.GoalHistory
import com.ducatus.databinding.ActivityGoalAddBinding
import com.ducatus.viewmodel.AmountViewModel
import com.ducatus.viewmodel.AmountViewModel2
import com.ducatus.viewmodel.ColorViewModel
import com.ducatus.viewmodel.IconViewModel
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
import com.google.firebase.ktx.Firebase
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class GoalAddActivity : AppCompatActivity() {
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityGoalAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private var firebaseUser: FirebaseUser? = null
    private var selectedDate: Long = 0
    private val amountViewModel: AmountViewModel by viewModels()
    private val amountViewModel2: AmountViewModel2 by viewModels()
    private val colorViewModel: ColorViewModel by viewModels()
    private val iconViewModel: IconViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalAddBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadData()
        setDatePicker()
        inputObserver()

        binding.tbGoalAdd.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbGoalAdd.inflateMenu(R.menu.check_menu)
        binding.tbGoalAdd.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.done -> {
                    validateData()
                    true
                }
                else -> false
            }
        }

        amountViewModel.amount.observe(this) { amount ->
            amount.getContentIfNotHandled()?.let { content ->
                binding.tfGoalAddTargetAmount.editText?.setText(content)
            }
        }

        amountViewModel2.amount.observe(this) { amount ->
            amount.getContentIfNotHandled()?.let { content ->
                binding.tfGoalAddSavedAlready.editText?.setText(content)
            }
        }

        colorViewModel.color.observe(this) { selectedColor ->
            setColor(selectedColor)
        }

        iconViewModel.icon.observe(this) { selectedIcon ->
            setIcon(selectedIcon)
        }

        binding.tfGoalAddTargetAmount.editText?.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("account", "account")

            val fragmentManager = supportFragmentManager
            val newFragment = AmountDialogFragment()
            newFragment.arguments = bundle
            newFragment.show(fragmentManager, "dialog")
        }

        binding.tfGoalAddSavedAlready.editText?.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("account", "account")

            val fragmentManager = supportFragmentManager
            val newFragment = AmountDialog2Fragment()
            newFragment.arguments = bundle
            newFragment.show(fragmentManager, "dialog")
        }

        binding.tfGoalAddColor.editText?.setOnClickListener {
            val fragmentManager = supportFragmentManager
            val newFragment = ColorDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        binding.tfGoalAddIcon.editText?.setOnClickListener {
            val fragmentManager = supportFragmentManager
            val newFragment = IconDialogFragment()
            newFragment.show(fragmentManager, "dialog")
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

    private fun setColor(selectedColor: String) {
        val color = resources.getIdentifier(
            selectedColor,
            "color",
            packageName
        )

        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(getColor(color))
        gradientDrawable.cornerRadius = 16f

        binding.viewGoalAddSelectedColor.background = gradientDrawable
        binding.tfGoalAddColor.tag = selectedColor
        binding.tfGoalAddColor.error = null
    }

    private fun setIcon(selectedIcon: String) {
        val icon = resources.getIdentifier(
            selectedIcon,
            "drawable",
            packageName
        )

        binding.ivGoalAddSelectedIcon.setImageResource(icon)
        binding.ivGoalAddSelectedIcon.setColorFilter(
            ResourcesCompat.getColor(
                resources,
                R.color.darker_gray,
                null
            )
        )

        binding.tfGoalAddIcon.tag = selectedIcon
        binding.tfGoalAddIcon.error = null
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

            binding.tfGoalAddTargetDate.editText?.setText(formattedDate)
            selectedDate = endOfDay.toInstant().toEpochMilli()
        }

        binding.tfGoalAddTargetDate.editText?.setOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "tag")
            }
        }

        binding.tfGoalAddTargetDate.setEndIconOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "tag")
            }
        }
    }

    private fun inputObserver() {
        var targetAmount: String? = null
        var savedAlready: String? = null

        binding.tfGoalAddTargetAmount.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                targetAmount = null
                binding.tfGoalAddTargetAmount.error = getString(R.string.amount_empty)
            }
            else {
                targetAmount = text.toString()
                binding.tfGoalAddTargetAmount.error = null
                binding.tfGoalAddSavedAlready.error = null

                if (savedAlready != null) {
                    if (text.toString().toDouble() < binding.tfGoalAddSavedAlready.editText?.text.toString().toDouble()) {
                        binding.tfGoalAddSavedAlready.error = getString(R.string.saved_amount_error)
                    }
                }
            }
        }

        binding.tfGoalAddTargetAmount.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) {
                text?.clear()
            }
        }

        binding.tfGoalAddSavedAlready.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                savedAlready = null
                binding.tfGoalAddSavedAlready.error = null
            }
            else {
                savedAlready = text.toString()
                binding.tfGoalAddSavedAlready.error = null

                if (targetAmount != null) {
                    if (text.toString().toDouble() > binding.tfGoalAddTargetAmount.editText?.text.toString().toDouble()) {
                        binding.tfGoalAddSavedAlready.error = getString(R.string.saved_amount_error)
                    }
                }
            }
        }

        binding.tfGoalAddSavedAlready.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) {
                text?.clear()
            }
        }

        binding.tfGoalAddName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfGoalAddName.error = getString(R.string.goal_name_empty)
            }
            else {
                binding.tfGoalAddName.error = null
            }
        }

        binding.tfGoalAddTargetDate.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfGoalAddTargetDate.error = null
        }
    }

    private fun validateData() {
        // hide keyboard
        try {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}


        val name = binding.tfGoalAddName.editText?.text.toString().trim { it <= ' ' }
        val targetAmount = binding.tfGoalAddTargetAmount.editText?.text.toString().trim { it <= ' ' }
        var savedAlready = binding.tfGoalAddSavedAlready.editText?.text.toString().trim { it <= ' ' }
        val targetDate = binding.tfGoalAddTargetDate.editText?.text.toString().trim { it <= ' ' }
        val color = binding.tfGoalAddColor.tag
        val icon = binding.tfGoalAddIcon.tag
        var notes: String? = binding.tfGoalAddNotes.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (TextUtils.isEmpty(name)) {
            binding.tfGoalAddName.error = getString(R.string.goal_name_empty)
            errors++
        }
        if (TextUtils.isEmpty(targetDate)) {
            binding.tfGoalAddTargetDate.error = getString(R.string.date_empty)
            errors++
        }
        if (color == null) {
            binding.tfGoalAddColor.error = getString(R.string.select_a_color)
            errors++
        }
        if (icon == null) {
            binding.tfGoalAddIcon.error = getString(R.string.select_an_icon)
            errors++
        }
        if (TextUtils.isEmpty(notes)) {
            notes = null
        }

        if (TextUtils.isEmpty(targetAmount)) {
            binding.tfGoalAddTargetAmount.error = getString(R.string.amount_empty)
            errors++
        }
        else {
            if (targetAmount.startsWith("0")) {
                binding.tfGoalAddTargetAmount.error = getString(R.string.amount_starts_0)
                errors++
            }
        }

        if (TextUtils.isEmpty(savedAlready)) {
            savedAlready = "0"
        }
        else {
            if (savedAlready.startsWith("0")) {
                binding.tfGoalAddSavedAlready.error = getString(R.string.amount_starts_0)
                errors++
            }
            else if (savedAlready > targetAmount) {
                binding.tfGoalAddSavedAlready.error = getString(R.string.saved_amount_error)
                errors++
            }
        }

        if (errors == 0) {
            firebaseUser?.let {
                showProgressDialog()
                val sharedPreferences = SharedPreferences(this)
                val currentAccountId = sharedPreferences.accountId.toString()

                val goal = Goal(
                    "",
                    name,
                    name.lowercase(),
                    selectedDate,
                    null,
                    targetAmount.toDouble(),
                    savedAlready.toDouble(),
                    color.toString(),
                    icon.toString(),
                    notes,
                    "A"
                )

                goalExists(it.uid, currentAccountId, goal)
            }
        }
    }

    private fun goalExists(uid: String, accountId: String, goal: Goal) {
        database = Firebase.database
        databaseReference = database.getReference("goals").child(uid).child(accountId)
        val query = databaseReference.orderByChild("nameLower").equalTo(goal.nameLower)
        query.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    addGoal(uid, accountId, goal)
                }
                else {
                    hideProgressDialog()
                    binding.tfGoalAddName.error = getString(R.string.goal_name_exists)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.llGoalAdd, getString(R.string.add_goal_error), 5000)
                    .show()
            }
    }

    private fun addGoal(uid: String, accountId: String, goal: Goal) {
        val key = databaseReference.push().key!!
        goal.id = key

        databaseReference.child(key).setValue(goal)
            .addOnSuccessListener {
                if (goal.savedAmount != 0.0) {
                    addSavedAmount(uid, accountId, goal)
                }
                else {
                    hideProgressDialog()
                    onBackPressed()
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.llGoalAdd, getString(R.string.add_goal_error), 5000)
                    .show()
            }
    }

    private fun addSavedAmount(uid: String, accountId: String, goal: Goal) {
        databaseReference =
            database.getReference("goalHistory")
                .child(uid)
                .child(accountId)
                .child(goal.id)

        val key = databaseReference.push().key!!

        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        ).toInstant().toEpochMilli()

        val goalHistory = GoalHistory(
            key,
            goal.savedAmount,
            zdtToday,
            goal.id
        )

        databaseReference.child(key).setValue(goalHistory)
            .addOnSuccessListener {
                hideProgressDialog()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.llGoalAdd, it.localizedMessage!!, 5000)
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
        bundle.putString("title", getString(R.string.adding))

        actionDialog = ActionDialogFragment()
        actionDialog.arguments = bundle
        actionDialog.show(supportFragmentManager, "dialog")
    }

    private fun hideProgressDialog() {
        actionDialog.dismiss()
    }
}