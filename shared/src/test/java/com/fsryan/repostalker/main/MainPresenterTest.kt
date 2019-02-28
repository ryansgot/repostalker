package com.fsryan.repostalker.main

import com.fsryan.repostalker.main.event.MainViewEvent
import com.fsryan.repostalker.testonly.RxJavaPluginsExtension
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

@ExtendWith(RxJavaPluginsExtension::class, MockKExtension::class)
class MainPresenterTest {

    @RelaxedMockK private lateinit var mockInteractor: Main.Interactor
    private lateinit var testObserver: TestObserver<MainViewEvent>
    private lateinit var presenterUnderTest: Main.Presenter

    companion object {
        @JvmStatic
        fun invalidCacheInvalidationInput() = listOf(
            arguments("42.3.1", MainViewEvent.forHidingSettings("Unable to store cache invalidation interval: 42.3.1")),
            arguments("-42", MainViewEvent.forHidingSettings("Invalid cache invalidation interval (-42); storing lowest allowed (${Main.LOWEST_INVALIDATION_INTERVAL})")),
            arguments((Main.LOWEST_INVALIDATION_INTERVAL - 1).toString(), MainViewEvent.forHidingSettings("Invalid cache invalidation interval (${Main.LOWEST_INVALIDATION_INTERVAL - 1}); storing lowest allowed (10)"))      // <-- too low
        )
    }

    @BeforeEach
    fun subscribeTestObserver() {
        presenterUnderTest = Main.createPresenter(mockInteractor)

        testObserver = TestObserver()
        presenterUnderTest.eventObservable().subscribe(testObserver)
    }

    @AfterEach
    fun flushTestObserver() {
        testObserver.dispose()
    }

    @Test
    @DisplayName("Should register for back navigation on ready")
    fun onReady() {
        every { mockInteractor.pushAndRegisterBackNavInterest(any()) } returns Completable.never()

        presenterUnderTest.onReady()

        verify { mockInteractor.pushAndRegisterBackNavInterest(eq(Main.MAIN_STACK_ENTRY)) }
    }

    @Test
    @DisplayName("Should send back event on receiving back event")
    fun propagateBack() {
        every { mockInteractor.pushAndRegisterBackNavInterest(any()) } returns Completable.complete()

        presenterUnderTest.onReady()

        testObserver.awaitCount(1)
        testObserver.assertValue(MainViewEvent.forNavBack())
    }

    @Test
    @DisplayName("Should show settings with fetched value on fetch success")
    fun invalidationIntervalFetchSuccess() {
        every { mockInteractor.fetchCacheInvalidationInterval(any()) } returns Single.just(42000L)

        presenterUnderTest.userRequestedSettings()

        testObserver.awaitCount(1)
        testObserver.assertValue(MainViewEvent.forShowingSettings(42L))
    }

    @Test
    @DisplayName("Should show settings with default on fetch error")
    fun invalidationIntervalFetchFailure() {
        every { mockInteractor.fetchCacheInvalidationInterval(any()) } returns Single.error(Exception())

        presenterUnderTest.userRequestedSettings()

        testObserver.awaitCount(1)
        testObserver.assertValue(MainViewEvent.forShowingSettings(Main.DEFAULT_INVALIDATION_INTERVAL))
    }

    @Test
    @DisplayName("Should dismiss settings with no message on storage success")
    fun invalidationIntervalStorageSuccess() {
        every { mockInteractor.storeCacheInvalidationInterval(any()) } returns Completable.complete()

        presenterUnderTest.userSavedSettings("42")

        testObserver.awaitCount(1)
        testObserver.assertValue(MainViewEvent.forHidingSettings())
    }

    @Test
    @DisplayName("Should dismiss settings with correct message when storage fails")
    fun invalidationIntervalStorageFailure() {
        every { mockInteractor.storeCacheInvalidationInterval(any()) } returns Completable.error(Exception())

        presenterUnderTest.userSavedSettings("42")

        testObserver.awaitCount(1)
        testObserver.assertValue(MainViewEvent.forHidingSettings("Unable to store cache invalidation interval: 42"))
    }

    @ParameterizedTest
    @MethodSource("invalidCacheInvalidationInput")
    @DisplayName("Should dismiss settings with correct message when input invalid")
    fun invalidCacheInvalidationIntervalStorage(invalidInput: String, expectedEvent: MainViewEvent) {
        every { mockInteractor.storeCacheInvalidationInterval(any()) } returns Completable.complete()

        presenterUnderTest.userSavedSettings(invalidInput)

        testObserver.awaitCount(1)
        testObserver.assertValue(expectedEvent)
    }

    @Test
    @DisplayName("Should dismiss settings when user cancels")
    fun userCanceledSettings() {
        presenterUnderTest.userCanceledSettings()

        testObserver.awaitCount(1)
        testObserver.assertValue(MainViewEvent.forHidingSettings())
    }

    @Test
    @DisplayName("Should NOT propagate nav back when corresponding back nav single received--will receive back event via pushAndRegister mechanism")
    fun userRequestedBackNavSuccess() {
        every { mockInteractor.requestBackNav() } returns Single.just(Main.MAIN_STACK_ENTRY)

        presenterUnderTest.userRequestedBackNav()

        testObserver.assertNoValues()
    }

    @Test
    @DisplayName("Should propagate nav back when corresponding back nav single errors--will not receive back event via pushAndRegister mechanism")
    fun userRequestedBackNavError() {
        every { mockInteractor.requestBackNav() } returns Single.error(Exception())

        presenterUnderTest.userRequestedBackNav()

        testObserver.awaitCount(1)
        testObserver.assertValue(MainViewEvent.forNavBack())
    }
}