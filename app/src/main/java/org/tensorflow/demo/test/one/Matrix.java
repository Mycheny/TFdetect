package org.tensorflow.demo.test.one;

public class Matrix {
    public static int[][] addMatrix(int [][]a, int [][]b){
        for (int i=0;i<a.length;i++) {
            for (int j=0;j<a[0].length;j++) {
                a[i][j] = a[i][j]+b[i][j];
            }
        }
        return a;
    }
    public static void main(String args[]){
        int a[][] = {{1,2,3,4},{1,2,3,4},{1,2,3,4}};
        int b[][] = {{1,1,1,1},{1,1,1,1},{1,1,1,1}};
        int [][]result = addMatrix(a, b);
        System.out.print(result[0][0]);
    }
}
