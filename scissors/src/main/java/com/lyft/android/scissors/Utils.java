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
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class Utils {

    public static void checkArg(boolean expression, String msg) {
        if (!expression) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void checkNotNull(Object object, String msg) {
        if (object == null) {
            throw new NullPointerException(msg);
        }
    }

    public static Bitmap asBitmap(Drawable drawable, int minWidth, int minHeight) {
        final Rect tmpRect = new Rect();
        drawable.copyBounds(tmpRect);
        if (tmpRect.isEmpty()) {
            tmpRect.set(0, 0, Math.max(minWidth, drawable.getIntrinsicWidth()), Math.max(minHeight, drawable.getIntrinsicHeight()));
            drawable.setBounds(tmpRect);
        }
        Bitmap bitmap = Bitmap.createBitmap(tmpRect.width(), tmpRect.height(), Bitmap.Config.ARGB_8888);
        drawable.draw(new Canvas(bitmap));
        return bitmap;
    }

    private final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    private static final String TAG = "scissors.Utils";

    public static Future<Void> flushToFile(final Bitmap bitmap,
            final Bitmap.CompressFormat format,
            final int quality,
            final File file) {

        return EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    file.getParentFile().mkdirs();

                    OutputStream outputStream = new FileOutputStream(file);
                    bitmap.compress(format, quality, outputStream);
                    outputStream.flush();
                    outputStream.close();
                } catch (final Throwable throwable) {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Error attempting to save bitmap.", throwable);
                    }
                }
            }
        }, null);
    }

    public static Future<Void> flushToStream(final Bitmap bitmap,
            final Bitmap.CompressFormat format,
            final int quality,
            final OutputStream outputStream,
            final boolean closeWhenDone) {

        return EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    bitmap.compress(format, quality, outputStream);
                    outputStream.flush();
                    if (closeWhenDone) {
                        outputStream.close();
                    }
                } catch (final Throwable throwable) {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Error attempting to save bitmap.", throwable);
                    }
                }
            }
        }, null);
    }
}
