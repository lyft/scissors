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

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;
import java.io.File;

/**
 * A {@link BitmapLoader} with transformation for {@link Picasso} image library.
 *
 * @see PicassoBitmapLoader#createUsing(CropView)
 * @see PicassoBitmapLoader#createUsing(CropView, Picasso)
 */
public class PicassoBitmapLoader implements BitmapLoader {

    private final Picasso picasso;
    private final Transformation transformation;

    public PicassoBitmapLoader(Picasso picasso, Transformation transformation) {
        this.picasso = picasso;
        this.transformation = transformation;
    }

    @Override
    public void load(@Nullable Object model, @NonNull ImageView imageView) {
        final RequestCreator requestCreator;

        if (model instanceof Uri || model == null) {
            requestCreator = picasso.load((Uri) model);
        } else if (model instanceof String) {
            requestCreator = picasso.load((String) model);
        } else if (model instanceof File) {
            requestCreator = picasso.load((File) model);
        } else if (model instanceof Integer) {
            requestCreator = picasso.load((Integer) model);
        } else {
            throw new IllegalArgumentException("Unsupported model " + model);
        }

        requestCreator
                .skipMemoryCache()
                .transform(transformation)
                .into(imageView);
    }

    public static BitmapLoader createUsing(CropView cropView) {
        return createUsing(cropView, Picasso.with(cropView.getContext()));
    }

    public static BitmapLoader createUsing(CropView cropView, Picasso picasso) {
        return new PicassoBitmapLoader(picasso,
                PicassoFillViewportTransformation.createUsing(cropView.getViewportWidth(), cropView.getViewportHeight()));
    }
}
