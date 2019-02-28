package com.fsryan.repostalker.rx

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import java.lang.UnsupportedOperationException

class AlarmingDisposableCompletableObserverTest {

    @Test
    @DisplayName("AlarmingDisposableCompletableObserver should not allow error to be called.")
    fun shouldThrowOnError() {
        assertThrows(IllegalStateException::class.java) {
            throwsOnNextAlarmingDisposableCompletableObserver().onError(Exception())
        }
    }

    private fun throwsOnNextAlarmingDisposableCompletableObserver() = object: AlarmingDisposableCompletableObserver() {
        override fun onComplete() = throw UnsupportedOperationException()
    }
}