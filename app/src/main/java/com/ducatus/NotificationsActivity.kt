package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.adapter.UserNotificationAdapter
import com.ducatus.data.UserNotification
import com.ducatus.databinding.ActivityNotificationsBinding
import com.ducatus.interfaces.UserNotificationInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class NotificationsActivity : AppCompatActivity(), UserNotificationInterface {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var currentAccountId: String
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private var firebaseUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        loadData()

        binding.tbNotifications.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbNotifications.inflateMenu(R.menu.delete_menu)
        binding.tbNotifications.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.delete -> {
                    firebaseUser?.let { confirmDelete(it.uid, currentAccountId) }
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun viewItem(type: String, itemId: String?) {
        val intent = getActivityIntent(type)
        intent?.let {
            if (itemId != null) {
                it.putExtra(type, itemId)
            }
            else {
                it.putExtra("notification", type)
            }

            startActivity(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun getActivityIntent(type: String): Intent? {
        var intent: Intent? = null
        when (type) {
            "challenge" -> intent = Intent(this, HomeActivity::class.java)
            "expense" -> intent = Intent(this, HomeActivity::class.java)
            "loanId" -> intent = Intent(this, LoanDetailActivity::class.java)
            "subscriptionId" -> intent = Intent(this, SubscriptionDetailActivity::class.java)
        }

        return intent
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val sharedPreferences = SharedPreferences(this)
            currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            loadNotifications(firebaseUser!!.uid, currentAccountId)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadNotifications(uid: String, accountId: String) {
        showProgressDialog()
        databaseReference = database.getReference("notifications").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val userNotifications = mutableListOf<UserNotification>()
                for (child in snapshot.children) {
                    val userNotification = child.getValue<UserNotification>()
                    userNotification?.let { userNotifications.add(it) }
                }

                adaptNotifications(userNotifications)
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clNotifications, getString(R.string.load_notifications_error), 5000)
                    .show()
            }
    }

    private fun adaptNotifications(userNotifications: MutableList<UserNotification>) {
        val userNotificationAdapter = UserNotificationAdapter(mutableListOf(), this)
        binding.rvNotifications.adapter = userNotificationAdapter
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)

        // sort by latest date
        userNotifications.sortByDescending { it.notifiedAt }

        for (userNotification in userNotifications) {
            userNotificationAdapter.addUserNotification(userNotification)
        }

        if (userNotificationAdapter.itemCount <= 0) {
            binding.tvNotificationsEmpty.visibility = View.VISIBLE
        }

        hideProgressDialog()
    }

    private fun confirmDelete(uid: String, accountId: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.delete_notifications_title))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ -> deleteNotifications(uid, accountId) }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    private fun deleteNotifications(uid: String, accountId: String) {
        databaseReference = database.getReference("notifications").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                loadNotifications(uid, accountId)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.clNotifications, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun showProgressDialog() {
        binding.pbNotifications.visibility = View.VISIBLE
        binding.tvNotificationsEmpty.visibility = View.GONE
        binding.rvNotifications.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbNotifications.visibility = View.GONE
        binding.rvNotifications.visibility = View.VISIBLE
    }

    private fun sessionExpired() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.session_expired))
            .setPositiveButton(resources.getString(R.string.log_in)) { _, _ -> }

        dialog.setOnDismissListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

}