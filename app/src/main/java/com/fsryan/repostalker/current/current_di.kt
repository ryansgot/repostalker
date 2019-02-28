package com.fsryan.repostalker.current

import com.fsryan.repostalker.interactor.InteractorComponent
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.SOURCE)
@Target(allowedTargets = [AnnotationTarget.CLASS, AnnotationTarget.FUNCTION])
annotation class CurrentFragmentScope

@Module
class CurrentFragmentModule {
    @Provides
    @CurrentFragmentScope
    fun currentPresenter(interactor: Current.Interactor): Current.Presenter = Current.createPresenter(interactor)
}

@CurrentFragmentScope
@Component(modules = [CurrentFragmentModule::class], dependencies = [InteractorComponent::class])
interface CurrentFragmentComponent {
    fun inject(fragment: CurrentFragment)
}