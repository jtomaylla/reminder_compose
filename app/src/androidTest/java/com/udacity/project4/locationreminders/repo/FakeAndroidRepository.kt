package com.udacity.project4.locationreminders.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class FakeAndroidRepository : ReminderDataSource {

    var remindersServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    private var shouldReturnError = false

    private val observableReminders = MutableLiveData<Result<List<ReminderDTO>>>()

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun refreshReminders() {

        observableReminders.value = getReminders()
    }

    override fun observeReminders(): LiveData<Result<List<ReminderDTO>>> {
        runBlocking {
            refreshReminders()
        }
        return observableReminders
    }

    override fun observeReminder(reminderId: String): LiveData<Result<ReminderDTO>> {
        runBlocking { refreshReminders() }
        return observableReminders.map { reminders ->
            when (reminders) {
                is Result.Error -> Result.Error(reminders.message)
                is Result.Success -> {
                    val reminder = reminders.data.firstOrNull() {
                        it.id == reminderId
                    } ?: return@map Result.Error("Not found")
                    Result.Success(reminder)
                }
                is Result.ErrorEx -> Result.Error("Error $reminderId")
                Result.Loading -> TODO()
            }
        }
    }

    override suspend fun refreshReminder(id: String) {

    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Test exception")
        }
        return Result.Success(remindersServiceData.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersServiceData[reminder.id] = reminder
        Timber.i("${remindersServiceData[reminder.id]}")
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Test exception")
        }
        remindersServiceData[id]?.let {
            return Result.Success(it)
        }
        return Result.Error("could not find task")
    }

    override suspend fun deleteAllReminders() {
        remindersServiceData.clear()
        refreshReminders()
    }

    fun insertReminders(reminders: List<ReminderDTO>) {
        for (reminder in reminders) {
            remindersServiceData[reminder.id] = reminder
        }
        runBlocking { refreshReminders() }
    }
}
