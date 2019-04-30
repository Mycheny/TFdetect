package org.tensorflow.demo.test.four;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Revert {
    public  static  void revertIntoFile(String content,String filepath) throws FileNotFoundException {
        OutputStream outputStream = new FileOutputStream(new File(filepath));
        try {
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String args[]) throws FileNotFoundException {
        String content = "water without a source and a tree without roots";
        String filepath = "d://a.txt";
        revertIntoFile(content, filepath);
    }
}
