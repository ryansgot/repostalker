package com.fsryan.repostalker.data.db

import android.support.test.runner.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GithubUsersTableTest : BaseDBTest() {

    @Test
    fun shouldCorrectlyInsertGithubUser() {
        val numSaved = dbDaoUnderTest.storeGithubUser(bypasslaneUser).blockingGet()
        assertEquals(1, numSaved)
    }

    @Test
    fun shouldCorrectlyRetrieveGithubUser() {
        dbDaoUnderTest.storeGithubUser(bypasslaneUser).blockingGet()
        val actual = dbDaoUnderTest.retrieveGithubUser(bypasslaneUser.login).blockingGet()
        assertEquals(bypasslaneUser, actual)
    }

    @Test(expected = NoRecordsException::class)
    fun shouldSkipInvalidatedGithubUsers() {
        dbDaoUnderTest.storeGithubUser(bypasslaneUser).blockingGet()

        Thread.sleep(2)

        try {
            dbDaoUnderTest.retrieveGithubUser(bypasslaneUser.login, validInterval = 1).blockingGet()
        } catch (e: Exception) {
            throw e.cause!!
        }
    }

    @Test
    fun shouldNotSkipValidGithubUsers() {
        dbDaoUnderTest.storeGithubUser(bypasslaneUser).blockingGet()

        // Allow the cache to be invalid by waiting before storing users gain
        Thread.sleep(100)

        dbDaoUnderTest.storeGithubUser(bypasslaneUser).blockingGet()

        val actual = dbDaoUnderTest.retrieveGithubUser(username = bypasslaneUser.login, validInterval = 100).blockingGet()
        assertEquals(bypasslaneUser, actual)
    }
}