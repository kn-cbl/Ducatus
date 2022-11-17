package com.ducatus.adapter

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.Tips
import com.ducatus.interfaces.TipsListener
import com.squareup.picasso.Callback
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.text.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TipsAdapter(
    private val mContext: Context,
    private val list: List<Tips>,
    private val listener: TipsListener
) :
    RecyclerView.Adapter<TipsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imgPoster: ImageView = itemView.findViewById(R.id.image_article1)
        var txtTitle: TextView = itemView.findViewById(R.id.textView_article1)
        var imgAuthor: ImageView = itemView.findViewById(R.id.image_author1)
        var txtAuthorName: TextView = itemView.findViewById(R.id.textView_authorName1)
        var txtPublishDate: TextView = itemView.findViewById(R.id.textView_publishDate1)
        var grid: GridLayout = itemView.findViewById(R.id.grid)
        var pb: ProgressBar = itemView.findViewById(R.id.pb)
        var rel: RelativeLayout = itemView.findViewById(R.id.rel)
        var authRel: RelativeLayout = itemView.findViewById(R.id.authRel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mView =
            LayoutInflater.from(mContext).inflate(R.layout.fragment_tips_articles, parent, false)

        return ViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tips = list.get(position)
        holder.txtTitle.text = tips.articleTitle
        holder.txtAuthorName.text = tips.articleAuthor
        if (!tips.articleDate.equals("")) {
            var detFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            var dets: LocalDate = LocalDate.parse(tips.articleDate, detFormatter)
            holder.txtPublishDate.text =
                dets.month.toString().substring(
                    0,
                    3
                ) + " " + dets.dayOfMonth.toString() + ", " + dets.year.toString()
        }
        if (!tips.imgPicture.equals("")) {
            val uri = Uri.parse(tips.imgPicture)
            Picasso.get().invalidate(uri)
            Picasso.get().load(uri)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
                .into(holder.imgPoster, object : Callback {
                    override fun onSuccess() {
                        holder.rel.visibility = View.VISIBLE
                        holder.pb.visibility = View.GONE
                    }

                    override fun onError(e: Exception) {
                        holder.rel.visibility = View.VISIBLE
                        holder.pb.visibility = View.GONE
                        Log.e("ERROR_LOADING_PHOTO", e.message.toString())
                    }
                })
        } else {
            holder.rel.visibility = View.VISIBLE
            holder.pb.visibility = View.GONE
        }

        holder.itemView.setOnClickListener(View.OnClickListener {
            listener.OnItemClickListener(position)
        })
    }

    override fun getItemCount(): Int {
        return list.size
    }
}