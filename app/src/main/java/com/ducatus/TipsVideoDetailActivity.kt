package com.ducatus

import android.os.Bundle
import com.ducatus.data.Tip
import com.ducatus.databinding.ActivityTipsVideoDetailBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.gson.Gson

class TipsVideoDetailActivity : YouTubeBaseActivity() {
    private lateinit var binding: ActivityTipsVideoDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTipsVideoDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadData()

        binding.btnTipsVideoDetail.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadData() {
        val strTip = intent.getStringExtra("tip")
        val tip = Gson().fromJson(strTip, Tip::class.java)
        val videoId = tip.link.substring(tip.link.lastIndexOf("=") + 1)
        initializeYoutubePlayer(videoId)
    }

    private fun initializeYoutubePlayer(videoId: String) {
        binding.ypvTipsVideoDetail.initialize(
            BuildConfig.YOUTUBE_API_KEY,
            object: YouTubePlayer.OnInitializedListener {
                override fun onInitializationSuccess(
                    provider: YouTubePlayer.Provider?,
                    player: YouTubePlayer?,
                    p2: Boolean
                ) {
                    player?.loadVideo(videoId)
                    player?.play()
                }

                override fun onInitializationFailure(
                    p0: YouTubePlayer.Provider?,
                    p1: YouTubeInitializationResult?
                ) {
                    Snackbar
                        .make(binding.clTipsVideoDetail, getString(R.string.load_video_error), 5000)
                        .show()
                }
            }
        )
    }
}