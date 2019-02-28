package com.fsryan.repostalker.current

import com.fsryan.repostalker.current.event.CurrentViewEvent
import com.fsryan.repostalker.current.event.UserDetails
import com.fsryan.repostalker.current.event.toUserDetails
import com.fsryan.repostalker.data.GithubUser
import com.fsryan.repostalker.interactor.NavInteractor
import com.fsryan.repostalker.main.Main
import com.fsryan.repostalker.rx.AlarmingDisposableCompletableObserver
import com.fsryan.repostalker.rx.AlarmingDisposableObserver
import com.fsryan.repostalker.rx.DisposableHelper
import com.fsryan.repostalker.swapOnCondition
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.subjects.PublishSubject

interface Current {

    companion object {
        fun createPresenter(interactor: Current.Interactor): Current.Presenter = CurrentPresenterImpl(interactor)
    }

    interface Presenter {
        fun eventObservable(): Observable<CurrentViewEvent>
        fun onReady()
        fun onUnready()
    }

    interface Interactor : NavInteractor {
        fun fetchLastUserRequested(): Single<String>
        fun fetchGithubUser(username: String): Single<GithubUser>
        fun fetchAvatarImage(githubId: Long): Single<ByteArray>
        fun userSelectedObservable(): Observable<String>
    }
}

private class CurrentPresenterImpl(private val interactor: Current.Interactor): Current.Presenter {
    private val eventSubject = PublishSubject.create<CurrentViewEvent>()
    @Volatile
    private lateinit var compositeDisposable: CompositeDisposable
    @Volatile
    private lateinit var fetchUserCompositeDisposable: CompositeDisposable

    override fun eventObservable(): Observable<CurrentViewEvent> = eventSubject

    override fun onReady() {
        compositeDisposable = CompositeDisposable()
        fetchUserCompositeDisposable = CompositeDisposable()
        compositeDisposable.add(fetchUserCompositeDisposable)

        fetchUserForLastRequested()

        compositeDisposable.add(
            interactor.userSelectedObservable()
                .subscribeWith(object : AlarmingDisposableObserver<String>() {
                    override fun onNext(username: String) {
                        fetchUserForLastRequested()
                    }
                })
        )
    }

    override fun onUnready() {
        compositeDisposable.dispose()
    }

    private fun fetchUserForLastRequested() {
        DisposableHelper.clearAndAdd(
            fetchUserCompositeDisposable,
            lastUserRequested()
                .doOnSuccess { username ->
                    if (username != Main.DEFAULT_USER) {
                        listenForBackNav(username)
                    }
                }.flatMap { username -> fetchUser(username = username) }
                .subscribeWith(createUserObserver())
        )
    }

    private fun lastUserRequested(): Single<String> = interactor.fetchLastUserRequested()
        .onErrorReturnItem(Main.DEFAULT_USER)
        .map { username -> username.swapOnCondition(alt = Main.DEFAULT_USER) { it == Main.MAIN_STACK_ENTRY } }

    private fun fetchUser(username: String): Single<UserDetails> = interactor.fetchGithubUser(username)
        .flatMap { user ->
            interactor.fetchAvatarImage(user.id)
                .onErrorReturnItem(ByteArray(0))
                .map { avatarImage -> user.toUserDetails(avatarImage) }
        }.doOnSubscribe { eventSubject.onNext(CurrentViewEvent.forDataLoading()) }

    private fun createUserObserver(): DisposableSingleObserver<UserDetails> = object: DisposableSingleObserver<UserDetails>() {
        override fun onSuccess(details: UserDetails) {
            eventSubject.onNext(CurrentViewEvent.forUserDetails(details))
        }

        override fun onError(e: Throwable) {
            // TODO: handle error
        }
    }

    private fun listenForBackNav(username: String) {
        compositeDisposable.add(
            interactor.registerBackNavInterest(username)
                .subscribeWith(object: AlarmingDisposableCompletableObserver() {
                    override fun onComplete() {
                        fetchUserForLastRequested()
                    }
                })
        )
    }
}