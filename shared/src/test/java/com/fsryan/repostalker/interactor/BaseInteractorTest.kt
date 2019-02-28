package com.fsryan.repostalker.interactor

import com.fsryan.repostalker.data.db.DbDao
import com.fsryan.repostalker.data.nav.Navigator
import com.fsryan.repostalker.data.network.GithubService
import com.fsryan.repostalker.data.prefs.UserPrefs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
abstract class BaseInteractorTest<T> {
    private lateinit var interactor: Interactor

    @RelaxedMockK
    protected lateinit var mockNavigator: Navigator
    @RelaxedMockK
    protected lateinit var mockUserPrefs: UserPrefs
    @RelaxedMockK
    protected lateinit var mockDbDao: DbDao
    @RelaxedMockK
    protected lateinit var mockGitubService: GithubService

    @BeforeEach
    fun initializeInteractorUnderTest() {
        interactor = Interactor.create(mockNavigator, mockUserPrefs, mockDbDao, mockGitubService)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun interactorUnderTest(): T = interactor as T
}