package com.fsryan.repostalker.main

import com.fsryan.repostalker.interactor.InteractorComponent
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.SOURCE)
@Target(allowedTargets = [AnnotationTarget.CLASS, AnnotationTarget.FUNCTION])
annotation class MainActivityScope

@Module
class MainActivityModule {
    @Provides
    @MainActivityScope
    fun mainPresenter(interactor: Main.Interactor): Main.Presenter = Main.createPresenter(interactor)
}

@MainActivityScope
@Component(modules = [MainActivityModule::class], dependencies = [InteractorComponent::class])
interface MainActivityComponent {
    fun inject(activity: MainActivity)
}