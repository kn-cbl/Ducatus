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
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ducatus.data.Subcategory
import com.ducatus.databinding.FragmentSubcategoryAddBinding
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

class SubcategoryAddFragment : Fragment() {
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentSubcategoryAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private val args: SubcategoryAddFragmentArgs by navArgs()
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

        binding = FragmentSubcategoryAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inputObserver()

        colorViewModel.color.observe(viewLifecycleOwner) { selectedColor ->
            val color = resources.getIdentifier(
                selectedColor,
                "color",
                activity.packageName
            )

            val gradientDrawable = GradientDrawable()
            gradientDrawable.setColor(activity.getColor(color))
            gradientDrawable.cornerRadius = 16f

            binding.viewAddSubcategorySelectedColor.background = gradientDrawable
            binding.tfAddSubcategoryColor.tag = selectedColor
            binding.tfAddSubcategoryColor.error = null
        }

        iconViewModel.icon.observe(viewLifecycleOwner) { selectedIcon ->
            val icon = resources.getIdentifier(
                selectedIcon,
                "drawable",
                activity.packageName
            )

            binding.ivAddSubcategorySelectedIcon.setImageResource(icon)
            binding.ivAddSubcategorySelectedIcon.setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    R.color.darker_gray,
                    null
                )
            )

            binding.tfAddSubcategoryIcon.tag = selectedIcon
            binding.tfAddSubcategoryIcon.error = null
        }

        binding.tfAddSubcategoryColor.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = ColorDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        binding.tfAddSubcategoryIcon.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = IconDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.done -> {
                    // validate data -> check if subcategory exists -> add subcategory
                    validateData()
                    true
                }
                else -> false
            }
        }
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
        val subcategoryColor = binding.tfAddSubcategoryColor.tag
        val subcategoryIcon = binding.tfAddSubcategoryIcon.tag
        var errors = 0

        if (TextUtils.isEmpty(subcategoryName)) {
            binding.tfAddSubcategoryName.error = getString(R.string.category_name_empty)
            errors++
        }
        if (subcategoryColor == null) {
            binding.tfAddSubcategoryColor.error = getString(R.string.select_a_color)
            errors++
        }
        if (subcategoryIcon == null) {
            binding.tfAddSubcategoryIcon.error = getString(R.string.select_an_icon)
            errors++
        }

        if (errors == 0) {
            auth = Firebase.auth
            val firebaseUser: FirebaseUser? = auth.currentUser
            if (firebaseUser != null) {
                val sharedPreferences = SharedPreferences(activity)
                val accountId = sharedPreferences.accountId.toString()

                val subcategoryData = mapOf(
                    "name" to subcategoryName,
                    "color" to subcategoryColor.toString(),
                    "icon" to subcategoryIcon.toString()
                )

                subcategoryExists(
                    firebaseUser.uid,
                    accountId,
                    subcategoryData
                )
            }
            else {
                sessionExpired()
            }
        }
    }

    private fun subcategoryExists(uid: String, accountId: String, subcategoryData: Map<String, String>) {
        showProgressDialog()
        database = Firebase.database
        databaseReference =
            database.getReference("subcategories")
                .child(uid)
                .child(accountId)
                .child(args.categoryId)

        val query = databaseReference.orderByChild("nameLower").equalTo(subcategoryData["name"])
        query.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    val key = databaseReference.push().key
                    val subcategory = Subcategory(
                        key,
                        subcategoryData["name"],
                        subcategoryData["name"]!!.lowercase(),
                        subcategoryData["color"],
                        subcategoryData["icon"]
                    )

                    addSubcategory(subcategory)
                }
                else {
                    hideProgressDialog()
                    binding.tfAddSubcategoryName.error = getString(R.string.subcategory_name_exists)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.add_subcategory_error), 5000)
                    .show()
            }
    }

    private fun addSubcategory(subcategory: Subcategory) {
        showProgressDialog()
        databaseReference.child(subcategory.id!!).setValue(subcategory)
            .addOnSuccessListener {
                hideProgressDialog()
                val action = SubcategoryAddFragmentDirections.actionSubcategoryAddFragmentToCategoryEditFragment(args.categoryId)
                findNavController().navigate(action)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.add_subcategory_error), 5000)
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