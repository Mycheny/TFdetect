package org.tensorflow.demo.test.doc;

import java.io.*;
import java.util.Scanner;

/**
 * 这个类演示了文档注释
 * @author Ayan Amhed
 * @version 1.2
 */
public class SquareNum {
    /**
     * This method returns the square of num.
     * This is a multiline description. You can use
     * as many lines as you like.
     * @param num The value to be squared.
     * @return num squared.
     */
    public double square(double num) {
        return num * num;
    }
    /**
     * 这个方法从用户输入一个数字
     * @return The value input as a double.
     * @exception IOException On input error.
     * @see IOException
     */
    public double getNumber() throws IOException {
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader inData = new BufferedReader(isr);
        Scanner scanner=new Scanner(System.in);
        String str = inData.readLine();
        return new Double(str).doubleValue();
    }

    public static <E> void printArray(E[] array){
        for (E e:array){
            System.out.print(e+"\t");
        }
        System.out.println();
    }

    /**
     * This method demonstrates square().
     * @param args Unused.
     * @return Nothing.
     * @exception IOException On input error.
     * @see IOException
     */
    public static void main(String args[]) throws IOException
    {
        printArray(new Integer[]{1, 2, 3});
        SquareNum ob = new SquareNum();
        double val;
        System.out.println("Enter value to be squared: ");
        val = ob.getNumber();
        val = ob.square(val);
        System.out.println("Squared value is " + val);
    }
}