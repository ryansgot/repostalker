package com.fsryan.repostalker.rx

import io.reactivex.observers.DisposableSingleObserver
import java.lang.IllegalStateException

abstract class AlarmingDisposableSingleObserver<T> : DisposableSingleObserver<T>() {
    final override fun onError(e: Throwable) {
        throw IllegalStateException("AlarmingDisposableSingleObservers do not allow errors", e)
    }
}