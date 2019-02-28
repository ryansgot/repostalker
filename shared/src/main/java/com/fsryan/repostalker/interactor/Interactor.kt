package com.fsryan.repostalker.interactor

import com.fsryan.repostalker.current.Current
import com.fsryan.repostalker.data.GithubMember
import com.fsryan.repostalker.data.GithubUser
import com.fsryan.repostalker.data.db.DbDao
import com.fsryan.repostalker.data.nav.Navigator
import com.fsryan.repostalker.data.network.GithubService
import com.fsryan.repostalker.data.prefs.UserPrefs
import com.fsryan.repostalker.followerlist.FollowerList
import com.fsryan.repostalker.main.Main
import io.reactivex.Single
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.ResponseBody
import java.util.*

interface Interactor : Main.Interactor, Current.Interactor, FollowerList.Interactor {
    companion object {
        fun create(navigator: Navigator,
                   userPrefs: UserPrefs,
                   dbDao: DbDao,
                   githubService: GithubService): Interactor = InteractorImpl(navigator, userPrefs, dbDao, githubService)
    }
}

private class InteractorImpl(private val navigator: Navigator,
                             private val userPrefs: UserPrefs,
                             private val dbDao: DbDao,
                             private val githubService: GithubService) : Interactor {

    companion object {
        private val GITHUB_SERVICE_FOLLOWER_ERROR_LIST = listOf<GithubMember>()
    }

    private val userSelectedSubject = PublishSubject.create<String>()

    // Main.Interactor
    override fun fetchCacheInvalidationInterval(defaultValue: Long): Single<Long> = userPrefs.retrieveCacheInvalidationInterval(defaultValue)
        .subscribeOn(Schedulers.io())

    override fun storeCacheInvalidationInterval(interval: Long): Completable = userPrefs.storeCacheInvalidationInterval(interval)
        .subscribeOn(Schedulers.io())

    // Current.Interactor
    override fun fetchGithubUser(username: String): Single<GithubUser> = internalCacheInvalidationInterval()
        .flatMap { validInterval -> dbDao.retrieveGithubUser(username, validInterval) }
        .onErrorResumeNext { githubService.user(username).flatMap { user -> syncUserWithAvatarImage(user) } }
        .subscribeOn(Schedulers.io())

    // this method is actually shared with FollowerList.Interactor
    override fun fetchAvatarImage(githubId: Long): Single<ByteArray> = dbDao.retrieveAvatarImage(githubId)
        .subscribeOn(Schedulers.io())

    override fun userSelectedObservable(): Observable<String> = userSelectedSubject

    private fun syncUserWithAvatarImage(user: GithubUser): Single<GithubUser> = dbDao.storeGithubUser(user)
        .observeOn(Schedulers.io())
        .subscribeOn(Schedulers.io())
        .onErrorReturnItem(0)
        .flatMap { storedCount ->
            return@flatMap if (storedCount == 0) Single.error<ResponseBody>(Exception("cannot retrieve avatar image because user not stored"))
            else githubService.downloadFile(user.avatarUrl)
        }.flatMap { resp -> dbDao.storeAvatarImage(user.id, resp.bytes()) }
        .onErrorReturnItem(0)
        .map { user }

    // FollowerList.Interactor
    // TODO: create user activity log
    override fun storeUserSelectedAction(username: String): Completable = Completable.fromAction { userSelectedSubject.onNext(username) }

    override fun fetchFollowersOf(username: String, userNameFilter: String?): Observable<GithubMember> = fetchGithubUser(username)
        .flatMapObservable { user ->
            internalCacheInvalidationInterval()
                .flatMapObservable { validInterval -> dbDao.retrieveFollowersOf(user.id, validInterval, userNameFilter) }
                .onErrorResumeNext(Function<Throwable, ObservableSource<GithubMember>> { syncFollowers(username, user, userNameFilter) })
        }

    // this method is actually shared with Current.Interactor
    override fun fetchLastUserRequested(): Single<String> = navigator.peek().map(stripSequence())

    private fun syncFollowers(username: String, user: GithubUser, usernameFilter: String?): Observable<GithubMember> = Single.fromCallable { Date() }
        .flatMapObservable { date ->
            val networkSingle = if (user.isOrganization()) githubService.membersOfOrg(user.login)
                                else githubService.followersOfUser(username)
            return@flatMapObservable networkSingle.onErrorReturnItem(GITHUB_SERVICE_FOLLOWER_ERROR_LIST)
                .flatMapObservable {
                    return@flatMapObservable if (it == GITHUB_SERVICE_FOLLOWER_ERROR_LIST) dbDao.retrieveFollowersOf(user.id, userNameFilter = usernameFilter)
                        else Observable.fromIterable(it)
                                .flatMapSingle { member -> syncFollower(user.id, member, date) } }
                                .filter { usernameFilter == null || it.login.contains(usernameFilter, ignoreCase = true) }
                                .sorted { gm1, gm2 -> gm1.login.compareTo(gm2.login, ignoreCase = true) }
        }

    private fun syncFollower(githubId: Long, follower: GithubMember, date: Date) = fetchGithubUser(follower.login)
        .flatMap { dbDao.storeGithubMember(follower) }
        .flatMap { githubService.downloadFile(follower.avatarUrl) }
        .flatMap { Single.zip(
            dbDao.storeAvatarImage(follower.id, it.bytes()).onErrorReturnItem(0),
            dbDao.addGithubMemberToFollowers(githubId, follower.id, date).onErrorReturnItem(0),
            BiFunction<Int, Int, GithubMember> { _, _ -> follower }
        ) }
        .onErrorReturnItem(follower)    // <-- if follower is available, then return it

    // NavInteractor
    override fun requestBackNav(): Single<String> = navigator.pop().map(stripSequence())

    override fun pushAndRegisterBackNavInterest(id: String): Completable = navigator.push(id)
        .flatMapCompletable { pushed -> registerBackNavInterest(pushed, true) }

    override fun registerBackNavInterest(id: String, exactMatch: Boolean): Completable = navigator.backEventObservable()
        .filter { test -> (exactMatch && test == id) || (!exactMatch && test.endsWith("|$id")) }
        .firstOrError()
        .flatMapCompletable { Completable.complete() }

    private fun internalCacheInvalidationInterval(): Single<Long> = fetchCacheInvalidationInterval(Main.DEFAULT_INVALIDATION_INTERVAL * 1000)
        .onErrorReturnItem(Main.DEFAULT_INVALIDATION_INTERVAL * 1000)

    private fun stripSequence(): (String) -> String = { str ->
        val delimIdx = str.lastIndexOf(Navigator.DELIMITER)
        if (delimIdx < 0) str else str.substring(delimIdx + 1)
    }
}