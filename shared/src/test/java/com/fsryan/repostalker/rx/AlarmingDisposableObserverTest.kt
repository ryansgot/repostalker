package com.fsryan.repostalker.rx

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import java.lang.UnsupportedOperationException

class AlarmingDisposableObserverTest {

    @Test
    @DisplayName("AlarmingDisposableObserver should not allow onComplete to be called.")
    fun shouldThrowWhenCompleting() {
        assertThrows(IllegalStateException::class.java) {
            throwsOnNextAlarmingDisposableObserver<Any>().onComplete()
        }
    }

    @Test
    @DisplayName("AlarmingDisposableObserver should not allow error to be called.")
    fun shouldThrowOnError() {
        assertThrows(IllegalStateException::class.java) {
            throwsOnNextAlarmingDisposableObserver<Any>().onError(Exception())
        }
    }

    private fun <T> throwsOnNextAlarmingDisposableObserver() = object: AlarmingDisposableObserver<T>() {
        override fun onNext(t: T) = throw UnsupportedOperationException()
    }
}