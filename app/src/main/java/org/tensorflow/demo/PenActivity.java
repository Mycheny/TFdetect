package org.tensorflow.demo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Size;
import android.util.TypedValue;

import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PenActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final int INPUT_SIZE_WIDTH = 640;
    private static final int INPUT_SIZE_HEIGHT = 480;
    private ClassifierPen detector;
    private static final String MODEL_FILE = "file:///android_asset/model_pen.pb";
    OverlayView trackingOverlay;

    private Integer sensorOrientation;
    private int previewHeight;
    private int previewWidth;
    private Matrix frameToCanvasMatrix;
    private long timestamp = 0;
    private boolean computingDetection = false;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private byte[] luminanceCopy;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private long lastProcessingTimeMs;

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        byte[] originalLuminance = getLuminance();
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("准备图片 " + currTimestamp + " 用于bg线程的检测。");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        if (luminanceCopy == null) {
            luminanceCopy = new byte[originalLuminance.length];
        }
        System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("图像运行检测 " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<float[]> results = detector.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        float minimumConfidence = 0.8f;

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();

                        trackingOverlay.postInvalidate();

                        requestRender();
                        computingDetection = false;
                    }
                });
    }

    @Override
    protected void onPreviewSizeChosen(Size size, int rotation) {
        try {
            detector = TensorflowPenDetector.create(getAssets(), MODEL_FILE, INPUT_SIZE_WIDTH, INPUT_SIZE_HEIGHT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();
        sensorOrientation = rotation - getScreenOrientation();

        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(INPUT_SIZE_WIDTH, INPUT_SIZE_HEIGHT, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        INPUT_SIZE_WIDTH, INPUT_SIZE_HEIGHT,
                        sensorOrientation, false);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.pen_overlay);
        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
//                        draw(canvas);
                    }
                });

    }

    private void draw(Canvas canvas) {
        final boolean rotated = sensorOrientation % 180 == 90;
        final float multiplier =
                Math.min(canvas.getHeight() / (float) (rotated ? previewWidth : previewHeight),
                        canvas.getWidth() / (float) (rotated ? previewHeight : previewWidth));
        frameToCanvasMatrix =
                ImageUtils.getTransformationMatrix(
                        previewWidth,
                        previewHeight,
                        (int) (multiplier * (rotated ? previewHeight : previewWidth)),
                        (int) (multiplier * (rotated ? previewWidth : previewHeight)),
                        sensorOrientation,
                        false);
//        final RectF trackedPos =
//                (objectTracker != null)
//                        ? recognition.trackedObject.getTrackedPositionInPreviewFrame()
//                        : new RectF(recognition.location);
//
//        Paint boxPaint = new Paint();
//        boxPaint.setColor(Color.RED);
//        boxPaint.setStyle(Paint.Style.STROKE);
//        boxPaint.setStrokeWidth(12.0f);
//        boxPaint.setStrokeCap(Paint.Cap.ROUND);
//        boxPaint.setStrokeJoin(Paint.Join.ROUND);
//        boxPaint.setStrokeMiter(100);
//        final float cornerSize = Math.min(w, h) / 8.0f;
//        canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);
//
//        final String labelString = "";
//        canvas.drawText(labelString, x, y, labelString);
    }

    /**
     * 给到当前Activity对应xml ID
     * @return
     */
    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_pen;
    }

    /**
     * 获得所需的预览帧大小
     * @return
     */
    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }
}
