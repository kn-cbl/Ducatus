package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ducatus.databinding.ActivityPrivacyBinding

class Privacy : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        binding = ActivityPrivacyBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.tbPrivacy.setNavigationOnClickListener {
            startActivity(Intent(this, Settings::class.java))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}