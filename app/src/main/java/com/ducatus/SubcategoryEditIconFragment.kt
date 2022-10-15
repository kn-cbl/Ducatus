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
import com.ducatus.databinding.FragmentSubcategoryEditIconBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SubcategoryEditIconFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentSubcategoryEditIconBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private val args: SubcategoryEditIconFragmentArgs by navArgs()
    private var colors: List<String> = listOf()
    private var icons: List<String> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        binding = FragmentSubcategoryEditIconBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
    }

    private fun loadData() {
        loadColors()
        loadIcons()

        binding.spEditSubcategoryColor.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedView = binding.spEditSubcategoryColor.selectedView
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

        binding.spEditSubcategoryIcon.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedView = binding.spEditSubcategoryIcon.selectedView
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

        binding.btnEditSubcategoryIconCancel.setOnClickListener {
            dismiss()
        }

        binding.btnEditSubcategoryIconSave.setOnClickListener {
            validateData()
        }
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

        binding.spEditSubcategoryColor.adapter = adapter
        binding.spEditSubcategoryColor.setSelection(colors.indexOf(args.subcategoryColor))
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

        binding.spEditSubcategoryIcon.adapter = adapter
        binding.spEditSubcategoryIcon.setSelection(icons.indexOf(args.subcategoryIcon))
    }

    private fun validateData() {
        val subcategoryColor = binding.spEditSubcategoryColor.selectedItem.toString()
        val subcategoryIcon = binding.spEditSubcategoryIcon.selectedItem.toString()

        if (subcategoryColor == args.subcategoryColor && subcategoryIcon == args.subcategoryIcon) {
            // dismiss if no changes were made
            dismiss()
        }
        else {
            saveColor(subcategoryColor, subcategoryIcon)
        }
    }

    private fun saveColor(subcategoryColor: String, subcategoryIcon: String) {
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val sharedPreferences = SharedPreferences(activity)
            val currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            databaseReference = database.getReference("subcategories").child(firebaseUser.uid).child(currentAccountId).child(args.categoryId).child(args.subcategoryId)
            databaseReference.child("subcategory_color").setValue(subcategoryColor)
                .addOnSuccessListener {
                    saveIcon(firebaseUser.uid, currentAccountId, subcategoryIcon)
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "Unable to save changes, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry)) { saveColor(subcategoryColor, subcategoryIcon) }
                        .show()
                }
        }
        else {
            hideProgressDialog()
            sessionExpired()
        }
    }

    private fun saveIcon(uid: String, currentAccountId: String, subcategoryIcon: String) {
        databaseReference = database.getReference("subcategories").child(uid).child(currentAccountId).child(args.categoryId).child(args.subcategoryId)
        databaseReference.child("subcategory_icon").setValue(subcategoryIcon)
            .addOnSuccessListener {
                hideProgressDialog()
                dismiss()
            }

            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to save changes, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { saveIcon(uid, currentAccountId, subcategoryIcon) }
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
        binding.pbEditSubcategoryIcon.visibility = View.VISIBLE
    }

    private fun hideProgressDialog() {
        binding.pbEditSubcategoryIcon.visibility = View.INVISIBLE
    }
}