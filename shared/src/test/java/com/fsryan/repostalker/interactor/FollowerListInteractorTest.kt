package com.fsryan.repostalker.interactor

import com.fsryan.repostalker.data.AdapterFactory
import com.fsryan.repostalker.data.GithubMember
import com.fsryan.repostalker.data.GithubUser
import com.fsryan.repostalker.followerlist.FollowerList
import com.fsryan.repostalker.main.Main
import com.fsryan.repostalker.testonly.RetrofitHelper
import com.fsryan.repostalker.testonly.RxJavaPluginsExtension
import com.fsryan.testtools.Fixtures
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import io.mockk.Called
import io.mockk.every
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(RxJavaPluginsExtension::class)
class FollowerListInteractorTest : BaseInteractorTest<FollowerList.Interactor>() {

    private lateinit var testObserver: TestObserver<GithubMember>

    companion object {
        private const val expectedInternalCacheInvalidationDefault = Main.DEFAULT_INVALIDATION_INTERVAL * 1000
        private val fixtures: Fixtures = Fixtures(Moshi.Builder()
            .add(AdapterFactory())
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .build())
        private val cacheInvalidationInterval = 42L
        private lateinit var bypassLaneUser: GithubUser
        private lateinit var stevechalkerUser: GithubUser
        private lateinit var pedlarUser: GithubUser
        private lateinit var bypassLaneFollowers: List<GithubMember>

        @BeforeAll
        @JvmStatic
        fun initbypassLaneUser() {
            bypassLaneUser = fixtures.get("bypasslane_user.json", GithubUser::class.java)
            stevechalkerUser = fixtures.get("stevechalker_user.json", GithubUser::class.java)
            pedlarUser = fixtures.get("pedlar_user.json", GithubUser::class.java)
            bypassLaneFollowers = fixtures.getList("bypasslane_members.json", GithubMember::class.java)
        }
    }

    @BeforeEach
    fun setUpTestObserverAndSetValueForCacheInvalidationIntervalAndSetGithubUser() {
        testObserver = TestObserver()
        every { mockUserPrefs.retrieveCacheInvalidationInterval(any()) } returns Single.just(cacheInvalidationInterval)
        every { mockDbDao.retrieveGithubUser(eq(bypassLaneUser.login), any()) } returns Single.just(bypassLaneUser)
        every { mockDbDao.retrieveGithubUser(eq(stevechalkerUser.login), any()) } returns Single.just(stevechalkerUser)
        every { mockDbDao.retrieveGithubUser(eq(pedlarUser.login), any()) } returns Single.just(pedlarUser)
    }

    @AfterEach
    fun flushTestObserver() {
        testObserver.dispose()
    }

    @Test
    @DisplayName("Should not sync when DbDao finds valid followers")
    fun shouldPassThroughFollowersRequestToDbDaoWithoutSync() {
        every { mockDbDao.retrieveFollowersOf(any(), any()) } returns Observable.fromIterable(bypassLaneFollowers)

        interactorUnderTest().fetchFollowersOf(bypassLaneUser.login).subscribe(testObserver)

        testObserver.awaitTerminalEvent()
        testObserver.assertValueSequence(bypassLaneFollowers)

        verifyOrder {
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
            mockDbDao.retrieveGithubUser(eq(bypassLaneUser.login), eq(cacheInvalidationInterval))
            mockDbDao.retrieveFollowersOf(eq(bypassLaneUser.id), eq(cacheInvalidationInterval))
        }

        verify { mockGitubService wasNot Called }
    }

    @Test
    @DisplayName("Should sync when DbDao does not find valid followers")
    fun sync() {
        every { mockDbDao.retrieveFollowersOf(any(), any()) } returns Observable.error(Exception())
        every { mockGitubService.membersOfOrg(eq(bypassLaneUser.login)) } returns Single.just(bypassLaneFollowers)
        every { mockDbDao.storeGithubMember(any()) } returns Single.just(1)
        every { mockGitubService.downloadFile(any()) } returns RetrofitHelper.fakeBinaryRespSingle()
        every { mockDbDao.storeAvatarImage(any(), any()) } returns Single.just(1)
        every { mockDbDao.addGithubMemberToFollowers(any(), any(), any()) } returns Single.just(1)

        interactorUnderTest().fetchFollowersOf(bypassLaneUser.login).subscribe(testObserver)

        testObserver.awaitTerminalEvent()
        testObserver.assertValueSequence(bypassLaneFollowers)

        verifyOrder {
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
            mockDbDao.retrieveGithubUser(eq(bypassLaneUser.login), eq(cacheInvalidationInterval))
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
            mockDbDao.retrieveFollowersOf(eq(bypassLaneUser.id), eq(cacheInvalidationInterval))
            mockGitubService.membersOfOrg(eq(bypassLaneUser.login))

            bypassLaneFollowers.forEach { follower ->
                mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
                mockDbDao.retrieveGithubUser(eq(follower.login), eq(cacheInvalidationInterval))
                mockDbDao.storeGithubMember(eq(follower))
                mockGitubService.downloadFile(eq(follower.avatarUrl))
                mockDbDao.storeAvatarImage(eq(follower.id), eq(RetrofitHelper.byteArray))
                mockDbDao.addGithubMemberToFollowers(eq(bypassLaneUser.id), eq(follower.id), any())
            }
        }
    }

    @Test
    @DisplayName("Should sync when DbDao does not find valid followers and not error when storage of avatar image or follower fails")
    fun storeAvatarImageAndFollowerFails() {
        every { mockDbDao.retrieveFollowersOf(any(), any()) } returns Observable.error(Exception())
        every { mockGitubService.membersOfOrg(eq(bypassLaneUser.login)) } returns Single.just(bypassLaneFollowers)
        every { mockDbDao.storeGithubMember(any()) } returns Single.just(1)
        every { mockGitubService.downloadFile(any()) } returns RetrofitHelper.fakeBinaryRespSingle()
        every { mockDbDao.storeAvatarImage(any(), any()) } returns Single.error(Exception())
        every { mockDbDao.addGithubMemberToFollowers(any(), any(), any()) } returns Single.error(Exception())

        interactorUnderTest().fetchFollowersOf(bypassLaneUser.login).subscribe(testObserver)

        testObserver.awaitTerminalEvent()
        testObserver.assertValueSequence(bypassLaneFollowers)

        verifyOrder {
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
            mockDbDao.retrieveGithubUser(eq(bypassLaneUser.login), eq(cacheInvalidationInterval))
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
            mockDbDao.retrieveFollowersOf(eq(bypassLaneUser.id), eq(cacheInvalidationInterval))
            mockGitubService.membersOfOrg(eq(bypassLaneUser.login))

            bypassLaneFollowers.forEach { follower ->
                mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
                mockDbDao.retrieveGithubUser(eq(follower.login), eq(cacheInvalidationInterval))
                mockDbDao.storeGithubMember(eq(follower))
                mockGitubService.downloadFile(eq(follower.avatarUrl))
                mockDbDao.storeAvatarImage(eq(follower.id), eq(RetrofitHelper.byteArray))
                mockDbDao.addGithubMemberToFollowers(eq(bypassLaneUser.id), eq(follower.id), any())
            }
        }
    }

    @Test
    @DisplayName("Should not error when DbDao does not find valid followers and storage of GithubMember fails--but should not download avatar image in this case")
    fun shouldNotErrorWhenSyncStorageOfFollowerFails() {
        every { mockDbDao.retrieveFollowersOf(any(), any()) } returns Observable.error(Exception())
        every { mockGitubService.membersOfOrg(eq(bypassLaneUser.login)) } returns Single.just(bypassLaneFollowers)
        every { mockDbDao.storeGithubMember(any()) } returns Single.error(Exception())

        interactorUnderTest().fetchFollowersOf(bypassLaneUser.login).subscribe(testObserver)

        testObserver.awaitTerminalEvent()
        testObserver.assertValueSequence(bypassLaneFollowers)

        verifyOrder {
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
            mockDbDao.retrieveGithubUser(eq(bypassLaneUser.login), eq(cacheInvalidationInterval))
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
            mockDbDao.retrieveFollowersOf(eq(bypassLaneUser.id), eq(cacheInvalidationInterval))
            mockGitubService.membersOfOrg(eq(bypassLaneUser.login))

            bypassLaneFollowers.forEach { follower ->
                mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
                mockDbDao.retrieveGithubUser(eq(follower.login), eq(cacheInvalidationInterval))
                mockDbDao.storeGithubMember(eq(follower))
            }
        }
    }

    @Test
    @DisplayName("Should requery without validInterval when cannot retrieve followers from network")
    fun shouldRefetchFromDbDaoWhenCannotGetFollowersFromGithubService() {
        every { mockDbDao.retrieveFollowersOf(any(), eq(cacheInvalidationInterval)) } returns Observable.error(Exception())
        every { mockDbDao.retrieveFollowersOf(any(), not(cacheInvalidationInterval)) } returns Observable.fromIterable(bypassLaneFollowers)
        every { mockGitubService.membersOfOrg(eq(bypassLaneUser.login)) } returns Single.error(Exception())

        interactorUnderTest().fetchFollowersOf(bypassLaneUser.login).subscribe(testObserver)

        testObserver.awaitTerminalEvent()
        testObserver.assertValueSequence(bypassLaneFollowers)

        verifyOrder {
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
            mockDbDao.retrieveGithubUser(eq(bypassLaneUser.login), eq(cacheInvalidationInterval))
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
            mockDbDao.retrieveFollowersOf(eq(bypassLaneUser.id), eq(cacheInvalidationInterval))
            mockGitubService.membersOfOrg(eq(bypassLaneUser.login))
            mockDbDao.retrieveFollowersOf(eq(bypassLaneUser.id), any())
        }
    }

    @Test
    @DisplayName("Should filter with given filter when synced")
    fun shouldCorrectlyApplyFilterWhenDbDaoFindsValidMembers() {
        every { mockDbDao.retrieveFollowersOf(any(), eq(cacheInvalidationInterval), eq("p")) } returns Observable.just(bypassLaneFollowers[0])

        interactorUnderTest().fetchFollowersOf(bypassLaneUser.login, userNameFilter = "p").subscribe(testObserver)

        testObserver.awaitTerminalEvent()
        testObserver.assertValueSequence(listOf(bypassLaneFollowers[0]))

        verifyOrder {
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
            mockDbDao.retrieveGithubUser(eq(bypassLaneUser.login), eq(cacheInvalidationInterval))
            mockDbDao.retrieveFollowersOf(eq(bypassLaneUser.id), eq(cacheInvalidationInterval), eq("p"))
        }
    }

    @Test
    @DisplayName("Should filter with given filter when not synced")
    fun shouldCorrectlyApplyFilterWhenDbDaoDoesNotFindValidMembers() {
        every { mockDbDao.retrieveFollowersOf(any(), any(), any()) } returns Observable.error(Exception())
        every { mockGitubService.membersOfOrg(eq(bypassLaneUser.login)) } returns Single.just(bypassLaneFollowers)
        every { mockDbDao.storeGithubMember(any()) } returns Single.just(1)
        every { mockGitubService.downloadFile(any()) } returns RetrofitHelper.fakeBinaryRespSingle()
        every { mockDbDao.storeAvatarImage(any(), any()) } returns Single.just(1)
        every { mockDbDao.addGithubMemberToFollowers(any(), any(), any()) } returns Single.just(1)

        interactorUnderTest().fetchFollowersOf(bypassLaneUser.login, userNameFilter = "p").subscribe(testObserver)

        testObserver.awaitTerminalEvent()
        testObserver.assertValueSequence(listOf(bypassLaneFollowers[0]))

        verifyOrder {
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
            mockDbDao.retrieveGithubUser(eq(bypassLaneUser.login), eq(cacheInvalidationInterval))
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
            mockDbDao.retrieveFollowersOf(eq(bypassLaneUser.id), eq(cacheInvalidationInterval), eq("p"))
            mockGitubService.membersOfOrg(eq(bypassLaneUser.login))

            bypassLaneFollowers.forEach { follower ->
                mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalCacheInvalidationDefault))
                mockDbDao.retrieveGithubUser(eq(follower.login), eq(cacheInvalidationInterval))
                mockDbDao.storeGithubMember(eq(follower))
                mockGitubService.downloadFile(eq(follower.avatarUrl))
                mockDbDao.storeAvatarImage(eq(follower.id), eq(RetrofitHelper.byteArray))
                mockDbDao.addGithubMemberToFollowers(eq(bypassLaneUser.id), eq(follower.id), any())
            }
        }
    }
}