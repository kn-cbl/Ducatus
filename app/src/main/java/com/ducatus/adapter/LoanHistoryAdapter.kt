package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.LoanHistory
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class LoanHistoryAdapter(
    private val loansHistory: MutableList<LoanHistory>,

) : RecyclerView.Adapter<LoanHistoryAdapter.LoanHistoryViewHolder>() {

    class LoanHistoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanHistoryViewHolder {
        return LoanHistoryViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_loan_history,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: LoanHistoryViewHolder, position: Int) {
        val currentLoanHistory = loansHistory[position]

        holder.itemView.apply {
            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(currentLoanHistory.date!!),
                ZoneId.systemDefault()
            )
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val formattedDate = dtf.format(zdt)

            val dtf2 = DateTimeFormatter.ofPattern("h:mm a")
            val formattedTime = dtf2.format(zdt)
            findViewById<TextView>(R.id.tvItemLoanHistoryDate).text = formattedDate
            findViewById<TextView>(R.id.tvItemLoanHistoryTime).text = formattedTime

            // lend or borrow
            val loanState = determineLoanType(currentLoanHistory.type!!)
            val amountColorRes = resources.getIdentifier(
                loanState,
                "color",
                context.packageName
            )

            val amountText = "₱" + String.format("%,.2f", currentLoanHistory.amount)
            findViewById<TextView>(R.id.tvItemLoanHistoryAmount).text = amountText
            findViewById<TextView>(R.id.tvItemLoanHistoryAmount).setTextColor(
                ContextCompat.getColor(context, amountColorRes)
            )

            val notesText = currentLoanHistory.notes ?: resources.getString(R.string.notes_empty)
            findViewById<TextView>(R.id.tvItemLoanHistoryNotes).text = notesText
        }
    }

    override fun getItemCount(): Int {
        return loansHistory.size
    }

    fun addLoanHistory(loanHistory: LoanHistory) {
        loansHistory.add(loanHistory)
        notifyItemInserted(loansHistory.size - 1)
    }

    private fun determineLoanType(type: String): String {
        var color = "bright_red"
        if (type == "B") {
            color = "green_secondary"
        }

        return color
    }
}