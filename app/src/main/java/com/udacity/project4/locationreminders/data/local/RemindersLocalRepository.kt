package com.udacity.project4.locationreminders.data.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.utils.wrapEspressoIdlingResource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

class RemindersLocalRepository(
    private val remindersDao: RemindersDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ReminderDataSource {

    private val observableReminders = MutableLiveData<Result<List<ReminderDTO>>>()

    override suspend fun refreshReminders() {
        observableReminders.value = getReminders()!!
    }

    override fun observeReminders(): LiveData<Result<List<ReminderDTO>>> {
        runBlocking { refreshReminders() }
        return observableReminders
    }

    override suspend fun refreshReminder(id: String) {
        refreshReminders()
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
                is Result.Error -> Result.Error(reminders.message)
            }
        }
    }


    override suspend fun getReminders(): Result<List<ReminderDTO>>? = withContext(ioDispatcher) {
        wrapEspressoIdlingResource {
            return@withContext try {
                Result.Success(remindersDao.getReminders())
            } catch (ex: Exception) {
                Result.Error(ex.localizedMessage)
            }
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) =
        wrapEspressoIdlingResource {
            withContext(ioDispatcher) {
                remindersDao.saveReminder(reminder)
            }
        }


    override suspend fun getReminder(id: String): Result<ReminderDTO> = withContext(ioDispatcher) {
        wrapEspressoIdlingResource {
            try {
                val reminder = remindersDao.getReminderById(id)
                if (reminder != null) {
                    return@withContext Result.Success(reminder)
                } else {
                    return@withContext Result.Error("Reminder not found!")
                }
            } catch (e: Exception) {
                return@withContext Result.Error(e.localizedMessage)
            }
        }
    }

    override suspend fun deleteAllReminders() {
        wrapEspressoIdlingResource {
            withContext(ioDispatcher) {
                remindersDao.deleteAllReminders()
            }
        }
    }
}
