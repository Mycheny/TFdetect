package org.tensorflow.demo.test.three;

import android.graphics.Point;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class GeometrySort {
    public static void sort(List<Geometry> list){
        System.out.print("排序前\n");
        for (Geometry geometry: list) {
            System.out.print(geometry);
        }
        System.out.print("\n排序后\n");
        Collections.sort(list, new Comparator<Geometry>() {
            @Override
            public int compare(Geometry geometry1, Geometry geometry2) {
                Double area1 = geometry1.getArea();
                return area1.compareTo(geometry2.getArea());
//                double a = geometry1.getArea()-geometry2.getArea();
//                return (int) Math.ceil(geometry1.getArea()-geometry2.getArea());
            }
        });
        for (Geometry geometry: list) {
            System.out.print(geometry);
        }
    }
    public static void main(String args[]){
        List<Geometry> geometryList = new LinkedList<>();
        geometryList.add(new Circle(2.0));
        geometryList.add(new Rectangle(4.0,3.0));
        sort(geometryList);
        System.out.println("\n"+Math.PI);
        double pi=0.0d;
        for(long i=1;i<=2000000000;i++) {
            pi += Math.pow(-1, (i + 1)) * 4 / (2 * i - 1);
            if (i%2000000==0) {
                System.out.println(String.format("%d", i/2000000));
            }
        }
        System.out.println("\n"+pi);
    }
}
