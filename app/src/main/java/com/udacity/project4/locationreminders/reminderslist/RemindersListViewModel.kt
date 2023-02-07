package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.udacity.project4.MyApp.Companion.geoReminders
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.launch

class RemindersListViewModel(
    app: Application,
    private val dataSource: ReminderDataSource
) : BaseViewModel(app) {

    val remindersList = MutableLiveData<List<ReminderDataItem>>()

    private val reminders: LiveData<Result<List<ReminderDTO>>> = dataSource.observeReminders()

    val error: LiveData<Boolean> = reminders.map { it is Result.Error }
    val empty: LiveData<Boolean> = reminders.map { (it as? Result.Success)?.data.isNullOrEmpty() }

    fun loadReminders() {
        showLoading.value = true
        viewModelScope.launch {

            val result = dataSource.getReminders()
            showLoading.postValue(false)
            when (result) {
                is Result.Success<*> -> {
                    val dataList = ArrayList<ReminderDataItem>()

                    dataList.addAll((result.data as List<ReminderDTO>).map { reminder ->

                        ReminderDataItem(
                            reminder.title,
                            reminder.description,
                            reminder.location,
                            reminder.latitude,
                            reminder.longitude,
                            reminder.id
                        )
                    })
                    remindersList.value = dataList

                }
                is Result.Error ->
                    showSnackBar.value = result.message
                else -> {}
            }

            invalidateShowNoData()
        }
    }

    fun invalidateShowNoData() {
        showNoData.value = remindersList.value == null || remindersList.value!!.isEmpty()
    }

    fun deleteReminders() {
        viewModelScope.launch {
            showLoading.postValue(false)
            dataSource.deleteAllReminders()
        }
    }

    fun refresh() {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.refreshReminders()
            showLoading.value = false
        }
    }

    fun invalidateData() {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.getReminders()
            showLoading.value = false
        }
    }
}