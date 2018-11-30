package org.tensorflow.demo.util;

public class Numpy {
    public float array[] = null;
    public double max = 0;
    public double min = 0;
    public double mean = 0;
    public double std = 0;

    public Numpy(float [] array){
        this.array = array;
        getMax();
        getMin();
        getMean();
        getStd();
    }

    private void getStd() {
        double sum = 0;
        for (float anArray : this.array) {
            sum+=Math.pow(anArray-this.mean, 2);
        }
        double variance = sum/this.array.length;
        std = Math.sqrt(variance);
    }

    private void getMean() {
        double sum = 0;
        for (float anArray : this.array) {
            sum+=anArray;
        }
        mean = sum/this.array.length;
    }

    private void getMin() {
        double temp = 255;
        for (float anArray : this.array) {
            if (temp > anArray) {
                temp = anArray;
            }
        }
        min = temp;
    }

    private void getMax() {
        double temp = 0;
        for (float anArray : this.array) {
            if (temp < anArray) {
                temp = anArray;
            }
        }
        max = temp;
    }
}
