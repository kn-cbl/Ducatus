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
import com.ducatus.data.Category

class CategoryAdapter(
    private val categories: MutableList<Category>,
    private val listener: CategoryInterface

) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        return CategoryViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_category,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val activity = listener.getActivityInterface()
        val currentCategory = categories[position]

        holder.itemView.apply {
            val iconColor = resources.getIdentifier(
                currentCategory.category_color.toString(),
                "color",
                activity.packageName
            )

            findViewById<FrameLayout>(R.id.flItemCategoryIcon).backgroundTintList = ContextCompat.getColorStateList(activity, iconColor)

            val icon = resources.getIdentifier(
                currentCategory.category_icon.toString(),
                "drawable",
                activity.packageName
            )

            findViewById<ImageView>(R.id.ivItemCategoryIcon).setImageResource(icon)
            findViewById<ImageView>(R.id.ivItemCategoryIcon).setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )

            findViewById<TextView>(R.id.tvItemCategoryName).text = currentCategory.category_name
            findViewById<ImageView>(R.id.ibItemCategoryEdit).setOnClickListener {
                listener.showPopup(it, position, currentCategory.category_id.toString())
            }
        }
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    fun addCategory(category: Category) {
        categories.add(category)
        notifyItemInserted(categories.size - 1)
    }

    fun removeCategory(position: Int) {
        categories.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, categories.size)
    }
}