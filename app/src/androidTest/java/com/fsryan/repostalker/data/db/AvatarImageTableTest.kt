package com.fsryan.repostalker.data.db

import android.support.test.InstrumentationRegistry.getContext
import android.support.test.runner.AndroidJUnit4
import com.fsryan.repostalker.test.ViewTestUtil
import com.fsryan.repostalker.test.ViewTestUtil.ryansgotDrawable
import junit.framework.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvatarImageTableTest : BaseDBTest() {

    private lateinit var avatarImage: ByteArray

    @Before
    fun addUser() {
        avatarImage = ViewTestUtil.bytesOfDrawable(getContext(), ryansgotDrawable())
        dbDaoUnderTest.storeGithubUser(ryansgotUser).blockingGet()
    }

    @Test
    fun shouldCorrectlyAssociateAvatarImage() {
        // associates avatar image with user
        val saved = dbDaoUnderTest.storeAvatarImage(ryansgotUser.id, avatarImage).blockingGet()
        assertEquals(1, saved)
    }

    @Test
    fun shouldCorrectlyRetrieveAvatarImage() {
        // associates avatar image with user
        dbDaoUnderTest.storeAvatarImage(ryansgotUser.id, avatarImage).blockingGet()

        val retrieved = dbDaoUnderTest.retrieveAvatarImage(ryansgotUser.id).blockingGet()
        assertArrayEquals(avatarImage, retrieved)
    }
}