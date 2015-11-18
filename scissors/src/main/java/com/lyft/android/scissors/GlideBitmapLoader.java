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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

/**
 * A {@link BitmapLoader} with transformation for {@link Glide} image library.
 *
 * @see GlideBitmapLoader#createUsing(CropView)
 * @see GlideBitmapLoader#createUsing(CropView, RequestManager, BitmapPool)
 */
public class GlideBitmapLoader implements BitmapLoader {

    private final RequestManager requestManager;
    private final BitmapTransformation transformation;

    public GlideBitmapLoader(@NonNull RequestManager requestManager, @NonNull BitmapTransformation transformation) {
        this.requestManager = requestManager;
        this.transformation = transformation;
    }

    @Override
    public void load(@Nullable Object model, @NonNull ImageView imageView) {
        requestManager.load(model)
                .asBitmap()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .transform(transformation)
                .into(imageView);
    }

    public static BitmapLoader createUsing(@NonNull CropView cropView) {
        return createUsing(cropView, Glide.with(cropView.getContext()), Glide.get(cropView.getContext()).getBitmapPool());
    }

    public static BitmapLoader createUsing(@NonNull CropView cropView, @NonNull RequestManager requestManager,
            @NonNull BitmapPool bitmapPool) {
        return new GlideBitmapLoader(requestManager,
                GlideFillViewportTransformation.createUsing(bitmapPool, cropView.getViewportWidth(), cropView.getViewportHeight()));
    }
}
