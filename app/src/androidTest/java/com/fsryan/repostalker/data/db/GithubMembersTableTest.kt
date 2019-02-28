package com.fsryan.repostalker.data.db

import android.support.test.runner.AndroidJUnit4
import io.reactivex.Observable
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GithubMembersTableTest : BaseDBTest() {
    @Test
    fun shouldCorrectlyInsertGithubMember() {
        dbDaoUnderTest.storeGithubUser(bypasslaneUser)
        val saved = Observable.fromIterable(bypassLaneMembers)
            .flatMapSingle { dbDaoUnderTest.storeGithubMember(it) }
            .reduce(0) { acc, next -> acc + next }
            .blockingGet()
        assertEquals(2, saved)
    }
}