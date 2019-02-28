package com.fsryan.repostalker.data

import android.content.Context
import com.fsryan.repostalker.data.network.ReferrerInterceptor
import com.fsryan.testtools.Fixtures
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class DataModuleTest {

    @RelaxedMockK
    private lateinit var mockContext: Context
    private lateinit var moduleUnderTest: DataModule

    @BeforeEach
    fun initializeDataModule() {
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.packageName } returns "this is the package name of the app"
        moduleUnderTest = DataModule(mockContext)
    }

    @AfterEach
    fun resetAndroidSchedulers() {
        RxAndroidPlugins.reset()
    }

    @Test
    @DisplayName("The Moshi object added to the graph should be able to deserialize github objects")
    fun shouldReturnCorrectMoshi() {
        val fixtures = Fixtures(moduleUnderTest.moshi())
        fixtures.get<GithubUser>("ryansgot_user.json", GithubUser::class.java)
        fixtures.getList<GithubMember>("ryansgot_followers.json", GithubMember::class.java)
    }

    @Test
    @DisplayName("The returned OkHttpClient should add referer to the headers")
    fun shouldReturnCorrectOkHttpClientDebug() {
        val actual = moduleUnderTest.okHttpClient()
        val referrerInterceptor = actual.interceptors().first { it::class.java == ReferrerInterceptor::class.java }
        assertNotNull(referrerInterceptor)
    }

    @Test
    @DisplayName("The returned Retrofit should have the correct baseurl")
    fun shouldReturnCorrectRetrofit() {
        val actual = moduleUnderTest.retrofit(
            moduleUnderTest.okHttpClient(),
            moduleUnderTest.moshiConverterFactory(moduleUnderTest.moshi())
        )
        assertEquals("https://api.github.com/", actual.baseUrl().toString())
    }

    @Test
    @DisplayName("The returned navigator should use the androidschedulers main thread as the subscribeOn scheduler")
    fun shouldReturnCorrectNavigator() {
        // It's worth explaining here that the below redirects the
        // AndroidSchedulers.mainThread() call to the scheduler defined
        // therein. As such you can be sure that the production code
        // will appropriately use AndroidSchedulers.mainThread()
        var count = 0
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.from { runnable -> count++; runnable.run() } }

        val actual = moduleUnderTest.navigator()
        actual.push("").blockingGet()
        actual.peek().blockingGet()
        actual.pop().blockingGet()

        assertEquals(3, count)
    }
}