package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentSubcategoryAddBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SubcategoryAddFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentSubcategoryAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private var colors: List<String> = listOf()
    private var icons: List<String> = listOf()
    private val args: SubcategoryAddFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        binding = FragmentSubcategoryAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        inputObserver()

        binding.spAddSubcategoryColor.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedView = binding.spAddSubcategoryColor.selectedView
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

        binding.spAddSubcategoryIcon.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedView = binding.spAddSubcategoryIcon.selectedView
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

        binding.btnAddSubcategory.setOnClickListener {
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

        binding.spAddSubcategoryColor.adapter = adapter
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

        binding.spAddSubcategoryIcon.adapter = adapter
    }

    private fun inputObserver() {
        binding.tfAddSubcategoryName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfAddSubcategoryName.error = getString(R.string.subcategory_name_empty)
            else binding.tfAddSubcategoryName.error = null
        }
    }

    private fun validateData() {
        // hide keyboard
        try {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        val subcategoryName = binding.tfAddSubcategoryName.editText?.text.toString().trim {it <= ' '}
        val subcategoryColor = binding.spAddSubcategoryColor.selectedItem.toString()
        val subcategoryIcon = binding.spAddSubcategoryIcon.selectedItem.toString()

        if (TextUtils.isEmpty(subcategoryName)) {
            binding.tfAddSubcategoryName.error = getString(R.string.category_name_empty)
        }
        else {
            auth = Firebase.auth
            val firebaseUser: FirebaseUser? = auth.currentUser
            if (firebaseUser != null) {
                val sharedPreferences = SharedPreferences(activity)
                val accountId = sharedPreferences.accountId.toString()
                subcategoryExists(firebaseUser.uid, accountId, subcategoryName, subcategoryColor, subcategoryIcon)
            }
            else {
                sessionExpired()
            }
        }
    }

    private fun subcategoryExists(uid: String, accountId: String, subcategoryName: String, subcategoryColor: String, subcategoryIcon: String) {
        showProgressDialog()
        database = Firebase.database
        databaseReference = database.getReference("subcategories").child(uid).child(accountId).child(args.categoryId)
        databaseReference.get()
            .addOnSuccessListener {
                var nameKey = false
                for (child in it.children) {
                    if (subcategoryName == child.child("subcategory_name").value.toString()) {
                        nameKey = true
                        break
                    }
                }

                if (!nameKey) {
                    val lastId = it.childrenCount.toInt()
                    addSubcategory(lastId, subcategoryName, subcategoryColor, subcategoryIcon)
                }
                else {
                    hideProgressDialog()
                    binding.tfAddSubcategoryName.error = getString(R.string.subcategory_name_exists)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to add subcategory, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { subcategoryExists(uid, accountId, subcategoryName, subcategoryColor, subcategoryIcon) }
                    .show()
            }
    }

    private fun addSubcategory(id: Int, subcategoryName: String, subcategoryColor: String, subcategoryIcon: String) {
        showProgressDialog()
        val subcategory = Subcategory(id, subcategoryName, subcategoryColor, subcategoryIcon)
        databaseReference.child(id.toString()).setValue(subcategory)
            .addOnSuccessListener {
                hideProgressDialog()
                dismiss()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to add subcategory, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { addSubcategory(id, subcategoryName, subcategoryColor, subcategoryIcon) }
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
        binding.pbAddSubcategory.visibility = View.VISIBLE
        binding.btnAddSubcategory.text = null
        binding.btnAddSubcategory.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.gray)
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbAddSubcategory.visibility = View.INVISIBLE
        binding.btnAddSubcategory.text = getString(R.string.add_category)
        binding.btnAddSubcategory.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.green_primary)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}