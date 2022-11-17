package com.ducatus.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.common.Common
import com.ducatus.data.Challenges
import com.ducatus.interfaces.ChallengeDetailListener
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CurrentChallengesAdapter(
    private val context: Context,
    private val list: List<Challenges>,
    private val listener: ChallengeDetailListener
) : RecyclerView.Adapter<CurrentChallengesAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtChallengeName: TextView =
            itemView.findViewById(R.id.textViewAmount_currentChallenges)
        var txtEarned: TextView = itemView.findViewById(R.id.earnedAmount_currentChallenges)
        var txtRemaining: TextView = itemView.findViewById(R.id.remainingAmount_currentChallenges)
        var txtAmount: TextView = itemView.findViewById(R.id.goalAmount_currentChallenges)
        var txtPercent: TextView = itemView.findViewById(R.id.percentProgress_currentChallenges)
        var lblRemaining: TextView = itemView.findViewById(R.id.remaining_currentChallenges)
        var txtContinue: TextView = itemView.findViewById(R.id.textView_continue)
        var pb: ProgressBar = itemView.findViewById(R.id.progressBar_currentChallenges)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mView =
            LayoutInflater.from(context).inflate(R.layout.item_current_challenges, parent, false)

        return ViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val challenges = list.get(position)
        holder.txtChallengeName.text = challenges.challengeName.toString()
        holder.txtEarned.text = "₱" + String.format("%.2f", challenges.earned)
        holder.txtAmount.text = "₱" + String.format("%.2f", challenges.amount)
        holder.txtRemaining.text = "₱" + String.format("%.2f", challenges.remaining)
        val percent = challenges.earned / challenges.amount * 100
        holder.txtPercent.text = String.format("%.2f", percent) + "%"
        holder.pb.progress = percent.toInt()
        val inBetweenDays =
            ChronoUnit.DAYS.between(LocalDate.parse(challenges.startDatePaid), LocalDate.now())
        var challengeDays = Common().getChallengeDaysMap().get(challenges.challengeName)!!
        Log.e("CHALLENGEDAYS", challengeDays.toString())
        Log.e("inBetweenDays", inBetweenDays.toString())
        var isExceed = inBetweenDays.toInt() <= challengeDays

        if (challenges.isFinished || !isExceed) {
            if (challenges.remaining != 0.0) {
                holder.lblRemaining.text = "Missed"
                holder.txtRemaining.setTextColor(context.getColor(R.color.bright_red))
                holder.pb.progressDrawable =
                    context.resources.getDrawable(R.drawable.custom_progress_redbg_2)
                holder.txtContinue.text = "RESTART CHALLENGE"
            }

            holder.txtContinue.setOnClickListener(View.OnClickListener {
                listener.onRestartChallenge(position)
            })
            holder.itemView.setOnClickListener(View.OnClickListener {
                Toast.makeText(context, "Long press to see details", Toast.LENGTH_SHORT).show()
            })
            holder.itemView.setOnLongClickListener(View.OnLongClickListener {
                listener.onClickFinishedChallenge(position)
                true
            })
        } else {
            holder.itemView.setOnClickListener(View.OnClickListener {
                listener.onTextListener(position)
            })
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}

