package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.ui.auth.data.model.Resource
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.utils.GeofenceHelper

@Composable
fun ReminderTitle(viewModel: SaveReminderViewModel,activity : Activity) {
    var title = activity?.intent!!.getStringExtra("title")
    if (title == null) title = ""
    var text by remember { mutableStateOf(TextFieldValue(title!!)) }
    TextField(
        value = text,
        onValueChange = {
            text = it
            viewModel.reminderTitle.value = it.text
            activity?.intent!!.putExtra("title",it.text)
        },
        label = { Text(text = "Title") },
        placeholder = { Text(text = "Your Placeholder/Hint") },
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = true,
            keyboardType = KeyboardType.Text,
        ),
        textStyle = TextStyle(color = MaterialTheme.colors.primary,
            fontSize = 15.sp,
            fontFamily = FontFamily.SansSerif),
        maxLines = 1,
        singleLine = true,
    )
}

@Composable
fun ReminderDescription(viewModel: SaveReminderViewModel,activity : Activity) {
    var description = activity?.intent!!.getStringExtra("description")
    if (description == null) description = ""
    var text by remember { mutableStateOf(TextFieldValue(description!!)) }
    TextField(
        value = text,
        onValueChange = {
            text = it
            viewModel.reminderDescription.value = it.text
            activity?.intent!!.putExtra("description",it.text)
        },
        label = { Text(text = "Description") },
        placeholder = { Text(text = "Your Placeholder/Hint") },
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = true,
            keyboardType = KeyboardType.Text,
        ),
        textStyle = TextStyle(color = MaterialTheme.colors.primary,
            fontSize = 15.sp,
            fontFamily = FontFamily.SansSerif),
        maxLines = 4,
        singleLine = false,
    )
}

@Composable
private fun SelectLocation(viewModel: SaveReminderViewModel) {
    var enabled by rememberSaveable{ mutableStateOf(true)}
    Text(
        text = "Select location"/*stringResource(id = R.string.select_location)*/ ,
        style = MaterialTheme.typography.h5,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable {
                enabled = false
                gotoSelectedLocation(viewModel)
            }
            .focusable(true)
            .background(Color.Magenta, RectangleShape)
    )
}

fun gotoSelectedLocation(viewModel: SaveReminderViewModel){
    viewModel.loading.value = true
    viewModel.navigationCommand.value = NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
    viewModel.loading.value = false
}

@Composable
private fun SelectedLocation(viewModel: SaveReminderViewModel) {
    var location = viewModel.reminderSelectedLocationStr.value
    if (location ==null) location=""
    Text(
        text = location,
        style = MaterialTheme.typography.h5,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .wrapContentWidth(Alignment.End)

    )
}

@Composable
private fun SaveLocation(viewModel: SaveReminderViewModel) {
    val activity = (LocalContext.current as? Activity)
    FloatingActionButton(
        onClick = {
            saveReminderLocation(viewModel)
            checkForegroundBackgroundPermissions(activity!!)
            checkDeviceLocationSettingsAndStartGeofence(true,activity!!)
        },
        backgroundColor = MaterialTheme.colors.secondary,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(vertical = 160.dp, )
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Add FAB",
        )
    }

}


private fun saveReminderLocation(viewModel: SaveReminderViewModel) {
    var selectedLatitude = 0.00
    var selectedLongitude = 0.00

    if (viewModel.reminderSelectedLocationStr.value != null){
        selectedLatitude = viewModel.latitude.value!!
        selectedLongitude = viewModel.longitude.value!!
    }

    val reminderDataItem = ReminderDataItem(
        viewModel.reminderTitle.value,
        viewModel.reminderDescription.value,
        viewModel.reminderSelectedLocationStr.value,
        selectedLatitude,
        selectedLongitude
    )

    viewModel.validateAndSaveReminder(reminderDataItem)

}

private fun checkForegroundBackgroundPermissions(activity: Activity) {
    val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    if (foregroundLocationPermissionApproved(activity)) {
        if (backgroundLocationPermissionApproved(runningQOrLater,activity)) {
            return
        }
    } else {
        checkForegroundBackgroundPermissions(activity)
    }

}


@TargetApi(29)
private fun foregroundLocationPermissionApproved(activity: Activity): Boolean {
    val accessFineLocationApproved = (
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ))
    val accessCoarseLocationApproved = (
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
    return accessFineLocationApproved  && accessCoarseLocationApproved
}

@TargetApi(29)
private fun backgroundLocationPermissionApproved(runningQOrLater: Boolean,activity: Activity): Boolean {
    val backgroundPermissionApproved =
        if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
        } else {
            true
        }
    return backgroundPermissionApproved
}

private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true,activity:Activity) {
    val locationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_LOW_POWER
    }
    val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

    val settingsClient = LocationServices.getSettingsClient(activity)
    val locationSettingsResponseTask =
        settingsClient.checkLocationSettings(builder.build())

    locationSettingsResponseTask.addOnFailureListener { exception ->
        if (exception is ResolvableApiException && resolve) {

            try {
                val activity: Activity? = activity
                if (activity != null) {
                    exception.startResolutionForResult(
                        activity,
                        SaveReminderFragment.REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                }

            } catch (sendEx: IntentSender.SendIntentException) {
                Log.d(
                    "ReminderContent",
                    "Error getting location settings resolution: " + sendEx.message
                )
            }
        } else {
//                Snackbar.make(
//                    binding.saveReminderView,
//                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
//                ).setAction(android.R.string.ok) {
//                    checkDeviceLocationSettingsAndStartGeofence(false)
//                }.show()
        }
    }
}

private fun setupGeofences(viewModel: SaveReminderViewModel,activity:Activity) {
    if (viewModel.remindersList.value != null && viewModel.remindersList.value!!.isNotEmpty()) {

        Log.d("ReminderContent", viewModel.remindersList.value!!.toString())
        addGeofences(
            viewModel.remindersList.value,
            SaveReminderFragment.GEOFENCE_RADIUS_IN_METERS,
            activity
        )
        navigateToReminderList(viewModel)
    }
}


@SuppressLint("MissingPermission")
private fun addGeofences(reminderList: List<ReminderDataItem>?, radius: Float,activity:Activity) {
    lateinit var geofencingClient: GeofencingClient
    val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(activity, GeofenceBroadcastReceiver::class.java)
        intent.action = ReminderListFragment.ACTION_GEOFENCE_EVENT

        PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    var geofenceList = mutableListOf<Geofence>()
    var geofenceHelper: GeofenceHelper? = null
    Log.d("ReminderContent", "addGeofences:" + reminderList.toString())
    for (remainder in reminderList!!) {
        val geofence = geofenceHelper!!.getGeofence(
            remainder.id,
            LatLng(remainder.latitude!!, remainder.longitude!!),
            radius,
            Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT
        )
        Log.d("ReminderContent", "addGeofences:" + geofence.toString())
        geofenceList.add(geofence)
    }

    val geofencingRequest: GeofencingRequest = geofenceHelper!!.getGeofencingRequest(
        geofenceList!!
    )

    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
        .addOnSuccessListener {
            Log.d(
                "ReminderContent",
                "onSuccess: Geofences Added..."
            )
        }
        .addOnFailureListener { e ->
            val errorMessage: String = geofenceHelper!!.getErrorString(e)
            Log.d("ReminderContent", "onFailure: $errorMessage")
        }

}

private fun navigateToReminderList(viewModel: SaveReminderViewModel) {
    viewModel.navigationCommand.postValue(
        NavigationCommand.To(
            SaveReminderFragmentDirections.actionSaveReminderFragmentToReminderListFragment()
        )
    )
}


@Composable
fun IndeterminateCircularProgressIndicator(isDisplayed:Boolean) {
    if (isDisplayed) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(50.dp),
            horizontalArrangement = Arrangement.Center
            ) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp),
                color = colorResource(id = R.color.colorAccent),
                strokeWidth = Dp(value = 4F)
            )
        }
    }
}

@Composable
fun ReminderContent(viewModel: SaveReminderViewModel,activity : Activity) {
    Surface {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(),) {
            ReminderTitle(viewModel,activity)
            ReminderDescription(viewModel,activity)
            IndeterminateCircularProgressIndicator(isDisplayed = false)
            Row(horizontalArrangement = Arrangement.Start){
                SelectLocation(viewModel)
                SelectedLocation(viewModel)
            }
        }

        Row(
            Modifier
                .padding(top = 260.dp, end = 16.dp)
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ){
            SaveLocation(viewModel)
        }

    }
}

@Composable
fun ReminderContentData(viewModel: SaveReminderViewModel = viewModel()) {
    val activity = (LocalContext.current as? Activity)
    ReminderContent(viewModel, activity!!)
}

//@Preview
//@Composable
//private fun ReminderContentPreview() {
//
//    MaterialTheme {
//        if (viewModel1 != null) {
//            ReminderContent(viewModel1)
//        }
//    }
//}