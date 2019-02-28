package com.fsryan.repostalker.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * <p>I wrote this in java because I took the bulk of it from this gist:
 * <a href="https://gist.github.com/ryansgot/e68d48947f957d81981135cd9b900e34">A jist on ryansgot's github page</a>
 */
public class DrawableMatcher extends TypeSafeMatcher<View> {

    public interface Extractor {
        @NonNull
        Drawable extract(@NonNull View v);
        Extractor FOR_IMAGE_VIEW = new Extractor() {
            @NonNull
            @Override
            public Drawable extract(@NonNull View v) {
                return ((ImageView) v).getDrawable();
            }
        };
    }

    private Context expectedDrawableContext;
    private String mReason;
    @DrawableRes
    private final int mDrawableId;
    private ColorFilter mExpectedColorFilter;
    private Extractor mExtractor;

    DrawableMatcher(Context expectedDrawableContext, @DrawableRes int drawableId, @Nullable ColorFilter expectedColorFilter, Extractor extractor) {
        this.expectedDrawableContext = expectedDrawableContext;
        mDrawableId = drawableId;
        mExpectedColorFilter = expectedColorFilter;
        mExtractor = extractor;
    }

    public static DrawableMatcher ofImageView(Context expectedDrawableContext, @DrawableRes int drawableId) {
        return ofImageView(expectedDrawableContext, drawableId, null);
    }

    public static DrawableMatcher ofImageView(Context expectedDrawableContext, @DrawableRes int drawableId, @Nullable ColorFilter expectedColorFilter) {
        return new DrawableMatcher(expectedDrawableContext, drawableId, expectedColorFilter, Extractor.FOR_IMAGE_VIEW);
    }

    @Override
    protected boolean matchesSafely(View target) {
        if (!(target instanceof ImageView) && mExtractor == null) {
            mReason = "view " + target.getId() + " is not an " + ImageView.class.getSimpleName() + ". Pass in a custom Extractor in order to use this matcher.";
            return false;
        }
        Drawable actualDrawable = mExtractor == null ? defaultExtract(target) : mExtractor.extract(target);
        if (mDrawableId < 0 && actualDrawable != null) {
            mReason = "expected no drawable for view " + target.getId() + ", but has one";
            return false;
        }

        Context expectedContext = expectedDrawableContext == null ? target.getContext() : expectedDrawableContext;
        Drawable expectedDrawable = ViewTestUtil.drawableById(expectedContext, mDrawableId);
        if (expectedDrawable == null) {
            mReason = "drawable with id " + mDrawableId + " does not exist";
            return false;
        }
        expectedDrawable.setColorFilter(mExpectedColorFilter);

        Bitmap expected = ViewTestUtil.getBitmap(expectedDrawable);
        Bitmap actual = ViewTestUtil.getBitmap(actualDrawable);
        if (!expected.sameAs(actual)) {
            mReason = "expected and actual bitmaps do not match";
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(mReason == null ? "" : mReason);
    }

    private static Drawable defaultExtract(View target) {
        ImageView targetImageView = (ImageView) target;
        return targetImageView.getDrawable();
    }
}
