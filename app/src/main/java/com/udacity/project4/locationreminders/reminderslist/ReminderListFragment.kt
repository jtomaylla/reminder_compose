package com.udacity.project4.locationreminders.reminderslist

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.GeofenceStatusCodes.GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.MyApp
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.locationreminders.geofence.ForegroundOnlyLocationService
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.utils.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import android.content.SharedPreferences
import android.location.Location
import androidx.lifecycle.Observer

class ReminderListFragment : BaseFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var geofencingClient: GeofencingClient

    private var geofenceHelper: GeofenceHelper? = null
    private val runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q


    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT

        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
    
    private var foregroundOnlyLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null

    // Listens for location broadcasts from ForegroundOnlyLocationService.
    private lateinit var foregroundOnlyBroadcastReceiver: ForegroundOnlyBroadcastReceiver

    private lateinit var sharedPreferences: SharedPreferences
    
    private lateinit var outputTextView: TextView

    // Monitors connection to the while-in-use service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
                "RemindersActivity.geofenceReminder.action.ACTION_GEOFENCE_EVENT"
        private const val REQUEST_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        private const val TAG = "RemindersListFragment"
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
        private const val GEOFENCE_RADIUS_IN_METERS = 100f
    }

    override val _viewModel: RemindersListViewModel by viewModel()
    private lateinit var binding: FragmentRemindersBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

        sharedPreferences =
                activity?.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)!!

        startReceivingLocationUpdates()

    }
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        geofenceHelper = GeofenceHelper(requireContext())

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        binding =
            DataBindingUtil.inflate(
                    inflater,
                    R.layout.fragment_reminders, container, false
            )
        binding.viewModel = _viewModel

        createChannel(requireContext())

        setHasOptionsMenu(true)

        setDisplayHomeAsUpEnabled(false)

        setTitle(getString(R.string.app_name))

        binding.refreshLayout.setOnRefreshListener {
            _viewModel.loadReminders()
            setupGeofences()
        }

        return binding.root
    }

    private fun startReceivingLocationUpdates() {
        val enabled = sharedPreferences.getBoolean(
                SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)

        if (enabled) {
            foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
        } else {
            if (backgroundLocationPermissionApproved()) {
                foregroundOnlyLocationService?.subscribeToLocationUpdates()
                        ?: Log.d(TAG, "Service Not Bound")
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        setupRecyclerView()

        setupMessages()

        updateButtonState(
                sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
        )
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        val serviceIntent = Intent(requireContext(), ForegroundOnlyLocationService::class.java)
        activity?.bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
        
        binding.addReminderFAB.setOnClickListener {

            navigateToAddReminder()
        }
    }

    private fun setupMessages() {
        _viewModel.showSnackBar.observe(
            this,
            {
                Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
            }
        )
        _viewModel.showToast.observe(this, {
            Toast.makeText(requireActivity(),it,Toast.LENGTH_LONG).show()
        })
        _viewModel.showSnackBarInt.observe(
            this,
            {
                Snackbar.make(requireView(), getString(it), Snackbar.LENGTH_LONG).show()
            }
        )

    }

    private fun updateButtonState(trackingLocation: Boolean) {
        if (trackingLocation) {
            Log.d(TAG, getString(R.string.stop_location_updates_button_text))
        } else {
            Log.d(TAG, getString(R.string.stop_location_updates_button_text))
        }
    }

    override fun onResume() {
        super.onResume()
        _viewModel.loadReminders()
        MyApp.geoReminders = _viewModel.remindersList
        Log.d(TAG, "onResume(): _viewModel.loadReminders()")
    }

    private fun navigateToAddReminder() {
        _viewModel.navigationCommand.postValue(
                NavigationCommand.To(
                        ReminderListFragmentDirections.toSaveReminder()
                )
        )
    }
    val adapter = RemindersListAdapter {}
    private fun setupRecyclerView() {
        binding.reminderssRecyclerView.setup(adapter)
        refreshAdapter()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                Timber.i("Logout Button Pressed")
                activity?.finish()
                AuthUI.getInstance().signOut(requireContext())
                        .addOnCompleteListener {
                            val intent = Intent(context, AuthenticationActivity::class.java)
                            intent.putExtra("isLogout", true)
                            startActivity(intent)
                        }


            }
        }
        return super.onOptionsItemSelected(item)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onStart() {
        super.onStart()
        checkPermissionsAndStartGeofencing()
    }
    
    override fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            activity?.unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        super.onStop()
    }
    
    private fun checkPermissionsAndStartGeofencing() {

        //if (backgroundLocationPermissionApproved()) {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            
            updateButtonState(
                    sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
            )
            sharedPreferences.registerOnSharedPreferenceChangeListener(this)

            val serviceIntent = Intent(requireContext(), ForegroundOnlyLocationService::class.java)
            activity?.bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)

            
            checkDeviceLocationSettingsAndStartGeofence()
        } else {

            //requestBackgroundLocationPermissions()
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    /*
*  Determines whether the app has the appropriate permissions across Android 10+ and all other
*  Android versions.
*/
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION))

        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }

        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /*
*  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
*/
    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            return
        }
        // Else request the permission
        // this provides the result[LOCATION_PERMISSION_INDEX]
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            runningQOrLater -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d(TAG, "Request foreground only location permission")
        ActivityCompat.requestPermissions(
            requireActivity(),
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

                    exception.startResolutionForResult(
                            requireActivity(),
                            REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                        binding.reminderssRecyclerView,
                        R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                setupGeofences()
            }
        }
    }

    private fun setupGeofences() {
        if (_viewModel.remindersList.value != null && _viewModel.remindersList.value!!.isNotEmpty()) {

            Log.d(TAG, _viewModel.remindersList.value!!.toString())
            addGeofences(
                    _viewModel.remindersList.value,
                    GEOFENCE_RADIUS_IN_METERS
            )

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }
    @TargetApi(29)
    private fun requestBackgroundLocationPermissions() {
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d(TAG, "Request foreground only location permission")
        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsArray,
            resultCode
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        removeGeofences()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                    // Permission was granted.
                    foregroundOnlyLocationService?.subscribeToLocationUpdates()
                else -> {
                    // Permission denied.
                    updateButtonState(false)

                    Snackbar.make(
                            binding.reminderssRecyclerView,
                            R.string.permission_denied_explanation,
                            Snackbar.LENGTH_LONG
                    )
                            .setAction(R.string.settings) {
                                // Build intent that displays the App settings screen.
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                val uri = Uri.fromParts(
                                        "package",
                                        BuildConfig.APPLICATION_ID,
                                        null
                                )
                                intent.data = uri
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                            }
                            .show()

                    checkDeviceLocationSettingsAndStartGeofence()
                }
            }
        }
        
    }

    @SuppressLint("MissingPermission")
    private fun removeGeofences() {
        if (!backgroundLocationPermissionApproved() ) {
            return
        }
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                activity?.let {
                    Log.d(TAG, getString(R.string.geofences_removed))
                    Toast.makeText(requireContext(), getString(R.string.geofences_removed), Toast.LENGTH_SHORT)
                            .show()
                }

            }
            addOnFailureListener {
                activity?.let {
                    Log.d(TAG, requireContext().getString(R.string.geofences_not_removed))
                }

            }
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
                if (errorMessage.contains(GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION.toString())) checkPermissionsAndStartGeofencing()
            }

    }

    fun refreshAdapter() {
        _viewModel.remindersList.observe(viewLifecycleOwner, Observer
            {
                _viewModel.refresh()
            }
        )
    }
    private fun logResultsToScreen(output: String) {

        Log.d(TAG, "logResultsToScreen: $output")

    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // Updates button states if new while in use location is added to SharedPreferences.
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {
            updateButtonState(sharedPreferences.getBoolean(
                    SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
            )
        }
    }
    /**
     * Receiver for location broadcasts from [ForegroundOnlyLocationService].
     */
    private inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val location = intent.getParcelableExtra<Location>(
                    ForegroundOnlyLocationService.EXTRA_LOCATION
            )

            if (location != null) {
                logResultsToScreen("Foreground location: ${location.toText()}")
            }
        }
    }
    
}
