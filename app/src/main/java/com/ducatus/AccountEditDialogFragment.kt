package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.ducatus.data.Account
import com.ducatus.databinding.FragmentAccountEditBinding
import com.ducatus.viewmodel.AccountViewModel
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
    private var currentName: String? = null
    private var currentBudget: Double? = null
    private var currentColor: String? = null
    private val args: AccountEditDialogFragmentArgs by navArgs()
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

        colorViewModel.selectedColor.observe(viewLifecycleOwner) { selectedColor ->
            setColor(selectedColor)
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
            loadAccount(firebaseUser.uid)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadAccount(uid: String) {
        database = Firebase.database
        databaseReference = database.getReference("accounts").child(uid).child(args.accountId)
        databaseReference.get()
            .addOnSuccessListener {
                val account = it.getValue<Account>()
                if (account != null) {
                    currentName = account.account_name.toString()
                    currentBudget = account.account_monthly_budget
                    currentColor = account.account_color.toString()

                    binding.tfEditAccountName.editText?.setText(currentName)
                    binding.tfEditAccountBudget.editText?.setText(currentBudget?.toInt().toString())

                    setColor(currentColor.toString())
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
        if (TextUtils.isEmpty(accountMonthlyBudget)) {
            binding.tfEditAccountBudget.error = getString(R.string.monthly_budget_empty)
            errors++
        }
        if (accountMonthlyBudget.startsWith("0")) {
            binding.tfEditAccountBudget.error = getString(R.string.budget_amount_0)
            errors++
        }
        if (accountColor == null) {
            binding.tfEditAccountColor.error = getString(R.string.select_a_color)
            errors++
        }

        if (errors == 0) {
            if (accountName == currentName && accountMonthlyBudget.toDouble() == currentBudget && accountColor == currentColor) {
                activity.onBackPressed()
            }
            else {
                updateAccount(
                    args.accountId,
                    accountName,
                    accountMonthlyBudget.toDouble(),
                    accountColor.toString()
                )
            }
        }
    }

    private fun updateAccount(accountId: String, accountName: String, accountMonthlyBudget: Double, accountColor: String) {
        showProgressDialog()
        val account = Account(accountId, accountName, accountColor, accountMonthlyBudget, accountMonthlyBudget)
        databaseReference.setValue(account)
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