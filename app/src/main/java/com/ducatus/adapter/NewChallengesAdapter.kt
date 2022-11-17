package com.ducatus.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.Challenges
import com.ducatus.interfaces.NewChallengeIntf

class NewChallengesAdapter(
    private val mContext: Context,
    private val list: List<Challenges>,
    private val listener: NewChallengeIntf
) :
    RecyclerView.Adapter<NewChallengesAdapter.ViewHolder>() {


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtChallengeName: TextView = itemView.findViewById(R.id.textViewAmount_newChallenges1)
        var txtPercentage: TextView = itemView.findViewById(R.id.percentProgress_newChallenges1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mView =
            LayoutInflater.from(mContext).inflate(R.layout.item_new_challenges, parent, false)
        return ViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val challenge = list.get(position)
        holder.txtChallengeName.text = challenge.challengeName
        if (challenge.earned != 0.0) {
            val percent = challenge.earned / challenge.amount * 100
            holder.txtPercentage.text = percent.toString() + "%"
        } else {
            holder.txtPercentage.text = "0%"
        }
        holder.itemView.setOnClickListener(View.OnClickListener {
            listener.OnItemClickListener(position)
        })
    }

    override fun getItemCount(): Int {
        return list.size
    }
}