package org.tensorflow.demo.test.three;

public class Rectangle extends Geometry{
    private double width;
    private double height;
    public Rectangle(double width,double height){
        this.width=width;
        this.height=height;
    }
    @Override
    public double getArea() {
        return width*height;
    }

    @Override
    public String toString() {
        return "Rectangle{" +
                "area=" + width*height +
                '}';
    }
}
