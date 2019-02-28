package com.fsryan.repostalker.data.db

import com.fsryan.repostalker.data.GithubMember
import com.fsryan.repostalker.data.GithubUser
import io.reactivex.Observable
import io.reactivex.Single
import java.util.*

interface DbDao {
    /**
     * Stores the [GithubUser]
     * @return a [Single] that tells you how many records were modified (should
     * be 1 on success)
     */
    fun storeGithubUser(user: GithubUser): Single<Int>

    /**
     * Retrieves the user with the specified [username] from the database. You
     * may submit a [validInterval], which will not return any record that was
     * modified before the current time minus the valid interval.
     *
     * This will error if there are no matching records.
     * @return a [Single] containing the [GithubUser] with the username
     * [username]
     */
    fun retrieveGithubUser(username: String, validInterval: Long = System.currentTimeMillis()): Single<GithubUser>

    /**
     * Stores the [GithubMember]
     * @return a [Single] that tells you how many ecords were modified (should
     * be 1 on success)
     */
    fun storeGithubMember(member: GithubMember): Single<Int>

    /**
     * Associates a [GithubMember] to a [GithubUser] as that users follower.
     * The [synchronizedDate] argument allows you to specify when the
     * synchronization occurred for cache invalidation purposes. Since you can
     * retrieve many github members at once, but you can only store one at a
     * time, this is a concession I had to make on the API.
     * @return a [Single] that tells you how many records were modified (should
     * be 1 on success)
     */
    fun addGithubMemberToFollowers(userGithubId: Long, memberGithubId: Long, synchronizedDate: Date): Single<Int>

    /**
     * Retrieves the followers of a github user specified by [githubId] from
     * the database. You may submit a [validInterval], which will not return
     * any record that was modified before the current time minus the valid
     * interval.
     *
     * This will error if there are no matching records.
     *
     * @return an [Observable] that emits one item for each [GithubMember]
     * associated with the user specified by the [githubId] within the the
     * [validInterval] and then completes
     */
    fun retrieveFollowersOf(githubId: Long, validInterval: Long = System.currentTimeMillis(), userNameFilter: String? = null): Observable<GithubMember>

    /**
     * Associates an avatar image (the [ByteArray]) with the github user
     * specified by [githubId].
     * @return a [Single] that tells you how many records were modified (should
     * be 1 on success)
     */
    fun storeAvatarImage(githubId: Long, image: ByteArray): Single<Int>
    fun retrieveAvatarImage(githubId: Long): Single<ByteArray>
}