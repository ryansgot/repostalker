package com.fsryan.repostalker.data.prefs

import android.content.Context
import android.content.SharedPreferences
import io.reactivex.Completable
import io.reactivex.Single
import java.lang.IllegalStateException

fun createUserPrefs(context: Context): UserPrefs = UserPrefsImpl(context)

private class UserPrefsImpl(context: Context) : UserPrefs {
    private val context = context.applicationContext

    companion object {
        private const val PREF_NAME = "user_prefs"
        private const val INVALIDATION_INTERVAL_KEY = "invalidation_interval"
    }

    override fun storeCacheInvalidationInterval(interval: Long) = Completable.fromAction {
        if (!acquirePrefs().edit().putLong(INVALIDATION_INTERVAL_KEY, interval).commit()) {
            throw IllegalStateException("Failed storage of invalidation interval")
        }
    }

    override fun retrieveCacheInvalidationInterval(defaultValue: Long) = Single.fromCallable<Long> {
        acquirePrefs().getLong(INVALIDATION_INTERVAL_KEY, defaultValue)
    }

    private fun acquirePrefs(): SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}