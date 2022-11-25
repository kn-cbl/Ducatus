package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.interfaces.SubscriptionHistoryInterface
import com.ducatus.R
import com.ducatus.data.SubscriptionHistory
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class SubscriptionHistoryAdapter(
    private val subscriptionsHistory: MutableList<SubscriptionHistory>,
    private val listener: SubscriptionHistoryInterface

) : RecyclerView.Adapter<SubscriptionHistoryAdapter.SubscriptionHistoryViewHolder>() {

    class SubscriptionHistoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionHistoryViewHolder {
        return SubscriptionHistoryViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_subscription_history,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SubscriptionHistoryViewHolder, position: Int) {
        val activity = listener.getActivityInterface()
        val currentSubscriptionHistory = subscriptionsHistory[position]

        holder.itemView.apply {
            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(currentSubscriptionHistory.dueAt!!),
                ZoneId.systemDefault()
            )
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val formattedDate = dtf.format(zdt)
            findViewById<TextView>(R.id.tvItemSubscriptionHistoryDate).text = formattedDate

            when (currentSubscriptionHistory.paidAt) {
                null -> {
                    findViewById<LinearLayout>(R.id.llItemSubscriptionHistory).setBackgroundColor(
                        ContextCompat.getColor(activity, R.color.blue_alpha)
                    )

                    val status = getElapsedTime(currentSubscriptionHistory.dueAt!!)
                    val statusIconTint =
                        if (status["overdue"] == "false") {
                            R.color.bright_blue
                        }
                        else {
                            R.color.bright_red
                        }

                    val dateText = "Due ${status["date"]}"
                    findViewById<TextView>(R.id.tvItemSubscriptionHistoryStatusDate).text = dateText
                    findViewById<TextView>(R.id.tvItemSubscriptionHistoryStatusDate).setTextColor(
                        ContextCompat.getColor(activity, statusIconTint)
                    )

                    findViewById<TextView>(R.id.tvItemSubscriptionHistoryStatusDate)
                        .setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.ic_local_due_date_24_bright_blue,
                            0,
                            0,
                            0
                        )

                    findViewById<TextView>(R.id.tvItemSubscriptionHistoryStatusDate)
                        .compoundDrawablesRelative[0]
                        .setTint((ContextCompat.getColor(activity, statusIconTint)))

                    findViewById<Button>(R.id.btnItemSubscriptionHistory).visibility = View.VISIBLE
                    findViewById<Button>(R.id.btnItemSubscriptionHistory).setOnClickListener {
                        listener.confirmPayment(currentSubscriptionHistory)
                    }
                }
                else -> {
                    val zdtPaidAt = ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(currentSubscriptionHistory.paidAt!!),
                        ZoneId.systemDefault()
                    )
                    val formattedStatusDate = dtf.format(zdtPaidAt)

                    val dateText = "Paid on $formattedStatusDate"
                    findViewById<TextView>(R.id.tvItemSubscriptionHistoryStatusDate).text = dateText

                    val amountText = "₱" + String.format("%,.2f", currentSubscriptionHistory.amount)
                    val amountTextView = findViewById<TextView>(R.id.tvItemSubscriptionHistoryAmount)
                    amountTextView.text = amountText
                    amountTextView.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return subscriptionsHistory.size
    }

    fun addSubscriptionHistory(subscriptionHistory: SubscriptionHistory) {
        subscriptionsHistory.add(subscriptionHistory)
        notifyItemInserted(subscriptionsHistory.size - 1)
    }

    private fun getElapsedTime(date: Long): Map<String, String> {
        val dateText: String
        var overdue = false

        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )
        val zdtDueDate = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(date),
            ZoneId.systemDefault()
        )

        val startDate = zdtToday.toInstant()
        val endDate = zdtDueDate.toInstant()
        val elapsedDays = ChronoUnit.DAYS.between(startDate, endDate)

        if (elapsedDays > 0) {
            dateText =
                if (elapsedDays.toInt() == 1) "in $elapsedDays day"
                else "in $elapsedDays days"
        }
        else if (elapsedDays.toInt() == 0) {
            dateText = "today"
        }
        else {
            dateText = "${elapsedDays * -1}d ago"
            overdue = true
        }

        return mapOf("date" to dateText, "overdue" to overdue.toString())
    }
}