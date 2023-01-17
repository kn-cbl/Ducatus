package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.interfaces.ChallengeInterface
import com.ducatus.R
import com.ducatus.data.Challenge
import com.google.android.material.progressindicator.LinearProgressIndicator

class ChallengeAdapter(
    private val challenges: MutableList<Challenge>,
    private val listener: ChallengeInterface

) : RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>() {

    class ChallengeViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        return ChallengeViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_challenge,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val currentChallenge = challenges[position]

        holder.itemView.apply {
            findViewById<TextView>(R.id.tvItemChallengeName).text = currentChallenge.title

            if (currentChallenge.dateStarted == null) {
                findViewById<TextView>(R.id.tvItemChallengeProgress).visibility = View.GONE
                findViewById<LinearLayout>(R.id.llItemChallengeAmountProgress).visibility = View.GONE
                findViewById<TextView>(R.id.tvItemChallengeAction).text =
                    resources.getString(R.string.get_started)
            }
            else {
                val challengeTotal = currentChallenge.challengeAmount
                val challengeEarned = currentChallenge.savedAmount
                val challengeRemaining = challengeTotal.minus(challengeEarned)

                val earnedText = "₱$challengeEarned"
                findViewById<TextView>(R.id.tvItemChallengeEarned).text = earnedText

                val challengeRemainingText = "₱$challengeRemaining"
                findViewById<TextView>(R.id.tvItemChallengeRemaining).text = challengeRemainingText

                val challengeTotalText = "₱$challengeTotal"
                findViewById<TextView>(R.id.tvItemChallengeAmount).text = challengeTotalText

                val challengeProgress = ((challengeEarned.toDouble() / challengeTotal.toDouble()) * 100).toInt()
                findViewById<LinearProgressIndicator>(R.id.pbItemChallenge).progress = challengeProgress

                val progressText = "${challengeProgress}%"
                findViewById<TextView>(R.id.tvItemChallengeProgress).text = progressText
            }

            if (currentChallenge.isFinished) {
                // has days missed
                // change remaining to missed
                // change remaining text and indicator color to red
                if (currentChallenge.challengeAmount != currentChallenge.savedAmount) {
                    findViewById<TextView>(R.id.tvItemChallengeRemainingTitle).text = resources.getString(R.string.missed)
                    findViewById<TextView>(R.id.tvItemChallengeRemaining).setTextColor(
                        ContextCompat.getColor(context, R.color.bright_red)
                    )

                    findViewById<LinearProgressIndicator>(R.id.pbItemChallenge).setIndicatorColor(
                        ContextCompat.getColor(context, R.color.bright_red)
                    )
                }

                findViewById<TextView>(R.id.tvItemChallengeAction).text =
                    resources.getString(R.string.view)
            }

            findViewById<TextView>(R.id.tvItemChallengeAction).setOnClickListener {
                listener.viewItem(currentChallenge)
            }
        }
    }

    override fun getItemCount(): Int {
        return challenges.size
    }

    fun addChallenge(challenge: Challenge) {
        challenges.add(challenge)
        notifyItemInserted(challenges.size - 1)
    }
}