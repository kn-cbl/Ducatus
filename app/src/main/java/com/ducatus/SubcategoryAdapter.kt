package com.ducatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView

class SubcategoryAdapter(
    private val subcategories: MutableList<Subcategory>,
    private val listener: SubcategoryInterface

) : RecyclerView.Adapter<SubcategoryAdapter.SubcategoryViewHolder>() {

    class SubcategoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubcategoryViewHolder {
        return SubcategoryViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_subcategory,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SubcategoryViewHolder, position: Int) {
        val activity = listener.getActivityInterface()
        val currentSubcategory = subcategories[position]

        holder.itemView.apply {
            val iconColor = resources.getIdentifier(
                currentSubcategory.subcategory_color.toString(),
                "color",
                activity.packageName
            )

            findViewById<FrameLayout>(R.id.flItemSubcategoryIcon).backgroundTintList = ContextCompat.getColorStateList(activity, iconColor)

            val icon = resources.getIdentifier(
                currentSubcategory.subcategory_icon.toString(),
                "drawable",
                activity.packageName
            )

            findViewById<ImageView>(R.id.ivItemSubcategoryIcon).setImageResource(icon)
            findViewById<ImageView>(R.id.ivItemSubcategoryIcon).setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )

            findViewById<TextView>(R.id.tvItemSubcategoryName).text = currentSubcategory.subcategory_name
            findViewById<ImageView>(R.id.ibItemSubcategoryEdit).tag = currentSubcategory.subcategory_id
            findViewById<ImageView>(R.id.ibItemSubcategoryEdit).setOnClickListener {
                listener.showPopup(it, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return subcategories.size
    }

    fun addSubcategory(subcategory: Subcategory) {
        subcategories.add(subcategory)
        notifyItemInserted(subcategories.size - 1)
    }

    fun removeSubcategory(position: Int) {
        subcategories.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, subcategories.size)
    }
}