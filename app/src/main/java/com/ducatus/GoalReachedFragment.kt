package com.ducatus

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.adapter.GoalsReachAdapter
import com.ducatus.common.Constants
import com.ducatus.data.GoalHistory
import com.ducatus.data.Goals
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.ducatus.interfaces.ReachGoalIntf
import com.ducatus.services.LocalFirebaseDatabase

class GoalReachedFragment : Fragment() {

    lateinit var recyclerView: RecyclerView
    lateinit var parentView: View
    lateinit var db: LocalFirebaseDatabase
    lateinit var pbLoading: ProgressBar
    lateinit var txtNoReach: TextView
    lateinit var accountID: String
    lateinit var adapter: GoalsReachAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val mView = inflater.inflate(R.layout.fragment_goal_reached_container, container, false)
        parentView = mView
        initViews(mView)
        return mView
    }

    private fun initViews(mView: View) {
        val sharedPreferences = SharedPreferences(mView.context)
        accountID = sharedPreferences.accountId!!
        recyclerView = mView.findViewById(R.id.recycler)
        pbLoading = mView.findViewById(R.id.progressBar)
        txtNoReach = mView.findViewById(R.id.txtNoReach)
        db = LocalFirebaseDatabase()
        loadData(mView)
    }

    private fun loadData(mView: View) {
        pbLoading.visibility = View.VISIBLE
        txtNoReach.visibility = View.GONE

        db.getAllDataFromDB("Goals", accountID, object : FirebaseDatabaseCallback {
            override fun onSuccessInsert(key: String) {
                TODO("Not yet implemented")
            }

            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                if (goalsList != null) {
                    var newGoal = mutableListOf<Goals>()
                    for (g in goalsList) {
                        if (g.status == Constants().GOAL_REACHED) {
                            newGoal.add(g)
                        }
                    }
                    if (newGoal.size == 0) {
                        pbLoading.visibility = View.GONE
                        txtNoReach.visibility = View.VISIBLE
                        Log.e("ERROR_IN_GOAL_REACHED", "No Reached Goals")
                        return
                    }

                    adapter = GoalsReachAdapter(requireContext(), newGoal, object : ReachGoalIntf {
                        override fun OnClick(mView: View, position: Int) {
                            val goal = newGoal.get(position)

                            if (goal.remaining == 0.0) {
                                GoalDetailCompleted.start(requireContext(), goal)
                            } else {
                                GoalDetailCompleted.start(requireContext(), goal)
                            }
                        }
                    })
                    recyclerView.layoutManager =
                        LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    recyclerView.adapter = adapter
                } else {
                    Log.e("ERROR_IN_GOAL_REACHED", "Empty Goals Result")
                    pbLoading.visibility = View.GONE
                    txtNoReach.visibility = View.VISIBLE
                }

            }

            override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                TODO("Not yet implemented")
            }

            override fun onError(e: Exception) {
                pbLoading.visibility = View.GONE
                txtNoReach.visibility = View.VISIBLE
                Log.e("ERROR_IN_GOAL_REACHED", e.message.toString())
            }
        })

    }

    override fun onResume() {
        super.onResume()
        recyclerView.adapter = null
        loadData(parentView)
    }
}