package com.ducatus

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import com.ducatus.common.TipContents
import com.ducatus.data.Tips
import com.squareup.picasso.Callback
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.tips_detail_article.*
import java.lang.Exception
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TipsDetailArticle : AppCompatActivity() {
    lateinit var tips: Tips
    lateinit var txtTitle: TextView
    lateinit var txtAuthorName: TextView
    lateinit var txtDate: TextView
    lateinit var txtArticleContent1: TextView
    lateinit var txtArticleContent2: TextView
    lateinit var imgPoster: ImageView
    lateinit var rel: RelativeLayout
    lateinit var pb: ProgressBar

    companion object {
        lateinit var t: Tips
        fun start(mContext: Context, act: Activity, tTips: Tips) {
            t = tTips
            val intent = Intent(mContext, TipsDetailArticle::class.java)
            mContext.startActivity(
                intent,
                ActivityOptions.makeSceneTransitionAnimation(act).toBundle()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tips_detail_article)
        initViews()
        initListeners()
    }

    private fun initListeners() {
        tipsDetailArticleToolbar.setOnClickListener(View.OnClickListener {
            onBackPressed()
        })
    }

    private fun initViews() {
        tips = TipsDetailArticle.t
        txtTitle = findViewById(R.id.textView_articleTitle)
        imgPoster = findViewById(R.id.img_articleImg)
        txtAuthorName = findViewById(R.id.textView_authorName)
        txtDate = findViewById(R.id.textView_publishDate)
        txtArticleContent1 = findViewById(R.id.textView_articleContent1)
        txtArticleContent2 = findViewById(R.id.textView_articleContent2)
        rel = findViewById(R.id.rel)
        pb = findViewById(R.id.pb)

        txtTitle.text = tips.articleTitle
        txtAuthorName.text = tips.articleAuthor

        val map = TipContents().getContentMap()
        if (map.containsKey(tips.articleTitle)) {
            val data = map.get(tips.articleTitle)
            var str = data!!.replace("\\","")
            str = str.replace(".nn",".\n\n")
            txtArticleContent1.text = str
            txtArticleContent2.visibility = View.GONE
        }

        if (!tips.articleDate.equals("")) {
            var detFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            var dets: LocalDate = LocalDate.parse(tips.articleDate, detFormatter)
            txtDate.text =
                dets.month.toString().substring(
                    0,
                    3
                ) + " " + dets.dayOfMonth.toString() + ", " + dets.year.toString()
        }

        val uri = Uri.parse(t.imgPicture)
        Picasso.get().invalidate(uri)
        Picasso.get().load(uri)
            .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
            .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
            .into(imgPoster, object : Callback {
                override fun onSuccess() {
                    rel.visibility = View.VISIBLE
                    pb.visibility = View.GONE
                }

                override fun onError(e: Exception) {
                    rel.visibility = View.VISIBLE
                    pb.visibility = View.GONE
                    Log.e("ERROR_DETAIL", e.message.toString())
                }
            })

    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}