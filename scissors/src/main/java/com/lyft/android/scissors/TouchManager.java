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

import android.annotation.TargetApi;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.view.MotionEvent;

class TouchManager {

    private final int maxNumberOfTouchPoints;
    private final CropViewConfig cropViewConfig;

    private final TouchPoint[] points;
    private final TouchPoint[] previousPoints;

    private float minimumScale;
    private float maximumScale;
    private Rect imageBounds;
    private int viewportWidth;
    private int viewportHeight;
    private int bitmapWidth;
    private int bitmapHeight;

    private int verticalLimit;
    private int horizontalLimit;

    private float scale = 1.0f;
    private TouchPoint position = new TouchPoint();

    public TouchManager(final int maxNumberOfTouchPoints, final CropViewConfig cropViewConfig) {
        this.maxNumberOfTouchPoints = maxNumberOfTouchPoints;
        this.cropViewConfig = cropViewConfig;

        points = new TouchPoint[maxNumberOfTouchPoints];
        previousPoints = new TouchPoint[maxNumberOfTouchPoints];
        minimumScale = cropViewConfig.getMinScale();
        maximumScale = cropViewConfig.getMaxScale();
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public void onEvent(MotionEvent event) {
        int index = event.getActionIndex();
        if (index >= maxNumberOfTouchPoints) {
            return; // We don't care about this pointer, ignore it.
        }

        if (isUpAction(event.getActionMasked())) {
            previousPoints[index] = null;
            points[index] = null;
        } else {
            updateCurrentAndPreviousPoints(event);
        }

        handleDragGesture();
        handlePinchGesture();
        handleDragOutsideViewport(event);
    }

    public void applyPositioningAndScale(Matrix matrix) {
        matrix.postTranslate(-bitmapWidth / 2.0f, -bitmapHeight / 2.0f);
        matrix.postScale(scale, scale);
        matrix.postTranslate(position.getX(), position.getY());
    }

    public void resetFor(int bitmapWidth, int bitmapHeight, int availableWidth, int availableHeight) {
        position.set(availableWidth / 2, availableHeight / 2);

        imageBounds = new Rect(0, 0, availableWidth / 2, availableHeight / 2);
        setViewport((int) (availableWidth*cropViewConfig.getViewportRatio()));
        setMinimumScale(availableWidth, availableHeight);

        this.bitmapWidth = bitmapWidth;
        this.bitmapHeight = bitmapHeight;

        horizontalLimit = computeLimit(bitmapWidth, viewportWidth);
        verticalLimit = computeLimit(bitmapHeight, viewportHeight);
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    private void handleDragGesture() {
        if (getDownCount() != 1) {
            return;
        }
        position.add(moveDelta(0));
    }

    private void handlePinchGesture() {
        if (getDownCount() != 2) {
            return;
        }
        updateScale();
        horizontalLimit = computeLimit((int) (bitmapWidth * scale), viewportWidth);
        verticalLimit = computeLimit((int) (bitmapHeight * scale), viewportHeight);
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void handleDragOutsideViewport(MotionEvent event) {
        if (imageBounds == null || !isUpAction(event.getActionMasked())) {
            return;
        }

        float newY = position.getY();
        int bottom = imageBounds.bottom;

        if (bottom - newY >= verticalLimit) {
            newY = bottom - verticalLimit;
        } else if (newY - bottom >= verticalLimit) {
            newY = bottom + verticalLimit;
        }

        float newX = position.getX();
        int right = imageBounds.right;
        if (newX <= right - horizontalLimit) {
            newX = right - horizontalLimit;
        } else if (newX > right + horizontalLimit) {
            newX = right + horizontalLimit;
        }

        position.set(newX, newY);
    }

    private void updateCurrentAndPreviousPoints(MotionEvent event) {
        for (int i = 0; i < maxNumberOfTouchPoints; i++) {
            if (i < event.getPointerCount()) {
                final float eventX = event.getX(i);
                final float eventY = event.getY(i);

                if (points[i] == null) {
                    points[i] = new TouchPoint(eventX, eventY);
                    previousPoints[i] = null;
                } else {
                    if (previousPoints[i] == null) {
                        previousPoints[i] = new TouchPoint();
                    }
                    previousPoints[i].copy(points[i]);
                    points[i].set(eventX, eventY);
                }
            } else {
                previousPoints[i] = null;
                points[i] = null;
            }
        }
    }

    private void setViewport(int w) {
        viewportWidth = w;
        viewportHeight = (int) (w * cropViewConfig.getViewportHeightRatio());
    }

    private void setMinimumScale() {
        float imageAspect = (float) bitmapWidth / bitmapHeight;
        float viewportAspect = (float) viewportWidth / viewportHeight;

        if (bitmapWidth < viewportWidth || bitmapHeight < viewportHeight) {
            minimumScale = 1f;
        } else if (imageAspect > viewportAspect) {
            minimumScale = (float) viewportHeight / bitmapHeight;
        } else {
            minimumScale = (float) viewportWidth / bitmapWidth;
        }
        scale = minimumScale;
    }

    private void setMinimumScale(int availableWidth, int availableHeight) {
        float imageAspect = (float) bitmapWidth / bitmapHeight;
        float viewportAspect = (float) availableWidth / availableHeight;

        if (bitmapWidth < availableWidth || bitmapHeight < availableHeight) {
            minimumScale = 1f;
        } else if (imageAspect > viewportAspect) {
            minimumScale = (float) availableHeight / bitmapHeight;
        } else {
            minimumScale = (float) availableWidth / bitmapWidth;
        }
        scale = minimumScale;
    }

    private void updateScale() {
        TouchPoint current = vector(points[0], points[1]);
        TouchPoint previous = previousVector(0, 1);
        float currentDistance = current.getLength();
        float previousDistance = previous.getLength();

        float newScale = scale;
        if (previousDistance != 0) {
            newScale *= currentDistance / previousDistance;
        }
        newScale = newScale < minimumScale ? minimumScale : newScale;
        newScale = newScale > maximumScale ? maximumScale : newScale;

        scale = newScale;
    }

    private boolean isPressed(int index) {
        return points[index] != null;
    }

    private int getDownCount() {
        int count = 0;
        for (TouchPoint point : points) {
            if (point != null) {
                count++;
            }
        }
        return count;
    }

    private TouchPoint moveDelta(int index) {
        if (isPressed(index)) {
            TouchPoint previous =
                    previousPoints[index] != null ? previousPoints[index] : points[index];
            return TouchPoint.subtract(points[index], previous);
        } else {
            return new TouchPoint();
        }
    }

    private TouchPoint previousVector(int indexA, int indexB) {
        return previousPoints[indexA] == null || previousPoints[indexB] == null
                ? vector(points[indexA], points[indexB])
                : vector(previousPoints[indexA], previousPoints[indexB]);
    }

    private static int computeLimit
            (int bitmapSize, int viewportSize) {
        return (bitmapSize - viewportSize) / 2;
    }

    private static TouchPoint vector(TouchPoint a, TouchPoint b) {
        return TouchPoint.subtract(b, a);
    }

    private static boolean isUpAction(int actionMasked) {
        return actionMasked == MotionEvent.ACTION_POINTER_UP || actionMasked == MotionEvent.ACTION_UP;
    }
}
