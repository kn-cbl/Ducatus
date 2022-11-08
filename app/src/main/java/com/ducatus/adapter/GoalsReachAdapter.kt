package com.ducatus.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.Goals
import com.ducatus.interfaces.ReachGoalIntf

class GoalsReachAdapter(
    private val context: Context,
    private val list: List<Goals>,
    private val listener: ReachGoalIntf
) :
    RecyclerView.Adapter<GoalsReachAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtDescription: TextView = itemView.findViewById(R.id.textView_goalReached1)
        var txtGoalAmount: TextView = itemView.findViewById(R.id.reachedAmount_goalReached1)
        var txtTargetDate: TextView = itemView.findViewById(R.id.dateReached_goalReached1)
        var imgCircle: ImageView = itemView.findViewById(R.id.sample_img_goalReached1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mView =
            LayoutInflater.from(context).inflate(R.layout.fragment_goal_reached, parent, false)

        return ViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val goals = list.get(position)
        holder.txtDescription.text = goals.goalDescription
        holder.txtTargetDate.text = "Reached " + goals.targetDate
        holder.txtGoalAmount.text = "P" + goals.goalAmount.toString()
        holder.imgCircle.setBackgroundColor(goals.color)
        holder.imgCircle.setImageResource(goals.icon)
        holder.itemView.setOnClickListener(View.OnClickListener { it ->
            listener.OnClick(it, position)
        })
    }

    override fun getItemCount(): Int {
        return list.size
    }
}