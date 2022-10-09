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

            findViewById<FrameLayout>(R.id.flSubcategoryIcon).backgroundTintList = ContextCompat.getColorStateList(activity, iconColor)

            val icon = resources.getIdentifier(
                currentSubcategory.subcategory_icon.toString(),
                "drawable",
                activity.packageName
            )

            findViewById<ImageView>(R.id.ivSubcategoryIcon).setImageResource(icon)
            findViewById<ImageView>(R.id.ivSubcategoryIcon).setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )

            findViewById<TextView>(R.id.tvSubcategoryName).text = currentSubcategory.subcategory_name
            findViewById<ImageView>(R.id.ivSubcategoryEdit).setOnClickListener {
                listener.showPopup(it)
            }
        }
    }

    override fun getItemCount(): Int {
        return subcategories.size
    }

    fun addSubcategory(category: Subcategory) {
        subcategories.add(category)
        notifyItemInserted(subcategories.size - 1)
    }
}