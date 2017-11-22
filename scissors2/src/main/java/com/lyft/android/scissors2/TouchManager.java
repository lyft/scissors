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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.IntDef;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.OverScroller;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

class TouchManager {

    private static final int MINIMUM_FLING_VELOCITY = 2500;

    private final CropViewConfig cropViewConfig;

    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector gestureDetector;

    private float minimumScale;
    private float maximumScale;
    private Rect imageBounds;
    private float aspectRatio;
    private int viewportWidth;
    private int viewportHeight;
    private int bitmapWidth;
    private int bitmapHeight;

    private int verticalLimit;
    private int horizontalLimit;

    private float scale = -1.0f;
    private final TouchPoint position = new TouchPoint();

    private final ImageView imageView;

    private final GestureAnimator gestureAnimator = new GestureAnimator(new GestureAnimator.OnAnimationUpdateListener() {
        @Override
        public void onAnimationUpdate(@GestureAnimator.AnimationType int animationType, float animationValue) {
            if(animationType == GestureAnimator.ANIMATION_X) {
                position.set(animationValue, position.getY());
                ensureInsideViewport();
            }
            else if(animationType == GestureAnimator.ANIMATION_Y) {
                position.set(position.getX(), animationValue);
                ensureInsideViewport();
            }
            else if(animationType == GestureAnimator.ANIMATION_SCALE) {
                scale = animationValue;
                setLimits();
            }

            imageView.invalidate();
        }

        @Override
        public void onAnimationFinished() {
            ensureInsideViewport();
        }
    });

    private final ScaleGestureDetector.OnScaleGestureListener scaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale = calculateScale(detector.getScaleFactor());
            setLimits();
            return true;
        }

        @Override public boolean onScaleBegin(ScaleGestureDetector detector) {return true;}
        @Override public void onScaleEnd(ScaleGestureDetector detector) {}
    };

    private final GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e2.getPointerCount() != 1) {
                return true;
            }

            TouchPoint delta = new TouchPoint(-distanceX, -distanceY);
            position.add(delta);
            ensureInsideViewport();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            velocityX /= 2;
            velocityY /= 2;

            if(Math.abs(velocityX) < MINIMUM_FLING_VELOCITY) {
                velocityX = 0;
            }
            if(Math.abs(velocityY) < MINIMUM_FLING_VELOCITY) {
                velocityY = 0;
            }

            if(velocityX == 0 && velocityY == 0) {
                return true;
            }

            int width = (int) (imageBounds.right * scale);
            int height = (int) (imageBounds.bottom * scale);

            OverScroller scroller = new OverScroller(imageView.getContext());
            scroller.fling((int) e1.getX(), (int) e1.getY(), (int) velocityX, (int) velocityY, -width, width, -height, height);

            TouchPoint target = new TouchPoint(scroller.getFinalX(), scroller.getFinalY());
            float x = velocityX == 0 ? position.getX() : target.getX() * scale;
            float y = velocityY == 0 ? position.getY() : target.getY() * scale;

            gestureAnimator.animateTranslation(position.getX(), x, position.getY(), y);

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            final float fromX, toX, fromY, toY, targetScale;

            TouchPoint eventPoint = new TouchPoint(e.getX(), e.getY());
            if(scale == minimumScale) {
                targetScale = maximumScale / 2;
                TouchPoint translatedTargetPosition = mapTouchCoordinateToMatrix(eventPoint, targetScale);
                TouchPoint centeredTargetPosition = centerCoordinates(translatedTargetPosition);
                fromX = position.getX();
                toX = centeredTargetPosition.getX();
                fromY = position.getY();
                toY = centeredTargetPosition.getY();
            }
            else {
                targetScale = minimumScale;
                TouchPoint translatedPosition = mapTouchCoordinateToMatrix(eventPoint, scale);
                TouchPoint centeredTargetPosition = centerCoordinates(translatedPosition);
                fromX = centeredTargetPosition.getX();
                toX = 0;
                fromY = centeredTargetPosition.getY();
                toY = 0;
            }

            gestureAnimator.animateDoubleTap(fromX, toX, fromY, toY, scale, targetScale);
            return true;
        }

        private TouchPoint centerCoordinates(TouchPoint coordinates) {
            float x = coordinates.getX() + (imageBounds.right / 2);
            float y = coordinates.getY() + (imageBounds.bottom / 2);
            return new TouchPoint(x, y);
        }
    };

    public TouchManager(final ImageView imageView, final CropViewConfig cropViewConfig) {
        this.imageView = imageView;
        scaleGestureDetector = new ScaleGestureDetector(imageView.getContext(), scaleGestureListener);
        gestureDetector = new GestureDetector(imageView.getContext(), gestureListener);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            scaleGestureDetector.setQuickScaleEnabled(true);
        }

        this.cropViewConfig = cropViewConfig;

        minimumScale = cropViewConfig.getMinScale();
        maximumScale = cropViewConfig.getMaxScale();
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public void onEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        if (isUpAction(event.getActionMasked())) {
            ensureInsideViewport();
        }
    }

    public void applyPositioningAndScale(Matrix matrix) {
        matrix.postTranslate(-bitmapWidth / 2.0f, -bitmapHeight / 2.0f);
        matrix.postScale(scale, scale);
        matrix.postTranslate(position.getX(), position.getY());
    }

    public void resetFor(int bitmapWidth, int bitmapHeight, int availableWidth, int availableHeight) {
        aspectRatio = cropViewConfig.getViewportRatio();
        imageBounds = new Rect(0, 0, availableWidth / 2, availableHeight / 2);
        setViewport(bitmapWidth, bitmapHeight, availableWidth, availableHeight);

        this.bitmapWidth = bitmapWidth;
        this.bitmapHeight = bitmapHeight;
        if (bitmapWidth > 0 && bitmapHeight > 0) {
            setMinimumScale();
            setLimits();
            resetPosition();
            ensureInsideViewport();
        }
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float ratio) {
        aspectRatio = ratio;
        cropViewConfig.setViewportRatio(ratio);
    }

    private TouchPoint mapTouchCoordinateToMatrix(TouchPoint coordinate, float targetScale) {
        float width = bitmapWidth * targetScale;
        float height = bitmapHeight * targetScale;

        float x0 = width / 2;
        float y0 = height / 2;

        float newX = coordinate.getX() * targetScale;
        newX = -(newX - x0);

        float newY = coordinate.getY() * targetScale;
        if(newY > y0) {
            newY = -(newY - y0);
        }
        else {
            newY = y0 - newY;
        }

        return new TouchPoint(newX, newY);
    }

    private void ensureInsideViewport() {
        if (imageBounds == null) {
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

    private void setViewport(int bitmapWidth, int bitmapHeight, int availableWidth, int availableHeight) {
        final float imageAspect = (float) bitmapWidth / bitmapHeight;
        final float viewAspect = (float) availableWidth / availableHeight;

        float ratio = cropViewConfig.getViewportRatio();
        if (Float.compare(0f, ratio) == 0) {
            // viewport ratio of 0 means match native ratio of bitmap
            ratio = imageAspect;
        }

        if (ratio > viewAspect) {
            // viewport is wider than view
            viewportWidth = availableWidth - cropViewConfig.getViewportOverlayPadding() * 2;
            viewportHeight = (int) (viewportWidth * (1 / ratio));
        } else {
            // viewport is taller than view
            viewportHeight = availableHeight - cropViewConfig.getViewportOverlayPadding() * 2;
            viewportWidth = (int) (viewportHeight * ratio);
        }
    }

    private void setLimits() {
        horizontalLimit = computeLimit((int) (bitmapWidth * scale), viewportWidth);
        verticalLimit = computeLimit((int) (bitmapHeight * scale), viewportHeight);
    }

    private void resetPosition() {
        position.set(imageBounds.right, imageBounds.bottom);
    }

    private void setMinimumScale() {
        final float fw = (float) viewportWidth / bitmapWidth;
        final float fh = (float) viewportHeight / bitmapHeight;
        minimumScale = Math.max(fw, fh);
        scale = Math.max(scale, minimumScale);
    }

    private float calculateScale(float newScaleDelta) {
        return Math.max(minimumScale, Math.min(scale * newScaleDelta, maximumScale));
    }

    private static int computeLimit(int bitmapSize, int viewportSize) {
        return (bitmapSize - viewportSize) / 2;
    }

    private static boolean isUpAction(int actionMasked) {
        return actionMasked == MotionEvent.ACTION_POINTER_UP || actionMasked == MotionEvent.ACTION_UP;
    }

    private static class GestureAnimator {
        @IntDef({ANIMATION_X, ANIMATION_Y, ANIMATION_SCALE})
        @Retention(RetentionPolicy.SOURCE)
        public @interface AnimationType {}
        public static final int ANIMATION_X = 0;
        public static final int ANIMATION_Y = 1;
        public static final int ANIMATION_SCALE = 2;

        interface OnAnimationUpdateListener {
            void onAnimationUpdate(@AnimationType int animationType, float animationValue);
            void onAnimationFinished();
        }

        private ValueAnimator xAnimator;
        private ValueAnimator yAnimator;
        private ValueAnimator scaleAnimator;

        private AnimatorSet animator;

        private final OnAnimationUpdateListener listener;

        public GestureAnimator(OnAnimationUpdateListener listener) {
            this.listener = listener;
        }

        final ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = ((float) animation.getAnimatedValue());

                if(animation == xAnimator) {
                    listener.onAnimationUpdate(ANIMATION_X, val);
                }
                else if(animation == yAnimator) {
                    listener.onAnimationUpdate(ANIMATION_Y, val);
                }
                else if(animation == scaleAnimator) {
                    listener.onAnimationUpdate(ANIMATION_SCALE, val);
                }
            }
        };

        private final Animator.AnimatorListener animatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(xAnimator != null) xAnimator.removeUpdateListener(updateListener);
                if(yAnimator != null) yAnimator.removeUpdateListener(updateListener);
                if(scaleAnimator != null) scaleAnimator.removeUpdateListener(updateListener);
                animator.removeAllListeners();
                listener.onAnimationFinished();
            }
        };

        public void animateTranslation(float fromX, float toX, float fromY, float toY) {
            if(animator != null) {
                animator.cancel();
            }

            xAnimator = ValueAnimator.ofFloat(fromX, toX);
            yAnimator = ValueAnimator.ofFloat(fromY, toY);
            scaleAnimator = null;

            xAnimator.addUpdateListener(updateListener);
            yAnimator.addUpdateListener(updateListener);

            animate(new DecelerateInterpolator(), 250, xAnimator, yAnimator);
        }

        public void animateDoubleTap(float fromX, float toX, float fromY, float toY, float fromScale, float toScale) {
            if(animator != null) {
                animator.cancel();
            }

            xAnimator = ValueAnimator.ofFloat(fromX, toX);
            yAnimator = ValueAnimator.ofFloat(fromY, toY);
            scaleAnimator = ValueAnimator.ofFloat(fromScale, toScale);

            xAnimator.addUpdateListener(updateListener);
            yAnimator.addUpdateListener(updateListener);
            scaleAnimator.addUpdateListener(updateListener);

            animate(new AccelerateDecelerateInterpolator(), 500, scaleAnimator, xAnimator, yAnimator);
        }

        private void animate(Interpolator interpolator, long duration, ValueAnimator first, ValueAnimator... animators) {
            animator = new AnimatorSet();
            animator.setDuration(duration);
            animator.setInterpolator(interpolator);
            animator.addListener(animatorListener);
            AnimatorSet.Builder builder = animator.play(first);
            for(ValueAnimator valueAnimator : animators) {
                builder.with(valueAnimator);
            }
            animator.start();
        }
    }
}
