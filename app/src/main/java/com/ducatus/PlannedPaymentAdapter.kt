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
import com.ducatus.data.PlannedPayment
import com.google.android.material.card.MaterialCardView

class PlannedPaymentAdapter(
    private val plannedPayments: MutableList<PlannedPayment>,
    private val listener: PlannedPaymentInterface

) : RecyclerView.Adapter<PlannedPaymentAdapter.PlannedPaymentViewHolder>() {

    class PlannedPaymentViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlannedPaymentViewHolder {
        return PlannedPaymentViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_planned_payment,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PlannedPaymentViewHolder, position: Int) {
        val activity = listener.getActivityInterface()
        val currentPlannedPayment = plannedPayments[position]

        holder.itemView.apply {
            var itemColor = currentPlannedPayment.categoryColor
            var itemIcon = currentPlannedPayment.categoryIcon
            var itemCategory = currentPlannedPayment.categoryName

            // check if item has subcategory data
            if (currentPlannedPayment.subcategoryId != null) {
                itemColor = currentPlannedPayment.subcategoryColor
                itemIcon = currentPlannedPayment.subcategoryIcon
                itemCategory = currentPlannedPayment.subcategoryName
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

            findViewById<FrameLayout>(R.id.flItemPlannedPaymentIcon).backgroundTintList = ContextCompat.getColorStateList(activity, iconColor)
            findViewById<ImageView>(R.id.ivItemPlannedPaymentIcon).setImageResource(icon)
            findViewById<ImageView>(R.id.ivItemPlannedPaymentIcon).setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )

            findViewById<TextView>(R.id.tvItemPlannedPaymentName).text = currentPlannedPayment.name
            findViewById<TextView>(R.id.tvItemPlannedPaymentCategory).text = itemCategory

            val amountText = "₱" + String.format("%,.2f", currentPlannedPayment.amount)
            findViewById<TextView>(R.id.tvItemPlannedPaymentAmount).text = amountText

            findViewById<MaterialCardView>(R.id.cvItemPlannedPayment).setOnClickListener {
                listener.viewItem(currentPlannedPayment.id!!)
            }
        }
    }

    override fun getItemCount(): Int {
        return plannedPayments.size
    }

    fun addPlannedPayment(plannedPayment: PlannedPayment) {
        plannedPayments.add(plannedPayment)
        notifyItemInserted(plannedPayments.size - 1)
    }
}