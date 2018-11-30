package org.tensorflow.demo;


import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.Tensor;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class TensorflowPenDetector implements ClassifierPen {
    private String inputName;
    private int inputHeight;
    private int inputWidht;
    private int[] intValues;
    private float[] byteValues;
    private String outputVectNmae;
    private String outputHeatNmae;
    private String confidence1Names;
    private String inputNames;
    private String[] outputNames;
    private float[] outputVect;
    private float[] outputHeat;
    private float[] confidence1;
    private float[] input;
    private TensorFlowInferenceInterface inferenceInterface;

    public static ClassifierPen create(final AssetManager assetManager, final String modelFilename, final int inputWidht, final int inputHeight) throws IOException {
        final TensorflowPenDetector tpd = new TensorflowPenDetector();
        tpd.inputWidht = inputWidht;
        tpd.inputHeight = inputHeight;
        tpd.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);
        final Graph g = tpd.inferenceInterface.graph();

        tpd.inputName = "Placeholder";
        tpd.outputVectNmae = "vect";
        tpd.outputHeatNmae = "heat";
        tpd.confidence1Names = "confidence1";
        tpd.inputNames = "input";
        tpd.outputNames = new String[]{tpd.outputVectNmae, tpd.outputHeatNmae, tpd.confidence1Names, tpd.inputNames};

        final Operation inputOp = g.operation(tpd.inputName);
        final Operation outputOpVect = g.operation(tpd.outputVectNmae);
        final Operation outputOpHeat = g.operation(tpd.outputHeatNmae);
        tpd.intValues = new int[tpd.inputWidht * tpd.inputHeight];
        tpd.byteValues = new float[tpd.inputWidht * tpd.inputHeight * 3];
        tpd.outputVect = new float[15*20*2];
        tpd.outputHeat = new float[15*20*6];
        tpd.confidence1 = new float[15*20*3];
        tpd.input = new float[tpd.inputWidht * tpd.inputHeight * 3];
        return tpd;
    }

    @Override
    public List<float[]> recognizeImage(Bitmap bitmap) {
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            byteValues[i * 3 + 2] = (float) (intValues[i] & 0xFF)/255;
            byteValues[i * 3 + 1] = (float) ((intValues[i] >> 8) & 0xFF)/255;
            byteValues[i * 3] = (float) ((intValues[i] >> 16) & 0xFF)/255;
//            byteValues[i * 3 + 2] = 0;
//            byteValues[i * 3 + 1] = 0;
//            byteValues[i * 3] = 0;
        }
        inferenceInterface.feed(inputName, byteValues, 1, inputWidht, inputHeight, 3);
        long[] dims = {1, inputWidht, inputHeight, 3};
        FloatBuffer a = FloatBuffer.wrap(byteValues);
        Tensor<Float> b = Tensor.create(dims, a);

        inferenceInterface.run(outputNames);
        inferenceInterface.fetch(outputNames[0], outputVect);
        inferenceInterface.fetch(outputNames[1], outputHeat);
        inferenceInterface.fetch(outputNames[2], confidence1);
        inferenceInterface.fetch(outputNames[3], input);
        final ArrayList<float[]> recognitions = new ArrayList<float[]>();
        recognitions.add(outputVect);
        recognitions.add(outputHeat);
        recognitions.add(confidence1);
        recognitions.add(input);
        return recognitions;
    }

    @Override
    public void enableStatLogging(boolean debug) {

    }

    @Override
    public String getStatString() {
        return null;
    }

    @Override
    public void close() {

    }
}
