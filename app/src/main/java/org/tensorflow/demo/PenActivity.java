package org.tensorflow.demo;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.tracking.MultiBoxTracker;
import org.tensorflow.demo.util.Deal;
import org.tensorflow.demo.util.Numpy;
import org.w3c.dom.Text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PenActivity extends CameraActivity implements OnImageAvailableListener, OnTouchListener {
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
    private Bitmap cropCopyBitmap = null;  //右下角视图
    private byte[] luminanceCopy;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private long lastProcessingTimeMs;
    private float offset_x = -1;
    private float offset_y = -1;

    private Deal deal = Deal.getInstance();

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

//                        Numpy vectArray = new Numpy(results.get(0));
//                        Numpy heatArray = new Numpy(results.get(1));
//                        Numpy confidence1Array = new Numpy(results.get(2));
//                        Numpy inputArray = new Numpy(results.get(3));
                        deal.setVect(results.get(0));
                        deal.setHeat(results.get(1));
                        List<Deal.Result> coord = deal.getPen();
                        float[] confidence1 = results.get(1);
                        byte[] byteHeat1 = slice(confidence1, 0, 3, 6);
                        Bitmap stitchBmp = Bitmap.createBitmap(20, 15, Bitmap.Config.ARGB_4444);
                        stitchBmp.copyPixelsFromBuffer(ByteBuffer.wrap(byteHeat1));

                        //开始绘制右下角视图
                        Matrix matrix = new Matrix();
                        matrix.postScale(32, 32);
                        Bitmap newbm = Bitmap.createBitmap(stitchBmp, 0, 0, 20, 15, matrix,true);

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);

                        canvas.drawBitmap(newbm, new Matrix(), new Paint());

                        final Paint paint = new Paint();
                        paint.setTextSize(50);
                        paint.setColor(Color.BLUE);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(2.0f);
                        canvas.drawText(String.format("FPS %.3f", 1000.0f/lastProcessingTimeMs),0, 50, paint);
                        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);

                        Paint paintLine = new Paint();
                        paintLine.setColor(Color.WHITE);
                        for (Deal.Result result: coord){
                            if(result.grade.count>9.0&result.grade.score>9){
                                canvas.drawLine(result.point1.x, result.point1.y, result.point2.x, result.point2.y, paintLine);
                            }
                        }

                        float minimumConfidence = 0.8f;

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();

                        trackingOverlay.postInvalidate();

                        requestRender();
                        computingDetection = false;
                    }
                });
    }

    public byte [] slice(float array[], int start, int end, int... arg){
        int channel = 3;
        int thickness = end-start;
        if(arg.length==1){
            channel = arg[0];
        }
        byte[] byteConfidence1 = new byte[900];
        //切片
        for(int i=0;i<byteConfidence1.length;i++){
            int index = i/thickness*channel+i%thickness+start;
            byteConfidence1[i] = (byte) (array[index]*255);
        }
        byte[] byteHeat1 = new byte[1200];
        for(int i=0;i<300;i++){
            byteHeat1[i*4] = byteConfidence1[i*3];
            byteHeat1[i*4+1] = byteConfidence1[i*3+1];
//            byteHeat1[i*4+2] = byteConfidence1[i*3+2];
            byteHeat1[i*4+2] = 0;
            // a
            byteHeat1[i*4+3] = (byte) (255*0.5);
        }
        return byteHeat1;
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
        trackingOverlay.setOnTouchListener(this);
        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        draw(canvas);
                    }
                });
        //在屏幕中加入右下角视图(addDebugCallback: 父类CameraActivity的方法)
        addDebugCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        final Bitmap copy = cropCopyBitmap;  //复制右下角视图

                        final int backgroundColor = Color.argb(0, 0, 0, 0);
                        canvas.drawColor(backgroundColor);

                        final Matrix matrix = new Matrix();
                        final float scaleFactor = 1;
                        if(offset_x==-1 | offset_y==-1){
                            offset_x = canvas.getWidth() - copy.getWidth() * scaleFactor;
                            offset_y = canvas.getHeight() - copy.getHeight() * scaleFactor;
                        }
                        matrix.postScale(scaleFactor, scaleFactor); //缩放
                        matrix.postTranslate(offset_x, offset_y); //平移
                        canvas.drawBitmap(copy, matrix, new Paint());
                        Paint paint = new Paint();
                        paint.setTextSize(50);
                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);
                        canvas.drawText("TIME "+lastProcessingTimeMs,0, 50, paint);
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
        Paint paint = new Paint();
        paint.setTextSize(50);
        paint.setColor(Color.CYAN);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawText("aaaa", 200, 500, paint);
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x,y;
        switch (event.getAction()) {
            /**
             * 点击的开始位置
             */
            case MotionEvent.ACTION_DOWN:
                x = event.getX();
                y = event.getY();
                this.offset_x = x;
                this.offset_y = y;
                break;
            /**
             * 触屏实时位置
             */
            case MotionEvent.ACTION_MOVE:
                x = event.getX();
                y = event.getY();
                this.offset_x = x;
                this.offset_y = y;
                break;
            /**
             * 离开屏幕的位置
             */
            case MotionEvent.ACTION_UP:
                x = event.getX();
                y = event.getY();
                this.offset_x = x;
                this.offset_y = y;
                break;
            default:
                break;
        }
        /**
         *  注意返回值
         *  true：view继续响应Touch操作；
         *  false：view不再响应Touch操作，故此处若为false，只能显示起始位置，不能显示实时位置和结束位置
         */
        return true;
    }
}
