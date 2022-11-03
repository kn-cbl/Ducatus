package com.ducatus.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.adapter.GoalAdapter.ViewHolder
import com.ducatus.data.Goals
import com.ducatus.interfaces.ActiveGoalIntf

class GoalAdapter(
    private val context: Context,
    private val mList: List<Goals>,
    private val listener: ActiveGoalIntf
) :
    RecyclerView.Adapter<ViewHolder>() {
    class ViewHolder(itemView: View, listener: ActiveGoalIntf) : RecyclerView.ViewHolder(itemView) {
        var txtDescription: TextView = itemView.findViewById(R.id.textView_goalActive1)
        var txtPercentage: TextView = itemView.findViewById(R.id.percentProgress_goalActive1)
        var txtEarned: TextView = itemView.findViewById(R.id.earnedAmount_goalActive1)
        var txtRemaining: TextView = itemView.findViewById(R.id.remainingAmount_goalActive1)
        var txtGoalAmount: TextView = itemView.findViewById(R.id.goalAmount_goalActive1)
        var txtTargetDate: TextView = itemView.findViewById(R.id.targetDate_goalActive1)
        var imgCircle: ImageView = itemView.findViewById(R.id.sample_img_goalActive1)

        init {
            itemView.setOnClickListener { mView ->
//                listener.OnClickListener(mView, adapterPosition)

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var mView: View = LayoutInflater.from(context)
            .inflate(R.layout.fragment_goal_active, parent, false)

        return ViewHolder(mView, listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var goal: Goals = mList.get(position)
        holder.txtDescription.text = goal.goalDescription
        holder.txtTargetDate.text = "Target date on " + goal.targetDate
        holder.txtGoalAmount.text = goal.goalAmount.toString()
        holder.txtEarned.text = goal.earned.toString()
        holder.txtRemaining.text = goal.remaining.toString()
        var percentage = goal.percentage.toInt()
        holder.txtPercentage.text = percentage.toString() + "%"
        holder.imgCircle.setBackgroundColor(goal.color)
    }

    override fun getItemCount(): Int {
        return mList.size
    }
}