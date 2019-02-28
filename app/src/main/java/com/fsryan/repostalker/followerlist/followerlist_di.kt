package com.fsryan.repostalker.followerlist

import com.fsryan.repostalker.interactor.InteractorComponent
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.SOURCE)
@Target(allowedTargets = [AnnotationTarget.CLASS, AnnotationTarget.FUNCTION])
annotation class FollowerListFragmentScope

@Module
class FollowerListFragmentModule {
    @Provides
    @FollowerListFragmentScope
    fun followerListPresenter(interactor: FollowerList.Interactor): FollowerList.Presenter = FollowerList.createPresenter(interactor)
}

@FollowerListFragmentScope
@Component(modules = [FollowerListFragmentModule::class], dependencies = [InteractorComponent::class])
interface FollowerListFragmentComponent {
    fun inject(fragment: FollowerListFragment)
}