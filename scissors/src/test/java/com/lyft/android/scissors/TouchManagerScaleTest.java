package com.lyft.android.scissors;

import android.graphics.Matrix;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowMatrix;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.robolectric.Shadows.shadowOf;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TouchManagerScaleTest {

    private static final int MAX_TOUCH_POINTS = 2;

    @ParameterizedRobolectricTestRunner.Parameters(name = "testCase {index}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {100, 200, 100, 200, 1f}, // no scaling perfect match
                {100, 100, 200, 200, 2f}, // scale up matching aspect ratio
                {100, 100, 50, 50, 0.5f}, // scale down matching bitmap ratio
                {100, 110, 200, 200, 2f}, // scale up wide bitmap ratio
                {100, 110, 50, 50, 0.5f}, // scale down wide bitmap ratio
                {110, 100, 200, 200, 2f}, // scale up narrow bitmap ratio
                {110, 100, 50, 50, 0.5f} // scale down narrow bitmap ratio
        });
    }

    final int bitmapWidth;
    final int bitmapHeight;
    final int viewportWidth;
    final int viewportHeight;
    final float expectedScale;

    public TouchManagerScaleTest(int bitmapWidth, int bitmapHeight, int viewportWidth, int viewportHeight, float expectedScale) {
        this.bitmapWidth = bitmapWidth;
        this.bitmapHeight = bitmapHeight;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.expectedScale = expectedScale;
    }

    @Test
    public void testTouchManagerScale() {
        TouchManager touchManager = new TouchManager(MAX_TOUCH_POINTS, new CropViewConfig());
        Matrix matrix = new Matrix();
        String expected = ShadowMatrix.SCALE + " " + expectedScale + " " + expectedScale;
        touchManager.resetFor(bitmapWidth, bitmapHeight, viewportWidth, viewportHeight);
        touchManager.applyPositioningAndScale(matrix);
        List<String> postOps = shadowOf(matrix).getPostOperations();
        if (postOps == null || postOps.size() != 3) {
            fail("postOps should be called exactly 3 times, was called "
                    + (postOps == null ? 0 : postOps.size()) + " times");
        }
        String operation = postOps.get(1);
        assertThat(operation).isEqualTo(expected);
    }

}
