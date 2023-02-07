package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.MyApp
import com.udacity.project4.locationreminders.reminderslist.GeoReminder
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.utils.NotificationHelper


class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        val notificationHelper = NotificationHelper(context)
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Log.d(TAG, "onReceive: Error receiving geofence event...")
            return
        }
        val geofenceList = geofencingEvent.triggeringGeofences
        for (geofence in geofenceList) {
            Log.d(TAG, "onReceive: geofence.requestId: " + geofence.requestId)
        }
        val geoId = geofenceList[0].requestId

        val geolist = MyApp.geoReminders!!.value

        Log.d(TAG,"onReceive: geolist:"+geolist.toString())

        var geo = geolist!!.filter { g -> g.id == geoId }.single()
        Log.d(TAG,"geo:"+geo.toString())

        val reminderDataItem: ReminderDataItem = ReminderDataItem(
                geo?.title,
                geo?.description,
                geo?.location,
                geo?.latitude!!.toDouble(),
                geo.longitude!!.toDouble(),
                geo.id!!)

        val transitionType = geofencingEvent.geofenceTransition
        when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Toast.makeText(context, "GEOFENCE_TRANSITION_ENTER", Toast.LENGTH_SHORT).show()
                notificationHelper.sendHighPriorityNotification(
                        reminderDataItem.title, reminderDataItem.description,
                    reminderDataItem
                )
            }
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                Toast.makeText(context, "GEOFENCE_TRANSITION_DWELL", Toast.LENGTH_SHORT).show()
                notificationHelper.sendHighPriorityNotification(
                        reminderDataItem.title, reminderDataItem.description,
                    reminderDataItem
                )
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Toast.makeText(context, "GEOFENCE_TRANSITION_EXIT", Toast.LENGTH_SHORT).show()
                notificationHelper.sendHighPriorityNotification(
                        reminderDataItem.title, reminderDataItem.description,
                    reminderDataItem
                )
            }
        }
    }
}

private const val TAG = "GeofenceReceiver"