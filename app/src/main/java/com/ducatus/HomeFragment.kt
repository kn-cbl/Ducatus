package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import androidx.drawerlayout.widget.DrawerLayout
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
        else {
            sessionExpired()
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

        binding.ivHomeAccountIcon.setColorFilter(
            ResourcesCompat.getColor(
                resources,
                imageColor,
                null
            )
        )

        binding.tvHomeAccountName.text = currentAccountName

        database = Firebase.database
        databaseReference = database.getReference("accounts").child(uid).child(currentAccountId)
        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val account = snapshot.getValue<Account>()
                if (account != null) {
                    val budget = "₱" + String.format("%,.2f", account.account_remaining_budget)
                    binding.tvHomeAccountBalance.text = budget
                    hideProgressDialog()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Failed to load data, ${error.message}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { loadAccount(uid) }
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
        binding.svHome.visibility = View.GONE
        binding.pbHome.visibility = View.VISIBLE
    }

    private fun hideProgressDialog() {
        binding.svHome.visibility = View.VISIBLE
        binding.pbHome.visibility = View.GONE
    }
}