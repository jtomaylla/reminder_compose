package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.utils.GeofenceHelper
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import android.app.Activity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.accompanist.themeadapter.material.MdcTheme


class SaveReminderFragment : BaseFragment() {

    override val _viewModel: SaveReminderViewModel by sharedViewModel<SaveReminderViewModel>()
    private lateinit var binding: FragmentSaveReminderBinding
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    private var geofenceHelper: GeofenceHelper? = null
    private lateinit var geofencingClient: GeofencingClient

    companion object {
        private const val TAG = "SaveReminderFragment"
        const val GEOFENCE_RADIUS_IN_METERS = 100f
        private val REQUEST_LOCATION_PERMISSION = 1
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val REQUEST_CODE_BACKGROUND = 201
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val BACKROUND_LOCATION_PERMISSION_INDEX = 1
        const val REQUEST_TURN_DEVICE_LOCATION_ON = 5
    }
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ReminderListFragment.ACTION_GEOFENCE_EVENT

        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        binding =
//            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)
        val x = activity?.intent!!.putExtra("title","")
        val binding = DataBindingUtil.inflate<FragmentSaveReminderBinding>(
            inflater, R.layout.fragment_save_reminder, container, false
        ).apply {
            setHasOptionsMenu(true)
            setDisplayHomeAsUpEnabled(true)


            composeView.apply  {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    MaterialTheme {
                        ReminderContentData(_viewModel)
                    }
                }
            }
        }

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //binding.lifecycleOwner = this

//        binding.selectLocation.setOnClickListener {
//
//            _viewModel.navigationCommand.value =
//                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
//        }

//        binding.saveReminder.setOnClickListener {
//            saveReminderViewModel()
//            // check Foreground and Background Permissions for Locatios
//            checkForegroundBackgroundPermissions()
//            // check is Device location is enabled
//            checkDeviceLocationSettingsAndStartGeofence()
//            // add current geofence to geofence list
//            setupGeofences()
//
//        }
    }

    private fun checkForegroundBackgroundPermissions() {

        if (foregroundLocationPermissionApproved()) {
            if (backgroundLocationPermissionApproved()) {
                return
            }
        } else {
            checkForegroundBackgroundPermissions()
        }

    }

    @TargetApi(29)
    private fun foregroundLocationPermissionApproved(): Boolean {
        val accessFineLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val accessCoarseLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
        return accessFineLocationApproved  && accessCoarseLocationApproved
    }

    @TargetApi(29)
    private fun requestForegroundLocationPermissions() {
        if (foregroundLocationPermissionApproved())
            return

        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        val resultCode = SaveReminderFragment.REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE

        Log.d(SaveReminderFragment.TAG, "Request foreground only location permission")
        this.requestPermissions(
            permissionsArray,
            resultCode
        )

    }

    @TargetApi(29)
    private fun backgroundLocationPermissionApproved(): Boolean {
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return backgroundPermissionApproved
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){

                try {
                    val activity: Activity? = activity
                    if(activity != null){
                        exception.startResolutionForResult(
                            requireActivity(),
                            SaveReminderFragment.REQUEST_TURN_DEVICE_LOCATION_ON
                        )
                    }

                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(SaveReminderFragment.TAG, "Error geting location settings resolution: " + sendEx.message)
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
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                setupGeofences()
                //navigateToReminderList()
            }
        }
    }

    private fun navigateToReminderList() {
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                SaveReminderFragmentDirections.actionSaveReminderFragmentToReminderListFragment()
            )
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SaveReminderFragment.REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }


    private fun setupMessages() {

        _viewModel.showToast.observe(this, {
            Toast.makeText(requireActivity(),it, Toast.LENGTH_LONG).show()
        })
        _viewModel.showSnackBarInt.observe(
            this,
            {
                Snackbar.make(requireView(), getString(it), Snackbar.LENGTH_LONG).show()
            }
        )
    }

//    override fun onStart() {
//        super.onStart()
//        checkPermissionsAndStartGeofencing()
//    }

    private fun checkPermissionsAndStartGeofencing() {
        //if (_viewModel.geofenceIsActive()) return
        if (foregroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            //requestForegroundAndBackgroundLocationPermissions()
            requestForegroundLocationPermissions()
        }
    }

    private fun saveReminderViewModel() {
        var selectedLatitude = 0.00
        var selectedLongitude = 0.00

        if (_viewModel.reminderSelectedLocationStr.value != null){
            selectedLatitude = _viewModel.latitude.value!!
            selectedLongitude = _viewModel.longitude.value!!
        }

        val reminderDataItem = ReminderDataItem(
                _viewModel.reminderTitle.value,
                _viewModel.reminderDescription.value,
                _viewModel.reminderSelectedLocationStr.value,
                selectedLatitude,
                selectedLongitude
        )

        _viewModel.validateAndSaveReminder(reminderDataItem)


    }

    private fun setupGeofences() {
        if (_viewModel.remindersList.value != null && _viewModel.remindersList.value!!.isNotEmpty()) {

            Log.d(TAG, _viewModel.remindersList.value!!.toString())
            addGeofences(
                    _viewModel.remindersList.value,
                    GEOFENCE_RADIUS_IN_METERS
            )
            navigateToReminderList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofences(reminderList: List<ReminderDataItem>?, radius: Float) {

        var geofenceList = mutableListOf<Geofence>()
        Log.d(TAG, "addGeofences:" + reminderList.toString())
        for (remainder in reminderList!!) {
            val geofence = geofenceHelper!!.getGeofence(
                    remainder.id,
                    LatLng(remainder.latitude!!, remainder.longitude!!),
                    radius,
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            Log.d(TAG, "addGeofences:" + geofence.toString())
            geofenceList.add(geofence)
        }

        val geofencingRequest: GeofencingRequest = geofenceHelper!!.getGeofencingRequest(
                geofenceList!!
        )

        this.geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(
                            TAG,
                            "onSuccess: Geofences Added..."
                    )
                }
                .addOnFailureListener { e ->
                    val errorMessage: String = geofenceHelper!!.getErrorString(e)
                    Log.d(TAG, "onFailure: $errorMessage")
                }

    }

    override fun onDestroy() {
        super.onDestroy()
        _viewModel.onClear()
    }


}
