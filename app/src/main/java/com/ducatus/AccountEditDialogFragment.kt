package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.ducatus.data.Account
import com.ducatus.databinding.FragmentAccountEditDialogBinding
import com.ducatus.viewmodel.UpdateViewModel
import com.ducatus.viewmodel.AmountViewModel
import com.ducatus.viewmodel.ColorViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AccountEditDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentAccountEditDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var currentAccount: Account
    private lateinit var sharedPreferences: SharedPreferences
    private val args: AccountEditDialogFragmentArgs by navArgs()
    private val amountViewModel: AmountViewModel by activityViewModels()
    private val colorViewModel: ColorViewModel by activityViewModels()
    private val updateViewModel: UpdateViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llAccounts)
        binding = FragmentAccountEditDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        inputObserver()

        amountViewModel.amount.observe(viewLifecycleOwner) { amount ->
            amount.getContentIfNotHandled()?.let { content ->
                binding.tfEditAccountBudget.editText?.setText(content)
            }
        }

        colorViewModel.color.observe(viewLifecycleOwner) { selectedColor ->
            setColor(selectedColor)
        }

        binding.tfEditAccountBudget.editText?.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("account", "account")

            val fragmentManager = childFragmentManager
            val newFragment = AmountDialogFragment()
            newFragment.arguments = bundle
            newFragment.show(fragmentManager, "dialog")
        }

        binding.tfEditAccountColor.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = ColorDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        binding.btnEditAccountCancel.setOnClickListener {
            dismiss()
        }

        binding.btnEditAccountSave.setOnClickListener {
            validateData()
        }
    }

    private fun loadData() {
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            loadAccount(firebaseUser.uid, args.accountId)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadAccount(uid: String, accountId: String) {
        database = Firebase.database
        databaseReference = database.getReference("accounts").child(uid)
        databaseReference.child(accountId).get()
            .addOnSuccessListener {
                val account = it.getValue<Account>()
                if (account != null) {
                    currentAccount = account
                    binding.tfEditAccountName.editText?.setText(currentAccount.name)
                    binding.tfEditAccountBudget.editText?.setText(currentAccount.monthlyBudget.toInt().toString())

                    setColor(currentAccount.color!!)
                }
            }
            .addOnFailureListener {
                Toast
                    .makeText(activity, it.localizedMessage!!.toString(), Toast.LENGTH_LONG)
                    .show()
            }
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

        binding.viewEditAccountSelectedColor.background = gradientDrawable
        binding.tfEditAccountColor.tag = selectedColor
        binding.tfEditAccountColor.error = null
    }

    private fun inputObserver() {
        binding.tfEditAccountName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfEditAccountName.error = getString(R.string.account_name_empty)
            else binding.tfEditAccountName.error = null
        }
        binding.tfEditAccountBudget.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfEditAccountBudget.error = getString(R.string.monthly_budget_empty)
            else if (text.toString().toDouble() < currentAccount.monthlyBudget) {
                val amount = currentAccount.monthlyBudget - text.toString().toDouble()
                val newRemainingBudget = currentAccount.remainingBudget - amount
                val newRemainingBalance = currentAccount.remainingBalance - amount

                if (newRemainingBudget < 0 || newRemainingBalance < 0) {
                    binding.tfEditAccountBudget.error = getString(R.string.amount_overflow_2)
                }
            }
            else binding.tfEditAccountBudget.error = null
        }
        binding.tfEditAccountBudget.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) text?.clear()
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

        val accountName = binding.tfEditAccountName.editText?.text.toString().trim {it <= ' '}
        val accountMonthlyBudget = binding.tfEditAccountBudget.editText?.text.toString().trim {it <= ' '}
        val accountColor = binding.tfEditAccountColor.tag
        var errors = 0

        if (TextUtils.isEmpty(accountName)) {
            binding.tfEditAccountName.error = getString(R.string.account_name_empty)
            errors++
        }
        if (accountColor == null) {
            binding.tfEditAccountColor.error = getString(R.string.select_a_color)
            errors++
        }
        if (TextUtils.isEmpty(accountMonthlyBudget)) {
            binding.tfEditAccountBudget.error = getString(R.string.monthly_budget_empty)
            errors++
        }
        else {
            if (accountMonthlyBudget.startsWith("0")) {
                binding.tfEditAccountBudget.error = getString(R.string.amount_starts_0)
                errors++
            }
            if (accountMonthlyBudget.toDouble() < currentAccount.monthlyBudget) {
                val amount = currentAccount.monthlyBudget - accountMonthlyBudget.toDouble()
                val newRemainingBudget = currentAccount.remainingBudget - amount
                val newRemainingBalance = currentAccount.remainingBalance - amount

                if (newRemainingBudget < 0 || newRemainingBalance < 0) {
                    binding.tfEditAccountBudget.error = getString(R.string.amount_overflow_2)
                    errors++
                }
            }
        }

        if (errors == 0) {
            var changes = 0
            if (accountName.lowercase() != currentAccount.nameLower) changes++
            if (accountMonthlyBudget.toDouble() != currentAccount.monthlyBudget) changes++
            if (accountColor != currentAccount.color) changes++

            if (changes == 0) {
                dismiss()
            }
            else {
                val account = Account(
                    currentAccount.id,
                    accountName,
                    accountName.lowercase(),
                    accountColor.toString(),
                    accountMonthlyBudget.toDouble(),
                    0.0,
                    0.0,
                    null
                )

                accountNameExists(account)
            }
        }
    }

    private fun accountNameExists(account: Account) {
        showProgressDialog()
        val query = databaseReference.orderByChild("nameLower").equalTo(account.nameLower)
        query.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    updateAccount(account)
                }
                else {
                    if (currentAccount.nameLower == account.nameLower) {
                        updateAccount(account)
                    }
                    else {
                        hideProgressDialog()
                        binding.tfEditAccountName.error = getString(R.string.account_name_exists)
                    }
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun updateAccount(account: Account) {
        showProgressDialog()
        val newRemainingBudget: Double
        val newRemainingBalance: Double

        if (account.monthlyBudget > currentAccount.monthlyBudget) {
            val amount = account.monthlyBudget - currentAccount.monthlyBudget
            newRemainingBudget = amount + currentAccount.remainingBudget
            newRemainingBalance = amount + currentAccount.remainingBalance
        }
        else {
            val amount = currentAccount.monthlyBudget - account.monthlyBudget
            newRemainingBudget = currentAccount.remainingBudget - amount
            newRemainingBalance = currentAccount.remainingBalance - amount
        }

        val zdt = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )

        val startOfDay = zdt.with(LocalTime.MIN)
        val nextMonth = startOfDay.plusMonths(1).toInstant().toEpochMilli()
        val renewsAt: Long = currentAccount.budgetRenewsAt ?: nextMonth

        account.remainingBudget = newRemainingBudget
        account.remainingBalance = newRemainingBalance
        account.budgetRenewsAt = renewsAt

        databaseReference.child(account.id!!).setValue(account)
            .addOnSuccessListener {
                sharedPreferences = SharedPreferences(activity)
                if (sharedPreferences.accountId!! == account.id) {
                    sharedPreferences.accountName = account.name
                    sharedPreferences.accountColor = account.color
                }

                updateViewModel.update(true)
                hideProgressDialog()
                dismiss()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
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
        binding.pbEditAccount.visibility = View.VISIBLE
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialog() {
        binding.pbEditAccount.visibility = View.INVISIBLE
        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}