package org.tensorflow.demo;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.nio.ByteBuffer;

public class Test {
    public void test(){
        Resources res = null;
//        Resources res = PenActivity.this.getResources();
        Bitmap bitmap= BitmapFactory.decodeResource(res, R.drawable.timg);
        int intValues[] = new int[bitmap.getWidth()*bitmap.getHeight()];
        byte byteValues[] = new byte[bitmap.getWidth()*bitmap.getHeight()*3];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < intValues.length; ++i) {
            byteValues[i * 3 + 2] = (byte) (intValues[i] & 0xFF);
            byteValues[i * 3 + 1] = (byte) ((intValues[i] >> 8) & 0xFF);
            byteValues[i * 3] = (byte) ((intValues[i] >> 16) & 0xFF);
        }

        byte[] image = new byte[bitmap.getWidth()*bitmap.getHeight()*4];
        for(int i=0;i<bitmap.getWidth()*bitmap.getHeight();i++){
            image[i*4] = (byte) byteValues[i*3];
            image[i*4+1] = (byte) byteValues[i*3+1];
            image[i*4+2] = (byte) byteValues[i*3+2];
            // a
            image[i*4+3] = (byte) -1;
        }
        Bitmap stitchBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_4444);
        stitchBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(image));

        int[] intValues2 = new int[bitmap.getWidth() * bitmap.getHeight()];
        for (int i = 0; i < intValues2.length; ++i) {
            intValues2[i] =
                    0xFF000000
                            | (((int) (byteValues[i * 3])) << 16)
                            | (((int) (byteValues[i * 3 + 1])) << 8)
                            | ((int) (byteValues[i * 3 + 2]));
        }
        Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_4444);
        newBitmap.setPixels(intValues, 0, newBitmap.getWidth(), 0, 0, newBitmap.getWidth(), newBitmap.getHeight());

        //                        Bitmap bmp = Bitmap.createBitmap(20, 15, Bitmap.Config.ARGB_4444);
//                        for(int x = 0;x < 20;x++){
//                            for(int y = 0;y < 15;y++){
//                                bmp.setPixel(x,y,Color.argb(255,(int)(heat[(x*20+y)*6]*255),0,0));
//                            }
//                        }
//        for(int i=0;i<confidence1.length/6;i++){
//            if(i%5==0&i/20%2!=0){
//                confidence1[i*6] = 1;
//            }else {
//                confidence1[i*6] = 0;
//            }
//            confidence1[i*6+1] = 1;
//            confidence1[i*6+2] = 0;
//            confidence1[i*6+3] = 0;
//            confidence1[i*6+4] = 0;
//            confidence1[i*6+5] = 0;
//        }
    }
}
