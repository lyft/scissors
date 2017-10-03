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

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Rect;

import static com.lyft.android.scissors.CropView.Extensions.LOADER_GLIDE;
import static com.lyft.android.scissors.CropView.Extensions.LOADER_INVALID;
import static com.lyft.android.scissors.CropView.Extensions.LOADER_PICASSO;
import static com.lyft.android.scissors.CropView.Extensions.LOADER_UIL;

class CropViewExtensions {

    static void pickUsing(Activity activity, int requestCode) {
        activity.startActivityForResult(
                createChooserIntent(),
                requestCode);
    }

    static void pickUsing(Fragment fragment, int requestCode) {
        fragment.startActivityForResult(
                createChooserIntent(),
                requestCode);
    }

    private static Intent createChooserIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        return Intent.createChooser(intent, null);
    }

    final static boolean HAS_PICASSO = canHasClass("com.squareup.picasso.Picasso");
    final static boolean HAS_GLIDE = canHasClass("com.bumptech.glide.Glide");
    final static boolean HAS_UIL = canHasClass("com.nostra13.universalimageloader.core.ImageLoader");

    static BitmapLoader resolveBitmapLoader(CropView cropView, @CropView.Extensions.ExtensionBitmapLoader int bitmapLoaderReference) {
        switch (bitmapLoaderReference) {
            case LOADER_INVALID:
                break;
            case LOADER_PICASSO:
                return PicassoBitmapLoader.createUsing(cropView);
            case LOADER_GLIDE:
                return GlideBitmapLoader.createUsing(cropView);
            case LOADER_UIL:
                return UILBitmapLoader.createUsing(cropView);
        }

        if (HAS_PICASSO) {
            return PicassoBitmapLoader.createUsing(cropView);
        }
        if (HAS_GLIDE) {
            return GlideBitmapLoader.createUsing(cropView);
        }
        if (HAS_UIL) {
            return UILBitmapLoader.createUsing(cropView);
        }
        throw new IllegalStateException("You must provide a BitmapLoader.");
    }

    static boolean canHasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    static Rect computeTargetSize(int sourceWidth, int sourceHeight, int viewportWidth, int viewportHeight) {

        if (sourceWidth == viewportWidth && sourceHeight == viewportHeight) {
            return new Rect(0, 0, viewportWidth, viewportHeight); // Fail fast for when source matches exactly on viewport
        }

        float scale;
        if (sourceWidth * viewportHeight > viewportWidth * sourceHeight) {
            scale = (float) viewportHeight / (float) sourceHeight;
        } else {
            scale = (float) viewportWidth / (float) sourceWidth;
        }
        final int recommendedWidth = (int) ((sourceWidth * scale) + 0.5f);
        final int recommendedHeight = (int) ((sourceHeight * scale) + 0.5f);
        return new Rect(0, 0, recommendedWidth, recommendedHeight);
    }
}
