package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.adapter.ChallengeAdapter
import com.ducatus.common.AppResources
import com.ducatus.data.Challenge
import com.ducatus.data.ChallengeHistory
import com.ducatus.databinding.FragmentChallengesNewBinding
import com.ducatus.interfaces.ChallengeInterface
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
import com.google.gson.Gson

class ChallengesNewFragment : Fragment(), ChallengeInterface {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentChallengesNewBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: DrawerLayout
    private lateinit var challenges: Map<Int, Challenge>
    private lateinit var currentAccountId: String
    private var firebaseUser: FirebaseUser? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        binding = FragmentChallengesNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
    }

    override fun onResume() {
        super.onResume()
        firebaseUser?.let {
            challenges = AppResources().getChallenges()
            loadChallengesHistory()
        }
    }

    override fun viewItem(challenge: Challenge) {
        val intent = Intent(activity, ChallengeDetailActivity::class.java)
        intent.putExtra("challenge", Gson().toJson(challenge))
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        firebaseUser?.run {
            val sharedPreferences = SharedPreferences(activity)
            currentAccountId = sharedPreferences.accountId.toString()
            database = Firebase.database
            databaseReference =
                database.getReference("challengeHistory")
                    .child(uid)
                    .child(currentAccountId)

        } ?: sessionExpired()
    }

    private fun loadChallengesHistory() {
        showProgressDialog()
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    for (innerChild in child.children) {
                        val challengeHistory = innerChild.getValue<ChallengeHistory>()
                        challengeHistory?.let {
                            if (it.position == 0) {
                                // first position of challenge history is always start date
                                challenges[it.challengeId]?.dateStarted = it.datePaid
                            }
                        }
                    }
                }

                adaptChallenges(challenges)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_challenges_error), 5000)
                    .show()
            }
    }

    private fun adaptChallenges(challenges: Map<Int, Challenge>) {
        val challengeNewAdapter = ChallengeAdapter(mutableListOf(), this)
        binding.rvChallengesNew.adapter = challengeNewAdapter
        binding.rvChallengesNew.layoutManager = LinearLayoutManager(activity)

        // filter not started challenges
        for ((_, challenge) in challenges) {
            if (challenge.dateStarted == null) {
                challengeNewAdapter.addChallenge(challenge)
            }
        }

        Log.d("challenges", "${challengeNewAdapter.itemCount}")


        hideProgressDialog()
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
        binding.pbChallengesNew.visibility = View.VISIBLE
        binding.rvChallengesNew.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbChallengesNew.visibility = View.GONE
        binding.rvChallengesNew.visibility = View.VISIBLE
    }
}