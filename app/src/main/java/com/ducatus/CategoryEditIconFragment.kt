package com.ducatus

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentCategoryEditIconBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class CategoryEditIconFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentCategoryEditIconBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private val args: CategoryEditIconFragmentArgs by navArgs()
    private var colors: List<String> = listOf()
    private var icons: List<String> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        binding = FragmentCategoryEditIconBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        binding.spEditCategoryColor.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedView = binding.spEditCategoryColor.selectedView
                val itemView = selectedView.findViewById<View>(R.id.viewHelperItem)
                val gradientDrawable = GradientDrawable()

                val iconColor = resources.getIdentifier(
                    colors[position],
                    "color",
                    activity.packageName
                )

                gradientDrawable.setColor(activity.getColor(iconColor))
                itemView.background = gradientDrawable
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }

        binding.spEditCategoryIcon.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedView = binding.spEditCategoryIcon.selectedView
                val itemView = selectedView.findViewById<View>(R.id.viewHelperItemIcon)

                val icon = resources.getIdentifier(
                    icons[position],
                    "drawable",
                    activity.packageName
                )

                itemView.setBackgroundResource(icon)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }

        binding.btnEditCategoryIconCancel.setOnClickListener {
            dismiss()
        }

        binding.btnEditCategoryIconSave.setOnClickListener {
            validateData()
        }
    }

    private fun loadData() {
        loadColors()
        loadIcons()
    }

    private fun loadColors() {
        colors = AppResources().getColors()
        val adapter = object: ArrayAdapter<String>(requireContext(), R.layout.spinner_item, R.id.txt_bundle, colors) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = getView(position, convertView, parent)
                val itemView = view.findViewById<View>(R.id.viewHelperItem)
                val gradientDrawable: GradientDrawable = itemView.background as GradientDrawable

                val iconColor = resources.getIdentifier(
                    colors[position],
                    "color",
                    activity.packageName
                )

                gradientDrawable.setColor(activity.getColor(iconColor))
                itemView.background = gradientDrawable
                return view
            }
        }

        binding.spEditCategoryColor.adapter = adapter
        binding.spEditCategoryColor.setSelection(colors.indexOf(args.categoryColor))
    }

    private fun loadIcons() {
        icons = AppResources().getIcons()
        val adapter = object: ArrayAdapter<String>(requireContext(), R.layout.spinner_item_icon, R.id.tvItemIcon, icons) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = getView(position, convertView, parent)
                val itemView = view.findViewById<View>(R.id.viewHelperItemIcon)

                val icon = resources.getIdentifier(
                    icons[position],
                    "drawable",
                    activity.packageName
                )

                itemView.background = ContextCompat.getDrawable(activity, icon)
                return view
            }
        }

        binding.spEditCategoryIcon.adapter = adapter
        binding.spEditCategoryIcon.setSelection(icons.indexOf(args.categoryIcon))
    }

    private fun validateData() {
        val categoryColor = binding.spEditCategoryColor.selectedItem.toString()
        val categoryIcon = binding.spEditCategoryIcon.selectedItem.toString()

        if (categoryColor == args.categoryColor && categoryIcon == args.categoryIcon) {
            // dismiss if no changes were made
            dismiss()
        }
        else {
            saveColor(categoryColor, categoryIcon)
        }
    }

    private fun saveColor(categoryColor: String, categoryIcon: String) {
        showProgressDialog()
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val sharedPreferences = SharedPreferences(activity)
            val currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            databaseReference = database.getReference("categories").child(firebaseUser.uid).child(currentAccountId).child(args.categoryId)
            databaseReference.child("category_color").setValue(categoryColor)
                .addOnSuccessListener {
                    saveIcon(firebaseUser.uid, currentAccountId, categoryIcon)
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "Unable to save changes, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry)) { saveColor(categoryColor, categoryIcon) }
                        .show()
                }
        }
        else {
            hideProgressDialog()
            sessionExpired()
        }
    }

    private fun saveIcon(uid: String, currentAccountId: String, categoryIcon: String) {
        showProgressDialog()
        databaseReference = database.getReference("categories").child(uid).child(currentAccountId).child(args.categoryId)
        databaseReference.child("category_icon").setValue(categoryIcon)
            .addOnSuccessListener {
                hideProgressDialog()
                dismiss()
            }

            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to save changes, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { saveIcon(uid, currentAccountId, categoryIcon) }
                    .show()
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
                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity.finish()
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.pbEditCategoryIcon.visibility = View.VISIBLE
    }

    private fun hideProgressDialog() {
        binding.pbEditCategoryIcon.visibility = View.INVISIBLE
    }
}