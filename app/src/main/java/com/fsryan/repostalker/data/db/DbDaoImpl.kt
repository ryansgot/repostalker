package com.fsryan.repostalker.data.db

import com.fsryan.repostalker.ForSure.*
import com.fsryan.repostalker.data.*
import com.fsryan.repostalker.toUTC
import com.fsryan.forsuredb.api.FSJoin
import com.fsryan.forsuredb.api.OrderBy
import com.fsryan.forsuredb.api.Retriever
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import java.util.*
import java.util.concurrent.Callable

class NoRecordsException : Exception("No matching records")

fun createDbDao(): DbDao = DbDaoImpl()

/**
 * Takes advantage of the forsuredb generated API to take more of the data
 * access logic away from the interactor. This is not 100% necessary in terms
 * of the design, but it both reduces the burden upon the interactor and
 * implements the Rx interface that the interactor would otherwise have to
 * implement.
 */
private class DbDaoImpl: DbDao {

    companion object {
        private val githubUsersApi = githubUsersTable().api
        private val githubMembersApi = githubMembersTable().api
        private val avatarImageApi = avatarImagesTable().api
    }

    override fun storeGithubUser(user: GithubUser) = Single.fromCallable<Int> {
        val result = githubUsersTable()
            .find().byGithubId(user.id)
                .then()
            .set().obj(user)
            .save()
        if (result.exception() != null) {
            throw result.exception()
        }
        return@fromCallable result.rowsAffected()
    }

    override fun retrieveGithubUser(username: String, validInterval: Long): Single<GithubUser> = Single.using(
        Callable<Retriever> {
            githubUsersTable()
                .find().byUsername(username)
                    .and().byModifiedAfterInclusive(dateForValidInterval(validInterval).toUTC())
                .then()
                .get()
        },
        Function<Retriever, SingleSource<GithubUser>> { retriever ->
            return@Function if (retriever.moveToFirst()) Single.just(githubUsersApi.get(retriever))
                else Single.error(NoRecordsException())
        },
        Consumer<Retriever> { retriever -> retriever.close() }
    )

    override fun storeGithubMember(member: GithubMember) = Single.fromCallable<Int> {
        val result = githubMembersTable()
            .find().byGithubId(member.id)
                .then()
            .set().obj(member)
            .save()
        if (result.exception() != null) {
            throw result.exception()
        }
        return@fromCallable result.rowsAffected()
    }

    override fun addGithubMemberToFollowers(userGithubId: Long, memberGithubId: Long, synchronizedDate: Date) = Single.fromCallable<Int> {
        val result = followersTable()
            .find().byUserId(userGithubId)
                .and().byFollowerId(memberGithubId)
                .then()
            .set().userId(userGithubId)
                .synchronizedDate(synchronizedDate.toUTC())
                .followerId(memberGithubId)
            .save()
        if (result.exception() != null) {
            throw result.exception()
        }
        return@fromCallable result.rowsAffected()
    }

    override fun retrieveFollowersOf(githubId: Long, validInterval: Long, userNameFilter: String?): Observable<GithubMember> = Observable.using(
        Callable<Retriever> { if (userNameFilter == null) followersWithoutFilter(githubId, validInterval) else followersWithFilter(githubId, validInterval, userNameFilter) },
        Function<Retriever, ObservableSource<GithubMember>> { retriever ->
            return@Function if (retriever.moveToFirst())
                retriever.flattenAsObservable { githubMembersApi.get(retriever) }
                else Observable.error(NoRecordsException())
        },
        Consumer<Retriever> { retriever -> retriever.close() }
    )

    override fun storeAvatarImage(githubId: Long, image: ByteArray) = Single.fromCallable<Int> {
        val result = avatarImagesTable()
            .find().byGithubId(githubId)
                .then()
            .set().userId(githubId)
                .image(image)
            .save()
        if (result.exception() != null) {
            throw result.exception()
        }
        return@fromCallable result.rowsAffected()
    }

    override fun retrieveAvatarImage(githubId: Long) = Single.using(
        Callable<Retriever> { avatarImagesTable().find().byGithubId(githubId).then().get() },
        Function<Retriever, SingleSource<ByteArray>> { retriever ->
            return@Function if (retriever.moveToFirst()) Single.just(avatarImageApi.image(retriever))
            else Single.error(NoRecordsException())
        },
        Consumer<Retriever> { retriever -> retriever.close() }
    )

    private fun followersWithoutFilter(githubId: Long, validInterval: Long): Retriever = followersTable()
        .find().byUserId(githubId)
            .and().bySynchronizedDateAfterInclusive(dateForValidInterval(validInterval).toUTC())
        .then()
        .joinGithubMembersTable(FSJoin.Type.INNER)
            .order().byLowercaseUsername(OrderBy.ORDER_ASC)
            .then()
        .then()
        .get()

    private fun followersWithFilter(githubId: Long, validInterval: Long, usernameFilter: String): Retriever = followersTable()
        .find().byUserId(githubId)
            .and().bySynchronizedDateAfterInclusive(dateForValidInterval(validInterval).toUTC())
        .then()
        .joinGithubMembersTable(FSJoin.Type.INNER)
            .find().byLowercaseUsernameLike(usernameFilter)
            .then()
            .order().byLowercaseUsername(OrderBy.ORDER_ASC)
            .then()
        .then()
        .get()

    private fun dateForValidInterval(validInterval: Long) = Date(System.currentTimeMillis() - validInterval)
}