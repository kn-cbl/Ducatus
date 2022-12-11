package com.ducatus.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.Tip
import com.ducatus.interfaces.TipInterface
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.squareup.picasso.Callback
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TipAdapter(
    private val tips: List<Tip>,
    private val listener: TipInterface

) : RecyclerView.Adapter<TipAdapter.TipViewHolder>() {

    class TipViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        return TipViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_tip,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        val currentTip = tips[position]

        holder.itemView.apply {
            findViewById<TextView>(R.id.tvItemTipTitle).text = currentTip.title

            findViewById<TextView>(R.id.tvItemTipIcon).text = currentTip.author[0].uppercase()
            findViewById<TextView>(R.id.tvItemTipAuthor).text = currentTip.author

            if (currentTip.date != 0L) {
                val zdt = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(currentTip.date),
                    ZoneId.systemDefault()
                )
                val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
                val formattedDate = dtf.format(zdt)
                findViewById<TextView>(R.id.tvItemTipDate).text = formattedDate
            }
            else {
                findViewById<TextView>(R.id.tvItemTipDate).visibility = View.GONE
            }

            val imageUri = Uri.parse(currentTip.articleImage)
            Picasso.get()
                .load(imageUri)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
                .into(findViewById(R.id.ivItemTip), object: Callback {
                    override fun onSuccess() {
                        findViewById<CircularProgressIndicator>(R.id.pbItemTip).visibility = View.GONE
                        findViewById<LinearLayout>(R.id.llItemTip).apply {
                            visibility = View.VISIBLE
                            isClickable = true
                        }
                    }

                    override fun onError(e: Exception?) {
                        findViewById<CircularProgressIndicator>(R.id.pbItemTip).visibility = View.GONE
                        findViewById<LinearLayout>(R.id.llItemTip).apply {
                            visibility = View.VISIBLE
                            isClickable = true
                        }
                    }
                })

            findViewById<LinearLayout>(R.id.llItemTip).setOnClickListener {
                listener.viewItem(currentTip)
            }
        }
    }

    override fun getItemCount(): Int {
        return tips.size
    }
}