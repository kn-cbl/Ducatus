package com.ducatus

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.ducatus.common.Common
import com.ducatus.common.Constants
import com.ducatus.data.LocalFormats
import com.ducatus.data.Tips
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.tips_detail_video.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TipsDetailVideo : AppCompatActivity() {

    lateinit var tips: Tips
    lateinit var txtTitle: TextView
    lateinit var txtAuthorName: TextView
    lateinit var txtDate: TextView
    lateinit var txtArticleContent1: TextView
    lateinit var pb: ProgressBar
    lateinit var playerView: SurfaceView
    lateinit var playerController: StyledPlayerControlView
    lateinit var exoplayer: ExoPlayer
    var videoURL: String = ""


    companion object {
        lateinit var t: Tips
        fun start(mContext: Context, act: Activity, tTips: Tips) {
            t = tTips
            val intent = Intent(mContext, TipsDetailVideo::class.java)
            mContext.startActivity(
                intent,
                ActivityOptions.makeSceneTransitionAnimation(act).toBundle()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tips_detail_video)
        initViews()
        initListeners()
    }

    private fun initListeners() {
        tipsDetailArticleToolbar.setOnClickListener(View.OnClickListener {
            onBackPressed()
        })
    }

    private fun initViews() {
        tips = TipsDetailVideo.t
        txtTitle = findViewById(R.id.textView_videoTitle)
        txtAuthorName = findViewById(R.id.textView_videoArtistName)
        txtDate = findViewById(R.id.textView_publishVideoDate)
        txtArticleContent1 = findViewById(R.id.textView_videoContent)
        playerView = findViewById(R.id.playerView)
        playerController = findViewById(R.id.playerController)
        pb = findViewById(R.id.pb)

        if (!tips.articleDate.equals("")) {
            var detFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            var dets: LocalDate = LocalDate.parse(tips.articleDate, detFormatter)
            txtDate.text =
                dets.month.toString().substring(
                    0,
                    3
                ) + " " + dets.dayOfMonth.toString() + ", " + dets.year.toString()
        }
        txtTitle.text = tips.articleTitle
        txtAuthorName.text = tips.articleAuthor
        txtArticleContent1.text = ""

        if (tips.link != "") {
            loadPureURL()
        }

    }

    private fun loadPureURL() {
        var sliceString = tips.link.split("v=")
        var watchPath = "/watch?v=" + sliceString[1]
        var watchID = sliceString[1]
        var map = Common().getYoutubeBody(watchPath, watchID)
        Log.e("MAP", map.toString())
        val finalBodyStr = Gson().toJson(map)
        var stringRequest = object : StringRequest(
            Request.Method.POST,
            Constants().YOUTUBE_URL,
            Response.Listener<String> { response ->
                try {
                    val myMap: Map<String, Object> =
                        Gson().fromJson<Map<String, Object>>(response, Map::class.java)
                    if (myMap.containsKey("videoDetails")) {
                        val myVideoDetailsMap: Map<String, Object> =
                            myMap["videoDetails"] as Map<String, Object>
                        txtArticleContent1.text =
                            myVideoDetailsMap.get("shortDescription") as String
                    }
                    if (myMap.containsKey("streamingData")) {
                        val myStreamMap: Map<String, Object> =
                            myMap["streamingData"] as Map<String, Object>

                        if (myStreamMap.containsKey("formats")) {
                            val jsonStr = Gson().toJson(myStreamMap["formats"])
                            val typeToken = object : TypeToken<List<LocalFormats>>() {}.type
                            val formats = Gson().fromJson<List<LocalFormats>>(
                                jsonStr,
                                typeToken
                            )
                            if (formats.size > 0) {
                                Log.e("FORMATS", formats.get(0).toString())
                                videoURL = formats.get(0).url
                                playVideo()
                            }
                        }

                    }

                } catch (e: Exception) {
                    Log.e("ERROR", e.message.toString())
                }

            },
            Response.ErrorListener { error ->
                Log.e("VOLLEYERR", error.toString())
            }) {

            @Override
            override fun getBody(): ByteArray {
                return finalBodyStr.toByteArray()
            }

            @Override
            override fun getBodyContentType(): String {
                return Constants().CONTENT_TYPE
            }
        }
        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(stringRequest)
    }

    private fun playVideo() {
        exoplayer = ExoPlayer.Builder(this).build()
        playerController.player = exoplayer
        val videoURI = Uri.parse(videoURL)
        playerController.keepScreenOn = true
        playerController.setShowNextButton(false)
        playerController.setShowPreviousButton(false)
        playerController.showTimeoutMs = 3000

        val dataSource = DefaultHttpDataSource.Factory()
        val mediaSource = ProgressiveMediaSource.Factory(dataSource)
            .createMediaSource(MediaItem.fromUri(videoURI))
        exoplayer.setMediaSource(mediaSource)
        exoplayer.setVideoSurfaceView(playerView)
        exoplayer.prepare()
        exoplayer.playWhenReady = true
        exoplayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.e("statechange", "statechange")
                super.onPlaybackStateChanged(playbackState)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("exoPlaybackException", error.message.toString())
                pb.visibility = View.VISIBLE
                super.onPlayerError(error)
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    ExoPlayer.STATE_READY -> {
                        if (!playerController.isVisible) {
                            playerController.show()
                        }
                        pb.visibility = View.GONE
                    }
                    ExoPlayer.STATE_ENDED -> {
                        if (!playerController.isVisible) {
                            playerController.show()
                        }
                    }
                }
            }
        })

        playerController.setOnClickListener(View.OnClickListener {
            if (!playerController.isVisible) {
                playerController.show()
            }
        })
        playerView.setOnClickListener(View.OnClickListener {
            if (!playerController.isVisible) {
                playerController.show()
            }
        })

    }

    fun releasePlayer() {
        if (exoplayer != null) {
            Log.e("release player", "release player")
            try {
                exoplayer.stop()
                exoplayer.release()
                exoplayer.clearMediaItems()
                exoplayer.clearVideoSurfaceView(playerView)
            } catch (e: Exception) {
                Log.e("ERROR_RELEASING_PLAYER", e.message.toString())
            }
        }
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    override fun onStop() {
        releasePlayer()
        super.onStop()
    }

    override fun onBackPressed() {
        playerView.visibility = View.INVISIBLE
        playerController.visibility = View.INVISIBLE
        releasePlayer()
        super.onBackPressed()
    }

}