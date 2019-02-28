package com.fsryan.repostalker.followerlist

import com.fsryan.repostalker.swapOnCondition
import com.fsryan.repostalker.data.GithubMember
import com.fsryan.repostalker.followerlist.event.FollowerDetails
import com.fsryan.repostalker.followerlist.event.FollowerListViewEvent
import com.fsryan.repostalker.followerlist.event.toFollowerDetails
import com.fsryan.repostalker.interactor.NavInteractor
import com.fsryan.repostalker.main.Main
import com.fsryan.repostalker.rx.AlarmingDisposableCompletableObserver
import com.fsryan.repostalker.rx.DisposableHelper
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableObserver
import io.reactivex.subjects.PublishSubject

interface FollowerList {

    companion object {
        fun createPresenter(interactor: FollowerList.Interactor): FollowerList.Presenter = PresenterImpl(interactor)
    }

    interface Presenter {
        fun eventObservable(): Observable<FollowerListViewEvent>
        fun onReady()
        fun onUnready()
        fun userRequestedFollower(userName: String)
        fun userRequestedFollowerListFilter(filter: String)
    }

    interface Interactor : NavInteractor {
        fun storeUserSelectedAction(username: String): Completable
        fun fetchFollowersOf(username: String, userNameFilter: String? = null): Observable<GithubMember>
        fun fetchAvatarImage(githubId: Long): Single<ByteArray>
        fun fetchLastUserRequested(): Single<String>
    }
}

private class PresenterImpl(private val interactor: FollowerList.Interactor): FollowerList.Presenter {
    private val eventSubject = PublishSubject.create<FollowerListViewEvent>()

    /**
     * Whenever you need fine-grained control of the subscriptions within the
     * presenter, and the [CompositeDisposable] could be accessed on some other
     * thread than the main thread, you must mark them as volatile and
     * initialize and dispose them in the [FollowerList.Presenter.onReady()]
     * and [FollowerList.Presenter.onUnready()] methods respectively.
     * Otherwise, you risk memory leaks and invalid behaviors based upon view
     * lifecycle.
     *
     * This approach allows the presenter to be more or less unconcerned with
     * the thread on which its code is running--with this one caveat.
     */
    @Volatile
    private lateinit var compositeDisposable: CompositeDisposable
    @Volatile
    private lateinit var followerDetailsCompositeDisposable: CompositeDisposable

    override fun eventObservable(): Observable<FollowerListViewEvent> = eventSubject

    override fun onReady() {
        compositeDisposable = CompositeDisposable()
        followerDetailsCompositeDisposable = CompositeDisposable()
        compositeDisposable.add(followerDetailsCompositeDisposable)
        // TODO: Should you should store the filter to preserve user context?
        fetchFollowersForLastUserRequested(userNameFilter = null)
    }

    override fun onUnready() {
        // disposing because new CompositeDisposable objects will be created in onReady()
        compositeDisposable.dispose()
    }

    override fun userRequestedFollower(username: String) {
        DisposableHelper.clearAndAdd(
            followerDetailsCompositeDisposable,
            interactor.storeUserSelectedAction(username)
                .andThen(fetchFollowers(username = username, pushBackNavInterest = true, userNameFilter = null))
                .subscribeWith(createFollowerObserver(clearFilterText = true))
        )
    }

    override fun userRequestedFollowerListFilter(filter: String) {
        fetchFollowersForLastUserRequested(filter)
    }

    private fun fetchFollowers(username: String, pushBackNavInterest: Boolean, userNameFilter: String? = null): Observable<FollowerDetails> {
        if (username != Main.DEFAULT_USER && userNameFilter == null) {
            // a non-null username filter should not result in an extra registration for back nav
            val backNavObservable = if (pushBackNavInterest) interactor.pushAndRegisterBackNavInterest(username)
            else interactor.registerBackNavInterest(username)
            compositeDisposable.add(backNavObservable.subscribeWith(createBackNavObserver()))
        }

        return interactor.fetchFollowersOf(username = username, userNameFilter = userNameFilter)
            .doOnSubscribe { eventSubject.onNext(FollowerListViewEvent.forLoading(clearFilterText = userNameFilter == null)) }
            // order must be preserved here--that is why concatMapSingle is used
            .concatMapSingle { member -> interactor.fetchAvatarImage(member.id)
                    .onErrorReturnItem(ByteArray(0))
                    .map { Pair(member, it) }
            }.map { pair -> pair.first.toFollowerDetails(pair.second) }
    }

    private fun fetchFollowersForLastUserRequested(userNameFilter: String? = null) {
        DisposableHelper.clearAndAdd(
            followerDetailsCompositeDisposable,
            interactor.fetchLastUserRequested()
                .onErrorReturnItem(Main.DEFAULT_USER)
                .map { username -> username.swapOnCondition(alt = Main.DEFAULT_USER) { it == Main.MAIN_STACK_ENTRY } }
                .flatMapObservable { username -> fetchFollowers(username = username, pushBackNavInterest =  false, userNameFilter = userNameFilter) }
                .subscribeWith(createFollowerObserver(clearFilterText = userNameFilter.isNullOrEmpty()))
        )
    }

    private fun createBackNavObserver(): DisposableCompletableObserver = object: AlarmingDisposableCompletableObserver() {
        override fun onComplete() {
            fetchFollowersForLastUserRequested()
        }
    }

    private fun createFollowerObserver(clearFilterText: Boolean): DisposableObserver<FollowerDetails> = object: DisposableObserver<FollowerDetails>() {
        var isFirst: Boolean = true

        override fun onComplete() {
            eventSubject.onNext(FollowerListViewEvent.forFinishedAddingFollowers(!isFirst))
        }

        override fun onNext(followerDetails: FollowerDetails) {
            val event = if (isFirst) FollowerListViewEvent.forShowingNewList(clearFilterText, followerDetails)
                    else FollowerListViewEvent.forAddingAFollower(followerDetails)
            eventSubject.onNext(event)
            isFirst = false
        }

        override fun onError(e: Throwable) {
            // TODO: clear up the error message based upon what was thrown
            eventSubject.onNext(FollowerListViewEvent.forErrorMessage(!isFirst, e.message))
        }
    }
}
