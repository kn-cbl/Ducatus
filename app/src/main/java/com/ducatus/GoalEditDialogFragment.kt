package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.ducatus.data.Goal
import com.ducatus.databinding.FragmentGoalEditDialogBinding
import com.ducatus.viewmodel.*
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

class GoalEditDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentGoalEditDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var currentGoalNameLower: String
    private lateinit var selectedGoal: Goal
    private var firebaseUser: FirebaseUser? = null
    private var selectedDate: Long = 0
    private val amountViewModel: AmountViewModel by activityViewModels()
    private val colorViewModel: ColorViewModel by activityViewModels()
    private val iconViewModel: IconViewModel by activityViewModels()
    private val goalViewModel: GoalViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentGoalEditDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        setDatePicker()
        inputObserver()

        amountViewModel.amount.observe(this) { amount ->
            amount.getContentIfNotHandled()?.let { content ->
                binding.tfEditGoalTargetAmount.editText?.setText(content)
            }
        }

        colorViewModel.color.observe(this) { selectedColor ->
            setColor(selectedColor)
        }

        iconViewModel.icon.observe(this) { selectedIcon ->
            setIcon(selectedIcon)
        }

        binding.tfEditGoalTargetAmount.editText?.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("account", "account")

            val fragmentManager = childFragmentManager
            val newFragment = AmountDialogFragment()
            newFragment.arguments = bundle
            newFragment.show(fragmentManager, "dialog")
        }

        binding.btnGoalEditCancel.setOnClickListener {
            dismiss()
        }

        binding.btnGoalEditSave.setOnClickListener {
            validateData()
        }
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            sessionExpired()
        }

        val strGoal = arguments?.getString("goal")
        selectedGoal = Gson().fromJson(strGoal, Goal::class.java)
        currentGoalNameLower = selectedGoal.nameLower

        binding.tfEditGoalName.editText?.setText(selectedGoal.name)
        binding.tfEditGoalTargetAmount.editText?.setText(selectedGoal.targetAmount.toInt().toString())

        selectedDate = selectedGoal.targetDate
        val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
        val zdtGoalDate = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(selectedGoal.targetDate),
            ZoneId.systemDefault()
        )
        val formattedDate = dtf.format(zdtGoalDate)
        binding.tfEditGoalTargetDate.editText?.setText(formattedDate)

        setColor(selectedGoal.color)
        setIcon(selectedGoal.icon)

        binding.tfEditGoalNotes.editText?.setText(selectedGoal.notes)
    }

    private fun setColor(selectedColor: String) {
        val color = resources.getIdentifier(
            selectedColor,
            "color",
            activity.packageName
        )

        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(activity.getColor(color))
        gradientDrawable.cornerRadius = 16f

        binding.viewEditGoalSelectedColor.background = gradientDrawable
        binding.tfEditGoalColor.tag = selectedColor
        binding.tfEditGoalColor.error = null
    }

    private fun setIcon(selectedIcon: String) {
        val icon = resources.getIdentifier(
            selectedIcon,
            "drawable",
            activity.packageName
        )

        binding.ivEditGoalSelectedIcon.setImageResource(icon)
        binding.ivEditGoalSelectedIcon.setColorFilter(
            ResourcesCompat.getColor(
                resources,
                R.color.darker_gray,
                null
            )
        )

        binding.tfEditGoalIcon.tag = selectedIcon
        binding.tfEditGoalIcon.error = null
    }

    private fun setDatePicker() {
        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )

        val janThisYear = ZonedDateTime.of(zdtToday.year, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())
        val lastTwentyYears = janThisYear.minusYears(20)
        val nextFiveYears = janThisYear.plusYears(5)

        val startDate = lastTwentyYears.toInstant().toEpochMilli()
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

            binding.tfEditGoalTargetDate.editText?.setText(formattedDate)
            selectedDate = endOfDay.toInstant().toEpochMilli()
        }

        binding.tfEditGoalTargetDate.editText?.setOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(childFragmentManager, "tag")
            }
        }

        binding.tfEditGoalTargetDate.setEndIconOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(childFragmentManager, "tag")
            }
        }
    }

    private fun inputObserver() {
        binding.tfEditGoalTargetAmount.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfEditGoalTargetAmount.error = getString(R.string.amount_empty)
            }
            else if (text.toString().toDouble() < selectedGoal.savedAmount) {
                binding.tfEditGoalTargetAmount.error = getString(R.string.target_amount_error)
            }
            else {
                binding.tfEditGoalTargetAmount.error = null
            }
        }

        binding.tfEditGoalTargetAmount.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) {
                text?.clear()
            }
        }

        binding.tfEditGoalName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfEditGoalName.error = getString(R.string.goal_name_empty)
            }
            else {
                binding.tfEditGoalName.error = null
            }
        }

        binding.tfEditGoalTargetDate.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) binding.tfEditGoalTargetDate.error = null
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

        val name = binding.tfEditGoalName.editText?.text.toString().trim { it <= ' ' }
        val targetAmount = binding.tfEditGoalTargetAmount.editText?.text.toString().trim { it <= ' ' }
        val targetDate = binding.tfEditGoalTargetDate.editText?.text.toString().trim { it <= ' ' }
        val color = binding.tfEditGoalColor.tag
        val icon = binding.tfEditGoalIcon.tag
        var notes: String? = binding.tfEditGoalNotes.editText?.text.toString().trim { it <= ' ' }
        var errors = 0

        if (TextUtils.isEmpty(name)) {
            binding.tfEditGoalName.error = getString(R.string.goal_name_empty)
            errors++
        }
        if (TextUtils.isEmpty(targetDate)) {
            binding.tfEditGoalTargetDate.error = getString(R.string.date_empty)
            errors++
        }
        if (color == null) {
            binding.tfEditGoalColor.error = getString(R.string.select_a_color)
            errors++
        }
        if (icon == null) {
            binding.tfEditGoalIcon.error = getString(R.string.select_an_icon)
            errors++
        }
        if (TextUtils.isEmpty(notes)) {
            notes = null
        }

        if (TextUtils.isEmpty(targetAmount)) {
            binding.tfEditGoalTargetAmount.error = getString(R.string.amount_empty)
            errors++
        }
        else {
            if (targetAmount.startsWith("0")) {
                binding.tfEditGoalTargetAmount.error = getString(R.string.amount_starts_0)
                errors++
            }
            else if (targetAmount.toDouble() < selectedGoal.savedAmount) {
                binding.tfEditGoalTargetAmount.error = getString(R.string.target_amount_error)
                errors++
            }
        }

        if (errors == 0) {
            var changes = 0
            if (name.lowercase() != selectedGoal.nameLower) changes++
            if (targetAmount.toDouble() != selectedGoal.targetAmount) changes++
            if (selectedDate != selectedGoal.targetDate) changes++
            if (color.toString() != selectedGoal.color) changes++
            if (icon.toString() != selectedGoal.icon) changes++
            if (notes != selectedGoal.notes) changes++

            if (changes == 0) {
                dismiss()
            }
            else {
                firebaseUser?.let {
                    showProgressDialog()
                    val sharedPreferences = SharedPreferences(activity)
                    val currentAccountId = sharedPreferences.accountId.toString()

                    selectedGoal.name = name
                    selectedGoal.nameLower = name.lowercase()
                    selectedGoal.targetAmount = targetAmount.toDouble()
                    selectedGoal.targetDate = selectedDate
                    selectedGoal.color = color.toString()
                    selectedGoal.icon = icon.toString()
                    selectedGoal.notes = notes

                    goalExists(it.uid, currentAccountId, selectedGoal)
                }
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
                    updateGoal(goal)
                }
                else {
                    if (currentGoalNameLower == goal.nameLower) {
                        updateGoal(goal)
                    }
                    else {
                        hideProgressDialog()
                        binding.tfEditGoalName.error = getString(R.string.goal_name_exists)
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

    private fun updateGoal(goal: Goal) {
        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )
        goal.updatedAt = zdtToday.toInstant().toEpochMilli()

        goal.status =
            if (goal.targetAmount == goal.savedAmount) "R"
            else "A"

        databaseReference.child(goal.id).setValue(goal)
            .addOnSuccessListener {
                goalViewModel.setGoal(goal)
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
        binding.pbEditGoal.visibility = View.VISIBLE
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialog() {
        binding.pbEditGoal.visibility = View.INVISIBLE
        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}