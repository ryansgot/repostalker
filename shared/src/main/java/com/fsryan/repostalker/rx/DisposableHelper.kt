package com.fsryan.repostalker.rx

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

class DisposableHelper {
    companion object {
        /**
         * had a little difficulty here--was getting class cast exceptions when
         * passing in DisposableObserver<*>, but the below fixed it. There is
         * probably something about kotlin type system that I don't know here.
         */
        fun clearAndAdd(cd: CompositeDisposable, d: Disposable) {
            cd.clear()
            cd.add(d)
        }
    }
}