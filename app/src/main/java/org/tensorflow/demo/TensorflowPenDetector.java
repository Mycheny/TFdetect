package org.tensorflow.demo;


import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TensorflowPenDetector implements ClassifierPen {
    private String inputName;
    private int inputHeight;
    private int inputWidht;
    private int[] intValues;
    private byte[] byteValues;
    private String outputVectNmae;
    private String outputHeatNmae;
    private String[] outputNames;
    private float[] outputVect;
    private float[] outputHeat;
    private TensorFlowInferenceInterface inferenceInterface;

    public static ClassifierPen create(final AssetManager assetManager, final String modelFilename, final int inputWidht, final int inputHeight) throws IOException {
        final TensorflowPenDetector tpd = new TensorflowPenDetector();
        tpd.inputWidht = inputWidht;
        tpd.inputHeight = inputHeight;
        tpd.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);
        final Graph g = tpd.inferenceInterface.graph();

        tpd.inputName = "Placeholder";
        tpd.outputVectNmae = "export/pen/stage4/Phase4_vect/Phase4_vect_Conv2d_5_3_pointwise/BatchNorm/FusedBatchNorm";
        tpd.outputHeatNmae = "export/pen/stage4/Phase4_heat/Phase4_heat_Conv2d_5_3_pointwise/BatchNorm/FusedBatchNorm";
        tpd.outputNames = new String[]{tpd.outputVectNmae, tpd.outputHeatNmae};

        final Operation inputOp = g.operation(tpd.inputName);
        final Operation outputOpVect = g.operation(tpd.outputVectNmae);
        final Operation outputOpHeat = g.operation(tpd.outputHeatNmae);
        tpd.intValues = new int[tpd.inputWidht * tpd.inputHeight];
        tpd.byteValues = new byte[tpd.inputWidht * tpd.inputHeight * 3];
        tpd.outputVect = new float[15*20*2];
        tpd.outputHeat = new float[15*20*6];
        return tpd;
    }

    @Override
    public List<float[]> recognizeImage(Bitmap bitmap) {
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            byteValues[i * 3 + 2] = (byte) (intValues[i] & 0xFF);
            byteValues[i * 3 + 1] = (byte) ((intValues[i] >> 8) & 0xFF);
            byteValues[i * 3] = (byte) ((intValues[i] >> 16) & 0xFF);
        }
        inferenceInterface.feed(inputName, byteValues, 1, inputWidht, inputHeight, 3);
        inferenceInterface.run(outputNames);
        inferenceInterface.fetch(outputNames[0], outputVect);
        inferenceInterface.fetch(outputNames[1], outputHeat);
        final ArrayList<float[]> recognitions = new ArrayList<float[]>();
        recognitions.add(outputVect);
        recognitions.add(outputHeat);
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
