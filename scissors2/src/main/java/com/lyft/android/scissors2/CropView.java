/*
 * Copyright (C) 2015 Lyft, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lyft.android.scissors2;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import java.io.File;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An {@link ImageView} with a fixed viewport and cropping capabilities.
 */
public class CropView extends ImageView {

    private TouchManager touchManager;
    private CropViewConfig config;

    private Paint viewportPaint = new Paint();
    private Paint bitmapPaint = new Paint();

    private Bitmap bitmap;
    private Matrix transform = new Matrix();
    private Extensions extensions;

    /** Corresponds to the values in {@link com.lyft.android.scissors2.R.attr#cropviewShape} */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ Shape.RECTANGLE, Shape.OVAL })
    public @interface Shape {

        int RECTANGLE = 0;
        int OVAL = 1;
    }

    @Shape
    private int shape = Shape.RECTANGLE;
    private Path ovalPath;
    private RectF ovalRect;

    public CropView(Context context) {
        super(context);
        initCropView(context, null);
    }

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initCropView(context, attrs);
    }

    void initCropView(Context context, AttributeSet attrs) {
        config = CropViewConfig.from(context, attrs);

        touchManager = new TouchManager(this, config);

        bitmapPaint.setFilterBitmap(true);
        setViewportOverlayColor(config.getViewportOverlayColor());
        shape = config.shape();

        // We need anti-aliased Paint to smooth the curved edges
        viewportPaint.setFlags(viewportPaint.getFlags() | Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bitmap == null) {
            return;
        }

        drawBitmap(canvas);
        if (shape == Shape.RECTANGLE) {
            drawSquareOverlay(canvas);
        } else {
            drawOvalOverlay(canvas);
        }
    }

    private void drawBitmap(Canvas canvas) {
        transform.reset();
        touchManager.applyPositioningAndScale(transform);

        canvas.drawBitmap(bitmap, transform, bitmapPaint);
    }

    private void drawSquareOverlay(Canvas canvas) {
        final int viewportWidth = touchManager.getViewportWidth();
        final int viewportHeight = touchManager.getViewportHeight();
        final int left = (getWidth() - viewportWidth) / 2;
        final int top = (getHeight() - viewportHeight) / 2;

        canvas.drawRect(0, top, left, getHeight() - top, viewportPaint); // left
        canvas.drawRect(0, 0, getWidth(), top, viewportPaint); // top
        canvas.drawRect(getWidth() - left, top, getWidth(), getHeight() - top, viewportPaint); // right
        canvas.drawRect(0, getHeight() - top, getWidth(), getHeight(), viewportPaint); // bottom
    }

    private void drawOvalOverlay(Canvas canvas) {
        if (ovalRect == null) {
            ovalRect = new RectF();
        }
        if (ovalPath == null) {
            ovalPath = new Path();
        }

        final int viewportWidth = touchManager.getViewportWidth();
        final int viewportHeight = touchManager.getViewportHeight();
        final int left = (getWidth() - viewportWidth) / 2;
        final int top = (getHeight() - viewportHeight) / 2;
        final int right = getWidth() - left;
        final int bottom = getHeight() - top;
        ovalRect.left = left;
        ovalRect.top = top;
        ovalRect.right = right;
        ovalRect.bottom = bottom;

        // top left arc
        ovalPath.reset();
        ovalPath.moveTo(left, getHeight() / 2); // middle of the left side of the circle
        ovalPath.arcTo(ovalRect, 180, 90, false); // draw arc to top
        ovalPath.lineTo(left, top); // move to top-left corner
        ovalPath.lineTo(left, getHeight() / 2); // move back to origin
        ovalPath.close();
        canvas.drawPath(ovalPath, viewportPaint);

        // top right arc
        ovalPath.reset();
        ovalPath.moveTo(getWidth() / 2, top); // middle of the top side of the circle
        ovalPath.arcTo(ovalRect, 270, 90, false); // draw arc to the right
        ovalPath.lineTo(right, top); // move to top-right corner
        ovalPath.lineTo(getWidth() / 2, top); // move back to origin
        ovalPath.close();
        canvas.drawPath(ovalPath, viewportPaint);

        // bottom right arc
        ovalPath.reset();
        ovalPath.moveTo(right, getHeight() / 2); // middle of the right side of the circle
        ovalPath.arcTo(ovalRect, 0, 90, false); // draw arc to the bottom
        ovalPath.lineTo(right, bottom); // move to bottom-right corner
        ovalPath.lineTo(right, getHeight() / 2); // move back to origin
        ovalPath.close();
        canvas.drawPath(ovalPath, viewportPaint);

        // bottom left arc
        ovalPath.reset();
        ovalPath.moveTo(getWidth() / 2, bottom); // middle of the bottom side of the circle
        ovalPath.arcTo(ovalRect, 90, 90, false); // draw arc to the left
        ovalPath.lineTo(left, bottom); // move to bottom-left corner
        ovalPath.lineTo(getWidth() / 2, bottom); // move back to origin
        ovalPath.close();
        canvas.drawPath(ovalPath, viewportPaint);

        // Draw the square overlay as well
        drawSquareOverlay(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resetTouchManager();
    }

    /**
     * Sets the color of the viewport overlay
     *
     * @param viewportOverlayColor The color to use for the viewport overlay
     */
    public void setViewportOverlayColor(@ColorInt int viewportOverlayColor) {
        viewportPaint.setColor(viewportOverlayColor);
        config.setViewportOverlayColor(viewportOverlayColor);
    }

    /**
     * Sets the padding for the viewport overlay
     *
     * @param viewportOverlayPadding The new padding of the viewport overlay
     */
    public void setViewportOverlayPadding(int viewportOverlayPadding) {
        config.setViewportOverlayPadding(viewportOverlayPadding);
        resetTouchManager();
        invalidate();
    }

    /**
     * Returns the native aspect ratio of the image.
     *
     * @return The native aspect ratio of the image.
     */
    public float getImageRatio() {
        Bitmap bitmap = getImageBitmap();
        return bitmap != null ? (float) bitmap.getWidth() / (float) bitmap.getHeight() : 0f;
    }

    /**
     * Returns the aspect ratio of the viewport and crop rect.
     *
     * @return The current viewport aspect ratio.
     */
    public float getViewportRatio() {
        return touchManager.getAspectRatio();
    }

    /**
     * Sets the aspect ratio of the viewport and crop rect.  Defaults to
     * the native aspect ratio if <code>ratio == 0</code>.
     *
     * @param ratio The new aspect ratio of the viewport.
     */
    public void setViewportRatio(float ratio) {
        if (Float.compare(ratio, 0) == 0) {
            ratio = getImageRatio();
        }
        touchManager.setAspectRatio(ratio);
        resetTouchManager();
        invalidate();
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        final Bitmap bitmap = resId > 0
                ? BitmapFactory.decodeResource(getResources(), resId)
                : null;
        setImageBitmap(bitmap);
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        final Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            bitmap = bitmapDrawable.getBitmap();
        } else if (drawable != null) {
            bitmap = Utils.asBitmap(drawable, getWidth(), getHeight());
        } else {
            bitmap = null;
        }

        setImageBitmap(bitmap);
    }

    @Override
    public void setImageURI(@Nullable Uri uri) {
        extensions().load(uri);
    }

    @Override
    public void setImageBitmap(@Nullable Bitmap bitmap) {
        this.bitmap = bitmap;
        resetTouchManager();
        invalidate();
    }

    /**
     * @return Current working Bitmap or <code>null</code> if none has been set yet.
     */
    @Nullable
    public Bitmap getImageBitmap() {
        return bitmap;
    }

    private void resetTouchManager() {
        final boolean invalidBitmap = bitmap == null;
        final int bitmapWidth = invalidBitmap ? 0 : bitmap.getWidth();
        final int bitmapHeight = invalidBitmap ? 0 : bitmap.getHeight();
        touchManager.resetFor(bitmapWidth, bitmapHeight, getWidth(), getHeight());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean result = super.dispatchTouchEvent(event);

        if (!isEnabled()) {
            return result;
        }

        touchManager.onEvent(event);
        invalidate();
        return true;
    }

    /**
     * Performs synchronous image cropping based on configuration.
     *
     * @return A {@link Bitmap} cropped based on viewport and user panning and zooming or <code>null</code> if no {@link Bitmap} has been
     * provided.
     */
    @Nullable
    public Bitmap crop() {
        if (bitmap == null) {
            return null;
        }

        final Bitmap src = bitmap;
        final Bitmap.Config srcConfig = src.getConfig();
        final Bitmap.Config config = srcConfig == null ? Bitmap.Config.ARGB_8888 : srcConfig;
        final int viewportHeight = touchManager.getViewportHeight();
        final int viewportWidth = touchManager.getViewportWidth();

        final Bitmap dst = Bitmap.createBitmap(viewportWidth, viewportHeight, config);

        Canvas canvas = new Canvas(dst);
        final int left = (getRight() - viewportWidth) / 2;
        final int top = (getBottom() - viewportHeight) / 2;
        canvas.translate(-left, -top);

        drawBitmap(canvas);

        return dst;
    }

    /**
     * Obtain current viewport width.
     *
     * @return Current viewport width.
     * <p>Note: It might be 0 if layout pass has not been completed.</p>
     */
    public int getViewportWidth() {
        return touchManager.getViewportWidth();
    }

    /**
     * Obtain current viewport height.
     *
     * @return Current viewport height.
     * <p>Note: It might be 0 if layout pass has not been completed.</p>
     */
    public int getViewportHeight() {
        return touchManager.getViewportHeight();
    }

    /**
     * Offers common utility extensions.
     *
     * @return Extensions object used to perform chained calls.
     */
    public Extensions extensions() {
        if (extensions == null) {
            extensions = new Extensions(this);
        }
        return extensions;
    }

    /**
     * Get the transform matrix
     */
    public Matrix getTransformMatrix() {
        return transform;
    }

    /**
     * Optional extensions to perform common actions involving a {@link CropView}
     */
    public static class Extensions {

        private final CropView cropView;

        Extensions(CropView cropView) {
            this.cropView = cropView;
        }

        /**
         * Load a {@link Bitmap} using an automatically resolved {@link BitmapLoader} which will attempt to scale image to fill view.
         *
         * @param model Model used by {@link BitmapLoader} to load desired {@link Bitmap}
         * @see PicassoBitmapLoader
         * @see GlideBitmapLoader
         */
        public void load(@Nullable Object model) {
            new LoadRequest(cropView)
                    .load(model);
        }

        /**
         * Load a {@link Bitmap} using given {@link BitmapLoader}, you must call {@link LoadRequest#load(Object)} afterwards.
         *
         * @param bitmapLoader {@link BitmapLoader} used to load desired {@link Bitmap}
         * @see PicassoBitmapLoader
         * @see GlideBitmapLoader
         */
        public LoadRequest using(@Nullable BitmapLoader bitmapLoader) {
            return new LoadRequest(cropView).using(bitmapLoader);
        }

        public enum LoaderType {
            PICASSO,
            GLIDE,
            UIL,
            CLASS_LOOKUP
        }

        /**
         * Load a {@link Bitmap} using a reference to a {@link BitmapLoader}, you must call {@link LoadRequest#load(Object)} afterwards.
         *
         * Please ensure that the library for the {@link BitmapLoader} you reference is available on the classpath.
         *
         * @param loaderType the {@link BitmapLoader} to use to load desired (@link Bitmap}
         * @see PicassoBitmapLoader
         * @see GlideBitmapLoader
         */
        public LoadRequest using(@NonNull LoaderType loaderType) {
            return new LoadRequest(cropView).using(loaderType);
        }

        /**
         * Perform an asynchronous crop request.
         *
         * @return {@link CropRequest} used to chain a configure cropping request, you must call either one of:
         * <ul>
         * <li>{@link CropRequest#into(File)}</li>
         * <li>{@link CropRequest#into(OutputStream, boolean)}</li>
         * </ul>
         */
        public CropRequest crop() {
            return new CropRequest(cropView);
        }

        /**
         * Perform a pick image request using {@link Activity#startActivityForResult(Intent, int)}.
         */
        public void pickUsing(@NonNull Activity activity, int requestCode) {
            CropViewExtensions.pickUsing(activity, requestCode);
        }

        /**
         * Perform a pick image request using {@link Fragment#startActivityForResult(Intent, int)}.
         */
        public void pickUsing(@NonNull Fragment fragment, int requestCode) {
            CropViewExtensions.pickUsing(fragment, requestCode);
        }
    }
}
