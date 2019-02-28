package com.fsryan.repostalker.data.prefs

import android.content.Context
import android.os.Environment
import android.support.test.InstrumentationRegistry.getTargetContext
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

/**
 * This rule will output the user prefs xml to the sdcard after each test and
 * clear the preferences before each test
 */
class PrefsTestRule(private val prefName: String) : ExternalResource() {
    private val outDir = File(Environment.getExternalStorageDirectory(), "test-ookla-out")
    private lateinit var outPath: String

    override fun apply(base: Statement, description: Description): Statement {
        // ensure that the test-ookla-out directory exists, if not, make it
        if (!outDir.exists()) {
            outDir.mkdirs()
            outDir.mkdir()
        }

        outPath = "${description.testClass.simpleName}${File.separator}${description.methodName}.prefs.xml"
        return super.apply(base, description)
    }

    override fun before() {
        getTargetContext().getSharedPreferences(prefName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    override fun after() {
        val outFile = File(outDir, outPath)
        outFile.delete()
        val prefsFile = File("${File.separator}data${File.separator}data${File.separator}${getTargetContext().packageName}${File.separator}shared_prefs", "$prefName.xml")
        prefsFile.copyTo(outFile)
    }
}