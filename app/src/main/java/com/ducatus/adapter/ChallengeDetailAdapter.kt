package com.ducatus.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.interfaces.ChallengeDetailListener

class ChallengeDetailAdapter(
    private val context: Context,
    private val list: Array<Int>,
    private val detailListener: ChallengeDetailListener
) :
    RecyclerView.Adapter<ChallengeDetailAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtChallenge: TextView = itemView.findViewById(R.id.txtChallenge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mView =
            LayoutInflater.from(context).inflate(R.layout.item_text_challenges, parent, false)
        return ViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val num = list.get(position)
        holder.txtChallenge.text = num.toString()
        holder.txtChallenge.setBackgroundResource(R.drawable.square_shape_black)
        holder.txtChallenge.setTextColor(context.resources.getColor(R.color.black))
        holder.txtChallenge.setOnClickListener(View.OnClickListener {
            detailListener.onTextListener(position)
        })
        holder.itemView.setOnClickListener(View.OnClickListener {
            detailListener.onTextListener(position)
        })
    }

    override fun getItemCount(): Int {
        return list.size
    }
}