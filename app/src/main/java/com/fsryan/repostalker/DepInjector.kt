package com.fsryan.repostalker

import com.fsryan.repostalker.current.CurrentFragmentComponent
import com.fsryan.repostalker.current.DaggerCurrentFragmentComponent
import com.fsryan.repostalker.data.DataModule
import com.fsryan.repostalker.followerlist.DaggerFollowerListFragmentComponent
import com.fsryan.repostalker.followerlist.FollowerListFragmentComponent
import com.fsryan.repostalker.interactor.DaggerInteractorComponent
import com.fsryan.repostalker.main.DaggerMainActivityComponent
import com.fsryan.repostalker.main.MainActivityComponent

internal class DepInjector(app: App): Components {
    private val interactorComponent = DaggerInteractorComponent.builder()
        .dataModule(DataModule(app))
        .build()

    override fun mainActivityComponent(): MainActivityComponent = DaggerMainActivityComponent.builder()
        .interactorComponent(interactorComponent)
        .build()

    override fun currentFragementComponent(): CurrentFragmentComponent = DaggerCurrentFragmentComponent.builder()
        .interactorComponent(interactorComponent)
        .build()

    override fun followerListFragmentComponent(): FollowerListFragmentComponent = DaggerFollowerListFragmentComponent.builder()
        .interactorComponent(interactorComponent)
        .build()
}