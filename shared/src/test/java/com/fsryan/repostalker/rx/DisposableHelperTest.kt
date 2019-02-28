package com.fsryan.repostalker.rx

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DisposableHelperTest {

    @Test
    fun shouldDisposeAndThenAdd() {
        val cd = CompositeDisposable()
        val disposable = BasicDisposable()

        cd.add(disposable)
        DisposableHelper.clearAndAdd(cd, BasicDisposable())
        assertTrue(disposable.isDisposed)
        assertEquals(1, cd.size())
    }

    class BasicDisposable : Disposable {
        private var disposed = false
        override fun isDisposed() = disposed

        override fun dispose() {
            disposed = true
        }
    }
}