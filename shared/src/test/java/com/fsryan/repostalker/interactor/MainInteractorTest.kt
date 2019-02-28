package com.fsryan.repostalker.interactor

import com.fsryan.repostalker.main.Main
import com.fsryan.repostalker.testonly.RxJavaPluginsExtension
import io.mockk.every
import io.mockk.verify
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(RxJavaPluginsExtension::class)
class MainInteractorTest : BaseInteractorTest<Main.Interactor>() {

    private lateinit var testObserver: TestObserver<Long>

    @BeforeEach
    fun setUpTestObserver() {
        testObserver = TestObserver()
    }

    @AfterEach
    fun flushTestObserver() {
        testObserver.dispose()
    }

    @Test
    @DisplayName("should get cache invalidation interval from user prefs")
    fun shouldPassThroughRequestForCacheInvalidationIntervalToUserPrefs() {
        val expected = 42L
        val defaultVal = 0L
        every { mockUserPrefs.retrieveCacheInvalidationInterval(any()) } returns Single.just(expected)

        interactorUnderTest().fetchCacheInvalidationInterval(defaultVal).subscribe(testObserver)

        verify { mockUserPrefs.retrieveCacheInvalidationInterval(eq(defaultVal)) }
        testObserver.awaitTerminalEvent()
        testObserver.assertValue(expected)
    }
    @Test
    @DisplayName("errors fetching cache invalidation interval should be handled by the caller")
    fun shouldPropagateErrorOnFetchingCacheInvalidationInterval() {
        val expected = Exception()
        every { mockUserPrefs.retrieveCacheInvalidationInterval(any()) } returns Single.error(expected)

        interactorUnderTest().fetchCacheInvalidationInterval(42L).subscribe(testObserver)

        testObserver.awaitTerminalEvent()
        testObserver.assertError(expected)
    }

    @Test
    @DisplayName("should store cache invalidation interval in user prefs")
    fun shouldPassThroughStoreCacheInvalidationIntervalRequestToUserPrefs() {
        val toStore = 42L
        every { mockUserPrefs.storeCacheInvalidationInterval(any()) } returns Completable.complete()

        interactorUnderTest().storeCacheInvalidationInterval(toStore).subscribe(testObserver)

        verify { mockUserPrefs.storeCacheInvalidationInterval(eq(toStore)) }
        testObserver.assertComplete()
    }

    @Test
    @DisplayName("errors storing cache invalidation interval should be handled by the caller")
    fun shouldPropagateErrorOnStorageFailure() {
        val expected = Exception()
        every { mockUserPrefs.storeCacheInvalidationInterval(any()) } returns Completable.error(expected)

        interactorUnderTest().storeCacheInvalidationInterval(42L).subscribe(testObserver)

        testObserver.awaitTerminalEvent()
        testObserver.assertError(expected)
    }
}