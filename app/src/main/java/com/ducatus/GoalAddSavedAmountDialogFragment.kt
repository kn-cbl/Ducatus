package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.ducatus.data.Goal
import com.ducatus.data.GoalHistory
import com.ducatus.databinding.FragmentGoalAddSavedAmountDialogBinding
import com.ducatus.viewmodel.AmountViewModel
import com.ducatus.viewmodel.GoalViewModel
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
import java.time.ZoneId
import java.time.ZonedDateTime

class GoalAddSavedAmountDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentGoalAddSavedAmountDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var selectedGoal: Goal
    private var firebaseUser: FirebaseUser? = null
    private val amountViewModel: AmountViewModel by activityViewModels()
    private val goalViewModel: GoalViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentGoalAddSavedAmountDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        inputObserver()

        amountViewModel.amount.observe(viewLifecycleOwner) { amount ->
            amount.getContentIfNotHandled()?.let { content ->
                binding.tfGoalAddSavedAmount.editText?.setText(content)
            }
        }

        binding.tfGoalAddSavedAmount.editText?.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("account", "account")

            val fragmentManager = childFragmentManager
            val newFragment = AmountDialogFragment()
            newFragment.arguments = bundle
            newFragment.show(fragmentManager, "dialog")
        }

        binding.btnGoalAddSavedAmountCancel.setOnClickListener {
            dismiss()
        }

        binding.btnGoalAddSavedAmountSave.setOnClickListener {
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
    }

    private fun inputObserver() {
        binding.tfGoalAddSavedAmount.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfGoalAddSavedAmount.error = getString(R.string.amount_empty)
            }
            else if (text.toString().toDouble() > selectedGoal.targetAmount) {
                binding.tfGoalAddSavedAmount.error = getString(R.string.saved_amount_error)
            }
            else if (text.toString().toDouble() + selectedGoal.savedAmount > selectedGoal.targetAmount) {
                binding.tfGoalAddSavedAmount.error = getString(R.string.saved_amount_error_2)
            }
            else {
                binding.tfGoalAddSavedAmount.error = null
            }
        }

        binding.tfGoalAddSavedAmount.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) {
                text?.clear()
            }
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

        val amount = binding.tfGoalAddSavedAmount.editText?.text.toString().trim { it <= ' ' }
        if (TextUtils.isEmpty(amount)) {
            binding.tfGoalAddSavedAmount.error = getString(R.string.amount_empty)
        }
        else if (amount.toDouble() > selectedGoal.targetAmount) {
            binding.tfGoalAddSavedAmount.error = getString(R.string.saved_amount_error)
        }
        else if (amount.toDouble() + selectedGoal.savedAmount > selectedGoal.targetAmount) {
            binding.tfGoalAddSavedAmount.error = getString(R.string.saved_amount_error_2)
        }
        else {
            firebaseUser?.let {
                showProgressDialog()

                val sharedPreferences = SharedPreferences(activity)
                val currentAccountId = sharedPreferences.accountId!!
                database = Firebase.database

                updateGoal(it.uid, currentAccountId, selectedGoal, amount.toDouble())
            }
        }
    }

    private fun updateGoal(uid: String, accountId: String, goal: Goal, amount: Double) {
        databaseReference =
            database.getReference("goals")
                .child(uid)
                .child(accountId)
                .child(goal.id)

        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        ).toInstant().toEpochMilli()

        goal.updatedAt = zdtToday
        goal.savedAmount += amount

        // has reached goal
        if (goal.savedAmount >= goal.targetAmount) {
            goal.reachedDate = zdtToday
            goal.status = "R"
        }

        databaseReference.setValue(goal)
            .addOnSuccessListener {
                addSavedAmount(uid, accountId, goal, amount)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage!!, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun addSavedAmount(uid: String, accountId: String, goal: Goal, amount: Double) {
        databaseReference =
            database.getReference("goalHistory")
                .child(uid)
                .child(accountId)
                .child(goal.id)

        val key = databaseReference.push().key!!

        val goalHistory = GoalHistory(
            key,
            amount,
            goal.updatedAt!!,
            goal.id
        )

        databaseReference.child(key).setValue(goalHistory)
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
        binding.pbGoalAddSavedAmount.visibility = View.VISIBLE
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialog() {
        binding.pbGoalAddSavedAmount.visibility = View.INVISIBLE
        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}