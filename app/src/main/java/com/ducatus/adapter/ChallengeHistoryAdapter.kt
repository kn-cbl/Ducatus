package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.ChallengeHistory

class ChallengeHistoryAdapter(
    private val challengesHistory: MutableList<ChallengeHistory>,
    private val currentChallengePosition: Int,

    ) : RecyclerView.Adapter<ChallengeHistoryAdapter.ChallengeHistoryViewHolder>() {

    class ChallengeHistoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val btnItemChallengeHistory: TextView = itemView.findViewById(R.id.tvItemChallengeHistory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeHistoryViewHolder {
        return ChallengeHistoryViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_challenge_history,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ChallengeHistoryViewHolder, position: Int) {
        val currentChallengeHistory = challengesHistory[position]

        val amountText = "₱" + currentChallengeHistory.amount
        holder.btnItemChallengeHistory.text = amountText

        if (currentChallengeHistory.position < currentChallengePosition) {
            holder.btnItemChallengeHistory.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.off_white)
            )

            holder.btnItemChallengeHistory.setBackgroundResource(
                R.drawable.btn_green_primary_bg
            )

            if (currentChallengeHistory.datePaid != null) { // paid
                holder.btnItemChallengeHistory.backgroundTintList =
                    ContextCompat.getColorStateList(holder.itemView.context, R.color.bright_green)
            }
            else { // missed challenge
                holder.btnItemChallengeHistory.backgroundTintList =
                    ContextCompat.getColorStateList(holder.itemView.context, R.color.bright_red)
            }
        }
        else if (currentChallengeHistory.position == currentChallengePosition) {
            if (currentChallengeHistory.datePaid != null) {
                holder.btnItemChallengeHistory.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.off_white)
                )
                holder.btnItemChallengeHistory.setBackgroundResource(
                    R.drawable.btn_green_primary_bg
                )
                holder.btnItemChallengeHistory.backgroundTintList =
                    ContextCompat.getColorStateList(holder.itemView.context, R.color.bright_green)
            }
            else {
                holder.btnItemChallengeHistory.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.bright_blue)
                )
                holder.btnItemChallengeHistory.setBackgroundResource(
                    R.drawable.selector_gray_outline
                )
                holder.btnItemChallengeHistory.backgroundTintList =
                    ContextCompat.getColorStateList(holder.itemView.context, R.color.bright_blue)
            }
        }
    }

    override fun getItemCount(): Int {
        return challengesHistory.size
    }
}