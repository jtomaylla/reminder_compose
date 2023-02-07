package com.udacity.project4.locationreminders.data.local

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.succeeded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.hasSize
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)

@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var reminderLocalRepo: RemindersLocalRepository
    private lateinit var reminderDatabase: RemindersDatabase

    @Before
    fun initDb() {
        reminderDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        reminderLocalRepo =
            RemindersLocalRepository(reminderDatabase.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDb() = reminderDatabase.close()

    @Test
    fun getReminders_whenSuccess_returnListOfReminders() = runBlocking() {

        //Given
        reminderLocalRepo.saveReminder(buildReminder())
        reminderLocalRepo.saveReminder(buildReminder())


        //When
        val results = reminderLocalRepo.getReminders()


        //Then
        assertThat(results.succeeded, `is`(true))
        val reminderList = results as Result.Success

        assertThat(reminderList, not(emptyArray<ReminderDTO>()))
        assertThat(reminderList.data, hasSize(greaterThanOrEqualTo(1)))
    }

    @Test
    fun saveReminder_whenValidReminderRecord_InsertRecordIntoDatabase() = runBlocking() {


        //Given
        val reminder = buildReminder()


        //When
        reminderLocalRepo.saveReminder(reminder)
        val reminderRecord = reminderLocalRepo.getReminder(reminder.id)

        //Then
        assertThat(reminderRecord.succeeded, `is`(true))
        reminderRecord as Result.Success
        assertThat(reminder.id, `is`(reminderRecord.data.id))
    }

    @Test
    fun deleteReminders_whenRemindersAreDelete_returnReminderNotFoundMessage() = runBlocking {

        //Given
        val reminder = buildReminder()
        reminderLocalRepo.saveReminder(reminder)


        //When
        reminderLocalRepo.deleteAllReminders()

        val reminderRecord = reminderLocalRepo.getReminder(reminder.id)

        //Then
        assertThat(reminderRecord.succeeded, `is`(false))
        reminderRecord as Result.Error

        assertThat(reminderRecord.message, `is`("Reminder not found!"))


    }

    @Test
    fun getReminder_whenReminderInserted_returnReminder() = runBlocking {

        //Given
        val reminder = buildReminder()


        //When
        reminderLocalRepo.saveReminder(reminder)

        val reminderRecord = reminderLocalRepo.getReminder(reminder.id)
        reminderRecord as Result.Success


        assertThat(reminderRecord.data.id, `is`(reminder.id))


    }

    private fun buildRemindersList() = arrayListOf(
        ReminderDTO(
            "testTitleOne", "testDescriptionA",
            "testlocationA", -12.1549766, -77.0053464
        ),
        ReminderDTO(
            "testTitleTwo", "testDescriptionB",
            "testlocationB", -12.146872989241414, -77.01168313622475
        ),
        ReminderDTO(
            "testTitleThree", "testDescriptionC",
            "testlocationC", -12.145991287095855, -77.01282374560833
        )
    )

    private fun buildReminder() = ReminderDTO(
        "testTitle",
        "testDescription", "testlocation", -12.1549766, -77.0053464
    )
}