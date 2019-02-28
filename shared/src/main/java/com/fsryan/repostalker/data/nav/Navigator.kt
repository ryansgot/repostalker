package com.fsryan.repostalker.data.nav

import java.util.Stack

import io.reactivex.*
import io.reactivex.subjects.PublishSubject

interface Navigator {
    companion object {
        const val DELIMITER: Char = '|'
        fun create(mainThreadScheduler: Scheduler): Navigator = NavigatorImpl(mainThreadScheduler)
    }

    fun push(id: String): Single<String>
    fun pop(): Single<String>
    fun peek(): Single<String>
    fun backEventObservable(): Observable<String>
}

/**
 *
 * Navigator is a stack that allows duplicate elements (except for the base
 * element). It is meant to hold the navigation state of the app. It will
 * throw a runtime exception if you try to navigate back when the stack is
 * empty, so you must ensure that the base navigating element of the app
 * remains on the bottom of the stack.
 *
 * Currently, it meets the needs of the app, but it is not full-featured.
 * It is likely to be insufficient if the navigation use cases become
 * complicated.
 *
 * You may access Navigator from any thread. It will handle dispatching
 * navigation work to the main thread. So if it is important that your Observer
 * be notified on a particular thread, then you should set the appropriate
 * observeOn scheduler.
 *
 * Navigator should get injected at application scope. The only acceptable user is
 * [Interactor][com.fsryan.ryan.repostalker.interactor.Interactor],
 * which implements
 * [NavInteractor][com.fsryan.ryan.repostalker.interactor.NavInteractor].
 * But this is not a problem because the idea behind
 * [Interactor][com.fsryan.ryan.repostalker.interactor.Interactor]
 * is to be the only interactor for the whole app. Thus, any interactor
 * interface that must provide an API for navigation must extend
 * [NavInteractor][com.fsryan.ryan.repostalker.interactor.NavInteractor]
 */
private class NavigatorImpl(private val mainThreadScheduler: Scheduler): Navigator {

    private lateinit var base: String
    private var sequenceNumber: Int = 0
    private val navStack = Stack<String>()
    private val mBackNavSubject = PublishSubject.create<String>()

    /**
     * Each id is pushed with a sequence number in the format: num|id
     * @param id the id of the item to put on the stack. This id will be
     * available when you call [.pop] or when your observer of
     * [.backEventObservable] fires an event.
     * @return a [Completable] that completes when pushed
     */
    override fun push(id: String): Single<String> = Single.fromCallable {
        val toPush = "${sequenceNumber++}|$id"
        if (navStack.isEmpty()) {
            base = id
        } else if (id == base) {
            return@fromCallable "0|$base"
        }
        navStack.push(toPush)
    }.subscribeOn(mainThreadScheduler)

    /**
     *
     * Pops off the stack and notifies all observers of
     * [.backEventObservable] when popped.
     *
     * Additionally, the id of the item will be sent through
     * [io.reactivex.SingleObserver.onSuccess]
     * @return a [io.reactivex.Single] that will terminate when the item
     * is popped off the stack.
     */
    override fun pop(): Single<String> = Single.fromCallable<String> {
        if (navStack.isEmpty()) {
            throw Exception("Cannot pop from empty stack")
        }
        return@fromCallable navStack.pop()
    }.doOnSuccess { mBackNavSubject.onNext(it) }
        .subscribeOn(mainThreadScheduler)

    /**
     * Returns the top of the nav stack or throws an error if empty
     */
    override fun peek(): Single<String> = Single.fromCallable<String> {
        if (navStack.isEmpty()) {
            throw Exception("empty")
        }
        return@fromCallable navStack.peek()
    }.subscribeOn(mainThreadScheduler)

    /**
     *
     * When items are removed via [.removeIfPresent], no event
     * is generated.
     * @return an [Observable] that, will output the id of each item
     * popped off the stack.
     */
    override fun backEventObservable(): Observable<String> = mBackNavSubject
}