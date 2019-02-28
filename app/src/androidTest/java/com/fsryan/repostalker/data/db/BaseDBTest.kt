package com.fsryan.repostalker.data.db

import android.Manifest
import android.support.test.rule.GrantPermissionRule
import com.fsryan.repostalker.data.AdapterFactory
import com.fsryan.repostalker.data.GithubMember
import com.fsryan.repostalker.data.GithubUser
import com.fsryan.testtools.Fixtures
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import java.util.*

open class BaseDBTest {

    companion object {
        protected val fixtures = Fixtures(Moshi.Builder()
            .add(AdapterFactory())
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .build())

        lateinit var bypasslaneUser: GithubUser
        lateinit var bypassLaneMembers: List<GithubMember>
        lateinit var ryansgotUser: GithubUser
        lateinit var ryansgotFollowers: List<GithubMember>

        @JvmStatic
        @BeforeClass
        fun initDataFromFixtures() {
            bypasslaneUser = fixtures.get("bypasslane_user.json", GithubUser::class.java)
            ryansgotUser = fixtures.get("ryansgot_user.json", GithubUser::class.java)
            bypassLaneMembers = fixtures.getList("bypasslane_members.json", GithubMember::class.java)
            ryansgotFollowers = fixtures.getList("ryansgot_followers.json", GithubMember::class.java)
        }
    }

    // grants permissions for the DBTestRule to copy the DB file to external
    // storage
    @get:Rule
    val gpRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    // For each test method, copies the database file to external storage
    @get:Rule
    val dbResetRule = DBTestRule("stalker-debug.db")

    protected lateinit var dbDaoUnderTest: DbDao

    @Before
    fun initializeDbDao() {
        dbDaoUnderTest = createDbDao()
    }
}