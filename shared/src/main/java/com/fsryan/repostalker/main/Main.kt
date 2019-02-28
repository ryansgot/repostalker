package com.fsryan.repostalker.main

import com.fsryan.repostalker.interactor.NavInteractor
import com.fsryan.repostalker.main.Main.Companion.DEFAULT_INVALIDATION_INTERVAL
import com.fsryan.repostalker.main.Main.Companion.LOWEST_INVALIDATION_INTERVAL
import com.fsryan.repostalker.main.Main.Companion.MAIN_STACK_ENTRY
import com.fsryan.repostalker.main.event.MainViewEvent
import com.fsryan.repostalker.rx.AlarmingDisposableCompletableObserver
import com.fsryan.repostalker.rx.AlarmingDisposableSingleObserver
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.subjects.PublishSubject

interface Main {

    companion object {
        const val DEFAULT_INVALIDATION_INTERVAL = (60 * 60).toLong()   // <-- one hour in seconds
        const val LOWEST_INVALIDATION_INTERVAL = 10L
        const val DEFAULT_USER = "bypasslane"
        const val MAIN_STACK_ENTRY = "main"
        fun createPresenter(interactor: Main.Interactor): Main.Presenter = MainPresenterImpl(interactor)
    }

    interface Presenter {
        fun eventObservable(): Observable<MainViewEvent>

        fun onReady()
        fun onUnready()
        fun userRequestedSettings()
        fun userCanceledSettings()
        fun userSavedSettings(cacheInvalidationInterval: String)
        fun userRequestedBackNav()
    }

    interface Interactor : NavInteractor {
        fun fetchCacheInvalidationInterval(defaultValue: Long): Single<Long>
        fun storeCacheInvalidationInterval(interval: Long): Completable
    }
}

private class MainPresenterImpl(private val interactor: Main.Interactor): Main.Presenter {
    private val eventSubject = PublishSubject.create<MainViewEvent>()
    private val compositeDisposable = CompositeDisposable()

    override fun eventObservable(): Observable<MainViewEvent> = eventSubject

    override fun onReady() {
        compositeDisposable.add(
            interactor.pushAndRegisterBackNavInterest(MAIN_STACK_ENTRY)
                .subscribeWith(object: AlarmingDisposableCompletableObserver() {
                    override fun onComplete() {
                        eventSubject.onNext(MainViewEvent.forNavBack())
                    }
                }))
    }

    override fun onUnready() {
        compositeDisposable.clear()
    }

    override fun userSavedSettings(cacheInvalidationInterval: String) {
        compositeDisposable.add(
            Single.fromCallable {
                val interval = cacheInvalidationInterval.toLong()
                return@fromCallable if (interval >= LOWEST_INVALIDATION_INTERVAL) Pair(interval, "")
                        else Pair(LOWEST_INVALIDATION_INTERVAL, "Invalid cache invalidation interval ($interval); storing lowest allowed ($LOWEST_INVALIDATION_INTERVAL)")
            }.flatMap { pair -> interactor.storeCacheInvalidationInterval(pair.first * 1000).toSingleDefault(pair.second) }
                .subscribeWith(object: DisposableSingleObserver<String>() {
                    override fun onSuccess(message: String) {
                        eventSubject.onNext(MainViewEvent.forHidingSettings(if (message.isEmpty()) null else message))
                    }

                    override fun onError(e: Throwable) {
                        eventSubject.onNext(MainViewEvent.forHidingSettings("Unable to store cache invalidation interval: $cacheInvalidationInterval"))
                    }
                })
        )
    }

    override fun userCanceledSettings() {
        eventSubject.onNext(MainViewEvent.forHidingSettings())
    }

    override fun userRequestedSettings() {
        compositeDisposable.add(
            interactor.fetchCacheInvalidationInterval(DEFAULT_INVALIDATION_INTERVAL * 1000)
                .onErrorReturnItem(DEFAULT_INVALIDATION_INTERVAL * 1000)
                .map { it / 1000 }
                .subscribeWith(object: AlarmingDisposableSingleObserver<Long>() {
                    override fun onSuccess(invalidtionInterval: Long) {
                        eventSubject.onNext(MainViewEvent.forShowingSettings(invalidtionInterval))
                    }
                })
        )
    }

    override fun userRequestedBackNav() {
        compositeDisposable.add(
            interactor.requestBackNav()
                .subscribeWith(object: DisposableSingleObserver<String>() {
                    override fun onSuccess(popped: String) {
                        println("popped off of nav backstack: '$popped'")
                    }

                    override fun onError(e: Throwable) {
                        // the navigation stack will be empty prior to onReady
                        // being called. This ensures that in this case, the
                        // back navigation can be handled when ready
                        eventSubject.onNext(MainViewEvent.forNavBack())
                    }
                })
        )
    }
}