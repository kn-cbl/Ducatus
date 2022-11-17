package com.ducatus

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.adapter.GoalHistoryAdapter
import com.ducatus.data.GoalHistory
import com.ducatus.data.Goals
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.ducatus.services.LocalFirebaseDatabase
import kotlinx.android.synthetic.main.goal_detail_completed.*

class GoalDetailCompleted : AppCompatActivity() {

    lateinit var txtPercentage: TextView
    lateinit var txtSavedAmount: TextView
    lateinit var txtGoalRemainingAmount: TextView
    lateinit var txtGoalDescription: TextView
    lateinit var imgGoal: ImageView
    lateinit var txtGoalAmount: TextView
    lateinit var txtEarnedAmount: TextView
    lateinit var btnSave: Button
    lateinit var btnSetReached: Button
    lateinit var pb: ProgressBar
    lateinit var txtTargetDate: TextView
    lateinit var recycler: RecyclerView
    lateinit var goals: Goals
    lateinit var db: LocalFirebaseDatabase
    lateinit var goalHistoryAdapter: GoalHistoryAdapter
    lateinit var ghList: List<GoalHistory>

    companion object {
        lateinit var goal: Goals
        fun start(context: Context, g: Goals) {
            goal = g
            val intent: Intent = Intent(context, GoalDetailCompleted::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
        setContentView(R.layout.goal_detail_completed)
        initViews()
        initListeners()
    }

    private fun initListeners() {
        goalDetailReached_toolbar.setOnClickListener(View.OnClickListener {
            onBackPressed()
        })
        goalDetailReached_toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.iconDelete -> {
                    val iBuilder = AlertDialog.Builder(this)
                    val dialogListener = object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            when (which) {
                                DialogInterface.BUTTON_NEGATIVE -> {
                                    db.deleteDataFromDB(
                                        "Goals",
                                        goals.accountID,
                                        goals.key,
                                        object : FirebaseDatabaseCallback {
                                            override fun onSuccessInsert(key: String) {
                                                for (gh in ghList) {
                                                    deleteGoalHistory(
                                                        "Goal History",
                                                        goals.accountID,
                                                        gh.goalHistoryKey
                                                    )
                                                }
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Successfully Deleted Goal",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                finish()
                                            }

                                            override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                                                TODO("Not yet implemented")
                                            }

                                            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                                                TODO("Not yet implemented")
                                            }

                                            override fun onError(e: Exception) {
                                                Log.e("ERROR_DELETE", e.message.toString())
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Failed To Delete Goal",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        })
                                }
                            }
                        }
                    }
                    iBuilder.setMessage("You are about to delete this goal, Do you want to proceed?")
                        .setPositiveButton("No", dialogListener)
                        .setNegativeButton("Yes", dialogListener)
                        .show()
                    true
                }
                else -> {
                    false
                }
            }
        })
    }

    private fun deleteGoalHistory(s: String, accountID: String, goalHistoryKey: String) {
        db.deleteDataFromDB(s, accountID, goalHistoryKey, object : FirebaseDatabaseCallback {
            override fun onSuccessInsert(key: String) {
                Log.w("SUCCESS_DELETE", "Successfully Delete Goal History: " + key)
            }

            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                TODO("Not yet implemented")
            }

            override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                TODO("Not yet implemented")
            }

            override fun onError(e: Exception) {
                Log.e("ERROR_DELETE", e.message.toString())
            }
        })
    }

    private fun initViews() {
        goals = GoalDetailCompleted.goal
        db = LocalFirebaseDatabase()
        txtPercentage = findViewById(R.id.textView_goalPercent)
        txtSavedAmount = findViewById(R.id.textView_savedAmount)
        txtGoalAmount = findViewById(R.id.textView_goalGoalAmount)
        txtGoalRemainingAmount = findViewById(R.id.textView_goalRemainingAmount)
        txtEarnedAmount = findViewById(R.id.textView_goalEarnedAmount)
        btnSave = findViewById(R.id.btn_savedGoalAmount)
        btnSetReached = findViewById(R.id.btn_setGoalReached)
        pb = findViewById(R.id.goalDetailReached_progressBar)
        txtTargetDate = findViewById(R.id.targetDate_goalDetailReached)
        txtGoalDescription = findViewById(R.id.textView_goalDetailReached)
        imgGoal = findViewById(R.id.img_goalDetailReached)
        recycler = findViewById(R.id.savedAmountHistoryListView)

        txtPercentage.setText(String.format("%.2f", goals.percentage) + "%")
        if (goals.earned == 0.0) {
            pb.setProgress(0)
        } else {
            pb.setProgress(goals.percentage.toInt())
        }
        txtGoalAmount.setText(String.format("P%.2f", goals.goalAmount))
        txtGoalRemainingAmount.setText(String.format("P%.2f", goals.remaining))
        txtEarnedAmount.setText(String.format("P%.2f", goals.earned))
        txtTargetDate.setText("Target Date: " + goals.targetDate)
        txtGoalDescription.setText(goals.goalDescription)
        imgGoal.setBackgroundColor(goals.color)
        try {
            imgGoal.setImageResource(goals.icon)
        } catch (e: Exception) {
            Log.e("ERROR_LOADING_IMG", e.message.toString())
        }
        txtSavedAmount.setText(String.format("P%.2f", goals.earned))
        loadHistory()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun loadHistory() {
        db.getAllDataFromDB("Goal History", goals.accountID, object :
            FirebaseDatabaseCallback {
            override fun onSuccessInsert(key: String) {
                TODO("Not yet implemented")
            }

            override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                var newHistory = mutableListOf<GoalHistory>()
                for (g in goalHistoryList) {
                    if (g.goalkey.equals(goals.key)) {
                        newHistory.add(g)
                    }
                }
                ghList = newHistory
                goalHistoryAdapter = GoalHistoryAdapter(applicationContext, newHistory)
                recycler.layoutManager =
                    GridLayoutManager(applicationContext, 2, GridLayoutManager.VERTICAL, false)
                recycler.adapter = goalHistoryAdapter
            }

            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                TODO("Not yet implemented")
            }

            override fun onError(e: Exception) {
                Log.e("ERROR_LOADING_HISTORY", e.message.toString())
            }
        })
    }
}