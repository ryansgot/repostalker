package com.fsryan.repostalker.testonly

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v7.app.AppCompatActivity

/**
 * Intended to aid in testing fragments. Since all Fragment classes are views,
 * and since all views in ViPID are 100% independent of other views, fragments
 * may be attached to any activity and work the exact same way they otherwise
 * would work.
 */
class EmptyActivity: AppCompatActivity() {

    companion object {
        fun intent(context: Context, @LayoutRes layoutRes: Int): Intent {
            return Intent(context, EmptyActivity::class.java)
                .putExtra(EXTRA_LAYOUT_ID, layoutRes)
        }

        private const val EXTRA_LAYOUT_ID = "extra_layout_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.extras != null && intent.extras!!.containsKey(EXTRA_LAYOUT_ID)) {
            setContentView(intent.extras!!.getInt(EXTRA_LAYOUT_ID))
        }
    }
}