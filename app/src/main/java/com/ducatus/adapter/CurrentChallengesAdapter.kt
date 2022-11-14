package com.ducatus.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.Challenges

class CurrentChallengesAdapter(
    private val context: Context,
    private val list: List<Challenges>
) : RecyclerView.Adapter<CurrentChallengesAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtChallengeName: TextView =
            itemView.findViewById(R.id.textViewAmount_currentChallenges)
        var txtEarned: TextView = itemView.findViewById(R.id.earnedAmount_currentChallenges)
        var txtRemaining: TextView = itemView.findViewById(R.id.remainingAmount_currentChallenges)
        var txtAmount: TextView = itemView.findViewById(R.id.goalAmount_currentChallenges)
        var txtPercent: TextView = itemView.findViewById(R.id.percentProgress_currentChallenges)
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
        holder.txtEarned.text = challenges.earned.toString()
        holder.txtAmount.text = challenges.amount.toString()
        holder.txtRemaining.text = challenges.remaining.toString()
        val percent = challenges.earned / challenges.amount * 100
        holder.txtPercent.text = percent.toString() + "%"
        holder.pb.progress = percent.toInt()
    }

    override fun getItemCount(): Int {
        return list.size
    }
}