package com.fsryan.repostalker.current

import android.content.Intent
import android.support.test.InstrumentationRegistry.getContext
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.fsryan.repostalker.FakeComponents
import com.fsryan.repostalker.R
import com.fsryan.repostalker.current.event.CurrentViewEvent
import com.fsryan.repostalker.current.event.userDetails
import com.fsryan.repostalker.test.ViewTestUtil
import com.fsryan.repostalker.test.ViewTestUtil.pedlarDrawable
import com.fsryan.repostalker.testonly.EmptyActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import io.reactivex.subjects.PublishSubject
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurrentFragmentTest {

    lateinit var eventSubject: PublishSubject<CurrentViewEvent>
    lateinit var mockPresenter: Current.Presenter

    @get:Rule
    val activityRule = object: ActivityTestRule<EmptyActivity>(EmptyActivity::class.java) {
        /**
         * This section of the test refreshes the [mockPresenter] object and
         * the [eventSubject] object. The [mockPresenter] will be registered
         * with the [FakeComponents] object registry in order to substitute
         * [mockPresenter] for the [Current.Presenter]. In this way, the
         * application's object graph need not be built just to test the UI.
         */
        override fun beforeActivityLaunched() {
            eventSubject = PublishSubject.create()
            mockPresenter = mockk(relaxed = true)
            every { mockPresenter.eventObservable() } returns eventSubject
            FakeComponents.get().addToInjectionRegistry(Current.Presenter::class.java, mockPresenter)
        }

        /**
         * After the test is finished, the previous [mockPresenter] object is
         * removed from the injection registry in [FakeComponents].
         */
        override fun afterActivityFinished() {
            FakeComponents.get().removeFromInjectionRegistry<Current.Presenter>(Current.Presenter::class.java)
        }

        /**
         * Directs [EmptyActivity] to set content view to
         * [R.layout.activity_test_current_fragment] so that the
         * [CurrentFragment] can load
         */
        override fun getActivityIntent(): Intent {
            return EmptyActivity.intent(getTargetContext(), R.layout.activity_test_current_fragment)
        }
    }

    /**
     * If [CurrentFragment] were to call these methods in the opposite order,
     * then the presenter would not be able to immediately send events in the
     * [Current.Presenter.onReady] method. With this order, the
     * [Current.Presenter] can be sure that when [Current.Presenter.onReady] is
     * called, all events will be received.
     */
    @Test
    fun shouldAcquireEventObservableAndThenInformPresenterOfReadiness() {
        verifyOrder {
            mockPresenter.eventObservable()
            mockPresenter.onReady()
        }
    }

    /**
     * When the user details are sent up to the view, this validates that the
     * view shows the correct information. Furthermore, it validates that the
     * other states (empty and loading) are not rendered.
     */
    @Test
    fun shouldShowCorrectInformationOnShowUserEvent() {
        val userDetails = userDetails {
            userName = "pedlar"
            avatarBytes = ViewTestUtil.bytesOfDrawable(getContext(), pedlarDrawable())
            location = "Austin, TX"
            email = "madison@something.com"
        }
        inject(CurrentViewEvent.forUserDetails(userDetails))

        onAvatar().check(matches(isDisplayed()))
        // Not checking avatar because, in practice it loads, but the pixels
        // are slightly different
        onUserNameText().check(matches(isDisplayed()))
        onUserNameText().check(matches(withText(userDetails.userName)))
        onUserEmailText().check(matches(isDisplayed()))
        onUserEmailText().check(matches(withText(userDetails.email)))
        onUserLocationText().check(matches(isDisplayed()))
        onUserLocationText().check(matches(withText(userDetails.location)))

        onCurrentMemberDataEmpty().check(matches(not(isDisplayed())))
        onProgressSpinner().check(matches(not(isDisplayed())))
    }

    /**
     * Validates that the view appropriately renders the event where the
     * current user details are empty. Furthermore, it validates that the
     * other states (nonempty and loading) are not rendered.
     */
    @Test
    fun shouldShowEmptyViewWhenNoDetailsLoaded() {
        inject(CurrentViewEvent.empty())

        onCurrentMemberDataEmpty().check(matches(isDisplayed()))

        onProgressSpinner().check(matches(not(isDisplayed())))
        checkUserDetailsNotDisplayed()
    }

    /**
     * Validates that the view appropriately renders the event where the
     * data is currently loading. Furthermore, it validates that the
     * other states (nonempty and empty) are not rendered.
     */
    @Test
    fun shouldShowSpinnerWhenLoading() {
        inject(CurrentViewEvent.forDataLoading())

        onProgressSpinner().check(matches(isDisplayed()))

        onCurrentMemberDataEmpty().check(matches(not(isDisplayed())))
        checkUserDetailsNotDisplayed()
    }

    private fun onAvatar() = onView(withId(R.id.memberAvatarImage))
    private fun onUserNameText() = onView(withId(R.id.memberLoginText))
    private fun onUserEmailText() = onView(withId(R.id.memberEmailText))
    private fun onUserLocationText() = onView(withId(R.id.memberLocationText))
    private fun onCurrentMemberDataEmpty() = onView(withId(R.id.currentMemberDataEmptyText))
    private fun onProgressSpinner() = onView(withId(R.id.currentMemberProgressSpinner))

    private fun checkUserDetailsNotDisplayed() {
        onAvatar().check(matches(not(isDisplayed())))
        onUserNameText().check(matches(not(isDisplayed())))
        onUserEmailText().check(matches(not(isDisplayed())))
        onUserLocationText().check(matches(not(isDisplayed())))
    }

    private fun inject(event: CurrentViewEvent) {
        eventSubject.onNext(event)
    }
}