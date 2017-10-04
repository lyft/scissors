package com.lyft.android.scissors;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.OutputStream;
import java.util.concurrent.Future;

public class CropRequest {

  private final CropView cropView;
  private Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
  private int quality = CropViewConfig.DEFAULT_IMAGE_QUALITY;

  CropRequest(@NonNull CropView cropView) {
    Utils.checkNotNull(cropView, "cropView == null");
    this.cropView = cropView;
  }

  /**
   * Compression format to use, defaults to {@link Bitmap.CompressFormat#JPEG}.
   *
   * @return current request for chaining.
   */
  public CropRequest format(@NonNull Bitmap.CompressFormat format) {
    Utils.checkNotNull(format, "format == null");
    this.format = format;
    return this;
  }

  /**
   * Compression quality to use (must be 0..100), defaults to {@value CropViewConfig#DEFAULT_IMAGE_QUALITY}.
   *
   * @return current request for chaining.
   */
  public CropRequest quality(int quality) {
    Utils.checkArg(quality >= 0 && quality <= 100, "quality must be 0..100");
    this.quality = quality;
    return this;
  }

  /**
   * Asynchronously flush cropped bitmap into provided file, creating parent directory if required. This is performed in another
   * thread.
   *
   * @param file Must have permissions to write, will be created if doesn't exist or overwrite if it does.
   * @return {@link Future} used to cancel or wait for this request.
   */
  public Future<Void> into(@NonNull File file) {
    final Bitmap croppedBitmap = cropView.crop();
    return Utils.flushToFile(croppedBitmap, format, quality, file);
  }

  /**
   * Asynchronously flush cropped bitmap into provided stream.
   *
   * @param outputStream Stream to write to
   * @param closeWhenDone wetter or not to close provided stream once flushing is done
   * @return {@link Future} used to cancel or wait for this request.
   */
  public Future<Void> into(@NonNull OutputStream outputStream, boolean closeWhenDone) {
    final Bitmap croppedBitmap = cropView.crop();
    return Utils.flushToStream(croppedBitmap, format, quality, outputStream, closeWhenDone);
  }
}
