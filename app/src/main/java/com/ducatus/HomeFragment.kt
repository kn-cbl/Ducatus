package com.ducatus

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.ducatus.data.Account
import com.ducatus.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class HomeFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: DrawerLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
    }

    private fun loadData() {
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            loadAccount(firebaseUser.uid)
        }
    }

    private fun loadAccount(uid: String) {
        showProgressDialog()
        val sharedPreferences = SharedPreferences(activity)
        val currentAccountId = sharedPreferences.accountId.toString()
        val currentAccountName = sharedPreferences.accountName
        val currentAccountColor = sharedPreferences.accountColor

        val imageColor = resources.getIdentifier(
            currentAccountColor,
            "color",
            activity.packageName
        )

        try{
            binding.ivHomeAccountIcon.setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    imageColor,
                    null
                )
            )
        }catch (x:Exception){
        }

        binding.tvHomeAccountName.text = currentAccountName

        database = Firebase.database
        databaseReference = database.getReference("accounts").child(uid).child(currentAccountId)
        databaseReference.get()
            .addOnSuccessListener {
                val account = it.getValue<Account>()
                if (account != null) {
                    val budget = "₱" + String.format("%,.2f", account.remainingBalance)
                    binding.tvHomeAccountBalance.text = budget
                    hideProgressDialog()
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun showProgressDialog() {
        binding.svHome.visibility = View.GONE
        binding.pbHome.visibility = View.VISIBLE
    }

    private fun hideProgressDialog() {
        binding.svHome.visibility = View.VISIBLE
        binding.pbHome.visibility = View.GONE
    }
}