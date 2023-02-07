package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {
    
    @get:Rule
    var instanteExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminder_whenReminderIsPopulated_thenInsertIntoDatabase() = runBlockingTest {

        //Given
        val reminderDTO = buildReminderItem()

        database.reminderDao().saveReminder(reminderDTO)

        //When
        val reminderById = database.reminderDao().getReminderById(reminderDTO.id)

        //Then
        assertThat<ReminderDTO>(
            reminderById as ReminderDTO,
            notNullValue()
        )
        assertThat(reminderById.id, `is`(reminderDTO.id))
        assertThat(reminderById.title, `is`(reminderDTO.title))
        assertThat(reminderById.location, `is`(reminderDTO.location))
        assertThat(reminderById.latitude, `is`(reminderDTO.latitude))
        assertThat(reminderById.longitude, `is`(reminderDTO.longitude))
    }

    @Test
    fun getReminders_whenNoReminderIdisProvided_returnListOfReminders() = runBlockingTest {


        //Given
        val reminderDTOs: List<ReminderDTO> = buildReminderLocationItems()

        database.reminderDao().saveMultipleReminders(reminderDTOs)

        //When
        val reminders = database.reminderDao().getReminders()


        //Then
        assertThat(reminders, not(emptyList()))
        assertThat(reminders, hasSize(reminderDTOs.size))

    }


    @Test
    fun getReminderById_whenReminderIsSave_thenReturnSaveReminderWithSameId() = runBlockingTest {

        //Given
        val reminderDTO = buildReminderItem()

        database.reminderDao().saveReminder(reminderDTO)


        //When
        val reminderById = database.reminderDao().getReminderById(reminderDTO.id)


        //Then
        assertThat<ReminderDTO>(
            reminderById as ReminderDTO,
            notNullValue()
        )
        assertThat(reminderById.id, `is`(reminderDTO.id))
        assertThat(reminderById.longitude, `is`(reminderDTO.longitude))
        assertThat(reminderById.latitude, `is`(reminderDTO.latitude))
    }


    @Test
    fun deleteAllReminders_whenAllReminderItemsAreDeleted_returnEmptyList() = runBlockingTest {

        //Given
        val reminderDTOs = buildReminderLocationItems()
        database.reminderDao().saveMultipleReminders(reminderDTOs)


        //When
        database.reminderDao().deleteAllReminders()

        val reminders = database.reminderDao().getReminders()


        //Then
        assertThat(reminders,  `is`(Collections.EMPTY_LIST))

    }


    private fun buildReminderItem() = ReminderDTO(
        "testTitle",
        "testDescription", "testlocation", -12.1549766, -77.0053464
    )


    private fun buildReminderLocationItems() = arrayListOf(
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
}
