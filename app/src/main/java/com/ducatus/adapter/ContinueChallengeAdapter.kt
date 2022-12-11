package com.ducatus.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.Challenges
import java.time.LocalDate
import java.time.chrono.ChronoLocalDate
import java.time.temporal.ChronoUnit

class ContinueChallengeAdapter(
    private val context: Context,
    private val challenge: Challenges
) : RecyclerView.Adapter<ContinueChallengeAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtChallenge: TextView = itemView.findViewById(R.id.txtChallenge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mView =
            LayoutInflater.from(context).inflate(R.layout.item_text_challenges, parent, false)
        return ViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val num = challenge.values[position]
        val availMaps = challenge.availedChallengeMap
        holder.txtChallenge.text = num.toString()
        if (availMaps.containsKey(position)) {
            holder.txtChallenge.setBackgroundResource(R.drawable.square_shape_green)
            holder.txtChallenge.setTextColor(context.resources.getColor(R.color.white))
        } else {
            //calculate in between days
            val inBetweenDays =
                ChronoUnit.DAYS.between(LocalDate.parse(challenge.startDatePaid), LocalDate.now())
            if (inBetweenDays.toInt() == position) {
                holder.txtChallenge.setBackgroundResource(R.drawable.square_shape_blue)
                holder.txtChallenge.setTextColor(context.resources.getColor(R.color.black))
            } else if (position < inBetweenDays.toInt()) {
                holder.txtChallenge.setBackgroundResource(R.drawable.square_shape_red)
                holder.txtChallenge.setTextColor(context.resources.getColor(R.color.black))
            } else {
                holder.txtChallenge.setBackgroundResource(R.drawable.square_shape_black)
                holder.txtChallenge.setTextColor(context.resources.getColor(R.color.black))
            }

        }

    }

    override fun getItemCount(): Int {
        return challenge.values.size
    }
}