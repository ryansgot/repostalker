package com.fsryan.repostalker

import android.app.Application
import android.content.Context
import com.fsryan.forsuredb.FSDBHelper
import com.fsryan.forsuredb.ForSureAndroidInfoFactory
import com.fsryan.forsuredb.moshiserialization.FSDbInfoMoshiSerializer

class App : Application() {

    /**
     * Defines a scope at which components can inject dependencies. [App.Scope]
     * is intended to exist for the entirety of the application
     */
    @Retention(AnnotationRetention.SOURCE)
    @Target(allowedTargets = [AnnotationTarget.CLASS, AnnotationTarget.FUNCTION])
    @javax.inject.Scope
    annotation class Scope

    lateinit private var components: Components

    companion object {
        fun componentsOf(context: Context) = (context.applicationContext as App).components
    }

    override fun onCreate() {
        super.onCreate()
        components = ComponentsLoader.loadComponentsOf(this)
        initForsureDb()
    }

    private fun initForsureDb() {
        val authority = "com.fsryan.repostalker.content"
        val tables = TableGenerator.generate(authority)
        val serializer = FSDbInfoMoshiSerializer()
        if (BuildConfig.DEBUG) {
            FSDBHelper.initDebug(this, "stalker-debug.db", tables, serializer)
        } else {
            FSDBHelper.init(this, "stalker.db", tables, serializer)
        }

        ForSureAndroidInfoFactory.init(this, authority)
        ForSure.init(ForSureAndroidInfoFactory.inst())
    }
}