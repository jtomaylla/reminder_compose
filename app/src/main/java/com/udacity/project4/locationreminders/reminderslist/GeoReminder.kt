package com.udacity.project4.locationreminders.reminderslist

import android.os.Parcel
import android.os.Parcelable

data class GeoReminder(
        val id: String?,
        val title:String?,
        val description:String?,
        val location: String?,
        val latitude: Double?,
        val longitude: Double?) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readDouble(),
            source.readDouble()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(id)
        writeString(title)
        writeString(description)
        writeString(location)
        writeDouble(latitude!!)
        writeDouble(longitude!!)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<GeoReminder> = object : Parcelable.Creator<GeoReminder> {
            override fun createFromParcel(source: Parcel): GeoReminder = GeoReminder(source)
            override fun newArray(size: Int): Array<GeoReminder?> = arrayOfNulls(size)
        }
    }
}