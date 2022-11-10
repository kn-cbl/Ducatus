package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ducatus.data.Subcategory
import com.ducatus.databinding.FragmentSubcategoryEditBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class SubcategoryEditFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentSubcategoryEditBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var selectedSubcategoryListener: ValueEventListener
    private lateinit var currentSubcategoryColor: String
    private lateinit var currentSubcategoryIcon: String
    private lateinit var currentSubcategoryName: String
    private val args: SubcategoryEditFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        binding = FragmentSubcategoryEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        showProgressDialog()

        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val sharedPreferences = SharedPreferences(activity)
            val currentAccountId = sharedPreferences.accountId.toString()

            setSelectedSubcategoryListener()

            database = Firebase.database
            databaseReference = database.getReference("subcategories").child(firebaseUser.uid).child(currentAccountId).child(args.categoryId).child(args.subcategoryId)
            databaseReference.addValueEventListener(selectedSubcategoryListener)
        }
        else {
            sessionExpired()
        }
    }

    override fun onStop() {
        super.onStop()
        databaseReference.removeEventListener(selectedSubcategoryListener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ibEditSubcategoryIcon.setOnClickListener {
            val action = SubcategoryEditFragmentDirections.actionSubcategoryEditFragmentToSubcategoryEditIconDialogFragment(args.categoryId, args.subcategoryId, currentSubcategoryColor, currentSubcategoryIcon)
            findNavController().navigate(action)
        }

        binding.ibEditSubcategoryName.setOnClickListener {
            val action = SubcategoryEditFragmentDirections.actionSubcategoryEditFragmentToSubcategoryEditNameDialogFragment(args.categoryId, args.subcategoryId, currentSubcategoryName)
            findNavController().navigate(action)
        }
    }

    private fun setSelectedSubcategoryListener() {
        selectedSubcategoryListener = object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val subcategory = snapshot.getValue<Subcategory>()
                if (subcategory != null) {
                    currentSubcategoryColor = subcategory.color.toString()
                    currentSubcategoryIcon = subcategory.icon.toString()
                    currentSubcategoryName = subcategory.name.toString()

                    val iconColor = resources.getIdentifier(
                        subcategory.color.toString(),
                        "color",
                        activity.packageName
                    )

                    binding.flSubcategoryIcon.backgroundTintList = ContextCompat.getColorStateList(activity, iconColor)

                    val icon = resources.getIdentifier(
                        subcategory.icon.toString(),
                        "drawable",
                        activity.packageName
                    )

                    binding.ivSubcategoryIcon.setImageResource(icon)
                    binding.ivSubcategoryIcon.setColorFilter(
                        ResourcesCompat.getColor(
                            resources,
                            R.color.white,
                            null
                        )
                    )

                    binding.tvSelectedSubcategoryName.text = subcategory.name
                    hideProgressDialog()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar
                    .make(rootLayout, error.message, 5000)
                    .show()
            }
        }
    }

    private fun sessionExpired() {
        Snackbar
            .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        // add 3 second delay
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // do nothing
            }
            override fun onFinish() {
                try {
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity.finish()
                }
                catch (e: Exception) {}
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.pbSubcategoryEdit.visibility = View.VISIBLE
        binding.llSubcategoryEdit.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbSubcategoryEdit.visibility = View.INVISIBLE
        binding.llSubcategoryEdit.visibility = View.VISIBLE
    }
}