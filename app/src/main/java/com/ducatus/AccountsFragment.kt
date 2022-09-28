package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentAccountsBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class AccountsFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentAccountsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        toolbar = activity.findViewById(R.id.tbAccounts)
        rootLayout = activity.findViewById(R.id.llAccounts)
        binding = FragmentAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadData()

        binding.ivEditSelectedAccount.setOnClickListener {
            showPopup(it)
        }
    }

    private fun loadData() {
        showProgressDialog()

        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            database = Firebase.database
            databaseReference = database.getReference("accounts/" + firebaseUser.uid + "/0")
            databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val account = snapshot.getValue(Account::class.java)
                    if (account != null) {
                        val imageColor = resources.getIdentifier(account.account_color.toString(), "color", activity.packageName)
                        binding.ivSelectedAccountImage.setColorFilter(ResourcesCompat.getColor(resources, imageColor, null))
                        binding.tvSelectedAccountName.text = account.account_name
                        binding.tvSelectedAccountBudget.text = "PHP " + account.account_monthly_budget.toString()

                        hideProgressDialog()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar
                        .make(rootLayout, "Failed to load data", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry") { loadData() }
                        .show()
                }
            })
        }
        else {
            sessionExpired()
        }
    }

    private fun showPopup(view: View) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.editAccount -> {
                    toolbar.title = "Edit Account"
                    val action = AccountsFragmentDirections.actionAccountsFragmentToAccountsEditFragment()
                    findNavController().navigate(action)
                    true
                }
                R.id.deleteAccount -> {
                    Toast.makeText(activity, "clicked delete", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popup.menuInflater.inflate(R.menu.edit_account_menu, popup.menu)
        popup.show()
    }

    private fun deleteAccount() {

    }

    private fun sessionExpired() {
        Snackbar
            .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        activity.finish()
    }

    private fun showProgressDialog() {
        binding.pbAccount.visibility = View.VISIBLE
        binding.clAccount.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbAccount.visibility = View.INVISIBLE
        binding.clAccount.visibility = View.VISIBLE
    }

//    private fun storeDefaultAccount(firebaseUser: FirebaseUser, username: String) {
//        showProgressDialog()
//        databaseReference = database.getReference("accounts/" + firebaseUser.uid)
//        databaseReference.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object:
//            ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                var accountId = 0
//                if (snapshot.exists()) {
//                    for (child in snapshot.children) {
//                        accountId = child.child("account_id").value.toString().toInt() + 1
//                    }
//                }
//                else {
//                    accountId = 0
//                }
//
//                val account = Account(accountId, username, 0, "R.color.green_primary")
//                databaseReference.child(accountId.toString()).setValue(account)
//                    .addOnSuccessListener {
//                        verifyEmail(firebaseUser.isEmailVerified)
//                    }
//                    .addOnFailureListener {
//                        hideProgressDialog()
//                        Snackbar
//                            .make(findViewById(R.id.clSignup), "Failed to store user data", Snackbar.LENGTH_INDEFINITE)
//                            .setAction("Retry") { storeDefaultAccount(firebaseUser, username) }
//                            .show()
//                    }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                hideProgressDialog()
//                Snackbar
//                    .make(findViewById(R.id.clSignup), "Failed to store user data", Snackbar.LENGTH_INDEFINITE)
//                    .setAction("Retry") { storeDefaultAccount(firebaseUser, username) }
//                    .show()
//            }
//        })
//    }
}