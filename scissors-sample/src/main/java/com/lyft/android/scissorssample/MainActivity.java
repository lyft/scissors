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
package com.lyft.android.scissorssample;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import com.lyft.android.scissors.CropView;
import com.squareup.leakcanary.RefWatcher;
import rx.Observable;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import java.io.File;
import java.util.List;

import static android.graphics.Bitmap.CompressFormat.JPEG;
import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

public class MainActivity extends Activity {

    private static final float[] ASPECT_RATIOS = { 0f, 1f, 6f/4f, 16f/9f };

    private static final String[] ASPECT_LABELS = { "\u00D8", "1:1", "6:4", "16:9" };

    @Bind(R.id.crop_view)
    CropView cropView;

    @Bind({ R.id.crop_fab, R.id.pick_mini_fab, R.id.ratio_fab })
    List<View> buttons;

    @Bind(R.id.pick_fab)
    View pickButton;

    CompositeSubscription subscriptions = new CompositeSubscription();

    private int selectedRatio = 0;
    private AnimatorListener animatorListener = new AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            ButterKnife.apply(buttons, VISIBILITY, View.INVISIBLE);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            ButterKnife.apply(buttons, VISIBILITY, View.VISIBLE);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCodes.PICK_IMAGE_FROM_GALLERY
                && resultCode == Activity.RESULT_OK) {
            Uri galleryPictureUri = data.getData();

            cropView.extensions()
                    .load(galleryPictureUri);

            updateButtons();
        }
    }

    @OnClick(R.id.crop_fab)
    public void onCropClicked() {
        final File croppedFile = new File(getCacheDir(), "cropped.jpg");

        Observable<Void> onSave = Observable.from(cropView.extensions()
                .crop()
                .quality(100)
                .format(JPEG)
                .into(croppedFile))
                .subscribeOn(io())
                .observeOn(mainThread());

        subscriptions.add(onSave
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void nothing) {
                        CropResultActivity.startUsing(croppedFile, MainActivity.this);
                    }
                }));
    }

    @OnClick({ R.id.pick_fab, R.id.pick_mini_fab })
    public void onPickClicked() {
        cropView.extensions()
                .pickUsing(this, RequestCodes.PICK_IMAGE_FROM_GALLERY);
    }

    @OnClick(R.id.ratio_fab)
    public void onRatioClicked() {
        final float oldRatio = cropView.getImageRatio();
        selectedRatio = (selectedRatio + 1) % ASPECT_RATIOS.length;

        // Since the animation needs to interpolate to the native
        // ratio, we need to get that instead of using 0
        float newRatio = ASPECT_RATIOS[selectedRatio];
        if (Float.compare(0, newRatio) == 0) {
            newRatio = cropView.getImageRatio();
        }

        ObjectAnimator viewportRatioAnimator = ObjectAnimator.ofFloat(cropView, "viewportRatio", oldRatio, newRatio)
                .setDuration(420);
        autoCancel(viewportRatioAnimator);
        viewportRatioAnimator.addListener(animatorListener);
        viewportRatioAnimator.start();

        Toast.makeText(this, ASPECT_LABELS[selectedRatio], Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        subscriptions.unsubscribe();

        RefWatcher refWatcher = App.getRefWatcher(this);
        refWatcher.watch(this, "MainActivity");
        refWatcher.watch(cropView, "cropView");
    }

    @OnTouch(R.id.crop_view)
    public boolean onTouchCropView(MotionEvent event) { // GitHub issue #4
        if (event.getPointerCount() > 1 || cropView.getImageBitmap() == null) {
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                ButterKnife.apply(buttons, VISIBILITY, View.INVISIBLE);
                break;
            default:
                ButterKnife.apply(buttons, VISIBILITY, View.VISIBLE);
                break;
        }
        return true;
    }

    private void updateButtons() {
        ButterKnife.apply(buttons, VISIBILITY, View.VISIBLE);
        pickButton.setVisibility(View.GONE);
    }

    static final ButterKnife.Setter<View, Integer> VISIBILITY = new ButterKnife.Setter<View, Integer>() {
        @Override
        public void set(final View view, final Integer visibility, int index) {
            view.setVisibility(visibility);
        }
    };

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    static void autoCancel(ObjectAnimator objectAnimator) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            objectAnimator.setAutoCancel(true);
        }
    }
}
