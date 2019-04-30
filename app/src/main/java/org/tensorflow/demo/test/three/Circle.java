package org.tensorflow.demo.test.three;

public class Circle extends Geometry {
    private double radius;

    public Circle(double radius){
        this.radius=radius;
    }
    @Override
    public double getArea() {
        return Math.PI*Math.pow(radius,2);
    }

    @Override
    public String toString() {
        return "Circle{" +
                "area=" + Math.PI*Math.pow(radius,2) +
                '}';
    }
}
