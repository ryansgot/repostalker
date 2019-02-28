package com.fsryan.repostalker.followerlist

import android.content.Intent
import android.support.annotation.DrawableRes
import android.support.test.InstrumentationRegistry.getContext
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.RootMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.fsryan.repostalker.FakeComponents
import com.fsryan.repostalker.R
import com.fsryan.repostalker.followerlist.event.FollowerListViewEvent
import com.fsryan.repostalker.followerlist.event.followerListViewEvent
import com.fsryan.repostalker.testonly.EmptyActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import io.reactivex.subjects.PublishSubject
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.support.test.espresso.matcher.ViewMatchers.hasDescendant
import com.fsryan.repostalker.test.RecyclerViewInteraction
import android.support.test.espresso.matcher.ViewMatchers
import android.widget.ImageView
import com.fsryan.repostalker.followerlist.event.FollowerDetails
import com.fsryan.repostalker.followerlist.event.followerDetails
import com.fsryan.repostalker.test.ViewTestUtil
import com.fsryan.repostalker.test.ViewTestUtil.*
import io.mockk.verify
import org.hamcrest.Matchers.allOf
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class FollowerListFragmentTest {

    companion object {
        @DrawableRes val followerAvatarIcons = IntArray(3) {
            when (it) {
                0 -> pedlarDrawable()
                1 -> ryansgotDrawable()
                2 -> steveChalkerDrawable()
                else -> throw IllegalStateException()
            }
        }
        val followerEmails = listOf("madison@something.com", "steve@something.com", "ryan@something.com")
        val followerLocations = listOf("Austin, TX", "Seattle, WA", "North Pole")
        val followerLogins = listOf("pedlar", "ryansgot", "steveChalker")
    }

    lateinit var eventSubject: PublishSubject<FollowerListViewEvent>
    lateinit var mockPresenter: FollowerList.Presenter

    @get:Rule
    val activityRule = object: ActivityTestRule<EmptyActivity>(EmptyActivity::class.java) {
        /**
         * This section of the test refreshes the [mockPresenter] object and
         * the [eventSubject] object. The [mockPresenter] will be registered
         * with the [FakeComponents] object registry in order to substitute
         * [mockPresenter] for the [FollowerList.Presenter]. In this way, the
         * application's object graph need not be built just to test the UI.
         */
        override fun beforeActivityLaunched() {
            eventSubject = PublishSubject.create()
            mockPresenter = mockk(relaxed = true)
            every { mockPresenter.eventObservable() } returns eventSubject
            FakeComponents.get().addToInjectionRegistry(FollowerList.Presenter::class.java, mockPresenter)
        }

        /**
         * After the test is finished, the previous [mockPresenter] object is
         * removed from the injection registry in [FakeComponents].
         */
        override fun afterActivityFinished() {
            FakeComponents.get().removeFromInjectionRegistry<FollowerList.Presenter>(FollowerList.Presenter::class.java)
        }

        /**
         * Directs [EmptyActivity] to set content view to
         * [R.layout.activity_test_FollowerList_fragment] so that the
         * [FollowerListFragment] can load
         */
        override fun getActivityIntent(): Intent {
            return EmptyActivity.intent(getTargetContext(), R.layout.activity_test_follower_list_fragment)
        }
    }

    /**
     * If [FollowerListFragment] were to call these methods in the opposite
     * order, then the presenter would not be able to immediately send events
     * in the [FollowerList.Presenter.onReady] method. With this order, the
     * [FollowerList.Presenter] can be sure that when
     * [FollowerList.Presenter.onReady] is called, all events will be received.
     */
    @Test
    fun shouldAcquireEventObservableAndThenInformPresenterOfReadiness() {
        verifyOrder {
            mockPresenter.eventObservable()
            mockPresenter.onReady()
        }
    }

    /**
     * The initial state of the filtering edit text should be disabled and the
     * intial state of the list should be not displayed.
     */
    @Test
    fun shouldHaveFilteringDisabledAndRecyclerGoneByDefault() {
        onFilterText().check(matches(withText("")))
        onFilterText().check(matches(not(isEnabled())))
        onFilterButton().check(matches(isEnabled()))
        onRecyclerView().check(matches(not(isDisplayed())))
        onProgressSpinner().check(matches(not(isDisplayed())))
    }

    /**
     * Validates that disabling filtering results in the filtering edit text
     * becoming disabled
     */
    @Test
    fun shouldDisableFilteringTextOnDisableFilteringEventClearingText() {
        inject(toggleFilteringEvent(enable = true))
        onFilterText().perform(replaceText("some text"))
        inject(toggleFilteringEvent(enable = false))

        onFilterText().check(matches(withText("")))
        onFilterText().check(matches(not(isEnabled())))
        onFilterButton().check(matches(not(isEnabled())))
    }

    /**
     * Validates that filtering will become enabled on receiving the filtering
     * enabled event.
     */
    @Test
    fun shouldEnableFilteringTextOnEnableFilteringEventClearingText() {
        inject(toggleFilteringEvent(enable = true))
        onFilterText().perform(replaceText("some text"))

        onFilterText().check(matches(withText("some text")))
        onFilterText().check(matches(isEnabled()))
        onFilterButton().check(matches(isEnabled()))
    }

    /**
     * Validates that showing a new list without  new follower details will not
     * display the list, but will display the empty text. Furthermore,
     * validates that the filter text will be cleared and disabled.
     */
    @Test
    fun shouldShowEmptyOnShowListEventWithoutFollowerDetails() {
        inject(FollowerListViewEvent.forShowingNewList())

        onEmptyFollowersText().check(matches(isDisplayed()))

        onRecyclerView().check(matches(not(isDisplayed())))
        onProgressSpinner().check(matches(not(isDisplayed())))
        onFilterText().check(matches(not(isEnabled())))
        onFilterButton().check(matches(not(isEnabled())))
        onFilterText().check(matches(withText("")))
    }

    /**
     * Validates that showing a new list with new follower details will show
     * a new followers list. Furthermore, validates that the filter text will
     * be cleared and enabled and that the empty text will not be displayed.
     */
    @Test
    fun shouldShowFollowersListOnNewNonemptyList() {
        val followerDetails = randomFollower()
        inject(FollowerListViewEvent.forShowingNewList(clearFilterText = true, firstFollowerDetails = followerDetails))

        onRecyclerView().check(matches(isDisplayed()))
        checkRecyclerViewItems(listOf(followerDetails))

        onEmptyFollowersText().check(matches(not(isDisplayed())))
        onProgressSpinner().check(matches(not(isDisplayed())))
        onFilterText().check(matches(isEnabled()))
        onFilterButton().check(matches(isEnabled()))
        onFilterText().check(matches(withText("")))
    }

    /**
     * Should correctly show follower details when email, location, and avatar
     * unknown.
     */
    @Test
    fun shouldShowFollowerListItemWithUnkownEmailLocationAvatar() {
        val followerDetails = followerDetails { userName = "secretive user" }
        inject(FollowerListViewEvent.forShowingNewList(clearFilterText = true, firstFollowerDetails = followerDetails))

        onRecyclerView().check(matches(isDisplayed()))
        checkRecyclerViewItems(listOf(followerDetails))
    }

    /**
     * Validates that sending multiple followers will appropriately populate the
     * recyclerview.
     */
    @Test
    fun shouldShowManyFollowersInRecyclerView() {
        val expected = List(32) { randomFollower() }
        inject(FollowerListViewEvent.forShowingNewList(clearFilterText = true, firstFollowerDetails = expected[0]))
        for (i in 1 until expected.size) {
            inject(FollowerListViewEvent.forAddingAFollower(expected[i]))
        }

        onRecyclerView().check(matches(isDisplayed()))
        checkRecyclerViewItems(expected)
    }

    /**
     * Validates that typing text into the filter text will result in the view
     * telling the presenter that the user requested follower list filtering.
     */
    @Test
    fun shouldInformPresenterWhenFilterTextChanges() {
        inject(toggleFilteringEvent(enable = true))
        onFilterText().perform(typeText("Hello, World!"))
        onFilterButton().perform(click())
        Espresso.closeSoftKeyboard()
        verify { mockPresenter.userRequestedFollowerListFilter("Hello, World!") }
    }

    /**
     * Validates that the progress spinner is displayed when the loading event
     * is received
     */
    @Test
    fun shouldShowLoadingWhenLoadingEventReceived() {
        inject(FollowerListViewEvent.forLoading())

        onProgressSpinner().check(matches(isDisplayed()))

        onFilterText().check(matches(not(isEnabled())))
        onFilterButton().check(matches(not(isEnabled())))
        onRecyclerView().check(matches(not(isDisplayed())))
        onEmptyFollowersText().check(matches(not(isDisplayed())))
    }

    /**
     * Validates that the error message will get shown appropriately in a toast
     * and that if no followers were loaded, the empty text is shown
     */
    @Test
    fun shouldShowErrorMessageInToastOnErrorEventAndEmptyTextWhenNoFollowersLoaded() {
        val errorMessage = "Some error Message"
        inject(FollowerListViewEvent.forErrorMessage(false, errorMessage))

        onEmptyFollowersText().check(matches(isDisplayed()))
        onView(withText(errorMessage)).inRoot(RootMatchers.isSystemAlertWindow())
            .check(matches(isDisplayed()))

        onProgressSpinner().check(matches(not(isDisplayed())))
        onFilterText().check(matches(not(isEnabled())))
        onFilterButton().check(matches(not(isEnabled())))
        onRecyclerView().check(matches(not(isDisplayed())))
    }

    /**
     * Validates that the error message will get shown appropriately in a toast
     * after followers have been loaded and that the recycler view is displayed
     * and that the filter text is enabled
     */
    @Test
    fun shouldShowErrorMessageInToastOnErrorEventAndEmptyTextWhenFollowersLoaded() {
        val errorMessage = "Some error Message"
        inject(FollowerListViewEvent.forShowingNewList(clearFilterText = true, firstFollowerDetails = randomFollower()))
        inject(FollowerListViewEvent.forErrorMessage(true, errorMessage))

        onRecyclerView().check(matches(isDisplayed()))
        onFilterText().check(matches(isEnabled()))
        onFilterButton().check(matches(isEnabled()))
        onView(withText(errorMessage)).inRoot(RootMatchers.isSystemAlertWindow())
            .check(matches(isDisplayed()))

        onProgressSpinner().check(matches(not(isDisplayed())))
        onEmptyFollowersText().check(matches(not(isDisplayed())))
    }

    private fun onFilterText() = onView(withId(R.id.filterTextEntry))
    private fun onFilterButton() = onView(withId(R.id.followerFilterButton))
    private fun onRecyclerView() = onView(withId(R.id.followerRecyclerView))
    private fun onEmptyFollowersText() = onView(withId(R.id.emptyFollowersText))
    private fun onProgressSpinner() = onView(withId(R.id.followerListProgressSpinner))

    private fun checkRecyclerViewItems(expected: List<FollowerDetails>) {
        RecyclerViewInteraction.onRecyclerView<FollowerDetails>(withId(R.id.followerRecyclerView))
            .withItems(expected)
            .check { item, view, noMatchingViewException ->
                // This is not a strong verification.
                // TODO: See if you can figure out why DrawableMatcher fails
                matches(hasDescendant(ViewMatchers.isAssignableFrom(ImageView::class.java)))
                    .check(view, noMatchingViewException)

                val expectedEmail = if (item.email == null) "" else item.email
                matches(hasDescendant(allOf(withId(R.id.followerItemEmail), withText(expectedEmail))))
                    .check(view, noMatchingViewException)

                val expectedLocation = if (item.location == null) "" else item.location
                matches(hasDescendant(allOf(withId(R.id.followerItemLocation), withText(expectedLocation))))
                    .check(view, noMatchingViewException)

                matches(hasDescendant(allOf(withId(R.id.followerItemUserName), withText(item.userName))))
                    .check(view, noMatchingViewException)
            }
    }

    private fun toggleFilteringEvent(enable: Boolean) = followerListViewEvent {
        showList = false
        showEmpty = false
        showLoading = false
        enableFiltering = enable
        disableFiltering = !enable
        clearFilterText = true
        clearList = false
    }

    private fun randomFollower() = followerDetails {
        avatarBytes = ViewTestUtil.bytesOfDrawable(getContext(), followerAvatarIcons[Random.nextInt(3)])
        email = followerEmails[Random.nextInt(3)]
        location = followerLocations[Random.nextInt(3)]
        userName = followerLogins[Random.nextInt(3)]
    }

    private fun inject(event: FollowerListViewEvent) {
        eventSubject.onNext(event)
    }
}