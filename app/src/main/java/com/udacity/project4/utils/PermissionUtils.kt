@file:Suppress("IMPLICIT_BOXING_IN_IDENTITY_EQUALS")

package com.udacity.project4.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat


const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

val runningQOrLater = VERSION.SDK_INT >= VERSION_CODES.Q

@RequiresApi(Build.VERSION_CODES.Q)
fun foregroundAndBackgroundLocationPermissionApproved(context: Context): Boolean {
    val foregroundLocationApproved = (
            PackageManager.PERMISSION_GRANTED ==
                    context?.let {
                        ActivityCompat.checkSelfPermission(
                            it,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    })

    val backgroundPermissionApproved =
        if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
        } else {
            true
        }

    return foregroundLocationApproved && backgroundPermissionApproved
}

@RequiresApi(Build.VERSION_CODES.Q)
fun requestForegroundAndBackgroundLocationPermissions(context: Context){

    if(foregroundAndBackgroundLocationPermissionApproved(context)){
         return
    }

    var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

  val resultCode = when {
        runningQOrLater -> {
            permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
            REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
        }
        else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
    }

    ActivityCompat.requestPermissions(
        context as Activity,
        permissionsArray,
        resultCode
    )

}

fun isPermissionGranted(context: Context?): Boolean {
    return context?.let {
        ActivityCompat.checkSelfPermission(
            it,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } === PackageManager.PERMISSION_GRANTED
}

