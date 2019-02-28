package com.fsryan.repostalker.main

import android.content.Intent
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.RootMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.fsryan.repostalker.FakeComponents
import com.fsryan.repostalker.R
import com.fsryan.repostalker.main.event.MainViewEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.subjects.PublishSubject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    /**
     * This [PublishSubject] will be used to inject events into the UI.
     */
    internal lateinit var eventSubject: PublishSubject<MainViewEvent>
    /**
     * This [Main.Presenter] will get injected into the activity as a substitute
     * for any other version. In this way, verifications against the presenter
     * will be possible.
     */
    internal lateinit var mockPresenter: Main.Presenter

    @get:Rule
    val activityRule = object: ActivityTestRule<MainActivity>(MainActivity::class.java) {
        /**
         * This section of the test refreshes the [mockPresenter] object and
         * the [eventSubject] object. The [mockPresenter] will be registered
         * with the [FakeComponents] object registry in order to substitute
         * [mockPresenter] for the [Main.Presenter]. In this way, the
         * application's object graph need not be built just to test the UI.
         */
        override fun beforeActivityLaunched() {
            eventSubject = PublishSubject.create()
            mockPresenter = mockk(relaxed = true)
            every { mockPresenter.eventObservable() } returns eventSubject
            FakeComponents.get().addToInjectionRegistry(Main.Presenter::class.java, mockPresenter)
        }

        /**
         * After the test is finished, the previous [mockPresenter] object is
         * removed from the injection registry in [FakeComponents].
         */
        override fun afterActivityFinished() {
            FakeComponents.get().removeFromInjectionRegistry<Main.Presenter>(Main.Presenter::class.java)
        }

        /**
         * prevents the activity from loading the fragments so that focus can
         * be upon testing just what the activity is supposed to do apart from
         * loading the fragments.
         */
        override fun getActivityIntent(): Intent {
            return MainActivity.intent(getTargetContext(), true)
        }
    }

    /**
     * If [MainActivity] were to call these methods in the opposite order,
     * then the presenter would not be able to immediately send events in the
     * [Main.Presenter.onReady] method. With this order, the [Main.Presenter]
     * can be sure that when [Main.Presenter.onReady] is called, all events
     * will be received.
     */
    @Test
    fun shouldAcquireEventSubjectFromPresenterThenCallPresenterOnReady() {
        verifyOrder {
            mockPresenter.eventObservable()
            mockPresenter.onReady()
        }
    }

    /**
     * Views are not allowed to perform business logic--they can only tell the
     * presenter when the user requested something. This test ensures that the
     * presenter is informed when the user selects to view settings.
     */
    @Test
    fun shouldInformPresenterWhenOverflowMenuSelected() {
        Espresso.openActionBarOverflowOrOptionsMenu(getTargetContext())
        onOverflowSettingsItem().perform(click())
        verify { mockPresenter.userRequestedSettings() }
    }

    /**
     * Ensures that when the view is told to display the settings dialog, it
     * correctly displays the dialog
     */
    @Test
    fun shouldDisplaySettingsDialogOnShowSettingsEvent() {
        val invalidationInterval = 60L
        inject(MainViewEvent.forShowingSettings(invalidationInterval))
        checkDialogDisplayed(invalidationInterval)
    }

    /**
     * Ensures that when the user clicks the save button in the dialog, the
     * value from the entry text is used. Furthermore, it ensures that the
     * [Main.Presenter.userCanceledSettings] method is not called.
     */
    @Test
    fun shouldInformPresenterWhenSettingsSavedByUser() {
        val newInterval = "20"
        inject(MainViewEvent.forShowingSettings(10L))

        onSettingsDialogEntryText().perform(replaceText(newInterval))
        onSettingsDialogSaveButton().perform(click())

        verify { mockPresenter.userSavedSettings(newInterval) }
        verify(exactly = 0) { mockPresenter.userCanceledSettings() }
    }

    /**
     * Ensures that when the user clicks the cancel button in the dialog, the
     * presenter is informed. Furthermore, it ensures that the
     * [Main.Presenter.userSavedSettings] method is not called.
     */
    @Test
    fun shouldInformPresenterWhenSettingsCanceledByUser() {
        val invalidationInterval = 10L
        inject(MainViewEvent.forShowingSettings(invalidationInterval))

        onSettingsDialogCancelButton().perform(click())

        verify { mockPresenter.userCanceledSettings() }
        verify(exactly = 0) { mockPresenter.userSavedSettings(any()) }
    }

    /**
     * Ensures that when the view is told to hide the settings dialog, it
     * correctly hides the dialog
     */
    @Test
    fun shouldHideSettingsDialogOnHideSettingsEvent() {
        inject(MainViewEvent.forShowingSettings(60L))
        inject(MainViewEvent.forHidingSettings())

        onSettingsDialogTitle().check(doesNotExist())
        onSettingsDialogCancelButton().check(doesNotExist())
        onSettingsDialogSaveButton().check(doesNotExist())
        onSettingsDialogEntryText().check(doesNotExist())
    }

    /**
     * Ensures that presenter is informed when back button is pressed
     */
    @Test
    fun shouldInformPresenterWhenBackButtonPressed() {
        Espresso.pressBackUnconditionally()
        verify { mockPresenter.userRequestedBackNav() }
    }

    /**
     * Ensures that when the view is told to display an error message, it
     * displays in a toast
     */
    @Test
    fun shouldDisplayErrorMessageOnErrorMessageEvent() {
        inject(MainViewEvent.forHidingSettings("some error"))
        onView(withText("some error")).inRoot(RootMatchers.isSystemAlertWindow())
            .check(matches(isDisplayed()))
    }

    private fun checkDialogDisplayed(invalidationInterval: Long) {
        onSettingsDialogTitle().check(matches(isDisplayed()))
        onSettingsDialogCancelButton().check(matches(isDisplayed()))
        onSettingsDialogSaveButton().check(matches(isDisplayed()))
        onSettingsDialogEntryText().check(matches(withText(invalidationInterval.toString())))
    }

    private fun onOverflowSettingsItem() = onView(withText(R.string.settings))
    private fun onSettingsDialogTitle() = onView(withText(R.string.settings_dialog_title))
    private fun onSettingsDialogCancelButton() = onView(withText(R.string.cancel))
    private fun onSettingsDialogSaveButton() = onView(withText(R.string.save))
    private fun onSettingsDialogEntryText() = onView(withId(R.id.settings_dialog_invalidation_interval_entry_text))

    private fun inject(event: MainViewEvent) {
        eventSubject.onNext(event)
    }
}