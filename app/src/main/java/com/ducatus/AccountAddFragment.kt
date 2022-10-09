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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentAccountAddBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class AccountAddFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentAccountAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private var colorNames: List<String> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llAccounts)
        toolbar = activity.findViewById(R.id.tbAccounts)
        toolbar.title = getString(R.string.add_account)

        binding = FragmentAccountAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadColors()
        inputObserver()

        binding.spAddAccountColor.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedView = binding.spAddAccountColor.selectedView
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

        binding.btnAddAccount.setOnClickListener {
            validateInput()
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

        binding.spAddAccountColor.adapter = adapter
    }

    private fun inputObserver() {
        binding.tfAddAccountName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfAddAccountName.error = getString(R.string.account_name_empty)
            else binding.tfAddAccountName.error = null
        }
        binding.tfAddAccountBudget.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty() || text.toString().toDouble() == 0.0) binding.tfAddAccountBudget.error = getString(R.string.monthly_budget_empty)
            else binding.tfAddAccountBudget.error = null
        }
    }

    private fun validateInput() {
        // hide keyboard
        try {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val accountName = binding.tfAddAccountName.editText?.text.toString().trim {it <= ' '}
            val accountMonthlyBudget = binding.tfAddAccountBudget.editText?.text.toString().trim {it <= ' '}
            val accountColor = binding.spAddAccountColor.selectedItem.toString()

            if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountMonthlyBudget) || accountMonthlyBudget.toDouble() == 0.0) {
                if (TextUtils.isEmpty(accountName)) binding.tfAddAccountName.error = getString(R.string.account_name_empty)
                if (TextUtils.isEmpty(accountMonthlyBudget) || accountMonthlyBudget.toInt() == 0) binding.tfAddAccountBudget.error = getString(R.string.monthly_budget_empty)
            }
            else {
                addAccount(firebaseUser.uid, accountName, accountMonthlyBudget.toDouble(), accountColor)
            }
        }
        else {
            sessionExpired()
        }
    }

    private fun addAccount(uid: String, accountName: String, accountMonthlyBudget: Double, accountColor: String) {
        showProgressDialog()

        database = Firebase.database
        databaseReference = database.getReference("accounts").child(uid)
        databaseReference.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var lastId = 0
                for (child in snapshot.children) {
                    lastId = child.child("account_id").value.toString().toInt() + 1
                }

                val account = Account(lastId, accountName, accountColor, accountMonthlyBudget, accountMonthlyBudget)
                databaseReference.child(lastId.toString()).setValue(account)
                    .addOnSuccessListener {
                        hideProgressDialog()
                        Snackbar
                            .make(rootLayout, "Successfully added account", Snackbar.LENGTH_LONG)
                            .show()

                        // add 3 second delay
                        object : CountDownTimer(3000, 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                // do nothing
                            }
                            override fun onFinish() {
                                try {
                                    val action = AccountAddFragmentDirections.actionAccountAddFragmentToAccountsFragment()
                                    findNavController().navigate(action)
                                }
                                catch (e: Exception) {}
                            }
                        }.start()
                    }
                    .addOnFailureListener {
                        hideProgressDialog()
                        Snackbar
                            .make(rootLayout, "Unable to add account, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.retry)) { addAccount(uid, accountName, accountMonthlyBudget, accountColor) }
                            .show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to add account, ${error.message}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { addAccount(uid, accountName, accountMonthlyBudget, accountColor) }
                    .show()
            }
        })
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

    private fun showProgressDialog() {
        binding.pbAddAccount.visibility = View.VISIBLE
        binding.btnAddAccount.text = null
        binding.btnAddAccount.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.gray)
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbAddAccount.visibility = View.INVISIBLE
        binding.btnAddAccount.text = getString(R.string.add_account)
        binding.btnAddAccount.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.green_primary)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}