package org.tensorflow.demo.util;

import android.graphics.Bitmap;
import android.graphics.Point;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理笔尖识别结果的工具类
 */
public class DealResult {
    private float[] vect = null;
    private float[] heat = null;

    private static DealResult dealResult = new DealResult();
    public static DealResult getInstance() {
        return dealResult;
    }

    public float [] slice(float array[], int start, int end, int... arg) {
        int channel = 3;
        int thickness = end - start;
        if (arg.length == 1) {
            channel = arg[0];
        }
        float[] byteConfidence1 = new float[thickness * 20 * 15];
        //切片
        for (int i = 0; i < byteConfidence1.length; i++) {
            if(i>byteConfidence1.length-10){
                int a = 1;
            }
            int index = i / thickness * channel + i % thickness + start;
            byteConfidence1[i] = array[index];
        }
        return byteConfidence1;
    }

    public Bitmap buildImage(float [] array, int channel){
        byte[] byteArray = new byte[20*15*4];
        for(int i=0;i<300;i++){
            byteArray[i*4] = (byte) (array[i*channel]*255);
            if(channel>=2) {
                byteArray[i*4+1] = (byte) (array[i*channel+1]*255);
            }else {
                byteArray[i*4+1] = 0;
            }
            byteArray[i*4+2] = 0;
            byteArray[i*4+3] = (byte) (255*0.7);
        }
        Bitmap stitchBmp = Bitmap.createBitmap(20, 15, Bitmap.Config.ARGB_4444);
        stitchBmp.copyPixelsFromBuffer(ByteBuffer.wrap(byteArray));
        return stitchBmp;
    }

    private List<Point> getPoint(float [] array, float... arg){
        List<Point> pointList = new ArrayList<Point>();
        float threshold;
        if(arg.length==0){
            threshold = 0.8f;
        }else {
            threshold = arg[0];
        }
        for(int i=0;i<array.length;i++){
            if(array[i]>threshold){
                Point point = new Point();
                point.y = i/20;
                point.x = i%20;
                pointList.add(point);
            }
        }
        return pointList;
    }



    public List<Result> getPen(){
        List<Result> resultList = new ArrayList<Result>();
        float [] confidence1 = slice(this.heat, 0, 1, 6);
        float [] confidence2 = slice(this.heat, 1, 2, 6);
        List<Point> pointList1 = getPoint(confidence1);
        List<Point> pointList2 = getPoint(confidence2);
        for(Point point1:pointList1){
            for(Point point2:pointList2){
                Result result = new Result();
                Point point11 = new Point();
                Point point22 = new Point();
                result.grade= getScore(point1, point2);
                float x10 = this.heat[6 * point1.x + 120 * point1.y + 2];
                float y10 = this.heat[6 * point1.x + 120 * point1.y + 3];
                float x20 = this.heat[6 * point1.x + 120 * point1.y + 4];
                float y20 = this.heat[6 * point1.x + 120 * point1.y + 5];
                point11.x = (int) ((point1.x+x10)*32);
                point11.y = (int) ((point1.y+y10)*32);
                result.point1=point11;
                point22.x = (int) ((point2.x+x20)*32);
                point22.y = (int) ((point2.y+y20)*32);
                result.point2=point22;
                resultList.add(result);
            }
        }
        return resultList;
    }

    private Grade getScore(Point point1, Point point2) {
        Grade grade = new Grade();
        int num_inter = 10;
        double dx = point1.x-point2.x;
        double dy = point1.y-point2.y;
        double normVec = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        if(normVec < 1e-4){
            return grade;
        }
        double vx = dx/normVec;
        double vy = dy/normVec;

        double sx = dx/num_inter;
        double sy = dy/num_inter;
        for (int i=0; i<10;i++){
            int x= (int) (point2.x+sx*i+0.5);
            int y= (int) (point2.y+sy*i+0.5);
            double pafXs = this.vect[40*y+2*x];
            double pafYs = this.vect[40*y+2*x+1];
            double local_scores = pafXs*vx+pafYs*vy;
            if (local_scores>0.2){
                grade.count+=1;
                grade.score+=local_scores;
            }
        }
        return grade;
    }

    public class Result{
        public Grade grade;
        public Point point1;
        public Point point2;
        Result(){}
    }

    public class Grade{
        public float count=0;
        public float score=0;
        Grade(){
        }
    }

    public float[] getVect() {
        return vect;
    }

    public void setVect(float[] vect) {
        this.vect = vect;
    }

    public float[] getHeat() {
        return heat;
    }

    public void setHeat(float[] heat) {
        this.heat = heat;
    }
}
