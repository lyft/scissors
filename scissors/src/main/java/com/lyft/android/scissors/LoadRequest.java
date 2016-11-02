package com.lyft.android.scissors;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.view.ViewTreeObserver;

import static com.lyft.android.scissors.CropViewExtensions.resolveBitmapLoader;

public class LoadRequest {

  private final CropView cropView;
  private BitmapLoader bitmapLoader;

  LoadRequest(CropView cropView) {
    Utils.checkNotNull(cropView, "cropView == null");
    this.cropView = cropView;
  }

  /**
   * Load a {@link Bitmap} using given {@link BitmapLoader}, you must call {@link LoadRequest#load(Object)} afterwards.
   *
   * @param bitmapLoader {@link BitmapLoader} to use
   * @return current request for chaining, you should call {@link #load(Object)} afterwards.
   */
  public LoadRequest using(@Nullable BitmapLoader bitmapLoader) {
    this.bitmapLoader = bitmapLoader;
    return this;
  }

  /**
   * Load a {@link Bitmap} using a {@link BitmapLoader} into {@link CropView}
   *
   * @param model Model used by {@link BitmapLoader} to load desired {@link Bitmap}
   */
  public void load(@Nullable Object model) {
    if (cropView.getWidth() == 0 && cropView.getHeight() == 0) {
      // Defer load until layout pass
      deferLoad(model);
      return;
    }
    performLoad(model);
  }

  void performLoad(Object model) {
    if (bitmapLoader == null) {
      bitmapLoader = resolveBitmapLoader(cropView);
    }
    bitmapLoader.load(model, cropView);
  }

  void deferLoad(final Object model) {
    if (!cropView.getViewTreeObserver().isAlive()) {
      return;
    }
    cropView.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            if (cropView.getViewTreeObserver().isAlive()) {
              //noinspection deprecation
              cropView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
            performLoad(model);
          }
        }
    );
  }
}
