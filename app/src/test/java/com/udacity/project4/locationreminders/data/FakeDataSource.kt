package com.udacity.project4.locationreminders.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.runBlocking

class FakeDataSource(var reminderServiceData: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    private var shouldReturnError = false

    private val observableReminders = MutableLiveData<Result<List<ReminderDTO>>>()

    override suspend fun refreshReminders() {
        observableReminders.value = getReminders()
    }

    override suspend fun refreshReminder(id: String) {
        refreshReminders()
    }

    override fun observeReminders(): LiveData<Result<List<ReminderDTO>>> {
        runBlocking { refreshReminders() }
        return observableReminders
    }

    override fun observeReminder(id: String): LiveData<Result<ReminderDTO>> {
        runBlocking { refreshReminders() }
        return observableReminders.map { reminders ->
            when (reminders) {
                is Result.Loading -> Result.Loading
                is Result.ErrorEx -> Result.ErrorEx(reminders.exception)
                is Result.Success -> {
                    val reminder = reminders.data.firstOrNull() { it.id == id }
                            ?: return@map Result.ErrorEx(Exception("Not found"))
                    Result.Success(reminder)
                }
                else -> Result.Error("Reminder Error for $id")
            }
        }
    }

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }
    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        if (shouldReturnError) {
            return Result.Error(Exception("Test exception").toString())
        }

        //reminderServiceData?.let{ return Result.Success(ArrayList(it))}
        //return  Result.Error("no data")
        return Result.Success(reminderServiceData!!.take(5))
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderServiceData?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {

        if (shouldReturnError) {
            return Result.Error(Exception("Test exception").toString())
        }

        return when(val reminderItemFound = reminderServiceData?.find { it.id == id }){
            null -> Result.Error("Reminder Not Found for $id")

            else -> Result.Success(reminderItemFound)
        }
    }

    override suspend fun deleteAllReminders() {
        reminderServiceData = mutableListOf()
    }



}