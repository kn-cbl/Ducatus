package com.ducatus

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ducatus.databinding.ActivityPlannedPaymentDetailBinding

class PlannedPaymentDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlannedPaymentDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlannedPaymentDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.tbPlannedPaymentDetail.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}