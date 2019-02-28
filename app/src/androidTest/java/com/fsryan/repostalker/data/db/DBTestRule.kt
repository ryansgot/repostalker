package com.fsryan.repostalker.data.db

import android.os.Environment
import android.support.test.InstrumentationRegistry
import com.fsryan.repostalker.ForSure.githubMembersTable
import com.fsryan.repostalker.ForSure.githubUsersTable
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

/**
 * This rule will output the database to the sdcard for each test so that,
 * should you need to view the database in an external viewer to see the final
 * state of the database for the test, you can. It also clears the data before
 * each test.
 */
class DBTestRule(private val dbName: String) : ExternalResource() {

    private val outDir = File(Environment.getExternalStorageDirectory(), "test-repostalker-out")
    private lateinit var outPath: String

    override fun apply(base: Statement?, description: Description?): Statement {
        if (!outDir.exists()) {
            outDir.mkdirs()
            outDir.mkdir()
        }
        outPath = "${description!!.testClass.simpleName}/${description.methodName}.db"
        return super.apply(base, description)
    }

    override fun before() {
        // deletes all data
        githubUsersTable().set().hardDelete()
        githubMembersTable().set().hardDelete()
    }

    override fun after() {
        val outFile = File(outDir, outPath)
        outFile.delete()
        val dbFile = InstrumentationRegistry.getTargetContext().getDatabasePath(dbName)
        dbFile.copyTo(outFile)
    }
}