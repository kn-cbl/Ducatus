package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
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
import com.ducatus.databinding.FragmentAccountEditBinding
import com.ducatus.viewmodel.AccountViewModel
import com.ducatus.viewmodel.AmountViewModel
import com.ducatus.viewmodel.ColorViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class AccountEditDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentAccountEditBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var currentData: Account
    private val args: AccountEditDialogFragmentArgs by navArgs()
    private val amountViewModel: AmountViewModel by activityViewModels()
    private val colorViewModel: ColorViewModel by activityViewModels()
    private val viewModel: AccountViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llAccounts)
        binding = FragmentAccountEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        inputObserver()

        amountViewModel.amount.observe(viewLifecycleOwner) { amount ->
            binding.tfEditAccountBudget.editText?.setText(amount)
        }

        colorViewModel.selectedColor.observe(viewLifecycleOwner) { selectedColor ->
            setColor(selectedColor)
        }

        binding.tfEditAccountBudget.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = AmountDialogFragment()

            val bundle = Bundle()
            bundle.putString("account", "account")
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
                    currentData = account
                    binding.tfEditAccountName.editText?.setText(currentData.name)
                    binding.tfEditAccountBudget.editText?.setText(currentData.monthlyBudget.toInt().toString())

                    setColor(currentData.color!!)
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
            else if (text.toString().toDouble() < currentData.monthlyBudget) {
                val amount = currentData.monthlyBudget - text.toString().toDouble()
                val newRemainingBudget = currentData.remainingBudget - amount
                val newRemainingBalance = currentData.remainingBalance - amount

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
            if (accountMonthlyBudget.toDouble() < currentData.monthlyBudget) {
                val amount = currentData.monthlyBudget - accountMonthlyBudget.toDouble()
                val newRemainingBudget = currentData.remainingBudget - amount
                val newRemainingBalance = currentData.remainingBalance - amount

                if (newRemainingBudget < 0 || newRemainingBalance < 0) {
                    binding.tfEditAccountBudget.error = getString(R.string.amount_overflow_2)
                    errors++
                }
            }
        }

        if (errors == 0) {
            if (accountName == currentData.name && accountMonthlyBudget.toDouble() == currentData.monthlyBudget && accountColor == currentData.color) {
                activity.onBackPressed()
            }
            else {
                val accountData = mapOf(
                    "id" to args.accountId,
                    "name" to accountName,
                    "monthlyBudget" to accountMonthlyBudget,
                    "color" to accountColor.toString()
                )

                accountNameExists(accountData)
            }
        }
    }

    private fun accountNameExists(accountData: Map<String, String>) {
        showProgressDialog()
        val query = databaseReference.orderByChild("nameLower").equalTo(accountData["name"])
        query.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    updateAccount(accountData)
                }
                else {
                    hideProgressDialog()
                    binding.tfEditAccountName.error = getString(R.string.account_name_exists)
                }

            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun updateAccount(accountData: Map<String, String>) {
        showProgressDialog()
        val newRemainingBudget: Double
        val newRemainingBalance: Double

        if (accountData["monthlyBudget"]!!.toDouble() > currentData.monthlyBudget) {
            val amount = accountData["monthlyBudget"]!!.toDouble() - currentData.monthlyBudget
            newRemainingBudget = amount + currentData.remainingBudget
            newRemainingBalance = amount + currentData.remainingBalance
        }
        else {
            val amount = currentData.monthlyBudget - accountData["monthlyBudget"]!!.toDouble()
            newRemainingBudget = currentData.remainingBudget - amount
            newRemainingBalance = currentData.remainingBalance - amount
        }

        val account = Account(
            accountData["id"],
            accountData["name"],
            accountData["name"]!!.lowercase(),
            accountData["color"],
            accountData["monthlyBudget"]!!.toDouble(),
            newRemainingBudget,
            newRemainingBalance,
            currentData.selected
        )

        databaseReference.child(account.id!!).setValue(account)
            .addOnSuccessListener {
                hideProgressDialog()
                viewModel.update(true)
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
        Snackbar
            .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        // add 3 second delay
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // do nothing
            }
            override fun onFinish() {
                try {
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity.finish()
                }
                catch (e: Exception) {}
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.pbEditAccount.visibility = View.VISIBLE
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbEditAccount.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}