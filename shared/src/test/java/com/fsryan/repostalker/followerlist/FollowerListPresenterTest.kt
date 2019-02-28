package com.fsryan.repostalker.followerlist

import com.fsryan.repostalker.data.AdapterFactory
import com.fsryan.repostalker.data.GithubMember
import com.fsryan.repostalker.followerlist.event.FollowerListViewEvent
import com.fsryan.repostalker.followerlist.event.toFollowerDetails
import com.fsryan.repostalker.main.Main
import com.fsryan.repostalker.testonly.RetrofitHelper
import com.fsryan.repostalker.testonly.RxJavaPluginsExtension
import com.fsryan.testtools.Fixtures
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verifyOrder
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(RxJavaPluginsExtension::class, MockKExtension::class)
class FollowerListPresenterTest {

    @RelaxedMockK
    private lateinit var mockInteractor: FollowerList.Interactor
    private lateinit var testObserver: TestObserver<FollowerListViewEvent>
    private lateinit var presenterUnderTest: FollowerList.Presenter

    companion object {
        private lateinit var bypassLaneFollowers: List<GithubMember>

        @BeforeAll
        @JvmStatic
        fun initBypasslaneFollowers() {
            bypassLaneFollowers = Fixtures(Moshi.Builder()
                    .add(AdapterFactory())
                    .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                    .build()).getList("bypasslane_members.json", GithubMember::class.java)
        }
    }

    @BeforeEach
    fun subscribeTestObserver() {
        presenterUnderTest = FollowerList.createPresenter(mockInteractor)

        testObserver = TestObserver()
        presenterUnderTest.eventObservable().subscribe(testObserver)
    }

    @AfterEach
    fun flushTestObserver() {
        testObserver.dispose()
    }

    @Test
    @DisplayName("Should request followers of bypasslane when last stack entry was ${Main.MAIN_STACK_ENTRY}; should not register back nav interest")
    fun onReadyWhenNoStackEntries() {
        every { mockInteractor.fetchLastUserRequested() } returns Single.just(Main.MAIN_STACK_ENTRY)

        presenterUnderTest.onReady()

        verifyOrder {
            mockInteractor.fetchLastUserRequested()
            mockInteractor.fetchFollowersOf(eq(Main.DEFAULT_USER))
        }
    }

    @Test
    @DisplayName("Should request followers of bypasslane on error retrieving last stack entry; should not register back nav interest")
    fun onReadyOnErrorRetrievingLastStackEntry() {
        every { mockInteractor.fetchLastUserRequested() } returns Single.error(Exception())

        presenterUnderTest.onReady()

        verifyOrder {
            mockInteractor.fetchLastUserRequested()
            mockInteractor.fetchFollowersOf(eq(Main.DEFAULT_USER))
        }
    }

    @Test
    @DisplayName("Should request followers of user on last stack entry not main; should register back nav interest")
    fun onReadyStackEntryRetrieved() {
        val lastStackEntry = "something"
        every { mockInteractor.fetchLastUserRequested() } returns Single.just(lastStackEntry)
        every { mockInteractor.registerBackNavInterest(any()) } returns Completable.never()

        presenterUnderTest.onReady()

        verifyOrder {
            mockInteractor.fetchLastUserRequested()
            mockInteractor.registerBackNavInterest(eq(lastStackEntry))
            mockInteractor.fetchFollowersOf(eq(lastStackEntry))
        }
    }

    @Test
    @DisplayName("Should Request follower registering back nav interest")
    fun userRequestedFollower() {
        val requested = "something"
        every { mockInteractor.fetchLastUserRequested() } returns Single.just(Main.MAIN_STACK_ENTRY)
        every { mockInteractor.fetchAvatarImage(any()) } returns Single.just(RetrofitHelper.byteArray)
        every { mockInteractor.pushAndRegisterBackNavInterest(any()) } returns Completable.never()
        every { mockInteractor.registerBackNavInterest(any()) } returns Completable.never()
        every { mockInteractor.fetchFollowersOf(any()) } returns Observable.fromIterable(bypassLaneFollowers)
        every { mockInteractor.storeUserSelectedAction(any()) } returns Completable.complete()

        presenterUnderTest.onReady()
        presenterUnderTest.userRequestedFollower(requested)

        verifyOrder {
            mockInteractor.storeUserSelectedAction(eq(requested))
            mockInteractor.pushAndRegisterBackNavInterest(eq(requested))
            mockInteractor.fetchFollowersOf(eq(requested))
        }
    }

    @Test
    @DisplayName("Should filter when filter requested--also should not register back nav interest")
    fun userRequestedFollowerFilter() {
        val requestedFilter = "p"
        every { mockInteractor.fetchLastUserRequested() } returns Single.just(Main.MAIN_STACK_ENTRY)

        presenterUnderTest.onReady()
        presenterUnderTest.userRequestedFollowerListFilter(requestedFilter)

        verifyOrder {
            mockInteractor.fetchLastUserRequested()
            mockInteractor.fetchFollowersOf(eq(Main.DEFAULT_USER))
            mockInteractor.fetchLastUserRequested()
            mockInteractor.fetchFollowersOf(eq(Main.DEFAULT_USER), eq(requestedFilter))
        }
    }

    @Test
    @DisplayName("Should emit events relating to fetching followers of user in correct order")
    fun emitFollowers() {
        every { mockInteractor.fetchLastUserRequested() } returns Single.just(Main.MAIN_STACK_ENTRY)
        every { mockInteractor.fetchAvatarImage(any()) } returns Single.just(RetrofitHelper.byteArray)
        every { mockInteractor.pushAndRegisterBackNavInterest(any()) } returns Completable.never()
        every { mockInteractor.registerBackNavInterest(any()) } returns Completable.never()
        every { mockInteractor.fetchFollowersOf(any()) } returns Observable.fromIterable(bypassLaneFollowers)

        presenterUnderTest.onReady()

        testObserver.awaitCount(4)
        testObserver.assertValues(
            FollowerListViewEvent.forLoading(),
            FollowerListViewEvent.forShowingNewList(clearFilterText = true, firstFollowerDetails = bypassLaneFollowers[0].toFollowerDetails(RetrofitHelper.byteArray)),
            FollowerListViewEvent.forAddingAFollower(followerDetails = bypassLaneFollowers[1].toFollowerDetails(RetrofitHelper.byteArray)),
            FollowerListViewEvent.forFinishedAddingFollowers(hasLoadedFollowers = true)
        )
    }

    @Test
    @DisplayName("Should emit events relating to fetching followers of user with filter in correct order")
    fun emitFollowersWithFilter() {
        every { mockInteractor.fetchLastUserRequested() } returns Single.just(Main.MAIN_STACK_ENTRY)
        every { mockInteractor.fetchAvatarImage(any()) } returns Single.just(RetrofitHelper.byteArray)
        every { mockInteractor.pushAndRegisterBackNavInterest(any()) } returns Completable.never()
        every { mockInteractor.registerBackNavInterest(any()) } returns Completable.never()
        every { mockInteractor.fetchFollowersOf(any()) } returns Observable.fromIterable(bypassLaneFollowers)
        every { mockInteractor.fetchFollowersOf(any(), eq("p")) } returns Observable.just(bypassLaneFollowers[0])

        presenterUnderTest.onReady()
        presenterUnderTest.userRequestedFollowerListFilter("p")

        testObserver.awaitCount(7)
        testObserver.assertValues(
            FollowerListViewEvent.forLoading(),
            FollowerListViewEvent.forShowingNewList(clearFilterText = true, firstFollowerDetails = bypassLaneFollowers[0].toFollowerDetails(RetrofitHelper.byteArray)),
            FollowerListViewEvent.forAddingAFollower(followerDetails = bypassLaneFollowers[1].toFollowerDetails(RetrofitHelper.byteArray)),
            FollowerListViewEvent.forFinishedAddingFollowers(hasLoadedFollowers = true),
            FollowerListViewEvent.forLoading(clearFilterText = false),
            FollowerListViewEvent.forShowingNewList(clearFilterText = false, firstFollowerDetails = bypassLaneFollowers[0].toFollowerDetails(RetrofitHelper.byteArray)),
            FollowerListViewEvent.forFinishedAddingFollowers(hasLoadedFollowers = true)
        )
    }

    @Test
    @DisplayName("Should emit correct event to show no followers when none returned")
    fun emitNoFollowersOnRequest() {
        every { mockInteractor.fetchLastUserRequested() } returns Single.just(Main.MAIN_STACK_ENTRY)
        every { mockInteractor.fetchAvatarImage(any()) } returns Single.just(RetrofitHelper.byteArray)
        every { mockInteractor.pushAndRegisterBackNavInterest(any()) } returns Completable.never()
        every { mockInteractor.registerBackNavInterest(any()) } returns Completable.never()
        every { mockInteractor.fetchFollowersOf(any()) } returns Observable.empty()

        presenterUnderTest.onReady()

        testObserver.awaitCount(2)
        testObserver.assertValues(
            FollowerListViewEvent.forLoading(),
            FollowerListViewEvent.forFinishedAddingFollowers(hasLoadedFollowers = false)
        )
    }

    @Test
    @DisplayName("Should emit correct event when fetching followers errors without follower")
    fun emitErrorMessageOnFetchFollowersFailure() {
        every { mockInteractor.fetchLastUserRequested() } returns Single.just(Main.MAIN_STACK_ENTRY)
        every { mockInteractor.fetchAvatarImage(any()) } returns Single.just(RetrofitHelper.byteArray)
        every { mockInteractor.pushAndRegisterBackNavInterest(any()) } returns Completable.never()
        every { mockInteractor.registerBackNavInterest(any()) } returns Completable.never()
        every { mockInteractor.fetchFollowersOf(any()) } returns Observable.error(Exception("message"))

        presenterUnderTest.onReady()

        testObserver.awaitCount(2)
        testObserver.assertValues(
            FollowerListViewEvent.forLoading(),
            FollowerListViewEvent.forErrorMessage(hasLoadedFollowers = false, message = "message")
        )
    }
}