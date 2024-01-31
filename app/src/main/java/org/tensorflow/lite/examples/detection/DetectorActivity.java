package org.tensorflow.lite.examples.detection;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.DetectorFactoryDinnerFood;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tflite.typeAPI;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.ClassifierThree;
import org.tensorflow.lite.examples.detection.tflite.DetectorFactory;

import org.tensorflow.lite.examples.detection.tflite.YoloV5Classifier;
import org.tensorflow.lite.examples.detection.tflite.typeAPISoup;
import org.tensorflow.lite.examples.detection.tflite.NEW;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker2;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker3;


public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    private static final int TF_OD_API_INPUT_SIZE = 416;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "trayyolov5.tflite";


    private static final String TF_OD_API_LABELS_FILE = "customclasses.txt";
    private static final int TF_OD_API_INPUT_SIZEE = 224;

    private static final String TF_OD_API_MODEL_FILEE = "vggEveningTrayBreakfastTray.tflite";
    private static final int TF_OD_API_INPUT_SIZEEE = 224;

    private static final String TF_OD_API_MODEL_FILEEE = "zRedSoupWhiteSoupClassification.tflite";


    private static final int TF_OD_API_INPUT_SIZEEEE = 224;


    private static final String TF_OD_API_MODEL_FILa = "brekfast.tflite";

    private static final int TF_OD_API_INPUT_SIZa = 224;

    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.9f;
    private static final float MINIMUM_CONFIDENCE_TF_OD_APIAY = 0.3f;

    private static final boolean MAINTAIN_ASPECT = true;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 640);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    private static final String TF_OD_API_LABELS_FILEEEE ="zRedSoupWhiteSoupClassification.tflite" ;


    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private YoloV5Classifier detector;
    private NEW detector2;



    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private Bitmap shiftBitmap = null;

    private Bitmap xyBitmap = null;
    private Bitmap copyOfOriginalBitmap = null;


    private boolean computingDetection = false;


    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;
    private MultiBoxTracker2 tracker2;
    private MultiBoxTracker3 tracker3;

    private BorderedText borderedText;



    private TFLiteObjectDetectionAPIModel detector1;

    private typeAPI detector4;

    private typeAPISoup detector5;




    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);
        tracker2 = new MultiBoxTracker2(this);
        tracker3 = new MultiBoxTracker3(this);

        final int modelIndex = modelView.getCheckedItemPosition();
        final String modelString = modelStrings.get(modelIndex);

        try {
            detector = DetectorFactory.getDetector(getAssets(), modelString);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        int cropSize = detector.getInputSize();

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();
        int targetW = (int) (previewWidth / 2.0);
        int targetH = (int) (previewHeight / 2.0);
        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);




        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        xyBitmap = Bitmap.createBitmap(targetW, targetH, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        // tracker.drawDebug(canvas);

                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
        OverlayView trackingOverlay2 = (OverlayView) findViewById(R.id.tracking_overlay);

        trackingOverlay2.addCallback(

                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas1) {
                        //   tracker2.drawDebug(canvas1);

                        tracker2.draw(canvas1);
                        if (isDebug()) {
                            tracker2.drawDebug(canvas1);
                        }
                    }
                });

        tracker2.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);


        OverlayView trackingOverlay3 = (OverlayView) findViewById(R.id.tracking_overlay);

        trackingOverlay3.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas1) {
                        tracker3.draw(canvas1);
                        if (isDebug()) {
                            tracker3.drawDebug(canvas1);
                        }
                    }
                });

        tracker3.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);


        try {
            detector1 =
                    (TFLiteObjectDetectionAPIModel) TFLiteObjectDetectionAPIModel.create(
                            this,
                            TF_OD_API_MODEL_FILEE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZEE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
        try {
            final String modelString1 = modelStrings.get(2);

            detector2 = DetectorFactoryDinnerFood.getDetector(getAssets(), modelString1);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        try {
            detector4 =
                    (typeAPI) typeAPI.create(
                            this,
                            TF_OD_API_MODEL_FILEEE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZEEE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }



        try {
            detector5 =
                    (typeAPISoup) typeAPISoup.create(
                            this,
                            TF_OD_API_MODEL_FILEEEE,
                            TF_OD_API_LABELS_FILEEEE,
                            TF_OD_API_INPUT_SIZEEE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }



     }



    protected void updateActiveModel() {
        // Get UI information before delegating to background
        final int modelIndex = modelView.getCheckedItemPosition();
        final int deviceIndex = deviceView.getCheckedItemPosition();
        String threads = threadsTextView.getText().toString().trim();
        final int numThreads = Integer.parseInt(threads);

        handler.post(() -> {
            if (modelIndex == currentModel && deviceIndex == currentDevice
                    && numThreads == currentNumThreads) {
                return;
            }
            currentModel = modelIndex;
            currentDevice = deviceIndex;
            currentNumThreads = numThreads;

            // Disable classifier while updating
            if (detector != null) {
                detector.close();
                detector = null;
            }

            // Lookup names of parameters.
            String modelString = modelStrings.get(modelIndex);
            String device = deviceStrings.get(deviceIndex);
            String modelString2 = modelStrings.get(2);

            LOGGER.i("Changing model to " + modelString + " device " + device);

            // Try to load model.

            try {
                detector = DetectorFactory.getDetector(getAssets(), modelString);
                detector2 = DetectorFactoryDinnerFood.getDetector(getAssets(), modelString2);

                // Customize the interpreter to the type of device we want to use.
                if (detector == null || detector2 == null  ) {
                    return;
                }
            }
            catch(IOException e) {
                e.printStackTrace();
                LOGGER.e(e, "Exception in updateActiveModel()");
                Toast toast =
                        Toast.makeText(
                                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }

            if (device.equals("CPU")) {
                detector.useCPU();
                detector2.useCPU();



            } else if (device.equals("GPU")) {
                detector.useGpu();
                detector2.useGpu();


            } else if (device.equals("NNAPI")) {
                detector.useNNAPI();
                detector2.useNNAPI();



            }
            detector.setNumThreads(numThreads);
            detector2.setNumThreads(numThreads);

            int cropSize = detector.getInputSize();
            croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

            frameToCropTransform =
                    ImageUtils.getTransformationMatrix(
                            previewWidth, previewHeight,
                            cropSize, cropSize,
                            sensorOrientation, MAINTAIN_ASPECT);

                     cropToFrameTransform = new Matrix();
            frameToCropTransform.invert(cropToFrameTransform);
        });
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;

        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        int imageWidth = rgbFrameBitmap.getWidth();
        int imageHeight = rgbFrameBitmap.getHeight();

         RectF imageRect = new RectF(0, 0, imageWidth, imageHeight);

         float centerX = imageRect.centerX();
        float centerY = imageRect.centerY();

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);


       canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

           runInBackground(new Runnable() {
               @SuppressLint("LongLogTag")
               @Override
               public void run() {
                   LOGGER.i("Running detection on image " + currTimestamp);
                   final long startTime = SystemClock.uptimeMillis();



                   final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                  // detector.close();
                   int imageWidthh = croppedBitmap.getWidth();
                   int imageHeightt = croppedBitmap.getHeight();

                   RectF imageRectt = new RectF(0, 0, imageWidthh, imageWidthh);


                   float centerXc = imageRectt.centerX();
                   float centerYc = imageRectt.centerY();


                   lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                   Log.e("CHECK", "run: " + results.size());

                   cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                   final Canvas canvas = new Canvas(cropCopyBitmap);

                   final Paint paint = new Paint();
                   paint.setColor(Color.RED);
                   paint.setStyle(Style.STROKE);
                   paint.setStrokeWidth(2.0f);

                   float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                   float minimumConfidenceay = MINIMUM_CONFIDENCE_TF_OD_APIAY;

                   final List<ClassifierThree.Recognition> mappedRecognitionsmy =
                           new LinkedList<>();
   //                final List<ClassifierFour.Recognition> mappedRecognitionsmysabah =
   //                        new LinkedList<>();
                   final List<Classifier.Recognition> mappedRecognitions =
                           new LinkedList<Classifier.Recognition>();

                   final List<Float> mappedRecognitionstotal =
                           new LinkedList<>();

                   for (final Classifier.Recognition result : results) {
                       final RectF location = result.getLocation();
                       if (location != null && result.getConfidence() >= minimumConfidence) {
                            detector.close();
                           canvas.drawRect(location, paint);
                           cropToFrameTransform.mapRect(location);
                           result.setLocation(location);

                           mappedRecognitions.add(result);

                           Bitmap copyOfOriginalBitmap = rgbFrameBitmap.copy(rgbFrameBitmap.getConfig(), true);
                           Bitmap secondBitmap = Bitmap.createBitmap(copyOfOriginalBitmap,
                                   (int) location.left, (int) location.top, (int) location.right - (int) location.left, (int) location.bottom - (int) location.top);




                           Bitmap secondScaledBitmap = Bitmap.createScaledBitmap(secondBitmap, 224, 224, true);
                           int imageWidth = secondScaledBitmap.getWidth();
                           int imageHeight = secondScaledBitmap.getHeight();


                           RectF imageRect = new RectF(0, 0, imageWidth, imageHeight);

                            float centerX = imageRect.centerX();
                           float centerY = imageRect.centerY();

                           final String resultLabel = detector1.recognizeImage1(secondScaledBitmap);
                           int imageWidthhh = secondScaledBitmap.getWidth();
                           int imageHeighttt = secondScaledBitmap.getHeight();

                            RectF imageRecttt = new RectF(0, 0, imageWidth, imageHeight);

                            float centerXcc = imageRecttt.centerX();
                           float centerYcc = imageRecttt.centerY();

                           Float confidence = -1f;
                           final Classifier.Recognition result1 = new Classifier.Recognition(
                                   "0", resultLabel, confidence, location);
                           result1.setLocation(location);
                           mappedRecognitions.add(result1);
                           final Canvas canvas1 = new Canvas(secondScaledBitmap);



//                           canvas1.drawBitmap(secondScaledBitmap, frameToCropTransform, null);
   //                          centerx1= (int) location.centerY();
   //                          centery1= (int)location.centerY();
                           // Process with detector2
                           if (secondScaledBitmap != null) {
                               if (resultLabel.equals("eveningTray")) {
                                   int total = 0;
                                   Matrix matrix = new Matrix();
                                   matrix.postRotate(90);
                                   Bitmap rotatedSecondScaledBitmap = Bitmap.createBitmap(secondScaledBitmap, 0, 0, secondScaledBitmap.getWidth(), secondScaledBitmap.getHeight(), matrix, true);

                                   int imageWidthhhh = rotatedSecondScaledBitmap.getWidth();
                                   int imageHeightttt = rotatedSecondScaledBitmap.getHeight();

                                    RectF imageRectttt = new RectF(0, 0, imageWidth, imageHeight);

   /
                                   float centerXccc = imageRectttt.centerX();
                                   float centerYccc = imageRectttt.centerY();
                         //         final ArrayList<ClassifierThree.Recognition> resultss = detector2.recognizeImage2(rotatedSecondScaledBitmap);

                                   final ArrayList<ClassifierThree.Recognition> resultss = detector2.recognizeImage2(croppedBitmap);




                                   ///
                                   for (final ClassifierThree.Recognition result2 : resultss) {


                                       final RectF location2 = result2.getLocation();


                                       if (location2 != null && result2.getConfidence() >= minimumConfidenceay) {

                                           canvas1.drawRect(location2, paint);
                                           canvas1.rotate(90, canvas1.getWidth() / 2, canvas1.getHeight() / 2);
                                          cropToFrameTransform.mapRect(location2);
                                           result2.setLocation(location2);


                                           mappedRecognitionsmy.add(result2);
                                        //   tracker2.trackResults(mappedRecognitionsmy, currTimestamp);




                                           //    Bitmap copyOfOriginalBitmap = rgbFrameBitmap.copy(rgbFrameBitmap.getConfig(), true);
                                           rotatedSecondScaledBitmap = Bitmap.createBitmap(copyOfOriginalBitmap,
                                                   (int) location2.left, (int) location2.top, (int) location2.right - (int) location2.left, (int) location2.bottom - (int) location2.top);
                                           Bitmap yeniBitmap = Bitmap.createScaledBitmap(rotatedSecondScaledBitmap, 224, 224, true);

                                           /////////
                                           if (result2.getDetectedClass() == 0) {
                                               Log.d("CHECK", "result2: soup");

                                               final String resultLabell = detector5.recognizeImageturcorba(yeniBitmap);
                                               final ClassifierThree.Recognition resulttur = new ClassifierThree.Recognition(
                                                       "0", resultLabell, confidence, location2);



                                               resulttur.setLocation(location2);
                                               mappedRecognitionsmy.add(resulttur);
                                               int price;
                                               if (resultLabell.contains("whiteSoup")) {
                                                   price = 10;
                                                   total += price;
                                               }

                                               if (resultLabell.contains("redSoup")) {
                                                   price = 10;
                                                   total += price;

                                               }
   //                                            mappedRecognitionstotal.add(total);
   //                                            tracker2.trackResultst(mappedRecognitionstotal, currTimestamp);
   //
                                           }
                                           if (result2.getDetectedClass() == 1) {
                                                final String resultLabell = detector4.recognizeImagetur(yeniBitmap);
                                               final ClassifierThree.Recognition resulttur = new ClassifierThree.Recognition(
                                                       "0", resultLabell, confidence, location2);

                                               resulttur.setLocation(location2);
                                               mappedRecognitionsmy.add(resulttur);
                                               int price;

                                               if (resultLabell.contains("rice")) {
                                                   price = 20;
                                                   total += price;

                                               }
                                               if (resultLabell.contains("stew")) {
                                                   price = 20;
                                                   total += price;
                                                }
                                               if (resultLabell.contains("beans")) {
                                                   price = 20;
                                                   total += price;

                                               }
                                               if (resultLabell.contains("pasta")) {
                                                   price = 20;
                                                   total += price;

                                               }
                                           }

                                           if (result2.getDetectedClass() == 2) {
                                               Log.d("CHECK", "result2: salad");

                                               int price;

                                               price = 5;
                                               total += price;

                                           }
                                           if (result2.getDetectedClass() == 3) {
                                               Log.d("CHECK", "result2: dessert");

                                               int price;

                                               price = 2;
                                               total += price;

                                           }
                                           if (result2.getDetectedClass() == 4) {

                                               int price;

                                               price = 3;
                                               total += price;

                                           }
                                           if (result2.getDetectedClass() == 5 ) {
                                               Log.d("CHECK", "result2: bread");
                                               int price;
                                               price = 1;
                                               total += price;

                                           }
   ///
                                       }
    //                                            final Classifier.Recognition toplamFiyatResult = new Classifier.Recognition(
   //                                                    "0", "Toplam Fiyat", confidence, location);
   //                                            toplamFiyatResult.setLocation(location);
   //                                            mappedRecognitions.add(toplamFiyatResult);
   //                                            final String resultLabel = detector1.recognizeImage1(secondScaledBitmap);
   //
   //                                            Float confidence = -1f;
   //                                            final Classifier.Recognition result1 = new Classifier.Recognition(
   //                                                    "0", resultLabel, confidence, location);
   //
   //                                            mappedRecognitions.add(toplamFiyat);
                                       String toplamFiyatLabel = "Total:" + toplamFiyat + " $";
                                       float toplamFiyatConfidence = 1.0f;
                                       RectF toplamFiyatLocation = new RectF(0, 0, 20, 100);
                                       float offsetX = 550.0f;
   // Etiketin konumunu sola ka
                                       toplamFiyatLocation.offset(200, offsetX);
                                       final Classifier.Recognition toplamFiyatResult = new Classifier.Recognition(
                                               "0", toplamFiyatLabel, toplamFiyatConfidence, toplamFiyatLocation);
                                       toplamFiyatResult.setLocation(toplamFiyatLocation);

                                       mappedRecognitions.add(toplamFiyatResult);


   //

                                   }
                               }
                           }

                       }

                       }
                   tracker.trackResults(mappedRecognitions, currTimestamp);
                 //  detector.close();
                   tracker2.trackResults(mappedRecognitionsmy, currTimestamp);

                   trackingOverlay.postInvalidate();
                   computingDetection = false;

                   runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                           showFrameInfo(previewWidth + "x" + previewHeight);
                           showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                           showInference(lastProcessingTimeMs + "ms");
                       }
                   });
               }
           });
       }






    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }
}