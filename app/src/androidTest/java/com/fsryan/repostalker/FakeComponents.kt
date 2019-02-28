package com.fsryan.repostalker

import android.support.test.InstrumentationRegistry
import com.fsryan.repostalker.current.Current
import com.fsryan.repostalker.current.CurrentFragment

import com.fsryan.repostalker.current.CurrentFragmentComponent
import com.fsryan.repostalker.followerlist.FollowerList
import com.fsryan.repostalker.followerlist.FollowerListFragment
import com.fsryan.repostalker.followerlist.FollowerListFragmentComponent
import com.fsryan.repostalker.main.Main
import com.fsryan.repostalker.main.MainActivity
import com.fsryan.repostalker.main.MainActivityComponent

/**
 * Facilitates injection of mock or fake components such that mocks or fakes
 * may be injected into instrumentation tests. For example, when testing an
 * [android.app.Activity], you should override
 * [android.support.test.rule.ActivityTestRule.beforeActivityLaunched] and
 * register the presenter object you want to inject by calling
 * ```kotlin
 * FakeComponents.get().addToInjectionRegistry(YourPresenter::class.java, yourPresenter)
 * ```
 * Then after your test activity finishes
 * ([android.support.test.rule.ActivityTestRule.afterActivityFinished]), you
 * should unregister the presenter by calling
 * ```kotlin
 * FakeComponents.get().removeFromInjectionRegistry<YourPresenter>(YourPresenter::class.java)
 * ```
 */
class FakeComponents : Components, MainActivityComponent, CurrentFragmentComponent, FollowerListFragmentComponent {
    private val injectionRegistry: MutableMap<Class<*>, Any> = mutableMapOf()

    companion object {
        fun get() = App.componentsOf(InstrumentationRegistry.getTargetContext()) as FakeComponents
    }

    fun addToInjectionRegistry(cls: Class<*>, obj: Any) = injectionRegistry.put(cls, obj)
    fun <T> removeFromInjectionRegistry(cls: Class<*>) = injectionRegistry.remove(cls) as T

    override fun mainActivityComponent() = this
    override fun currentFragementComponent() = this
    override fun followerListFragmentComponent() = this

    override fun inject(activity: MainActivity) {
        activity.presenter = injectionRegistry[Main.Presenter::class.java] as Main.Presenter
    }

    override fun inject(fragment: CurrentFragment) {
        fragment.presenter = injectionRegistry[Current.Presenter::class.java] as Current.Presenter
    }

    override fun inject(fragment: FollowerListFragment) {
        fragment.presenter = injectionRegistry[FollowerList.Presenter::class.java] as FollowerList.Presenter
    }
}