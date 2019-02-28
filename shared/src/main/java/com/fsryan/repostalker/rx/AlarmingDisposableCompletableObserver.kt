package com.fsryan.repostalker.rx

import io.reactivex.observers.DisposableCompletableObserver
import java.lang.IllegalStateException

abstract class AlarmingDisposableCompletableObserver : DisposableCompletableObserver() {
    final override fun onError(e: Throwable) {
        throw IllegalStateException("AlarmingDisposableCompletableObservers do not allow errors", e)
    }
}