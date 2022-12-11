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
import com.ducatus.interfaces.SubscriptionInterface
import com.ducatus.R
import com.ducatus.data.Subscription
import com.google.android.material.card.MaterialCardView
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SubscriptionAdapter(
    private val subscriptions: MutableList<Subscription>,
    private val listener: SubscriptionInterface

) : RecyclerView.Adapter<SubscriptionAdapter.SubscriptionViewHolder>() {

    class SubscriptionViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        return SubscriptionViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_subscription,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        val currentSubscription = subscriptions[position]

        holder.itemView.apply {
            var itemColor = currentSubscription.categoryColor
            var itemIcon = currentSubscription.categoryIcon
            var itemCategory = currentSubscription.categoryName

            // check if item has subcategory data
            if (currentSubscription.subcategoryId != null) {
                itemColor = currentSubscription.subcategoryColor
                itemIcon = currentSubscription.subcategoryIcon
                itemCategory = currentSubscription.subcategoryName
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

            findViewById<FrameLayout>(R.id.flItemSubscriptionIcon).backgroundTintList =
                ContextCompat.getColorStateList(context, iconColor)

            findViewById<ImageView>(R.id.ivItemSubscriptionIcon).setImageResource(icon)
            findViewById<ImageView>(R.id.ivItemSubscriptionIcon).setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )

            findViewById<TextView>(R.id.tvItemSubscriptionName).text = currentSubscription.name
            findViewById<TextView>(R.id.tvItemSubscriptionCategory).text = itemCategory

            when (currentSubscription.frequency) {
                0 -> {
                    if (isOverdue(currentSubscription.dueDate!!) && currentSubscription.paidAt == null) {
                        findViewById<ImageView>(R.id.ivItemSubscriptionTypeIcon)
                            .setImageResource(R.drawable.ic_baseline_warning_24_red)
                    }
                    else {
                        findViewById<ImageView>(R.id.ivItemSubscriptionTypeIcon).visibility =
                            View.INVISIBLE
                    }


                    findViewById<TextView>(R.id.tvItemSubscriptionFrequency).text =
                        resources.getString(R.string.one_time)
                }
                else -> {
                    val frequencyText =
                        if (currentSubscription.recurrence == 1) "Every ${currentSubscription.recurrence} month"
                        else "Every ${currentSubscription.recurrence} months"

                    findViewById<TextView>(R.id.tvItemSubscriptionFrequency).text = frequencyText

                    if (isOverdue(currentSubscription.renewsAt!!)) {
                        findViewById<ImageView>(R.id.ivItemSubscriptionTypeIcon)
                            .setImageResource(R.drawable.ic_baseline_warning_24_red)
                    }
                    else {
                        findViewById<ImageView>(R.id.ivItemSubscriptionTypeIcon)
                            .setImageResource(R.drawable.ic_local_due_date_24_bright_blue)
                    }
                }
            }

            val amountText = "₱" + String.format("%,.2f", currentSubscription.amount)
            findViewById<TextView>(R.id.tvItemSubscriptionAmount).text = amountText

            val dtf = DateTimeFormatter.ofPattern("MM/dd/yy")
            val zdtDueDate = when (currentSubscription.frequency) {
                1 -> {
                    ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(currentSubscription.renewsAt!!),
                        ZoneId.systemDefault()
                    )
                }
                else -> {
                    ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(currentSubscription.dueDate!!),
                        ZoneId.systemDefault()
                    )
                }
            }

            var formattedDate = dtf.format(zdtDueDate)
            var dateText = "Due on $formattedDate"

            if (currentSubscription.paidAt != null) {
                val zdtPaidAt = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(currentSubscription.paidAt!!),
                    ZoneId.systemDefault()
                )
                formattedDate = dtf.format(zdtPaidAt)
                dateText = "Paid on $formattedDate"
            }

            findViewById<TextView>(R.id.tvItemSubscriptionDate).text = dateText

            findViewById<MaterialCardView>(R.id.cvItemSubscription).setOnClickListener {
                listener.viewItem(currentSubscription.id!!)
            }
        }
    }

    override fun getItemCount(): Int {
        return subscriptions.size
    }

    fun addSubscription(subscription: Subscription) {
        subscriptions.add(subscription)
        notifyItemInserted(subscriptions.size - 1)
    }

    private fun isOverdue(dueDate: Long): Boolean {
        val zdtDueDate = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(dueDate),
            ZoneId.systemDefault()
        )

        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )

        val date = zdtDueDate.toInstant().toEpochMilli()
        val today = zdtToday.toInstant().toEpochMilli()

        if (today > date) {
            return true
        }

        return false
    }
}