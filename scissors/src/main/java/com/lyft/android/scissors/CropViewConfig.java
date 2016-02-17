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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;

class CropViewConfig {

    public static final float DEFAULT_VIEWPORT_HEIGHT_RATIO = 1f;
    public static final float DEFAULT_MAXIMUM_SCALE = 10f;
    public static final float DEFAULT_MINIMUM_SCALE = 0f;
    public static final int DEFAULT_IMAGE_QUALITY = 100;
    public static final int DEFAULT_VIEWPORT_HEADER_FOOTER_COLOR = 0xC8000000; // Black with 200 alpha

    public static final int DEFAULT_CROP_BORDER_COLOR = Color.RED;
    public static final float DEFAULT_VIEWPORT_RATIO = 1f;

    private float viewportHeightRatio = DEFAULT_VIEWPORT_HEIGHT_RATIO;
    private float maxScale = DEFAULT_MAXIMUM_SCALE;
    private float minScale = DEFAULT_MINIMUM_SCALE;
    private int viewportHeaderFooterColor = DEFAULT_VIEWPORT_HEADER_FOOTER_COLOR;
    private int cropBorderColor = DEFAULT_CROP_BORDER_COLOR;
    private float viewportRatio=DEFAULT_VIEWPORT_RATIO;

    public int getCropBorderColor() {
        return cropBorderColor;
    }

    void setCropBorderColor(int cropBorderColor) {
        this.cropBorderColor = cropBorderColor;
    }

    public float getViewportRatio() {
        return viewportRatio;
    }

    void setViewportRatio(float viewportRatio) {
        this.viewportRatio =
                viewportRatio <= 0 ? DEFAULT_VIEWPORT_RATIO : viewportRatio;
    }

    public int getViewportHeaderFooterColor() {
        return viewportHeaderFooterColor;
    }

    void setViewportHeaderFooterColor(int viewportHeaderFooterColor) {
        this.viewportHeaderFooterColor = viewportHeaderFooterColor;
    }

    public float getViewportHeightRatio() {
        return viewportHeightRatio;
    }

    void setViewportHeightRatio(float viewportHeightRatio) {
        this.viewportHeightRatio =
                viewportHeightRatio <= 0 ? DEFAULT_VIEWPORT_HEIGHT_RATIO : viewportHeightRatio;
    }

    public float getMaxScale() {
        return maxScale;
    }

    void setMaxScale(float maxScale) {
        this.maxScale = maxScale <= 0 ? DEFAULT_MAXIMUM_SCALE : maxScale;
    }

    public float getMinScale() {
        return minScale;
    }

    void setMinScale(float minScale) {
        this.minScale = minScale <= 0 ? DEFAULT_MINIMUM_SCALE : minScale;
    }

    public static CropViewConfig from(Context context, AttributeSet attrs) {
        final CropViewConfig cropViewConfig = new CropViewConfig();

        if (attrs == null) {
            return cropViewConfig;
        }

        TypedArray attributes = context.obtainStyledAttributes(
                attrs,
                R.styleable.CropView);

        cropViewConfig.setViewportHeightRatio(
                attributes.getFloat(R.styleable.CropView_cropviewViewportHeightRatio,
                        CropViewConfig.DEFAULT_VIEWPORT_HEIGHT_RATIO));

        cropViewConfig.setMaxScale(attributes.getFloat(R.styleable.CropView_cropviewMaxScale, CropViewConfig.DEFAULT_MAXIMUM_SCALE));

        cropViewConfig.setMinScale(attributes.getFloat(R.styleable.CropView_cropviewMinScale, CropViewConfig.DEFAULT_MINIMUM_SCALE));

        cropViewConfig.setViewportHeaderFooterColor(attributes.getColor(R.styleable.CropView_cropviewViewportHeaderFooterColor, CropViewConfig.DEFAULT_VIEWPORT_HEADER_FOOTER_COLOR));

        cropViewConfig.setCropBorderColor(attributes.getColor(R.styleable.CropView_cropviewBorderColor, CropViewConfig.DEFAULT_CROP_BORDER_COLOR));
        cropViewConfig.setViewportRatio(attributes.getFloat(R.styleable.CropView_cropviewRatio, CropViewConfig.DEFAULT_VIEWPORT_RATIO));
        attributes.recycle();

        return cropViewConfig;
    }
}
