package com.fsryan.repostalker

import android.util.Log

/**
 * [ComponentsLoader] that first checks for the presence of the FakeComponents
 * class and returns that if it's there (will be there for instrumented test
 * cases) and returns the normal [DepInjector] if not.
 */
internal object ComponentsLoader {
    fun loadComponentsOf(app: App): Components {
        try {
            return Class.forName("com.fsryan.repostalker.FakeComponents")
                .asSubclass(Components::class.java)
                .newInstance()
        } catch (e: Exception) {
            Log.e("ComponentsLoader", "Not in test environment", e)
        }

        return DepInjector(app)
    }
}