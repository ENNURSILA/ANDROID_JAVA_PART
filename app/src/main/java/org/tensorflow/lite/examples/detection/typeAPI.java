package org.tensorflow.lite.examples.detection.tflite;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Trace;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.metadata.MetadataExtractor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class turAPI implements Classifiertur {
    private static final String TAG = "TFLiteObjectDetectionAPIModelWithInterpreter";


    private static final int NUM_DETECTIONS = 10;
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;
    private static final int NUM_THREADS = 4;
    private boolean isModelQuantized;

    private int inputSize;

    private final List<String> labels = new ArrayList<>();
    private int[] intValues;


    private float[][][] outputLocations;
    private float[][] outputClasses;
    private float[][] outputScores;
    private float[] numDetections;

    private ByteBuffer imgData;

    private MappedByteBuffer tfLiteModel;
    private Interpreter.Options tfLiteOptions;
    private Interpreter tfLite;

    private float[][] foodMap;
    private static final int FOOD_SIZE = 4;
    private static final float WHITE_THRESH = 255f;

    turAPI() {}
    /** Memory-map the model file in Assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    public static Classifiertur create(
            final Context context,
            final String modelFilename,
            final String labelFilename,
            final int inputSize,
            final boolean isQuantized)
            throws IOException {
        final typeAPI d = new typeAPI();

        AssetManager am = context.getAssets();
        InputStream is = am.open(labelFilename);

        MappedByteBuffer modelFile = loadModelFile(context.getAssets(), modelFilename);
        MetadataExtractor metadata = new MetadataExtractor(modelFile);
        try (BufferedReader br =
                     new BufferedReader(
                             new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                //Log.w(TAG, line);
                d.labels.add(line);
            }
        }

        d.inputSize = inputSize;

        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(NUM_THREADS);
            options.setUseXNNPACK(true);
            d.tfLite = new Interpreter(modelFile, options);
            d.tfLiteModel = modelFile;
            d.tfLiteOptions = options;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        d.isModelQuantized = isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];

        d.outputLocations = new float[1][NUM_DETECTIONS][2];
        d.outputClasses = new float[1][NUM_DETECTIONS];
        d.outputScores = new float[1][NUM_DETECTIONS];
        d.numDetections = new float[1];
        return d;
    }

    @Override
    public String recognizeImagetur(final Bitmap bitmap) {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage1");

        Trace.beginSection("preprocessBitmap");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];


                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else {
                    imgData.putFloat((pixelValue & 0xFF) / WHITE_THRESH);
                    imgData.putFloat(((pixelValue >> 8) & 0xFF) / WHITE_THRESH);
                    imgData.putFloat(((pixelValue >> 16) & 0xFF) / WHITE_THRESH);
                }
            }
        }


        Trace.endSection();
        // Copy the input data into TensorFlow.
        Trace.beginSection("feed");


        Object[] inputArray = {imgData};

        Map<Integer, Object> outputMap = new HashMap<>();

        foodMap = new float[1][FOOD_SIZE];



        outputMap.put(0, foodMap);
        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("run");


        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);


        Log.w("traytype", "!!!traytype: " + foodMap[0][0] + "|" + foodMap[0][1] + "|" + foodMap[0][2]+ "|" + foodMap[0][3]);

        Integer foodInd = 0;

        Float max = -1f;
        for (int i = 0; i < foodMap[0].length; i++) {
            float currValue = foodMap[0][i];

            if (currValue > max) {
                max = currValue;
                foodInd = i;
            }

        }


        List<String> foodList = Arrays.asList("rice", "stew","beans","pasta");


        // String label = "traytype: " + foodList.get(foodInd);
        String label = foodList.get(foodInd) ;

        Trace.endSection();
        final int numDetectionsOutput = 1;
        final ArrayList<Recognition> recognitions = new ArrayList<>(numDetectionsOutput);


        Trace.endSection();
        return label;// donen label
    }

//    private int fiyatHesaplaGetir(int foodInd) {
//        int fiyat = 0;
//        if(foodInd==0){
//             fiyat = 20;
//        }
//        else if(foodInd==1){
//            fiyat = 40;
//        }
//        else if(foodInd==2){
//            fiyat = 30;
//        }
//        else if(foodInd==3){
//            fiyat = 35;
//        }
//        return fiyat;
//    }


//    @Override
//    public List<Recognition> recognizeImage(Bitmap bitmap) {
//        return null;
//    }



    @Override
    public void enableStatLogging(boolean debug) {

    }

    @Override
    public String getStatString() {
        return null;
    }

    @Override
    public void close() {
        if (tfLite != null) {
            tfLite.close();
            tfLite = null;
        }
    }

    @Override
    public void setNumThreads(int num_threads) {
        if (tfLite != null) {
            tfLiteOptions.setNumThreads(num_threads);
            recreateInterpreter();
        }
    }

    @Override
    public void setUseNNAPI(boolean isChecked) {
        if (tfLite != null) {
            tfLiteOptions.setUseNNAPI(isChecked);
            recreateInterpreter();
        }
    }

    private void recreateInterpreter() {
        tfLite.close();
        tfLite = new Interpreter(tfLiteModel, tfLiteOptions);
    }

    @Override
    public float getObjThresh() {
        return 0;
    }



}



//package org.tensorflow.lite.examples.detection.tflite;
//
//import android.app.Activity;
//import android.content.res.AssetFileDescriptor;
//import android.graphics.Bitmap;
//import android.os.SystemClock;
//import android.util.Log;
//
//import org.tensorflow.lite.Interpreter;
//import org.tensorflow.lite.examples.detection.DetectorActivity;
//
//import java.io.BufferedReader;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.MappedByteBuffer;
//import java.nio.channels.FileChannel;
//import java.util.AbstractMap;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Map;
//import java.util.PriorityQueue;
//
//import static java.lang.Math.min;
//
//import android.content.Context;
//import android.content.res.AssetFileDescriptor;
//import android.content.res.AssetManager;
//import android.graphics.Bitmap;
//import android.os.Trace;
//import android.util.Log;
//import java.io.BufferedReader;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.MappedByteBuffer;
//import java.nio.channels.FileChannel;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import org.tensorflow.lite.Interpreter;
//import org.tensorflow.lite.support.metadata.MetadataExtractor;
//
//public class TFLiteObjectDetectionAPIModel implements ClassifierTwo {
//
//    static final int DIM_IMG_SIZE_X = 224;
//    static final int DIM_IMG_SIZE_Y = 224;
//    //  private static final String TAG = "ImageClassifier";
//    /**
//     * Name of the model file stored in Assets.
//     */
//    private static final String MODEL_PATH = "yenimiz.tflite";
//    /**
//     * Name of the label file stored in Assets.
//     */
//    private static final String LABEL_PATH = "coco.txt";
//    /**
//     * Number of results to show in the UI.
//     */
//    private static final int RESULTS_TO_SHOW = 3;
//    /**
//     * Dimensions of inputs.
//     */
//    private static final int DIM_BATCH_SIZE = 1;
//    private static final int DIM_PIXEL_SIZE = 3;
//    /* Preallocated buffers for storing image data in. */
//    private int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
//
//    /**
//     * An instance of the driver class to run model inference with Tensorflow Lite.
//     */
//    private Interpreter tflite;
//
//    /**
//     * Labels corresponding to the output of the vision model.
//     */
//    private List<String> labelList;
//
//    /**
//     * A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs.
//     */
//    private ByteBuffer imgData = null;
//
//    /**
//     * An array to hold inference results, to be feed into Tensorflow Lite as outputs.
//     */
//    private byte[][] labelProbArray = null;
//
//    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
//            new PriorityQueue<>(
//                    RESULTS_TO_SHOW,
//                    new Comparator<Map.Entry<String, Float>>() {
//                        @Override
//                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
//                            return (o1.getValue()).compareTo(o2.getValue());
//                        }
//                    });
//
//    /**
//     * Initializes an {@code ImageClassifier}.
//     */
//    TFLiteObjectDetectionAPIModel(Activity activity) throws IOException {
//        tflite = new Interpreter(loadModelFile(activity));
//        labelList = loadLabelList(activity);
//        imgData =
//                ByteBuffer.allocateDirect(
//                        DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
//        imgData.order(ByteOrder.nativeOrder());
//        labelProbArray = new byte[1][labelList.size()];
//    //    Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");
//    }
//
//    public TFLiteObjectDetectionAPIModel() {
//
//    }
//
//
//    /**
//     * Classifies a frame from the preview stream.
//     */
//    String classifyFrame(Bitmap bitmap) {
//        if (tflite == null) {
//        //    Log.e(TAG, "Image classifier has not been initialized; Skipped.");
//            return "Uninitialized Classifier.";
//        }
//        recognizeImage1(bitmap);
//        // Here's where the magic happens!!!
//        long startTime = SystemClock.uptimeMillis();
//        tflite.run(imgData, labelProbArray);
//        long endTime = SystemClock.uptimeMillis();
//   //     Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime));
//        String textToShow = printTopKLabels();
//        textToShow = Long.toString(endTime - startTime) + "ms" + textToShow;
//        return textToShow;
//    }
//
//    /**
//     * Reads label list from Assets.
//     */
//    private List<String> loadLabelList(Activity activity) throws IOException {
//        List<String> labelList = new ArrayList<String>();
//        BufferedReader reader =
//                new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
//        String line;
//        while ((line = reader.readLine()) != null) {
//            labelList.add(line);
//        }
//        reader.close();
//        return labelList;
//    }
//
//    /**
//     * Memory-map the model file in Assets.
//     */
//    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
//        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
//        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
//        FileChannel fileChannel = inputStream.getChannel();
//        long startOffset = fileDescriptor.getStartOffset();
//        long declaredLength = fileDescriptor.getDeclaredLength();
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
//    }
//
//
//    ///ben ekledim
//
//    public static ClassifierTwo create(
//            final Context context,
//            final String modelFilename,
//            final String labelFilename,
//            final int inputSize,
//            final boolean isQuantized)
//            throws IOException {
//        final TFLiteObjectDetectionAPIModel d = new TFLiteObjectDetectionAPIModel();
//
//        AssetManager am = context.getAssets();
//        InputStream is = am.open(labelFilename);
//
//
//
//
//
//        return d;
// }
//
//
//    ///ben ekledim
//
//
//    /**
//     * Writes Image data into a {@code ByteBuffer}.
//     * @return
//     */
//    public String recognizeImage1(Bitmap bitmap) {
//        if (imgData == null) {
//            return null;
//        }
//        imgData.rewind();
//        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
//        // Convert the image to floating point.
//        int pixel = 0;
//        long startTime = SystemClock.uptimeMillis();
//        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
//            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
//                final int val = intValues[pixel++];
//                imgData.put((byte) ((val >> 16) & 0xFF));
//                imgData.put((byte) ((val >> 8) & 0xFF));
//                imgData.put((byte) (val & 0xFF));
//            }
//        }
//        long endTime = SystemClock.uptimeMillis();
//      //  Log.d(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
//        return null;
//    }
//
//    @Override
//    public void enableStatLogging(boolean debug) {
//
//    }
//
//    @Override
//    public String getStatString() {
//        return null;
//    }
//
//
//    /**
//     * Prints top-K labels, to be shown in UI as the results.
//     */
//    private String printTopKLabels() {
//        for (int i = 0; i < labelList.size(); ++i) {
//            sortedLabels.add(
//                    new AbstractMap.SimpleEntry<>(labelList.get(i), (labelProbArray[0][i] & 0xff) / 255.0f));
//            if (sortedLabels.size() > RESULTS_TO_SHOW) {
//                sortedLabels.poll();
//            }
//        }
//        String textToShow = "";
//        final int size = sortedLabels.size();
//        for (int i = 0; i < size; ++i) {
//            Map.Entry<String, Float> label = sortedLabels.poll();
//            textToShow = "\n" + label.getKey() + ":" + Float.toString(label.getValue()) + textToShow;
//        }
//        return textToShow;
//    }
//
//    public void close() {
//        tflite.close();
//        tflite = null;
//    }
//
//    @Override
//    public void setNumThreads(int num_threads) {
//    }
//
//    @Override
//    public void setUseNNAPI(boolean isChecked) {
//
//    }
//
//    @Override
//    public float getObjThresh() {
//        return 0;
//    }
//}