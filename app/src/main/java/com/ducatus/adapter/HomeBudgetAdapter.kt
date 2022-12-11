package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.interfaces.HomeBudgetsGoalsInterface
import com.ducatus.R
import com.ducatus.data.Budget
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson

class HomeBudgetAdapter(
    private val budgets: MutableList<Budget>,
    private val listener: HomeBudgetsGoalsInterface

) : RecyclerView.Adapter<HomeBudgetAdapter.HomeBudgetViewHolder>() {

    class HomeBudgetViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeBudgetViewHolder {
        return HomeBudgetViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_budget_goal,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: HomeBudgetViewHolder, position: Int) {
        val currentBudget = budgets[position]

        holder.itemView.apply {
            val iconColor = resources.getIdentifier(
                currentBudget.categoryColor,
                "color",
                context.packageName
            )

            ContextCompat.getColor(context, iconColor).apply {
                findViewById<MaterialCardView>(R.id.cvItemBudgetGoal).strokeColor = this
                findViewById<LinearProgressIndicator>(R.id.pbItemBudgetGoal).setIndicatorColor(this)
            }

            findViewById<FrameLayout>(R.id.flItemBudgetGoalView).backgroundTintList =
                ContextCompat.getColorStateList(context, iconColor)

            findViewById<TextView>(R.id.tvItemBudgetGoalName).text = currentBudget.categoryName
            findViewById<TextView>(R.id.tvItemBudgetGoalProgress).visibility = View.GONE

            val budgetProgress = ((currentBudget.amountSpent / currentBudget.amountTotal) * 100).toInt()
            findViewById<LinearProgressIndicator>(R.id.pbItemBudgetGoal).progress = budgetProgress

            val spentText = "₱" + String.format("%,.2f", currentBudget.amountSpent)
            findViewById<TextView>(R.id.tvItemBudgetGoalSpent).text = spentText

            val budgetTotalText = "₱" + String.format("%,.2f", currentBudget.amountTotal)
            findViewById<TextView>(R.id.tvItemBudgetGoalLimit).text = budgetTotalText

            findViewById<MaterialCardView>(R.id.cvItemBudgetGoal).setOnClickListener {
                listener.viewItem(Gson().toJson(currentBudget), "B")
            }
        }
    }

    override fun getItemCount(): Int {
        return budgets.size
    }

    fun addBudget(budget: Budget) {
        budgets.add(budget)
    }
}