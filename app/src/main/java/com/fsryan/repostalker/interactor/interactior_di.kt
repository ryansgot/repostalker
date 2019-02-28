package com.fsryan.repostalker.interactor

import com.fsryan.repostalker.App
import com.fsryan.repostalker.current.Current
import com.fsryan.repostalker.data.DataModule
import com.fsryan.repostalker.data.db.DbDao
import com.fsryan.repostalker.data.nav.Navigator
import com.fsryan.repostalker.data.network.GithubService
import com.fsryan.repostalker.data.prefs.UserPrefs
import com.fsryan.repostalker.followerlist.FollowerList
import com.fsryan.repostalker.main.Main
import dagger.Component
import dagger.Module
import dagger.Provides

@Module
class InteractorModule {
    @Provides
    @App.Scope
    fun mainInteractor(interactor: Interactor): Main.Interactor = interactor

    @Provides
    @App.Scope
    fun currentInteractor(interactor: Interactor): Current.Interactor = interactor

    @Provides
    @App.Scope
    fun followerListInteractor(interactor: Interactor): FollowerList.Interactor = interactor

    @Provides
    @App.Scope
    fun interactor(navigator: Navigator, userPrefs: UserPrefs, dbDao: DbDao, githubService: GithubService) = Interactor.create(navigator, userPrefs, dbDao, githubService)
}

@App.Scope
@Component(modules = [InteractorModule::class, DataModule::class])
interface InteractorComponent {
    fun mainInteractor(): Main.Interactor
    fun currentInteractor(): Current.Interactor
    fun followerListInteractor(): FollowerList.Interactor
}