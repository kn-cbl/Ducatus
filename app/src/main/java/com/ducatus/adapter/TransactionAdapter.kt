package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.interfaces.TransactionInterface
import com.ducatus.data.Transaction
import com.google.android.material.card.MaterialCardView

class TransactionAdapter(
    private val transactions: MutableList<Transaction>,
    private val listener: TransactionInterface

) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        return TransactionViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_transaction,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val currentTransaction = transactions[position]

        holder.itemView.apply {
            var itemColor = currentTransaction.categoryColor
            var itemIcon = currentTransaction.categoryIcon
            var itemName = currentTransaction.categoryName

            // check if item has subcategory data
            if (currentTransaction.subcategoryId != null) {
                itemColor = currentTransaction.subcategoryColor
                itemIcon = currentTransaction.subcategoryIcon
                itemName = currentTransaction.subcategoryName
            }

            val iconColor = resources.getIdentifier(
                itemColor,
                "color",
                context.packageName
            )

            val icon = resources.getIdentifier(
                itemIcon,
                "drawable",
                context.packageName
            )

            findViewById<FrameLayout>(R.id.flItemTransactionIcon).backgroundTintList =
                ContextCompat.getColorStateList(context, iconColor)

            findViewById<ImageView>(R.id.ivItemTransactionIcon).setImageResource(icon)
            findViewById<ImageView>(R.id.ivItemTransactionIcon).setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )

            findViewById<TextView>(R.id.tvItemTransactionName).text = currentTransaction.name
            findViewById<TextView>(R.id.tvItemTransactionCategory).text = itemName
            findViewById<TextView>(R.id.tvItemTransactionType).text = currentTransaction.paymentType

            // expense or income
            val transactionState = determineTransactionType(currentTransaction.type)
            val amountColorRes = resources.getIdentifier(
                transactionState["color"],
                "color",
                context.packageName
            )

            val amountText = transactionState["currency"] + String.format("%,.2f", currentTransaction.amount)
            findViewById<TextView>(R.id.tvItemTransactionAmount).text = amountText
            findViewById<TextView>(R.id.tvItemTransactionAmount).setTextColor(
                ContextCompat.getColor(context, amountColorRes)
            )

            findViewById<MaterialCardView>(R.id.cvItemTransaction).setOnClickListener {
                listener.viewItem(currentTransaction)
            }
        }
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
        notifyItemInserted(transactions.size - 1)
    }

    private fun determineTransactionType(type: Int): Map<String, String> {
        var currency = "-₱"
        var color = "bright_red"

        if (type == 1) {
            currency = "₱"
            color = "green_secondary"
        }

        return mapOf("currency" to currency, "color" to color)
    }
}