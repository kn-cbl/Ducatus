package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.Goal
import com.ducatus.interfaces.HomeBudgetsGoalsInterface
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson

class HomeGoalAdapter(
    private val goals: MutableList<Goal>,
    private val listener: HomeBudgetsGoalsInterface

) : RecyclerView.Adapter<HomeGoalAdapter.HomeGoalViewHolder>() {

    class HomeGoalViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeGoalViewHolder {
        return HomeGoalViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_budget_goal,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: HomeGoalViewHolder, position: Int) {
        val currentGoal = goals[position]

        holder.itemView.apply {
            val iconColor = resources.getIdentifier(
                currentGoal.color,
                "color",
                context.packageName
            )

            ContextCompat.getColor(context, iconColor).apply {
                findViewById<MaterialCardView>(R.id.cvItemBudgetGoal).strokeColor = this
                findViewById<LinearProgressIndicator>(R.id.pbItemBudgetGoal).setIndicatorColor(this)
            }

            findViewById<FrameLayout>(R.id.flItemBudgetGoalView).backgroundTintList =
                ContextCompat.getColorStateList(context, iconColor)

            findViewById<TextView>(R.id.tvItemBudgetGoalName).text = currentGoal.name

            val goalProgress = ((currentGoal.savedAmount / currentGoal.targetAmount) * 100).toInt()
            val progressText = "${goalProgress}%"
            findViewById<TextView>(R.id.tvItemBudgetGoalProgress).text = progressText
            findViewById<LinearProgressIndicator>(R.id.pbItemBudgetGoal).progress = goalProgress

            val savedText = "₱" + String.format("%,.2f", currentGoal.savedAmount)
            findViewById<TextView>(R.id.tvItemBudgetGoalSpent).text = savedText

            val goalTotalText = "₱" + String.format("%,.2f", currentGoal.targetAmount)
            findViewById<TextView>(R.id.tvItemBudgetGoalLimit).text = goalTotalText

            findViewById<MaterialCardView>(R.id.cvItemBudgetGoal).setOnClickListener {
                listener.viewItem(Gson().toJson(currentGoal), "G")
            }
        }
    }

    override fun getItemCount(): Int {
        return goals.size
    }

    fun addGoal(goal: Goal) {
        goals.add(goal)
    }
}