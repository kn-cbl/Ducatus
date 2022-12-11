package com.ducatus

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.ducatus.common.AppResources
import com.ducatus.data.Tip
import com.ducatus.databinding.ActivityTipsArticleDetailBinding
import com.google.gson.Gson
import com.squareup.picasso.Callback
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TipsArticleDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTipsArticleDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTipsArticleDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadData()

        binding.tbTipsArticleDetail.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun loadData() {
        showProgressDialog()
        val strTip = intent.getStringExtra("tip")
        val tip = Gson().fromJson(strTip, Tip::class.java)

        binding.tvTipsArticleDetailTitle.text = tip.title
        binding.tvTipsArticleDetailAuthor.text = tip.author
        binding.tvTipsArticleDetailIcon.text = tip.author[0].uppercase()

        val content = AppResources().getTipsContent()[tip.title]
        binding.tvTipsArticleDetailContent.text = content

        if (tip.date != 0L) {
            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(tip.date),
                ZoneId.systemDefault()
            )
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val dateText = dtf.format(zdt)
            binding.tvTipsArticleDetailDate.text = dateText
        }
        else {
            binding.tvTipsArticleDetailDate.visibility = View.GONE
        }

        val imageUri = Uri.parse(tip.articleImage)
        Picasso.get()
            .load(imageUri)
            .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
            .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
            .into(binding.ivTipsArticleDetail, object: Callback {
                override fun onSuccess() {
                    hideProgressDialog()
                }

                override fun onError(e: Exception?) {
                    // could not load image
                }
            })
    }

    private fun showProgressDialog() {
        binding.pbTipsArticleDetail.visibility = View.VISIBLE
    }

    private fun hideProgressDialog() {
        binding.pbTipsArticleDetail.visibility = View.GONE
    }
}