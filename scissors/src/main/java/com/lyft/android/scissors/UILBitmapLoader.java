package com.lyft.android.scissors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;

/**
 * A {@link BitmapLoader} with transformation for {@link ImageLoader} image library.
 *
 * @see UILBitmapLoader#createUsing(CropView)
 * @see UILBitmapLoader#createUsing(CropView, ImageLoader)
 */
public class UILBitmapLoader implements BitmapLoader {

    private final ImageLoader imageLoader;
    private final BitmapDisplayer bitmapDisplayer;

    public UILBitmapLoader(ImageLoader imageLoader, BitmapDisplayer bitmapDisplayer) {
        this.imageLoader = imageLoader;
        this.bitmapDisplayer = bitmapDisplayer;
    }

    public static BitmapLoader createUsing(CropView cropView) {
        return createUsing(cropView, ImageLoader.getInstance());
    }

    public static BitmapLoader createUsing(CropView cropView, ImageLoader imageLoader) {
        return new UILBitmapLoader(imageLoader, UILFillViewportDisplayer.createUsing(cropView.getViewportWidth(), cropView.getViewportHeight()));
    }

    @Override
    public void load(@Nullable Object model, @NonNull ImageView view) {
        final DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(false)
                .cacheOnDisk(false)
                .displayer(bitmapDisplayer)
                .build();

        if (model instanceof String || model == null) {
            imageLoader.displayImage((String) model, view, options);
        } else {
            throw new IllegalArgumentException("Unsupported model " + model);
        }

    }
}
