package com.ducatus

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ducatus.databinding.FragmentBudgetsBinding
import com.google.android.material.snackbar.Snackbar

class BudgetsFragment : Fragment() {
    private lateinit var binding: FragmentBudgetsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBudgetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ibAddBudget.setOnClickListener {
            validateData()
        }
    }

    private fun validateData() {

    }

    private fun addBudget() {

    }

//    private fun sessionExpired() {
//        Snackbar
//            .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
//            .show()
//
//        // add 3 second delay
//        object : CountDownTimer(3000, 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                // do nothing
//            }
//            override fun onFinish() {
//                val intent = Intent(activity, LoginActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                startActivity(intent)
//                activity.finish()
//            }
//        }.start()
//    }
//
//    private fun showProgressDialog() {
//        binding.pbCategories.visibility = View.VISIBLE
//        binding.rvCategories.visibility = View.GONE
//    }
//
//    private fun hideProgressDialog() {
//        binding.pbCategories.visibility = View.INVISIBLE
//        binding.rvCategories.visibility = View.VISIBLE
//    }
}