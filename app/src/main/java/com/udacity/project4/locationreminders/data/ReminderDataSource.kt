package com.udacity.project4.locationreminders.data

import androidx.lifecycle.LiveData
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.flow.Flow

interface ReminderDataSource {

    fun observeReminders(): LiveData<Result<List<ReminderDTO>>>

    suspend fun refreshReminders()

    fun observeReminder(id: String): LiveData<Result<ReminderDTO>>

    suspend fun refreshReminder(id: String)

    suspend fun getReminders(): Result<List<ReminderDTO>>?

    suspend fun saveReminder(reminder: ReminderDTO)

    suspend fun getReminder(id: String): Result<ReminderDTO>

    suspend fun deleteAllReminders()

}