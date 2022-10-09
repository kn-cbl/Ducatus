package com.ducatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView

class BudgetAdapter(
    private val budgets: MutableList<Budget>

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
        val currentBudget = budgets[position]

        holder.itemView.apply {
//            val imageColor = resources.getIdentifier(
//                currentBudget.account_color.toString(),
//                "color",
//                activity.packageName
//            )
//
//            findViewById<ImageView>(R.id.ivBudgetImage).setColorFilter(
//                ResourcesCompat.getColor(
//                    resources,
//                    imageColor,
//                    null
//                )
//            )
//
//            findViewById<TextView>(R.id.tvBudgetName).text = currentBudget.account_name
//            findViewById<TextView>(R.id.tvBudgetBudget).text = "₱" + String.format("%,.2f", currentBudget.account_monthly_budget)
//            findViewById<ImageView>(R.id.ibBudgetEdit).tag = currentBudget.account_id
//            findViewById<ImageView>(R.id.ibBudgetEdit).setOnClickListener {
//                listener.showPopup(it, 2)
//            }
        }
    }

    override fun getItemCount(): Int {
        return budgets.size
    }

    fun addBudget(account: Budget) {
        budgets.add(account)
        notifyItemInserted(budgets.size - 1)
    }
}