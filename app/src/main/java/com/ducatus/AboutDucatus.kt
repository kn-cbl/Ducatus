package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ducatus.databinding.ActivityAboutDucatusBinding

class AboutDucatus : AppCompatActivity() {
    private lateinit var binding: ActivityAboutDucatusBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_ducatus)

        binding = ActivityAboutDucatusBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.tbAboutApp.setNavigationOnClickListener {
            startActivity(Intent(this, Settings::class.java))
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}