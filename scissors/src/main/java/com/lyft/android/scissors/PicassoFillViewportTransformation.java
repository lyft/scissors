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
import com.squareup.picasso.Transformation;

class PicassoFillViewportTransformation implements Transformation {

    private final int viewportWidth;
    private final int viewportHeight;

    public PicassoFillViewportTransformation(int viewportWidth, int viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    @Override
    public Bitmap transform(Bitmap source) {
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

        return result;
    }

    @Override
    public String key() {
        return viewportWidth + "x" + viewportHeight;
    }

    public static Transformation createUsing(int viewportWidth, int viewportHeight) {
        return new PicassoFillViewportTransformation(viewportWidth, viewportHeight);
    }
}
