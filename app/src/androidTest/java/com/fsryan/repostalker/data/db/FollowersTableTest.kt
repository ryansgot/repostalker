package com.fsryan.repostalker.data.db

import android.support.test.runner.AndroidJUnit4
import com.fsryan.repostalker.data.GithubMember
import com.fsryan.repostalker.data.GithubUser
import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class FollowersTableTest : BaseDBTest() {

    @Test
    fun shouldAssociateMembersToUsers() {
        val numSaved = storeUserAndFollowers(bypasslaneUser, bypassLaneMembers)
        assertEquals(2, numSaved)
    }

    @Test
    fun shouldCorrectlyRetrieveFollowersAlphabetically() {
        storeUserAndFollowers(bypasslaneUser, bypassLaneMembers)

        // expected list should be sorted by the username (login)
        val expected = ArrayList(bypassLaneMembers)
        expected.sortWith(Comparator { gm1, gm2 -> gm1.login.compareTo(gm2.login, ignoreCase = true) })

        val retrieved = dbDaoUnderTest.retrieveFollowersOf(bypasslaneUser.id).toList().blockingGet()
        assertEquals(expected, retrieved)
    }

    @Test
    fun shouldCorrectlyRetrieveFollowersMatchingFilter() {
        storeUserAndFollowers(ryansgotUser, ryansgotFollowers)

        // expected list should be sorted by the username (login)
        val expected = ArrayList(ryansgotFollowers.filter { it.login.toLowerCase().contains('a') })
        expected.sortWith(Comparator { gm1, gm2 -> gm1.login.compareTo(gm2.login, ignoreCase = true) })

        // not using a validInterval means that as long as the record was
        // modified in the epoch, then it should be returned
        val retrieved = dbDaoUnderTest.retrieveFollowersOf(githubId = ryansgotUser.id, userNameFilter = "a").toList().blockingGet()
        assertEquals(expected, retrieved)
    }

    @Test(expected = NoRecordsException::class)
    fun shouldUseSynchronizedDateForSkippingInvalidatedFollowers() {
        storeUserAndFollowers(ryansgotUser, ryansgotFollowers)

        // ensures that no records will be returned that were modified in the
        // last 1 millisecond
        Thread.sleep(2)

        try {
            dbDaoUnderTest.retrieveFollowersOf(githubId = ryansgotUser.id, validInterval = 1).toList().blockingGet()
        } catch (e: Exception) {
            throw e.cause!! // <-- RxJava2 will put the cause as the actual exception that was thrown
        }
    }

    @Test
    fun shouldNotSkipValidFollowers() {
        val expected = ArrayList(ryansgotFollowers)
        expected.sortWith(Comparator { gm1, gm2 -> gm1.login.compareTo(gm2.login, ignoreCase = true) })

        storeUserAndFollowers(ryansgotUser, ryansgotFollowers)

        // Allow the cache to be invalid by waiting before storing users gain
        Thread.sleep(500)

        addMembersAsFollowers(ryansgotUser.id, ryansgotFollowers)

        val actual = dbDaoUnderTest.retrieveFollowersOf(githubId = ryansgotUser.id, validInterval = 500).toList().blockingGet()
        assertEquals(expected, actual)
    }

    private fun storeUserAndFollowers(user: GithubUser, followers: List<GithubMember>): Int {
        dbDaoUnderTest.storeGithubUser(user).blockingGet()
        Observable.fromIterable(followers)
            .flatMapSingle { dbDaoUnderTest.storeGithubMember(it) }
            .toList()
            .blockingGet()
        return addMembersAsFollowers(user.id, followers)
    }

    private fun addMembersAsFollowers(id: Long, followers: List<GithubMember>): Int {
        val synchronizedDate = Date()
        return Observable.fromIterable(followers)
            .flatMapSingle { dbDaoUnderTest.addGithubMemberToFollowers(id, it.id, synchronizedDate) }
            .reduce(0) { acc, next -> acc + next }
            .blockingGet()
    }
}