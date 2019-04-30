package org.tensorflow.demo;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.Tensor;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class TensorflowPenLSTMDetector implements ClassifierPen {
    private String inputImageName;
    private String outputPenXName;
    private String outputPenYName;
    private String initialLstmHName;
    private String initialLstmCName;
    private String finalLstmHName;
    private String finalLstmCName;
    private int inputHeight;
    private int inputWidht;
    private int[] intValues;
    private float[] byteValues;
    private float[] initialLstmH;
    private float[] initialLstmC;

    private String[] outputNames;
    private int []outputPenX;
    private int []outputPenY;
    private float[] finalLstmH;
    private float[] finalLstmC;
    private float[] input;
    private TensorFlowInferenceInterface inferenceInterface;

    public static ClassifierPen create(final AssetManager assetManager, final String modelFilename, final int inputWidht, final int inputHeight) throws IOException {
        final TensorflowPenLSTMDetector tpd = new TensorflowPenLSTMDetector();
        tpd.inputWidht = inputWidht;
        tpd.inputHeight = inputHeight;
        tpd.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);
        final Graph g = tpd.inferenceInterface.graph();

        tpd.inputImageName = "input_image";
        tpd.outputPenXName = "pen_x";
        tpd.outputPenYName = "pen_y";
        tpd.initialLstmHName = "ConvLSTM/LSTM_H";
        tpd.initialLstmCName = "ConvLSTM/LSTM_C";
        tpd.finalLstmHName = "Network/ConvLSTM/rnn/while/Exit_4";
        tpd.finalLstmCName = "Network/ConvLSTM/rnn/while/Exit_3";

        tpd.outputNames = new String[]{tpd.outputPenXName, tpd.outputPenYName, tpd.finalLstmCName, tpd.finalLstmHName};

        tpd.intValues = new int[tpd.inputWidht * tpd.inputHeight];
        tpd.byteValues = new float[tpd.inputWidht * tpd.inputHeight * 3];
        tpd.initialLstmH = new float[15*20*128];
        tpd.initialLstmC = new float[15*20*128];
        tpd.finalLstmH = new float[15*20*128];
        tpd.finalLstmC = new float[15*20*128];
        tpd.outputPenX = new int[1];
        tpd.outputPenY = new int[1];
        tpd.input = new float[tpd.inputWidht * tpd.inputHeight * 3];
        return tpd;
    }

    @Override
    public List<float[]> recognizeImage(Bitmap bitmap) {
        return null;
    }

    @Override
    public List<int []> recognizeImageIntBuffer(Bitmap bitmap) {
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            byteValues[i * 3 + 2] = (float) (intValues[i] & 0xFF)/255;
            byteValues[i * 3 + 1] = (float) ((intValues[i] >> 8) & 0xFF)/255;
            byteValues[i * 3] = (float) ((intValues[i] >> 16) & 0xFF)/255;
//            byteValues[i * 3 + 2] = 0;
//            byteValues[i * 3 + 1] = 0;
//            byteValues[i * 3] = 0;
        }
        inferenceInterface.feed(inputImageName, byteValues, 1, inputHeight, inputWidht, 3);
        inferenceInterface.feed(initialLstmHName, initialLstmH, 1, 15, 20, 128);
        inferenceInterface.feed(initialLstmCName, initialLstmC, 1, 15, 20, 128);

        inferenceInterface.run(outputNames);
        inferenceInterface.fetch(outputNames[0], outputPenX);
        inferenceInterface.fetch(outputNames[1], outputPenY);
        inferenceInterface.fetch(outputNames[2], finalLstmH);
        inferenceInterface.fetch(outputNames[3], finalLstmC);
        final ArrayList<int []> recognitions = new ArrayList<int []>();
        recognitions.add(outputPenX);
        recognitions.add(outputPenY);
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
