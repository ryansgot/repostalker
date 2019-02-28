package com.fsryan.repostalker

/**
 * [ComponentsLoader] that always returns [DepInjector]
 */
internal object ComponentsLoader {
    fun loadComponentsOf(app: App): Components = DepInjector(app)
}