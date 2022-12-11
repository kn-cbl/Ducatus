package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.interfaces.LoanInterface
import com.ducatus.R
import com.ducatus.data.Loan
import com.google.android.material.divider.MaterialDivider
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class LoanActiveAdapter(
    private val loans: MutableList<Loan>,
    private val listener: LoanInterface

) : RecyclerView.Adapter<LoanActiveAdapter.LoanActiveViewHolder>() {

    class LoanActiveViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanActiveViewHolder {
        return LoanActiveViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_loan,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: LoanActiveViewHolder, position: Int) {
        val currentLoanActive = loans[position]

        holder.itemView.apply {
            findViewById<TextView>(R.id.tvItemLoanIcon).text = currentLoanActive.name?.get(0)?.uppercase()
            findViewById<TextView>(R.id.tvItemLoanName).text = currentLoanActive.name

            val notesText = currentLoanActive.notes ?: resources.getString(R.string.notes_empty)
            findViewById<TextView>(R.id.tvItemLoanNotes).text = notesText

            // lend or borrow
            val loanType = determineLoanType(currentLoanActive.type!!)
            val amountColorRes = resources.getIdentifier(
                loanType,
                "color",
                context.packageName
            )

            val amountText = "₱" + String.format("%,.2f", currentLoanActive.amount)
            findViewById<TextView>(R.id.tvItemLoanAmount).text = amountText
            findViewById<TextView>(R.id.tvItemLoanAmount).setTextColor(
                ContextCompat.getColor(context, amountColorRes)
            )

            val loanState = isOverdue(currentLoanActive)
            val dateColorRes = resources.getIdentifier(
                loanState["color"],
                "color",
                context.packageName
            )

            findViewById<TextView>(R.id.tvItemLoanDate).text = loanState["date"]
            findViewById<TextView>(R.id.tvItemLoanDate).setTextColor(
                ContextCompat.getColor(context, dateColorRes)
            )

            if (loanState["time"] != "") {
                findViewById<TextView>(R.id.tvItemLoanTime).text = loanState["time"]
            }
            else {
                findViewById<TextView>(R.id.tvItemLoanTime).visibility = View.GONE
            }

            findViewById<LinearLayout>(R.id.llItemLoan).setOnClickListener {
                listener.viewItem(currentLoanActive.id!!)
            }

            if (position == loans.size - 1) {
                findViewById<MaterialDivider>(R.id.mdItemLoan).visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int {
        return loans.size
    }

    fun addLoan(loan: Loan) {
        loans.add(loan)
        notifyItemInserted(loans.size - 1)
    }

    private fun determineLoanType(type: String): String {
        var color = "bright_red"
        if (type == "B") {
           color = "green_secondary"
        }

        return color
    }

    private fun isOverdue(loan: Loan): Map<String, String> {
        val color: String
        val dateText: String
        var timeText = ""

        val startDate = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(loan.dueDate!!),
            ZoneId.systemDefault()
        )
        val endDate = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )

        val startEpoch = startDate.toInstant().toEpochMilli()
        val endEpoch = endDate.toInstant().toEpochMilli()

        if (startEpoch > endEpoch) {
            val dtf = DateTimeFormatter.ofPattern("MM/dd/yy")
            val formattedDate = dtf.format(startDate)

            color = "darker_gray"
            dateText = "Due on $formattedDate"

            val dtf2 = DateTimeFormatter.ofPattern("h:mm a")
            timeText = dtf2.format(startDate)
        }
        else {
            color = "bright_red"

            val elapsedYears = ChronoUnit.YEARS.between(startDate, endDate)
            val elapsedMonths = ChronoUnit.MONTHS.between(startDate, endDate)
            val elapsedDays = ChronoUnit.DAYS.between(startDate, endDate)
            val elapsedHours = ChronoUnit.HOURS.between(startDate, endDate)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startDate, endDate)
            val elapsedSeconds = ChronoUnit.SECONDS.between(startDate, endDate)

            dateText =
                if (elapsedYears > 0) {
                    "${elapsedYears}y overdue"
                }
                else if (elapsedMonths > 0) {
                    "${elapsedMonths}m overdue"
                }
                else if (elapsedDays > 0) {
                    "${elapsedDays}d overdue"
                }
                else if (elapsedHours > 0) {
                    "${elapsedHours}h overdue"
                }
                else if (elapsedMinutes > 0) {
                    "${elapsedMinutes}min. overdue"
                }
                else {
                    "${elapsedSeconds}s overdue"
                }
        }

        return mapOf("color" to color, "date" to dateText, "time" to timeText)
    }
}