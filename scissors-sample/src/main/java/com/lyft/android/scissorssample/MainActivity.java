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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import com.lyft.android.scissors.CropView;
import com.squareup.leakcanary.RefWatcher;
import java.io.File;
import java.util.List;
import rx.Observable;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import static android.graphics.Bitmap.CompressFormat.JPEG;
import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

public class MainActivity extends Activity {

    @Bind(R.id.crop_view)
    CropView cropView;

    @Bind({ R.id.crop_fab, R.id.pick_mini_fab })
    List<View> buttons;

    @Bind(R.id.pick_fab)
    View pickButton;

    CompositeSubscription subscriptions = new CompositeSubscription();

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
}
