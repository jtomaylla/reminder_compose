package com.udacity.project4.locationreminders.reminderslist

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

data class ReminderDataItem(
    @field:SerializedName("title")
    var title: String?,
    @field:SerializedName("description")
    var description: String?,
    @field:SerializedName("location")
    var location: String?,
    @field:SerializedName("latitude")
    var latitude: Double?,
    @field:SerializedName("longitude")
    var longitude: Double?,
    @field:SerializedName("id")
    var id: String = UUID.randomUUID().toString()
) : Serializable