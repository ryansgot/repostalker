package com.fsryan.repostalker.rx

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import java.lang.UnsupportedOperationException

class AlarmingDisposableSingleObserverTest {

    @Test
    @DisplayName("AlarmingDisposableSingleObserver should not allow error to be called.")
    fun shouldThrowOnError() {
        assertThrows(IllegalStateException::class.java) {
            throwsOnNextAlarmingDisposableSingleObserver<Any>().onError(Exception())
        }
    }

    private fun <T> throwsOnNextAlarmingDisposableSingleObserver() = object: AlarmingDisposableSingleObserver<T>() {
        override fun onSuccess(t: T) = throw UnsupportedOperationException()
    }
}