package com.fsryan.repostalker

import com.fsryan.repostalker.current.CurrentFragmentComponent
import com.fsryan.repostalker.followerlist.FollowerListFragmentComponent
import com.fsryan.repostalker.main.MainActivityComponent

/**
 * This is the interface at which your testing code diverges from your
 * production code. Note that there are two implementations of this
 * 1. [com.fsryan.ryan.repostalker.FakeComponents]
 * 2. [com.fsryan.ryan.repostalker.DepInjector]
 *
 * FakeComponents is the one that injects dependencies into activities
 * or fragments or other platform-instantiated classes. DepInjector is the one
 * which makes use of dagger 2 and injects dependencies into your production
 * code.
 */
interface Components {
    fun mainActivityComponent(): MainActivityComponent
    fun currentFragementComponent(): CurrentFragmentComponent
    fun followerListFragmentComponent(): FollowerListFragmentComponent
}