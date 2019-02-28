package com.fsryan.repostalker.current

import com.fsryan.repostalker.current.event.CurrentViewEvent
import com.fsryan.repostalker.current.event.toUserDetails
import com.fsryan.repostalker.data.AdapterFactory
import com.fsryan.repostalker.data.GithubUser
import com.fsryan.repostalker.testonly.RetrofitHelper
import com.fsryan.repostalker.testonly.RxJavaPluginsExtension
import com.fsryan.testtools.Fixtures
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(RxJavaPluginsExtension::class, MockKExtension::class)
class CurrentPresenterTest {

    @RelaxedMockK
    private lateinit var mockInteractor: Current.Interactor
    private lateinit var testObserver: TestObserver<CurrentViewEvent>
    private lateinit var presenterUnderTest: Current.Presenter
    private lateinit var userSelectedObservable: PublishSubject<String>

    companion object {
        private lateinit var bypasslaneUser: GithubUser

        @BeforeAll
        @JvmStatic
        fun initBypasslaneFollowers() {
            bypasslaneUser = Fixtures(
                Moshi.Builder()
                    .add(AdapterFactory())
                    .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                    .build()).get("bypasslane_user.json", GithubUser::class.java)
        }
    }

    @BeforeEach
    fun subscribeTestObserver() {
        userSelectedObservable = PublishSubject.create<String>()
        every { mockInteractor.fetchLastUserRequested() } returns Single.just(bypasslaneUser.login)
        every { mockInteractor.fetchGithubUser(any()) } returns Single.just(bypasslaneUser)
        every { mockInteractor.fetchAvatarImage(any()) } returns Single.just(RetrofitHelper.byteArray)
        every { mockInteractor.userSelectedObservable() } returns userSelectedObservable

        presenterUnderTest = Current.createPresenter(mockInteractor)

        testObserver = TestObserver()
        presenterUnderTest.eventObservable().subscribe(testObserver)
    }

    @AfterEach
    fun flushTestObserver() {
        testObserver.dispose()
    }

    @Test
    @DisplayName("Should initially send loading event and then event for user details")
    fun onReady() {
        presenterUnderTest.onReady()

        testObserver.awaitCount(2)
        testObserver.assertValues(
            CurrentViewEvent.forDataLoading(),
            CurrentViewEvent.forUserDetails(bypasslaneUser.toUserDetails(RetrofitHelper.byteArray))
        )
    }

    @Test
    @DisplayName("Should NOT register for back button press when last user is bypasslane user")
    fun onReadyNotListeningToBackEvents() {
        presenterUnderTest.onReady()

        verify(exactly = 0) { mockInteractor.registerBackNavInterest(any(), any()) }
    }

    @Test
    @DisplayName("Should register for back button press when last user is bypasslane user")
    fun onReadyListeningToBackEvents() {
        val expected = "some other login"
        every { mockInteractor.fetchLastUserRequested() } returns Single.just(expected)
        every { mockInteractor.registerBackNavInterest(any(), any()) } returns Completable.never()

        presenterUnderTest.onReady()

        verify(exactly = 1) { mockInteractor.registerBackNavInterest(eq(expected), eq(false)) }
    }

    @Test
    @DisplayName("Should send event when registered to back button presses")
    fun onReadyListeningToBackEventsGetsBackEvent() {
        val expected = "some other login"
        every { mockInteractor.fetchLastUserRequested() } returns Single.just(expected)
        every { mockInteractor.registerBackNavInterest(eq(expected), any()) } returns Completable.complete() andThen Completable.never()

        presenterUnderTest.onReady()

        testObserver.awaitCount(4)
        testObserver.assertValues(
            CurrentViewEvent.forDataLoading(),
            CurrentViewEvent.forUserDetails(bypasslaneUser.toUserDetails(RetrofitHelper.byteArray)),
            // the above values are for onReady
            // the below values are for respondingto the back event
            CurrentViewEvent.forDataLoading(),
            CurrentViewEvent.forUserDetails(bypasslaneUser.toUserDetails(RetrofitHelper.byteArray))
        )
    }
}