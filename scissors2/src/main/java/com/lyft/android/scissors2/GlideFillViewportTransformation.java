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

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.Charset;
import java.security.MessageDigest;

class GlideFillViewportTransformation extends BitmapTransformation {

    private static final String ID = "com.lyft.android.scissors.GlideFillViewportTransformation";
    private static final byte[] ID_BYTES = ID.getBytes(Charset.defaultCharset());

    private final int viewportWidth;
    private final int viewportHeight;

    public GlideFillViewportTransformation(int viewportWidth, int viewportHeight) {
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
    public boolean equals(Object obj) {
        if (obj instanceof GlideFillViewportTransformation) {
            GlideFillViewportTransformation other = (GlideFillViewportTransformation) obj;
            return other.viewportWidth == viewportWidth && other.viewportHeight == viewportHeight;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = viewportWidth * 31 + viewportHeight;
        return hash * 17 + ID.hashCode();
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }

    public static BitmapTransformation createUsing(int viewportWidth, int viewportHeight) {
        return new GlideFillViewportTransformation(viewportWidth, viewportHeight);
    }
}
