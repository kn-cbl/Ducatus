package com.ducatus.pdfservice

import android.Manifest.permission.*
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.content.ContextCompat
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import androidx.core.app.ActivityCompat.requestPermissions

class AppPermission {
    companion object {
        const val REQUEST_PERMISSION: Int = 123
        val PERMISSIONS = arrayOf(
            WRITE_EXTERNAL_STORAGE,
            READ_EXTERNAL_STORAGE
        )

        fun hasPermission(context: Context) =
            ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED

        fun requestPermission(activity: Activity) {
            requestPermissions(
                activity,
                arrayOf(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE),
                REQUEST_PERMISSION
            )
        }
    }
}