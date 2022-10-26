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
import com.ducatus.data.Transaction
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.DateFormat
import java.util.*

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
        val activity = listener.getActivityInterface()
        val currentTransaction = transactions[position]

        holder.itemView.apply {
            var itemColor = currentTransaction.category_color
            var itemIcon = currentTransaction.category_icon
            var itemName = currentTransaction.category_name

            // check if item has subcategory data
            if (currentTransaction.subcategory_id != null) {
                itemColor = currentTransaction.subcategory_color
                itemIcon = currentTransaction.subcategory_icon
                itemName = currentTransaction.subcategory_name
            }

            val iconColor = resources.getIdentifier(
                itemColor,
                "color",
                activity.packageName
            )

            val icon = resources.getIdentifier(
                itemIcon,
                "drawable",
                activity.packageName
            )

            findViewById<FrameLayout>(R.id.flItemTransactionIcon).backgroundTintList = ContextCompat.getColorStateList(activity, iconColor)
            findViewById<ImageView>(R.id.ivItemTransactionIcon).setImageResource(icon)
            findViewById<ImageView>(R.id.ivItemTransactionIcon).setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )

            findViewById<TextView>(R.id.tvItemTransactionCategory).text = itemName
            findViewById<TextView>(R.id.tvItemTransactionType).text = currentTransaction.transaction_payment_type

            // expense or income
            val transactionState = determineTransactionType(currentTransaction.transaction_type)
            val amountColorRes = resources.getIdentifier(
                transactionState["color"],
                "color",
                activity.packageName
            )

            val amountText = transactionState["currency"] + String.format("%,.2f", currentTransaction.transaction_amount)
            findViewById<TextView>(R.id.tvItemTransactionAmount).text = amountText
            findViewById<TextView>(R.id.tvItemTransactionAmount).setTextColor(ContextCompat.getColor(activity, amountColorRes))

            val dateText = determineDateText(currentTransaction.transaction_date!!)
            findViewById<TextView>(R.id.tvItemTransactionDate).text = dateText
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
        val currency: String
        val color: String
        when (type) {
            0 -> {
                currency = "-P"
                color = "bright_red"
            }
            1 -> {
                currency = "P"
                color = "green_secondary"
            }
            else -> {
                currency = "P"
                color = "darker_gray"
            }
        }

        return mapOf("currency" to currency, "color" to color)
    }

    private fun determineDateText(date: Long): String {
        val formattedDate =
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
                .format(Date(date))

        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = today

        val dateToday = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
            .format(Date(calendar.timeInMillis))

        var dateText = formattedDate
        if (formattedDate == dateToday) {
            dateText = "Today"
        }

        return dateText
    }
}