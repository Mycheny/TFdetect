package org.tensorflow.demo.util;

public class Numpy {
    public float array[] = null;
    public double max = 0;
    public double min = 0;
    public double mean = 0;
    public double std = 0;

    public Numpy(float [] array){
        this.array = array;
        init();
    }

    private void init() {
        double sum_mean = 0;
        double sum_std = 0;
        double temp_min = 255;
        double temp_max = 0;
        for (float anArray : this.array) {
            sum_mean+=anArray;
            if (temp_min > anArray) {
                temp_min = anArray;
            }else if (temp_max < anArray) {
                temp_max = anArray;
            }
        }
        mean = sum_mean/this.array.length;


        for (float anArray : this.array) {
            sum_std+=Math.pow(anArray-this.mean, 2);
        }
        double variance = sum_std/this.array.length;
        std = Math.sqrt(variance);
        min = temp_min;
        max = temp_max;
    }

    @Override
    public String toString() {
        return "Numpy{" +
                "max=" + max +
                ", min=" + min +
                ", mean=" + mean +
                ", std=" + std +
                '}';
    }
}
