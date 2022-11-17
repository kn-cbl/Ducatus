package com.ducatus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.adapter.ContinueChallengeAdapter
import com.ducatus.data.Challenges
import kotlinx.android.synthetic.main.challenges_total_earned.*

class ChallengesTotalEarned : AppCompatActivity() {

    lateinit var challenges: Challenges
    lateinit var challengeAdapter: ContinueChallengeAdapter
    lateinit var recycler: RecyclerView
    lateinit var txtMissedDays: TextView
    lateinit var txtSaveDays: TextView
    lateinit var txtMissedAmount: TextView
    lateinit var txtSaveAmount: TextView
    lateinit var txtConclusion: TextView
    lateinit var txtChallengeName: TextView

    companion object {
        lateinit var c: Challenges
        fun start(context: Context, ch: Challenges) {
            c = ch
            val intent = Intent(context, ChallengesTotalEarned::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_in_right_short, R.anim.slide_out_right_short)
        setContentView(R.layout.challenges_total_earned)
        initViews()
        initListener()
    }

    private fun initListener() {
        challenges_toolbar1.setOnClickListener(View.OnClickListener {
            onBackPressed()
        })
    }

    private fun initViews() {
        challenges = ChallengesTotalEarned.c
        recycler = findViewById(R.id.recycler)
        txtMissedDays = findViewById(R.id.missedDays)
        txtSaveDays = findViewById(R.id.savedDays)
        txtMissedAmount = findViewById(R.id.missedDays_amount)
        txtSaveAmount = findViewById(R.id.savedDays_amount)
        txtConclusion = findViewById(R.id.texttotalEarnedChallenges)
        txtChallengeName = findViewById(R.id.goalSavings_Text)

        val availableMap = challenges.availedChallengeMap
        val values = challenges.values
        var missedDays = 0
        var savedDays = 0

        for (i in values.indices) {
            if (availableMap.containsKey(i)) {
                savedDays++
            } else {
                missedDays++
            }
        }

        txtMissedDays.text = "$missedDays days missed"
        txtSaveDays.text = "$savedDays days saved"
        txtChallengeName.text = "${challenges.challengeName}"
        txtSaveAmount.text = "₱" + String.format("%.2f", challenges.earned)
        txtMissedAmount.text = "₱" + String.format("%.2f", challenges.remaining)
        txtConclusion.text = "You have earned ₱" + String.format(
            "%.2f",
            challenges.earned
        ) + " out of ₱" + String.format("%.2f", challenges.amount)

        challengeAdapter = ContinueChallengeAdapter(this@ChallengesTotalEarned, challenges)
        recycler.layoutManager =
            GridLayoutManager(this@ChallengesTotalEarned, 6, GridLayoutManager.VERTICAL, false)
        recycler.adapter = challengeAdapter
    }
}