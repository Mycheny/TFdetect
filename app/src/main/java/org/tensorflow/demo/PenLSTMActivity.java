package org.tensorflow.demo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.util.DealResult;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;

public class PenLSTMActivity extends CameraActivity implements OnImageAvailableListener, OnTouchListener {
    private static final Logger LOGGER = new Logger();
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final int INPUT_SIZE_WIDTH = 640;
    private static final int INPUT_SIZE_HEIGHT = 480;
    private ClassifierPen detector;
    private static final String MODEL_FILE = "file:///android_asset/lstm_model_pen.pb";
    OverlayView trackingOverlay;

    private Integer sensorOrientation;
    private int previewHeight;
    private int previewWidth;
    private Matrix frameToCanvasMatrix;
    private long timestamp = 0;
    private boolean computingDetection = false;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null; //主视图
    private Bitmap cropCopyBitmap = null;  //右下角视图
    private byte[] luminanceCopy;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private long lastProcessingTimeMs;
    private float offset_x = -1;
    private float offset_y = -1;

    private int []coordinates = new int[2];

    public String listToString(List list, char separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i)).append(separator);

        }
        return sb.toString().substring(0, sb.toString().length() - 1);
    }

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
                        final List<int []> results = detector.recognizeImageIntBuffer(croppedBitmap);
                        LOGGER.i("result", String.valueOf(results.get(0)[0])+String.valueOf(results.get(1)[0]));
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

//                        Numpy vectArray = new Numpy(results.get(0));
//                        Numpy heatArray = new Numpy(results.get(1));
//                        Numpy confidence1Array = new Numpy(results.get(2));
//                        Numpy inputArray = new Numpy(results.get(3));
                        coordinates[0] = results.get(0)[0];
                        coordinates[1] = results.get(1)[0];
//
                        //开始绘制右下角视图
                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);

                        canvas.drawBitmap(croppedBitmap, new Matrix(), new Paint());

                        final Paint paint = new Paint();
                        paint.setTextSize(50);
                        paint.setColor(Color.BLUE);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(2.0f);
                        canvas.drawText(String.format("FPS %.3f", 1000.0f/lastProcessingTimeMs),0, 50, paint);
                        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);

                        Paint paintLine = new Paint();
                        paintLine.setColor(Color.WHITE);
                        if (coordinates!=null){
                            canvas.drawRect(coordinates[0]-10, coordinates[1]-10, coordinates[0]+10, coordinates[1]+10, paint);
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
            detector = TensorflowPenLSTMDetector.create(getAssets(), MODEL_FILE, INPUT_SIZE_WIDTH, INPUT_SIZE_HEIGHT);
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
                        if(coordinates!=null){
                            Paint paint = new Paint();
                            paint.setTextSize(50);
                            paint.setColor(Color.CYAN);
                            paint.setStyle(Paint.Style.STROKE);
                            int x = canvas.getWidth()*coordinates[0]/640;
                            int y = (int) (canvas.getHeight()*(486.0/764)*coordinates[1]/480);
                            canvas.drawRect(x-10, y-10, x+10, y+10, paint);
                        }
                    }
                });
        //在屏幕中加入右下角视图(addDebugCallback: 父类CameraActivity的方法)
        addDebugCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        final Bitmap copy = cropCopyBitmap;  //复制右下角视图
                        if (copy!=null){
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
                            int x = canvas.getWidth()*coordinates[0]/640;
                            int y = (int) (canvas.getHeight()*(486.0/764)*coordinates[1]/480);
                            canvas.drawText("POINT1 "+coordinates[0] + " " + coordinates[1],0, 150, paint);
                            canvas.drawText("POINT2 "+x + " " + y,0, 250, paint);
                            canvas.drawText("wh "+canvas.getWidth() + " " + canvas.getHeight(),0, 350, paint);
                            canvas.drawRect(x-10, y-10, x+10, y+10, paint);
                        }
                    }
                });


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
