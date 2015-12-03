package com.lyft.android.scissors;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;

class UILFillViewportDisplayer implements BitmapDisplayer {
    private final int viewportWidth;
    private final int viewportHeight;

    public UILFillViewportDisplayer(int viewportWidth, int viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    public static BitmapDisplayer createUsing(int viewportWidth, int viewportHeight) {
        return new UILFillViewportDisplayer(viewportWidth, viewportHeight);
    }

    @Override
    public void display(Bitmap source, ImageAware imageAware, LoadedFrom loadedFrom) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        Rect target = CropViewExtensions.computeTargetSize(sourceWidth, sourceHeight, viewportWidth, viewportHeight);
        final Bitmap result = Bitmap.createScaledBitmap(
                source,
                target.width(),
                target.height(),
                true);

        if (result != source) {
            source.recycle();
        }

        imageAware.setImageBitmap(result);
    }
}
