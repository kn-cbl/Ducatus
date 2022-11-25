package com.ducatus.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R

class GridIconsAdapter(private var context: Context, private var list: List<String>) :
    RecyclerView.Adapter<GridIconsAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imgIcon: ImageView = itemView.findViewById(R.id.ivItemIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mView = LayoutInflater.from(context).inflate(R.layout.dialog_item_icon, parent, false)

        return ViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val icon = context.resources.getIdentifier(
            list[position],
            "drawable",
            context.packageName
        )
        holder.imgIcon.setImageResource(icon)
        holder.itemView.setOnClickListener(View.OnClickListener {
            
        })
    }

    override fun getItemCount(): Int {
        return list.size
    }
}