package com.ducatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.DateFormat
import java.util.*

class BudgetAdapter(
    private val budgets: MutableList<Budget>,
    private val listener: BudgetInterface

) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    class BudgetViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        return BudgetViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_budget,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val activity = listener.getActivityInterface()
        val currentBudget = budgets[position]

        holder.itemView.apply {
            val currentBudgetDate =
                DateFormat
                    .getDateInstance(DateFormat.MEDIUM, Locale.US)
                    .format(Date(currentBudget.budget_created_at!!.toLong() * 1000))

            if (position > 0) {
                val previousBudgetDate =
                    DateFormat
                        .getDateInstance(DateFormat.MEDIUM, Locale.US)
                        .format(Date(budgets[position - 1].budget_created_at!!.toLong() * 1000))

                if (previousBudgetDate == currentBudgetDate) {
                    findViewById<TextView>(R.id.tvItemBudgetDate).visibility = View.GONE
                }
                else {
                    findViewById<TextView>(R.id.tvItemBudgetDate).text = currentBudgetDate
                }
            }
            else {
                findViewById<TextView>(R.id.tvItemBudgetDate).text = currentBudgetDate
            }

            val iconColor = resources.getIdentifier(
                currentBudget.category_color,
                "color",
                activity.packageName
            )

            findViewById<FrameLayout>(R.id.flItemBudgetCategoryIcon).backgroundTintList = ContextCompat.getColorStateList(activity, iconColor)

            val icon = resources.getIdentifier(
                currentBudget.category_icon,
                "drawable",
                activity.packageName
            )

            findViewById<ImageView>(R.id.ivItemBudgetCategoryIcon).setImageResource(icon)
            findViewById<ImageView>(R.id.ivItemBudgetCategoryIcon).setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )

            findViewById<TextView>(R.id.tvItemBudgetName).text = currentBudget.budget_name
            findViewById<TextView>(R.id.tvItemBudgetCategory).text = currentBudget.category_name

            val budgetTotal = currentBudget.budget_amount_total.toString().toDouble()
            val budgetSpent = currentBudget.budget_amount_spent.toString().toDouble()
            val budgetLeft = budgetTotal.minus(budgetSpent)

            val spentText = "₱" + String.format("%,.2f", budgetSpent)
            findViewById<TextView>(R.id.tvItemBudgetSpent).text = spentText
            findViewById<TextView>(R.id.tvItemBudgetSpent).setTextColor(
                ContextCompat.getColor(activity, iconColor)
            )

            val budgetLeftText = "₱" + String.format("%,.2f", budgetLeft)
            findViewById<TextView>(R.id.tvItemBudgetLeft).text = budgetLeftText
            findViewById<TextView>(R.id.tvItemBudgetLeft).setTextColor(
                ContextCompat.getColor(activity, iconColor)
            )

            val budgetTotalText = "₱" + String.format("%,.2f", budgetTotal)
            findViewById<TextView>(R.id.tvItemBudgetLimit).text = budgetTotalText
            findViewById<TextView>(R.id.tvItemBudgetLimit).setTextColor(
                ContextCompat.getColor(activity, iconColor)
            )

            findViewById<LinearProgressIndicator>(R.id.pbItemBudget).progress = ((budgetSpent / budgetTotal) * 100).toInt()
            findViewById<LinearProgressIndicator>(R.id.pbItemBudget).setIndicatorColor(ContextCompat.getColor(activity, iconColor))
            findViewById<ImageView>(R.id.ibViewItemBudget).tag = currentBudget.budget_id
            findViewById<ImageView>(R.id.ibViewItemBudget).setOnClickListener {
                listener.viewItem(currentBudget.budget_id.toString())
            }
        }
    }

    override fun getItemCount(): Int {
        return budgets.size
    }

    fun addBudget(budget: Budget) {
        budgets.add(budget)
        notifyItemInserted(budgets.size - 1)
    }
}