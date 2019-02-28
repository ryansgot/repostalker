package com.fsryan.repostalker.interactor

import com.fsryan.repostalker.current.Current
import com.fsryan.repostalker.data.AdapterFactory
import com.fsryan.repostalker.data.GithubUser
import com.fsryan.repostalker.main.Main
import com.fsryan.repostalker.testonly.RetrofitHelper
import com.fsryan.repostalker.testonly.RxJavaPluginsExtension
import com.fsryan.testtools.Fixtures
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import io.mockk.every
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(RxJavaPluginsExtension::class)
class CurrentInteractorTest : BaseInteractorTest<Current.Interactor>() {

    private lateinit var testObserver: TestObserver<GithubUser>

    companion object {
        private const val expectedInternalDefaultCacheInvalidation = Main.DEFAULT_INVALIDATION_INTERVAL * 1000
        private const val cacheInvalidationInterval = 42L
        private lateinit var ryansgotUser: GithubUser

        @BeforeAll
        @JvmStatic
        fun initRyansgotUser() {
            ryansgotUser = Fixtures(Moshi.Builder()
                    .add(AdapterFactory())
                    .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                    .build()).get("ryansgot_user.json", GithubUser::class.java)
        }

    }

    @BeforeEach
    fun setUpTestObserverAndSetValueForCacheInvalidationInterval() {
        testObserver = TestObserver()
        every { mockUserPrefs.retrieveCacheInvalidationInterval(any()) } returns Single.just(cacheInvalidationInterval)
    }

    @AfterEach
    fun flushTestObserver() {
        testObserver.dispose()
    }

    @Test
    @DisplayName("DbDao finding valid user means network should not be used")
    fun shouldNotRetrieveUserFromNetworkWhenDbDaoFindsValidUser() {
        every { mockDbDao.retrieveGithubUser(any(), any()) } returns Single.just(ryansgotUser)

        interactorUnderTest().fetchGithubUser(ryansgotUser.login).subscribe(testObserver)

        testObserver.awaitTerminalEvent()
        testObserver.assertValue(ryansgotUser)

        // this ensures that 0 will not be used for the cache invalidation
        // interval, even though the interactor does not know a good default
        verifyOrder {
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalDefaultCacheInvalidation))
            mockDbDao.retrieveGithubUser(eq(ryansgotUser.login), eq(cacheInvalidationInterval))
        }
        // ensures that, when the dbdao finds a valid user, the user will not
        // be retrieved from the network
        verify(exactly = 0) { mockGitubService.user(any()) }
        verify(exactly = 0) { mockGitubService.downloadFile(any()) }
    }

    @Test
    @DisplayName("DbDao NOT finding valid user means network should be used to get GithubUser")
    fun shouldRetrieveUserFromNetworkWhenDbDaoDoesNotFindValidUser() {
        every { mockDbDao.retrieveGithubUser(any(), any()) } returns Single.error(Exception())
        every { mockGitubService.user(any()) } returns Single.just(ryansgotUser)
        every { mockDbDao.storeGithubUser(any()) } returns Single.just(1)
        every { mockGitubService.downloadFile(any()) } returns RetrofitHelper.fakeBinaryRespSingle()
        every { mockDbDao.storeAvatarImage(any(), any()) } returns Single.just(1)

        interactorUnderTest().fetchGithubUser(ryansgotUser.login).subscribe(testObserver)

        testObserver.awaitTerminalEvent()
        testObserver.assertValue(ryansgotUser)

        // ensures correct ordering of calls to synchronize user locally
        verifyOrder {
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalDefaultCacheInvalidation))
            mockDbDao.retrieveGithubUser(eq(ryansgotUser.login), eq(cacheInvalidationInterval))
            mockGitubService.user(eq(ryansgotUser.login))
            mockDbDao.storeGithubUser(eq(ryansgotUser))
            mockGitubService.downloadFile(eq(ryansgotUser.avatarUrl))
            mockDbDao.storeAvatarImage(eq(ryansgotUser.id), any())
        }
    }

    @Test
    @DisplayName("Should not attempt to retrieve avatar image if user storage failed because storage will fail")
    fun shouldNotAttemptToRetrieveAvatarImageInfUserStorageFailed() {
        every { mockDbDao.retrieveGithubUser(any(), any()) } returns Single.error(Exception())
        every { mockGitubService.user(any()) } returns Single.just(ryansgotUser)
        every { mockDbDao.storeGithubUser(any()) } returns Single.error(Exception())

        interactorUnderTest().fetchGithubUser(ryansgotUser.login).subscribe(testObserver)

        testObserver.awaitTerminalEvent()
        testObserver.assertValue(ryansgotUser)

        // ensures correct ordering of calls to synchronize user locally
        verifyOrder {
            mockUserPrefs.retrieveCacheInvalidationInterval(eq(expectedInternalDefaultCacheInvalidation))
            mockDbDao.retrieveGithubUser(eq(ryansgotUser.login), eq(cacheInvalidationInterval))
            mockGitubService.user(eq(ryansgotUser.login))
            mockDbDao.storeGithubUser(eq(ryansgotUser))
        }
        // ensures that the avatar url will not be downloaded or storage attempted
        verify(exactly = 0) { mockGitubService.downloadFile(any()) }
        verify(exactly = 0) { mockDbDao.storeAvatarImage(any(), any()) }
    }

    @Test
    @DisplayName("Should pass through request for avatar image to dbDao")
    fun shouldPassThroughRequestForAvatarImageToDbDao() {
        val imageObserver = TestObserver<ByteArray>()
        val expected = ByteArray(0)
        every { mockDbDao.retrieveAvatarImage(any()) } returns Single.just(expected)

        interactorUnderTest().fetchAvatarImage(ryansgotUser.id).subscribe(imageObserver)

        imageObserver.awaitTerminalEvent()
        imageObserver.assertValue(expected)

        verify { mockDbDao.retrieveAvatarImage(eq(ryansgotUser.id)) }
    }
}