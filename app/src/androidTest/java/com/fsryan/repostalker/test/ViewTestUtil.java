package com.fsryan.repostalker.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * <p>I wrote this in java because I took the bulk of it from this gist:
 * <a href="https://gist.github.com/ryansgot/e68d48947f957d81981135cd9b900e34">A jist on ryansgot's github page</a>
 */
public abstract class ViewTestUtil {

    @DrawableRes
    public static int pedlarDrawable() {
        return com.fsryan.repostalker.test.R.drawable.pedlar;
    }

    @DrawableRes
    public static int ryansgotDrawable() {
        return com.fsryan.repostalker.test.R.drawable.ryansgot;
    }

    @DrawableRes
    public static int steveChalkerDrawable() {
        return com.fsryan.repostalker.test.R.drawable.stevechalker;
    }

    public static Bitmap getBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Drawable drawableById(Context context, @DrawableRes int id) {
        final Drawable drawable = ContextCompat.getDrawable(context, id);
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                ? (DrawableCompat.wrap(drawable)).mutate()
                : drawable;
    }

    public static byte[] bytesOfDrawable(Drawable d) {
        Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] ret = stream.toByteArray();
        try {
            return ret;
        } finally {
            try {
                stream.close();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    public static byte[] bytesOfDrawable(Context context, @DrawableRes int id) {
        return bytesOfDrawable(ContextCompat.getDrawable(context, id));
    }
}
