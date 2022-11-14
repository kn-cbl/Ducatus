package com.ducatus

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.adapter.ChallengeDetailAdapter
import com.ducatus.data.*
import com.ducatus.interfaces.ChallengesIntf
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.ducatus.services.LocalFirebaseDatabase
import kotlinx.android.synthetic.main.challenges_detail1.*
import java.lang.Exception
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ChallengesDetail1 : AppCompatActivity() {
    lateinit var challenges: Challenges
    lateinit var txtChallengeName: TextView
    lateinit var recycler: RecyclerView
    lateinit var detailAdapter: ChallengeDetailAdapter
    lateinit var txtAmount: TextView
    lateinit var txtDate: TextView
    lateinit var txtTime: TextView
    lateinit var btnSave: Button
    lateinit var pd: ProgressDialog
    lateinit var db: LocalFirebaseDatabase
    var accountID: String = ""

    companion object {
        lateinit var ch: Challenges
        lateinit var cIntf: ChallengesIntf

        fun start(context: Context, c: Challenges, cListener: ChallengesIntf) {
            ch = c
            cIntf = cListener
            val intent = Intent(context, ChallengesDetail1::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
        setContentView(R.layout.challenges_detail1)
        initViews()
        initListener()
    }

    private fun initListener() {
        toolbarChallenges.setOnClickListener(View.OnClickListener {
            finish()
        })
        btnSave.setOnClickListener(View.OnClickListener {
            val mBuilder = AlertDialog.Builder(this)
            val dListener = object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    when (which) {
                        DialogInterface.BUTTON_NEGATIVE -> {
                            pd.show()
                            var challengeHistory = ChallengeHistory()
                            challengeHistory.accountID = accountID
                            challengeHistory.challengeName = challenges.challengeName
                            challengeHistory.datePaid = txtDate.text.toString()
                            challengeHistory.timePaid = txtTime.text.toString()
                            challengeHistory.amount = challenges.values.get(0)
                            challengeHistory.valueIndex = 0

                            var entities = LocalEntities()
                            entities.challengeHistory = challengeHistory

                            db.writeToDb(
                                entities,
                                "Challenge History",
                                object : FirebaseDatabaseCallback {
                                    override fun onSuccessInsert(key: String) {
                                        pd.dismiss()
                                        Toast.makeText(
                                            applicationContext,
                                            "Successfully Added Saved Amount On This Challenge",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        cIntf.OnProccessDone()
                                        finish()
                                    }

                                    override fun onError(e: Exception) {
                                        pd.dismiss()
                                        Toast.makeText(
                                            applicationContext,
                                            "Failed Add Saved Amount On This Challenge",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        Log.e("ERROR_ADDING_CHALLENGES", e.message.toString())
                                    }
                                })
                        }
                        else -> {
                            dialog.cancel()
                        }
                    }
                }
            }
            mBuilder.setMessage("You are about to confirm save amount, Do you still want to proceed?")
                .setNegativeButton("Yes", dListener)
                .setPositiveButton("No", dListener)
                .show()
        })
    }


    private fun initViews() {
        challenges = ChallengesDetail1.ch
        txtChallengeName = findViewById(R.id.goalSavings_Text)
        recycler = findViewById(R.id.recyclerText)
        accountID = SharedPreferences(this).accountId.toString()
        db = LocalFirebaseDatabase()
        pd = ProgressDialog(this)
        pd.setMessage("Sending Request ...")
        pd.setCancelable(false)
        txtAmount = findViewById(R.id.textAmount_challenges1)
        btnSave = findViewById(R.id.btn_confirmSavedAmount)
        txtDate = findViewById(R.id.textDate_challenges1)
        txtTime = findViewById(R.id.textTime_challenges1)
        txtAmount.text = "₱" + challenges.values.get(0).toString()
        val detFormatter = DateTimeFormatter.ofPattern("MMM dd, YYYY")
        txtDate.text = detFormatter.format(LocalDate.now())
        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
        txtTime.text =
            timeFormatter.format(LocalDateTime.now()).toString()

        txtChallengeName.text = challenges.challengeName

        if (challenges.values.isNotEmpty()) {
            detailAdapter =
                ChallengeDetailAdapter(this, challenges.values, object : ChallengeDetailListener {
                    override fun onTextListener(position: Int) {
                        Toast.makeText(applicationContext, "Clicked", Toast.LENGTH_SHORT).show()
                    }
                })
            recycler.layoutManager =
                object : GridLayoutManager(this, 6, GridLayoutManager.VERTICAL, false) {
                    override fun canScrollVertically(): Boolean {
                        return false
                    }
                }
            recycler.adapter = detailAdapter
            recycler.scrollToPosition(0)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}