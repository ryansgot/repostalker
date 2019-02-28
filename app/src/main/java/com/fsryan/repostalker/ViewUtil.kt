package com.fsryan.repostalker

import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.view.View
import java.io.ByteArrayInputStream

object ViewUtil {
    fun bytesToBitmap(bytes: ByteArray?): Bitmap? {
        if (bytes == null) {
            return null
        }

        val bytes = ByteArrayInputStream(bytes)
        bytes.use { data ->
            return BitmapDrawable(data).bitmap
        }
    }

    fun setVisibility(visibility: Int, vararg views: View) {
        views.forEach { it.visibility = visibility }
    }
}