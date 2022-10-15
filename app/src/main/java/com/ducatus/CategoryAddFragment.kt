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
import com.google.android.material.appbar.MaterialToolbar
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
    private lateinit var toolbar: MaterialToolbar
    private var colors: List<String> = listOf()
    private var icons: List<String> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        toolbar = activity.findViewById(R.id.tbCategories)
        toolbar.inflateMenu(R.menu.check_menu)

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

        binding.spAddCategoryIcon.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedView = binding.spAddCategoryIcon.selectedView
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

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.done -> {
                    validateData()
                    true
                }
                else -> false
            }
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

        binding.spAddCategoryColor.adapter = adapter
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

    private fun validateData() {
        // hide keyboard
        try {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        val categoryName = binding.tfAddCategoryName.editText?.text.toString().trim {it <= ' '}
        val categoryNature = binding.tfAddCategoryNature.editText?.text.toString().trim {it <= ' '}
        val categoryColor = binding.spAddCategoryColor.selectedItem.toString()
        val categoryIcon = binding.spAddCategoryIcon.selectedItem.toString()

        if (TextUtils.isEmpty(categoryName) || TextUtils.isEmpty(categoryNature)) {
            if (TextUtils.isEmpty(categoryName)) binding.tfAddCategoryName.error = getString(R.string.category_name_empty)
            if (TextUtils.isEmpty(categoryNature)) binding.tfAddCategoryNature.error = getString(R.string.category_nature_empty)
        }
        else {
            auth = Firebase.auth
            val firebaseUser: FirebaseUser? = auth.currentUser
            if (firebaseUser != null) {
                // parse selected nature
                val nature = when (categoryNature) {
                    "Essentials" -> 0
                    "Wants" -> 1
                    "Savings" -> 2
                    else -> 3
                }
                val sharedPreferences = SharedPreferences(activity)
                val accountId = sharedPreferences.accountId.toString()

                categoryExists(firebaseUser.uid, accountId, categoryName, nature, categoryColor, categoryIcon)
            }
            else {
                sessionExpired()
            }
        }
    }

    private fun categoryExists(uid: String, accountId: String, categoryName: String, categoryNature: Int, categoryColor: String, categoryIcon: String) {
        showProgressDialog()
        database = Firebase.database
        databaseReference = database.getReference("categories").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                var nameKey = false
                for (child in snapshot.children) {
                    if (categoryName == child.child("category_name").value.toString()) {
                        nameKey = true
                        break
                    }
                }

                if (!nameKey) {
                    val lastId = snapshot.childrenCount.toInt()
                    addCategory(lastId, uid, categoryName, categoryNature, categoryColor, categoryIcon)
                }
                else {
                    hideProgressDialog()
                    binding.tfAddCategoryName.error = getString(R.string.category_name_exists)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to add category, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { categoryExists(uid, accountId, categoryName, categoryNature, categoryColor, categoryIcon) }
                    .show()
            }
    }

    private fun addCategory(id: Int, uid: String, categoryName: String, categoryNature: Int, categoryColor: String, categoryIcon: String) {
        showProgressDialog()
        val category = Category(id, categoryName, categoryNature, categoryColor, categoryIcon)
        databaseReference.child(id.toString()).setValue(category)
            .addOnSuccessListener {
                hideProgressDialog()
                val action = CategoryAddFragmentDirections.actionCategoryAddFragmentToCategoriesFragment()
                findNavController().navigate(action)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to add category, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { addCategory(id, uid, categoryName, categoryNature, categoryColor, categoryIcon) }
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
        binding.pbAddCategory.visibility = View.VISIBLE
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbAddCategory.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}