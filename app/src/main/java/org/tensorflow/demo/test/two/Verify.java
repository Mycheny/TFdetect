package org.tensorflow.demo.test.two;

import java.util.regex.Pattern;

public class Verify {
    public static  boolean verifyPhone(String str){
        String pattern = "[1]{1}[356789]{1}[0-9]{9}";
        boolean result = Pattern.matches(pattern, str);
        return result;
    }
    public static void main(String args[]){
        String content = "15078945612";
        System.out.println("是否是电话号码： " + verifyPhone(content));
    }
}
