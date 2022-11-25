package com.ducatus

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.adapter.GoalAdapter
import com.ducatus.adapter.GoalPauseAdapter
import com.ducatus.common.Common
import com.ducatus.common.Constants
import com.ducatus.data.GoalHistory
import com.ducatus.data.Goals
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.ducatus.interfaces.GoalDetailIntf
import com.ducatus.interfaces.PauseGoalIntf
import com.ducatus.services.LocalFirebaseDatabase
import java.lang.Exception


class GoalPausedFragment : Fragment() {

    lateinit var recycler: RecyclerView
    lateinit var txtNoPause: TextView
    lateinit var goalAdapter: GoalPauseAdapter
    lateinit var pb: ProgressBar
    lateinit var db: LocalFirebaseDatabase
    lateinit var accountID: String
    lateinit var parentView: View
    lateinit var gList: List<Goals>

    companion object {

        fun reload() {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val mView = inflater.inflate(R.layout.fragment_goal_paused_container, container, false)
        parentView = mView
        initViews(mView)
        return mView
    }

    private fun initViews(mView: View) {
        recycler = mView.findViewById(R.id.recycler)
        txtNoPause = mView.findViewById(R.id.txtNoPause)
        db = LocalFirebaseDatabase()
        txtNoPause.visibility = View.GONE
        pb = mView.findViewById(R.id.progressBar)
        val sharedPreferences = SharedPreferences(mView.context)
        accountID = sharedPreferences.accountId!!
        loadData(mView)
    }


    private fun loadData(mView: View) {
        pb.visibility = View.VISIBLE
        val callBack = object : FirebaseDatabaseCallback {
            override fun onError(e: Exception) {
                Log.e("ERROR", e.message.toString())
                pb.visibility = View.GONE
                txtNoPause.visibility = View.VISIBLE
            }

            override fun onSuccessInsert(key: String) {
                TODO("Not yet implemented")
            }

            override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                TODO("Not yet implemented")
            }

            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                pb.visibility = View.GONE
                if (goalsList != null) {
                    val newGoal = mutableListOf<Goals>()
                    for (g in goalsList) {
                        if (g.status == Constants().GOAL_PAUSE) {
                            newGoal.add(g)
                        }
                    }
                    if (newGoal.size == 0) {
                        txtNoPause.visibility = View.VISIBLE
                        return;
                    }
                    gList = newGoal
                    goalAdapter =
                        GoalPauseAdapter(requireContext(), newGoal, object : PauseGoalIntf {
                            override fun OnClick(mView: View, position: Int) {
                                GoalDetail.start(requireContext(), gList.get(position), object :
                                    GoalDetailIntf {
                                    override fun deleteSubmitted() {
                                        recycler.adapter = null
                                        loadData(parentView)
                                    }

                                    override fun onSuccessUpdate() {
                                        recycler.adapter = null
                                        loadData(parentView)
                                    }
                                }, true)
                            }
                        })
                    recycler.layoutManager =
                        LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    recycler.adapter = goalAdapter
                } else {
                    txtNoPause.visibility = View.VISIBLE
                }
            }
        }
        db.getAllDataFromDB("Goals Pause", accountID, callBack)

    }

    override fun onResume() {
        super.onResume()
        txtNoPause.visibility = View.GONE
        pb.visibility = View.VISIBLE
        recycler.adapter = null
        loadData(parentView)
    }
}