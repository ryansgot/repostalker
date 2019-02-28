package com.fsryan.repostalker.rx

import io.reactivex.observers.DisposableObserver
import java.lang.IllegalStateException

abstract class AlarmingDisposableObserver<T> : DisposableObserver<T>() {
    final override fun onComplete() {
        throw IllegalStateException("AlarmingDisposableObservers are not allowed to complete. They must simply fall out of scope and be cleaned up.")
    }

    final override fun onError(e: Throwable) {
        throw IllegalStateException("AlarmingDisposableObservers do not allow errors", e)
    }
}