package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.interfaces.HomeOverviewInterface
import com.ducatus.R
import com.ducatus.common.AppResources
import com.ducatus.data.Transaction
import com.google.gson.Gson
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class HomeTransactionAdapter(
    private val transactions: MutableList<Transaction>,
    private val listener: HomeOverviewInterface

) : RecyclerView.Adapter<HomeTransactionAdapter.HomeTransactionViewHolder>() {

    class HomeTransactionViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeTransactionViewHolder {
        return HomeTransactionViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_transaction_recent,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: HomeTransactionViewHolder, position: Int) {
        val currentTransaction = transactions[position]

        holder.itemView.apply {
            findViewById<LinearLayout>(R.id.llItemTransactionRecent).setOnClickListener {
                listener.viewItem('T', Gson().toJson(currentTransaction))
            }

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

            findViewById<FrameLayout>(R.id.flItemTransactionRecentIcon).backgroundTintList =
                ContextCompat.getColorStateList(context, iconColor)

            findViewById<ImageView>(R.id.ivItemTransactionRecentIcon).setImageResource(icon)
            findViewById<ImageView>(R.id.ivItemTransactionRecentIcon).setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )

            findViewById<TextView>(R.id.tvItemTransactionRecentName).text = currentTransaction.name
            findViewById<TextView>(R.id.tvItemTransactionRecentCategory).text = itemName

            val paymentTypes = AppResources().getPaymentTypes()
            val paymentType =
                if (currentTransaction.paymentType != 4) paymentTypes[currentTransaction.paymentType]
                else currentTransaction.paymentTypeOthers

            findViewById<TextView>(R.id.tvItemTransactionRecentType).text = paymentType

            // expense or income
            val amountColor =
                if (currentTransaction.type == 0) "bright_red"
                else "green_secondary"

            val amountColorRes = resources.getIdentifier(
                amountColor,
                "color",
                context.packageName
            )

            val amountText = "(₱" + String.format("%,.2f", currentTransaction.amount) + ")"
            findViewById<TextView>(R.id.tvItemTransactionRecentAmount).text = amountText
            findViewById<TextView>(R.id.tvItemTransactionRecentAmount).setTextColor(
                ContextCompat.getColor(context, amountColorRes)
            )

            val dateText = determineDateText(currentTransaction.date!!)
            findViewById<TextView>(R.id.tvItemTransactionRecentDate).text = dateText["date"]
            findViewById<TextView>(R.id.tvItemTransactionRecentTime).text = dateText["time"]
        }
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
        notifyItemInserted(transactions.size - 1)
    }

    private fun determineDateText(date: Long): Map<String, String> {
        val zdt = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(date),
            ZoneId.systemDefault()
        )
        val transactionDate = zdt.dayOfYear

        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )
        val today = zdtToday.dayOfYear

        val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
        val formattedDate = dtf.format(zdt)

        val dtf2 = DateTimeFormatter.ofPattern("h:mm a")
        val formattedTime = dtf2.format(zdt)

        var dateText = formattedDate
        if (transactionDate == today) {
            dateText = "Today"
        }

        return mapOf("date" to dateText, "time" to formattedTime)
    }
}