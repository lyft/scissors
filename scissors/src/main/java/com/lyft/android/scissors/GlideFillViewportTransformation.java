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
package com.lyft.android.scissors;

import android.graphics.Bitmap;
import android.graphics.Rect;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

class GlideFillViewportTransformation extends BitmapTransformation {

    private final int viewportWidth;
    private final int viewportHeight;

    public GlideFillViewportTransformation(BitmapPool bitmapPool, int viewportWidth, int viewportHeight) {
        super(bitmapPool);
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    @Override
    protected Bitmap transform(BitmapPool bitmapPool, Bitmap source, int outWidth, int outHeight) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        Rect target = CropViewExtensions.computeTargetSize(sourceWidth, sourceHeight, viewportWidth, viewportHeight);

        int targetWidth = target.width();
        int targetHeight = target.height();

        return Bitmap.createScaledBitmap(
                source,
                targetWidth,
                targetHeight,
                true);
    }

    @Override
    public String getId() {
        return getClass().getName();
    }

    public static BitmapTransformation createUsing(BitmapPool bitmapPool, int viewportWidth, int viewportHeight) {
        return new GlideFillViewportTransformation(bitmapPool, viewportWidth, viewportHeight);
    }
}
