package com.udacity.project4

import android.app.Application
import android.provider.Settings.Global.getString
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.repo.FakeAndroidRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.ExpressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import kotlin.collections.arrayListOf as arrayListOf1
import androidx.test.rule.ActivityTestRule
import com.google.android.material.internal.ContextUtils.getActivity
import org.junit.Rule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import android.os.IBinder
import android.provider.DocumentsContract
import android.text.TextUtils
import android.view.View

import android.view.WindowManager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.test.espresso.Root
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.util.SnackbarUtils
import org.hamcrest.Description
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.AllOf
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.mockito.Mockito


@RunWith(AndroidJUnit4::class)
@LargeTest

class RemindersActivityTest :
    AutoCloseKoinTest() {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private interface DismissAction {
        fun dismiss(snackbar: Snackbar?)
    }

    @Before
    fun init() {
        stopKoin()

        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }

    }

    @Before
    fun setUpResources(): Unit = IdlingRegistry.getInstance().run {
        register(ExpressoIdlingResource.countingIdlingResource)
        register(dataBindingIdlingResource)
    }

    @After
    fun tearDown(): Unit = IdlingRegistry.getInstance().run {
        unregister(ExpressoIdlingResource.countingIdlingResource)
        unregister(dataBindingIdlingResource)

        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun loginFireBase_whenSuccessful_returnAuthenticatedUser(){


        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)


        activityScenario.close()
    }

    @Test
    fun loadReminders_whenEmptyList_showMessage() = runBlocking() {
        //Given

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        //When

        dataBindingIdlingResource.monitorActivity(activityScenario)

        //Then

        onView(withText("No Data")).check(matches(isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun saveReminder_whenValidateLocation_returnLocationDisplayed() = runBlocking() {

        //Given

        val reminder = buildReminder()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        //When
        repository.saveReminder(reminder)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //Then
        onView(withText(reminder.title)).check(matches(isDisplayed()))
        onView(withText(reminder.description)).check(matches(isDisplayed()))
        activityScenario.close()
    }

    @Test
    fun saveReminder_whenClickButton_showToastMessage() = runBlocking() {
        //Given
        val reminder = buildReminder()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        //When
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withText("No Data")).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText(reminder.title))
        onView(withId(R.id.reminderDescription)).perform(typeText(reminder.description))

        //Then
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.map)).perform(longClick())

        onView(withId(R.id.save_button)).perform(click())

        onView(withText("Custom Location Indicated")).check(matches(isDisplayed()))
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withText("Reminder Saved !")).inRoot(ToastMatcher())
            .check(matches(isDisplayed()))


        activityScenario.close()
    }

    @Test
    fun saveReminder_whenValidateTitle_showSnackBarMessage() = runBlocking() {
        //Given
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        //When
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())

        //Then

        onView(withId(R.id.saveReminder)).perform(click())

        Thread.sleep(9000)

        // Verify that we're showing the message
        withText("Please enter title")
            .matches(
                AllOf.allOf(
                    isDescendantOfA(isAssignableFrom(SnackbarLayout::class.java)),
                    isCompletelyDisplayed()
                )
            )

        activityScenario.close()
    }

    @Test
    fun saveReminder_whenValidateDescription_showSnackBarMessage() = runBlocking() {
        //Given
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        //When
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("reminder.title"))
        //Then
        Espresso.closeSoftKeyboard()

        onView(withId(R.id.saveReminder)).perform(click())

        Thread.sleep(9000)

        // Verify that we're showing the message
        withText("Please enter description")
            .matches(
                AllOf.allOf(
                    isDescendantOfA(isAssignableFrom(SnackbarLayout::class.java)),
                    isCompletelyDisplayed()
                )
            )

        activityScenario.close()
    }

    @Test
    fun saveReminder_whenValidateLocation_showSnackBarMessage() = runBlocking() {
        //Given
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        //When
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())

        onView(withId(R.id.reminderTitle)).perform(typeText("reminder.title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("reminder.description"))

        //Then
        Espresso.closeSoftKeyboard()

        onView(withId(R.id.saveReminder)).perform(click())

        Thread.sleep(9000)

        // Verify that we're showing the message
        withText("Please select location")
            .matches(
                AllOf.allOf(
                    isDescendantOfA(isAssignableFrom(SnackbarLayout::class.java)),
                    isCompletelyDisplayed()
                )
            )

        activityScenario.close()
    }

    @Test
    fun reminderLocation_EndToEndTesting() = runBlocking() {
        //Given
        val reminder = buildReminder()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)


        //When
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withText("No Data")).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText(reminder.title))
        onView(withId(R.id.reminderDescription)).perform(typeText(reminder.description))


        //Then
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.map)).perform(longClick())

        openActionBarOverflowOrOptionsMenu(getApplicationContext())
        onView(withText("Hybrid Map")).perform(click())
        openActionBarOverflowOrOptionsMenu(getApplicationContext())
        onView(withText("Satellite Map")).perform(click())
        openActionBarOverflowOrOptionsMenu(getApplicationContext())
        onView(withText("Terrain Map")).perform(click())
        openActionBarOverflowOrOptionsMenu(getApplicationContext())
        onView(withText("Normal Map")).perform(click())
        onView(withId(R.id.save_button)).perform(click())

        onView(withText("Custom Location Indicated")).check(matches(isDisplayed()))
        onView(withId(R.id.saveReminder)).perform(click())
        activityScenario.close()
    }


    private fun buildReminder() = ReminderDTO(
        "someTitleD",
        "someDescriptionD", "someLocationD", 32.776665,
        -96.796989
    )

    private fun buildReminderList() = emptyList<ReminderDTO>()

}

class ToastMatcher : TypeSafeMatcher<Root>() {
    override fun describeTo(description: Description) {
        description.appendText("is toast")
    }

    override fun matchesSafely(root: Root): Boolean {
        val type: Int = root.getWindowLayoutParams().get().type
        if (type == WindowManager.LayoutParams.TYPE_TOAST) {
            val windowToken: IBinder = root.getDecorView().getWindowToken()
            val appToken: IBinder = root.getDecorView().getApplicationWindowToken()
            if (windowToken === appToken) {
                //means this window isn't contained by any other windows.
                return true
            }
        }
        return false
    }
}

