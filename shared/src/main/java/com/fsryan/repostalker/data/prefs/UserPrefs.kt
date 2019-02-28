package com.fsryan.repostalker.data.prefs

import io.reactivex.Completable
import io.reactivex.Single

interface UserPrefs {
    /**
     * Interval is in seconds
     */
    fun retrieveCacheInvalidationInterval(defaultValue: Long): Single<Long>

    /**
     * Interval is in seconds
     */
    fun storeCacheInvalidationInterval(interval: Long): Completable
}