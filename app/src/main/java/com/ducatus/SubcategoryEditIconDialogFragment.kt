package com.ducatus

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentSubcategoryEditIconDialogBinding
import com.ducatus.viewmodel.ColorViewModel
import com.ducatus.viewmodel.IconViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SubcategoryEditIconDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentSubcategoryEditIconDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private var updated: Boolean = false
    private val args: SubcategoryEditIconDialogFragmentArgs by navArgs()
    private val colorViewModel: ColorViewModel by activityViewModels()
    private val iconViewModel: IconViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        binding = FragmentSubcategoryEditIconDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // set current color and icon
        setColor(args.subcategoryColor)
        setIcon(args.subcategoryIcon)

        colorViewModel.color.observe(viewLifecycleOwner) { selectedColor ->
            setColor(selectedColor)
        }

        iconViewModel.icon.observe(viewLifecycleOwner) { selectedIcon ->
            setIcon(selectedIcon)
        }

        binding.tfEditSubcategoryColor.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = ColorDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        binding.tfEditSubcategoryIcon.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = IconDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        binding.btnEditSubcategoryIconCancel.setOnClickListener {
            dismiss()
        }

        binding.btnEditSubcategoryIconSave.setOnClickListener {
            // validate data -> save color and icon
            validateData()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (updated) {
            val fragment = parentFragmentManager.findFragmentById(R.id.fcCategories)
            if (fragment is DialogInterface.OnDismissListener) {
                (fragment as DialogInterface.OnDismissListener?)?.onDismiss(dialog)
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

        binding.viewEditSubcategorySelectedColor.background = gradientDrawable
        binding.tfEditSubcategoryColor.tag = selectedColor
        binding.tfEditSubcategoryColor.error = null
    }

    private fun setIcon(selectedIcon: String) {
        val icon = resources.getIdentifier(
            selectedIcon,
            "drawable",
            activity.packageName
        )

        binding.ivEditSubcategorySelectedIcon.setImageResource(icon)
        binding.ivEditSubcategorySelectedIcon.setColorFilter(
            ResourcesCompat.getColor(
                resources,
                R.color.darker_gray,
                null
            )
        )

        binding.tfEditSubcategoryIcon.tag = selectedIcon
        binding.tfEditSubcategoryIcon.error = null
    }

    private fun validateData() {
        val subcategoryColor = binding.tfEditSubcategoryColor.tag.toString()
        val subcategoryIcon = binding.tfEditSubcategoryIcon.tag.toString()

        if (subcategoryColor == args.subcategoryColor && subcategoryIcon == args.subcategoryIcon) {
            // dismiss if no changes were made
            dismiss()
        }
        else {
            auth = Firebase.auth
            val firebaseUser: FirebaseUser? = auth.currentUser
            if (firebaseUser != null) {
                val sharedPreferences = SharedPreferences(activity)
                val currentAccountId = sharedPreferences.accountId.toString()

                database = Firebase.database
                saveColor(firebaseUser.uid, currentAccountId, subcategoryColor, subcategoryIcon)
            }
            else {
                sessionExpired()
            }
        }
    }

    private fun saveColor(uid: String, accountId: String, subcategoryColor: String, subcategoryIcon: String) {
        showProgressDialog()
        databaseReference = database.getReference("subcategories").child(uid).child(accountId).child(args.categoryId).child(args.subcategoryId)
        databaseReference.child("color").setValue(subcategoryColor)
            .addOnSuccessListener {
                saveIcon(uid, accountId, subcategoryIcon)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun saveIcon(uid: String, accountId: String, subcategoryIcon: String) {
        databaseReference = database.getReference("subcategories").child(uid).child(accountId).child(args.categoryId).child(args.subcategoryId)
        databaseReference.child("icon").setValue(subcategoryIcon)
            .addOnSuccessListener {
                hideProgressDialog()
                updated = true
                dismiss()
            }

            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
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
        binding.pbEditSubcategoryIcon.visibility = View.VISIBLE
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialog() {
        binding.pbEditSubcategoryIcon.visibility = View.INVISIBLE
        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}