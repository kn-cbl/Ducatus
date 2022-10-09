package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentCategoryAddBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class CategoryAddFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentCategoryAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private var colorNames: List<String> = listOf()
    private var iconNames: List<String> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        binding = FragmentCategoryAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        inputObserver()

        binding.spAddCategoryColor.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedView = binding.spAddCategoryColor.selectedView
                val itemView = selectedView.findViewById<View>(R.id.viewHelperItem)
                val gradientDrawable = GradientDrawable()

                val iconColor = resources.getIdentifier(
                    colorNames[position],
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

        binding.spAddCategoryIcon.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedView = binding.spAddCategoryIcon.selectedView
                val itemView = selectedView.findViewById<View>(R.id.viewHelperItemIcon)

                val icon = resources.getIdentifier(
                    iconNames[position],
                    "drawable",
                    activity.packageName
                )

                itemView.setBackgroundResource(icon)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }

        binding.btnAddCategory.setOnClickListener {
            validateInput()
        }
    }

    private fun loadData() {
        loadNatures()
        loadColors()
        loadIcons()
    }

    private fun loadNatures() {
        val natures = listOf("Essentials", "Wants", "Savings")
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, natures)
        (binding.tfAddCategoryNature.editText as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun loadColors() {
        colorNames = listOf(
            "color_one", "color_two", "color_three", "color_four", "color_five",
            "color_six", "color_seven", "color_eight", "color_nine", "color_ten",
            "color_eleven", "color_twelve", "color_thirteen", "color_fourteen", "color_fifteen",
            "color_sixteen", "color_seventeen", "color_eighteen", "color_nineteen", "color_twenty",
            "color_twenty_one", "color_twenty_two", "color_twenty_three", "color_twenty_four", "color_twenty_five",
        )

        val adapter = object: ArrayAdapter<String>(requireContext(), R.layout.spinner_item, R.id.txt_bundle, colorNames) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = getView(position, convertView, parent)
                val itemView = view.findViewById<View>(R.id.viewHelperItem)
                val gradientDrawable: GradientDrawable = itemView.background as GradientDrawable

                val iconColor = resources.getIdentifier(
                    colorNames[position],
                    "color",
                    activity.packageName
                )

                gradientDrawable.setColor(activity.getColor(iconColor))
                itemView.background = gradientDrawable
                return view
            }
        }

        binding.spAddCategoryColor.adapter = adapter
    }

    private fun loadIcons() {
        iconNames = listOf(
            "ic_baseline_devices_24", "ic_baseline_wallet_24", "ic_baseline_fastfood_24",
            "ic_baseline_home_24", "investment", "ic_baseline_videogame_asset_24",
            "ic_outline_shopping_bag_24", "ic_baseline_directions_bus_24", "ic_baseline_directions_car_24",
            "ic_baseline_more_horiz_24"
        )

        val adapter = object: ArrayAdapter<String>(requireContext(), R.layout.spinner_item_icon, R.id.tvItemIcon, iconNames) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = getView(position, convertView, parent)
                val itemView = view.findViewById<View>(R.id.viewHelperItemIcon)

                val icon = resources.getIdentifier(
                    iconNames[position],
                    "drawable",
                    activity.packageName
                )

                itemView.background = ContextCompat.getDrawable(activity, icon)
                return view
            }
        }

        binding.spAddCategoryIcon.adapter = adapter
    }

    private fun inputObserver() {
        binding.tfAddCategoryName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfAddCategoryName.error = getString(R.string.category_name_empty)
            else binding.tfAddCategoryName.error = null
        }
        binding.tfAddCategoryNature.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfAddCategoryNature.error = getString(R.string.category_nature_empty)
            else binding.tfAddCategoryNature.error = null
        }
    }

    private fun validateInput() {
        // hide keyboard
        try {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val categoryName = binding.tfAddCategoryName.editText?.text.toString().trim {it <= ' '}
            val categoryNature = binding.tfAddCategoryNature.editText?.text.toString().trim {it <= ' '}
            val categoryColor = binding.spAddCategoryColor.selectedItem.toString()
            val categoryIcon = binding.spAddCategoryIcon.selectedItem.toString()

            if (TextUtils.isEmpty(categoryName) || TextUtils.isEmpty(categoryNature)) {
                if (TextUtils.isEmpty(categoryName)) binding.tfAddCategoryName.error = getString(R.string.category_name_empty)
                if (TextUtils.isEmpty(categoryNature)) binding.tfAddCategoryNature.error = getString(R.string.category_nature_empty)
            }
            else {
                // parse selected nature
                val nature = when (categoryNature) {
                    "Essentials" -> 0
                    "Wants" -> 1
                    "Savings" -> 2
                    else -> 3
                }
                val sharedPreferences = SharedPreferences(activity)
                val accountId = sharedPreferences.accountId.toString()

                addCategory(firebaseUser.uid, accountId, categoryName, nature, categoryColor, categoryIcon)
            }
        }
        else {
            sessionExpired()
        }
    }

    private fun addCategory(uid: String, accountId: String, categoryName: String, categoryNature: Int, categoryColor: String, categoryIcon: String) {
        showProgressDialog()
        database = Firebase.database
        databaseReference = database.getReference("categories").child(uid).child(accountId)
        databaseReference.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var lastId = 0
                for (child in snapshot.children) {
                    lastId = child.child("category_id").value.toString().toInt() + 1
                }

                val category = Category(lastId, categoryName, categoryNature, categoryColor, categoryIcon)
                databaseReference.child(lastId.toString()).setValue(category)
                    .addOnSuccessListener {
                        hideProgressDialog()
                        Snackbar
                            .make(rootLayout, "Successfully added category", Snackbar.LENGTH_LONG)
                            .show()

                        // add 3 second delay
                        object : CountDownTimer(3000, 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                // do nothing
                            }
                            override fun onFinish() {
                                try {
                                    val action = CategoryAddFragmentDirections.actionCategoryAddFragmentToCategoriesFragment()
                                    findNavController().navigate(action)
                                }
                                catch (e: Exception) {}
                            }
                        }.start()
                    }
                    .addOnFailureListener {
                        hideProgressDialog()
                        Snackbar
                            .make(rootLayout, "Unable to add category, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.retry)) { addCategory(uid, accountId, categoryName, categoryNature, categoryColor, categoryIcon) }
                            .show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to add category, ${error.message}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { addCategory(uid, accountId, categoryName, categoryNature, categoryColor, categoryIcon) }
                    .show()
            }
        })
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
        binding.pbAddCategory.visibility = View.VISIBLE
        binding.btnAddCategory.text = null
        binding.btnAddCategory.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.gray)
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbAddCategory.visibility = View.INVISIBLE
        binding.btnAddCategory.text = getString(R.string.add_category)
        binding.btnAddCategory.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.green_primary)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}