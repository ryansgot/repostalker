package com.fsryan.repostalker.interactor

import com.fsryan.repostalker.testonly.RxJavaPluginsExtension
import io.mockk.every
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(RxJavaPluginsExtension::class)
class NavInteractorTest : BaseInteractorTest<NavInteractor>() {

    private lateinit var testObserver: TestObserver<String>

    @BeforeEach
    fun setUpTestObserverandMakeNavigatorPushComplete() {
        testObserver = TestObserver()
        every { mockNavigator.push(any()) } returns Single.just("0|me")
    }

    @AfterEach
    fun flushTestObserver() {
        testObserver.dispose()
    }

    @Test
    fun shouldCompleteOnEmittingRegisteredItem() {
        every { mockNavigator.backEventObservable() } returns Observable.just("0|me")

        interactorUnderTest().pushAndRegisterBackNavInterest("me").subscribe(testObserver)

        testObserver.assertComplete()
    }

    @Test
    fun shouldNotCompleteWhenUnregisteredIDPublished() {
        every { mockNavigator.backEventObservable() } returns Observable.just("not me")

        interactorUnderTest().pushAndRegisterBackNavInterest("me").subscribe(testObserver)

        testObserver.assertNotComplete()
    }

    @Test
    fun shouldPassThroughRequestToPopFromNavStack() {
        every { mockNavigator.pop() } returns Single.just("something")

        interactorUnderTest().requestBackNav().subscribe(testObserver)

        verify { mockNavigator.pop() }
        testObserver.assertValue("something")
    }
}