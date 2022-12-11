package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ducatus.data.Category
import com.ducatus.databinding.FragmentCategoryAddBinding
import com.ducatus.viewmodel.ColorViewModel
import com.ducatus.viewmodel.IconViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class CategoryAddFragment : Fragment() {
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentCategoryAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private val colorViewModel: ColorViewModel by activityViewModels()
    private val iconViewModel: IconViewModel by activityViewModels()

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
        loadNatures()
        inputObserver()

        colorViewModel.color.observe(viewLifecycleOwner) { selectedColor ->
            setColor(selectedColor)
        }

        iconViewModel.icon.observe(viewLifecycleOwner) { selectedIcon ->
            setIcon(selectedIcon)
        }

        binding.tfAddCategoryColor.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = ColorDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        binding.tfAddCategoryIcon.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = IconDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.done -> {
                    // validate data -> check if category exists -> add category
                    validateData()
                    true
                }
                else -> false
            }
        }
    }

    private fun setColor(selectedColor: String) {
        val color = resources.getIdentifier(
            selectedColor,
            "color",
            activity.packageName
        )

        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(activity.getColor(color))
        gradientDrawable.cornerRadius = 16f

        binding.viewAddCategorySelectedColor.background = gradientDrawable
        binding.tfAddCategoryColor.tag = selectedColor
        binding.tfAddCategoryColor.error = null
    }

    private fun setIcon(selectedIcon: String) {
        val icon = resources.getIdentifier(
            selectedIcon,
            "drawable",
            activity.packageName
        )

        binding.ivAddCategorySelectedIcon.setImageResource(icon)
        binding.ivAddCategorySelectedIcon.setColorFilter(
            ResourcesCompat.getColor(
                resources,
                R.color.darker_gray,
                null
            )
        )

        binding.tfAddCategoryIcon.tag = selectedIcon
        binding.tfAddCategoryIcon.error = null
    }

    private fun loadNatures() {
        val natures = listOf("Essentials", "Wants", "Savings")
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, natures)
        (binding.tfAddCategoryNature.editText as? AutoCompleteTextView)?.setAdapter(adapter)
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

        val name = binding.tfAddCategoryName.editText?.text.toString().trim {it <= ' '}
        val nature = binding.tfAddCategoryNature.editText?.text.toString().trim {it <= ' '}
        val color = binding.tfAddCategoryColor.tag
        val icon = binding.tfAddCategoryIcon.tag
        var errors = 0

        if (TextUtils.isEmpty(name)) {
            binding.tfAddCategoryName.error = getString(R.string.category_name_empty)
            errors++
        }
        if (TextUtils.isEmpty(nature)) {
            binding.tfAddCategoryNature.error = getString(R.string.category_nature_empty)
            errors++
        }
        if (color == null) {
            binding.tfAddCategoryColor.error = getString(R.string.select_a_color)
            errors++
        }
        if (icon == null) {
            binding.tfAddCategoryIcon.error = getString(R.string.select_an_icon)
            errors++
        }

        if (errors == 0) {
            showProgressDialog()
            auth = Firebase.auth
            val firebaseUser: FirebaseUser? = auth.currentUser
            if (firebaseUser != null) {
                // parse selected nature
                val parsedNature = when (nature) {
                    "Essentials" -> 0
                    "Wants" -> 1
                    "Savings" -> 2
                    else -> 3
                }
                val sharedPreferences = SharedPreferences(activity)
                val accountId = sharedPreferences.accountId.toString()

                val category = Category(
                    null,
                    name,
                    name.lowercase(),
                    parsedNature,
                    color.toString(),
                    icon.toString(),
                    false
                )

                categoryExists(firebaseUser.uid, accountId, category)
            }
            else {
                hideProgressDialog()
                sessionExpired()
            }
        }
    }

    private fun categoryExists(uid: String, accountId: String, category: Category) {
        database = Firebase.database
        databaseReference = database.getReference("categories").child(uid).child(accountId)
        val query = databaseReference.orderByChild("nameLower").equalTo(category.nameLower)
        query.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    val key = databaseReference.push().key
                    category.id = key
                    addCategory(category)
                }
                else {
                    hideProgressDialog()
                    binding.tfAddCategoryName.error = getString(R.string.category_name_exists)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.add_category_error),5000)
                    .show()
            }
    }

    private fun addCategory(category: Category) {
        databaseReference.child(category.id!!).setValue(category)
            .addOnSuccessListener {
                hideProgressDialog()
                val action = CategoryAddFragmentDirections.actionCategoryAddFragmentToCategoriesFragment()
                findNavController().navigate(action)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.add_category_error),5000)
                    .show()
            }
    }

    private fun sessionExpired() {
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.session_expired))
            .setPositiveButton(resources.getString(R.string.log_in)) { _, _ -> }

        dialog.setOnDismissListener {
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity.finish()
        }

        dialog.show()
    }

    private fun showProgressDialog() {
        val bundle = Bundle()
        bundle.putString("title", getString(R.string.adding))

        actionDialog = ActionDialogFragment()
        actionDialog.arguments = bundle
        actionDialog.show(childFragmentManager, "dialog")
    }

    private fun hideProgressDialog() {
        actionDialog.dismiss()
    }
}