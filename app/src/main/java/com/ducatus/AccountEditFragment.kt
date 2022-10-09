package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentAccountEditBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class AccountEditFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentAccountEditBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private var currentName: String? = null
    private var currentBudget: Double? = null
    private var currentColor: String? = null
    private var colorNames: List<String> = listOf()
    private val args: AccountEditFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llAccounts)
        toolbar = activity.findViewById(R.id.tbAccounts)
        toolbar.title = getString(R.string.edit_account)
        toolbar.inflateMenu(R.menu.check_menu)

        binding = FragmentAccountEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        inputObserver()

        binding.spEditColor.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedView = binding.spEditColor.selectedView
                val color = selectedView.findViewById<View>(R.id.viewHelperItem)
                val gradientDrawable = GradientDrawable()

                val iconColor = resources.getIdentifier(
                    colorNames[position],
                    "color",
                    activity.packageName
                )

                gradientDrawable.setColor(activity.getColor(iconColor))
                color.background = gradientDrawable
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.done -> {
                    validateChanges()
                    true
                }
                else -> false
            }
        }
    }

    private fun inputObserver() {
        binding.tfEditAccountName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfEditAccountName.error = getString(R.string.account_name_empty)
            else binding.tfEditAccountName.error = null
        }
        binding.tfEditAccountBudget.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty() || text.toString().toDouble() == 0.0) binding.tfEditAccountBudget.error = getString(R.string.monthly_budget_empty)
            else binding.tfEditAccountBudget.error = null
        }
    }

    private fun loadData() {
        showLoadingData()
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            loadColors()

            database = Firebase.database
            databaseReference = database.getReference("accounts").child(firebaseUser.uid).child(args.accountId)
            databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val account = snapshot.getValue<Account>()
                    if (account != null) {
                        currentName = account.account_name.toString()
                        currentBudget = account.account_monthly_budget
                        currentColor = account.account_color

                        binding.tfEditAccountName.editText?.setText(currentName)
                        binding.tfEditAccountBudget.editText?.setText(currentBudget?.toInt().toString())

                        val index = colorNames.indexOf(account.account_color)
                        binding.spEditColor.setSelection(index)

                        hideLoadingData()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar
                        .make(rootLayout, "Unable to load data, ${error.message}", Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry)) { loadData() }
                        .show()
                }
            })
        }
        else {
            sessionExpired()
        }
    }

    private fun loadColors() {
        colorNames = listOf(
            "color_one", "color_two", "color_three", "color_four", "color_five",
            "color_six", "color_seven", "color_eight", "color_nine", "color_ten",
            "color_eleven", "color_twelve", "color_thirteen", "color_fourteen", "color_fifteen",
            "color_sixteen", "color_seventeen", "color_eighteen", "color_nineteen", "color_twenty",
            "color_twenty_one", "color_twenty_two", "color_twenty_three", "color_twenty_four", "color_twenty_five",
        )

        val adapter = object: ArrayAdapter<String>(requireContext(), R.layout.spinner_item, R.id.txt_bundle, colorNames) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = getView(position, convertView, parent)
                val color = view.findViewById<View>(R.id.viewHelperItem)
                val gradientDrawable: GradientDrawable = color.background as GradientDrawable

                val iconColor = resources.getIdentifier(
                    colorNames[position],
                    "color",
                    activity.packageName
                )

                gradientDrawable.setColor(activity.getColor(iconColor))
                color.background = gradientDrawable
                return view
            }
        }

        binding.spEditColor.adapter = adapter
    }

    private fun validateChanges() {
        // hide keyboard
        try {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        val accountName = binding.tfEditAccountName.editText?.text.toString().trim {it <= ' '}
        val accountMonthlyBudget = binding.tfEditAccountBudget.editText?.text.toString().trim {it <= ' '}
        val accountColor = binding.spEditColor.selectedItem.toString()

        if (accountName == currentName && accountMonthlyBudget.toDouble() == currentBudget && accountColor == currentColor) {
            activity.onBackPressed()
        }
        else {
            if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountMonthlyBudget) || accountMonthlyBudget.toDouble() == 0.0) {
                if (TextUtils.isEmpty(accountName)) binding.tfEditAccountName.error = getString(R.string.account_name_empty)
                if (TextUtils.isEmpty(accountMonthlyBudget)) binding.tfEditAccountBudget.error = getString(R.string.monthly_budget_empty)
            }
            else {
                updateAccount(accountName, accountMonthlyBudget.toDouble(), accountColor)
            }
        }
    }

    private fun updateAccount(accountName: String, accountMonthlyBudget: Double, accountColor: String) {
        showProgressDialog()

        val account = Account(0, accountName, accountColor, accountMonthlyBudget, accountMonthlyBudget)
        databaseReference.setValue(account)
            .addOnSuccessListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Successfully updated account", Snackbar.LENGTH_LONG)
                    .show()

                // add 3 second delay
                object : CountDownTimer(3000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        // do nothing
                    }
                    override fun onFinish() {
                        try {
                            val action = AccountEditFragmentDirections.actionAccountEditFragmentToAccountsFragment()
                            findNavController().navigate(action)
                        }
                        catch (e: Exception) {}
                    }
                }.start()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to update account, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { updateAccount(accountName, accountMonthlyBudget, accountColor) }
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
                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity.finish()
            }
        }.start()
    }

    private fun showLoadingData() {
        binding.pbEditAccountLoad.visibility = View.VISIBLE
        binding.llEditAccount.visibility = View.GONE
    }

    private fun hideLoadingData() {
        binding.pbEditAccountLoad.visibility = View.INVISIBLE
        binding.llEditAccount.visibility = View.VISIBLE
    }

    private fun showProgressDialog() {
        binding.pbEditAccountLoad.visibility = View.VISIBLE
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbEditAccountLoad.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}