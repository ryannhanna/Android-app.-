package com.smartfilemanager.app.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtil {

    const val VIDEO_PERMISSION = Manifest.permission.READ_MEDIA_VIDEO

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, VIDEO_PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun shouldShowRationale(activity: Activity): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, VIDEO_PERMISSION)

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
