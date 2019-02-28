package com.fsryan.repostalker.data.prefs

import android.Manifest
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPrefsTest {

    @get:Rule
    val permsTestRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @get:Rule
    val userPrefsRule = PrefsTestRule("user_prefs")

    private lateinit var userPrefsUnderTest: UserPrefs

    @Before
    fun initUserPrefs() {
        userPrefsUnderTest = createUserPrefs(getTargetContext())
    }

    @Test
    fun shouldRetrieveDefaultValueWhenUserPrefNotStored() {
        val retrieved = userPrefsUnderTest.retrieveCacheInvalidationInterval(1L).blockingGet()
        assertEquals(1, retrieved)
    }

    @Test
    fun shouldCorrectlyStoreAndRetrieve() {
        userPrefsUnderTest.storeCacheInvalidationInterval(100L).blockingAwait()
        val retrieved = userPrefsUnderTest.retrieveCacheInvalidationInterval(1).blockingGet()
        assertEquals(100L, retrieved)
    }
}