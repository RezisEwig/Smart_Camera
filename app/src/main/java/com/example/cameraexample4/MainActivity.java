package com.example.cameraexample4;
/*
 원본 코드
 https://github.com/SkyeBeFreeman/SkyeCamera
 https://github.com/googlearchive/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java

 수정
 webnautes
 */


import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import static androidx.core.math.MathUtils.clamp;
import static java.lang.Math.round;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity{

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceViewHolder;
    private Handler mHandler;
    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mSession;
    private int mDeviceRotation;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private SensorManager mSensorManager;
    private DeviceOrientation deviceOrientation;
    int mDSI_height, mDSI_width;
    private String imageFilePath;

    Boolean SR = false;
    Boolean LL = false;


    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(ExifInterface.ORIENTATION_NORMAL, 0);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_90, 90);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_180, 180);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_270, 270);
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 상태바를 안보이도록 합니다.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 화면 켜진 상태를 유지합니다.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);


        ImageButton cameraButton = findViewById(R.id.take_photo);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });

        mSurfaceView = findViewById(R.id.surfaceView);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        deviceOrientation = new DeviceOrientation();

        initSurfaceView();



        
        //버튼 리스너
        ToggleButton buttonSR = findViewById(R.id.superResolution);
        ToggleButton buttonLL = findViewById(R.id.lowLightEnhancement);



        buttonSR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {//ON
                    //buttonLL.setEnabled(false);//LL비활성화
                    //buttonLL.setClickable(false);//LL클릭 불가능
                    if (buttonLL.isChecked()){
                        buttonLL.setChecked(false);
                        buttonLL.setBackgroundDrawable(
                                getResources().getDrawable(R.drawable.image_light_off)
                        );
                        LL = false;
                    }
                    SR = true;
                    Toast.makeText(MainActivity.this, "SR클릭-ON", Toast.LENGTH_SHORT).show();
                    //이미지를 교체
                    buttonSR.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.image_super_on)
                    );
                }
                else{//OFF
                    //buttonLL.setEnabled(true);//LL활성화
                    //buttonLL.setClickable(true);//LL클릭 가능
                    SR = false;
                    Toast.makeText(MainActivity.this, "SR클릭-OFF", Toast.LENGTH_SHORT).show();
                    buttonSR.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.image_super_off)
                    );
                }

            }
        });

        buttonLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(buttonLL.isChecked()){
                    //buttonSR.setEnabled(false); //SR비활성화
                    //buttonSR.setClickable(false); //SR클릭 불가능
                    if (buttonSR.isChecked()){
                        buttonSR.setChecked(false);
                        buttonSR.setBackgroundDrawable(
                                getResources().getDrawable(R.drawable.image_super_off)
                        );
                        SR = false;
                    }
                    LL = true;
                    Toast.makeText(MainActivity.this, "LL클릭-ON", Toast.LENGTH_SHORT).show();
                    buttonLL.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.image_light_on)
                    );
                }
                else{
                    //buttonSR.setEnabled(true);//SR활성화
                    //buttonSR.setClickable(true);//SR클릭 가능
                    LL = false;
                    Toast.makeText(MainActivity.this, "LL클릭-OFF", Toast.LENGTH_SHORT).show();
                    buttonLL.setBackgroundDrawable(
                        getResources().getDrawable(R.drawable.image_light_off)
                    );
                }
            }
        });





        


    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(deviceOrientation.getEventListener(), mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(deviceOrientation.getEventListener(), mMagnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(deviceOrientation.getEventListener());
    }

    public void initSurfaceView() {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mDSI_height = displayMetrics.heightPixels;
        mDSI_width = displayMetrics.widthPixels;


        mSurfaceViewHolder = mSurfaceView.getHolder();
        mSurfaceViewHolder.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCameraAndPreview();
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)//알트탭했음
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

                if (mCameraDevice != null) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }


        });
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void initCameraAndPreview() {
        HandlerThread handlerThread = new HandlerThread("CAMERA2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        Handler mainHandler = new Handler(getMainLooper());
        ImageButton changeButton = findViewById(R.id.btn_cameraChange);

        try {




            String mCameraId = "" + CameraCharacteristics.LENS_FACING_FRONT; // 후면 카메라 사용
            //String mCameraId = "" + CameraCharacteristics.LENS_FACING_BACK; // 전면 카메라 사용

            CameraManager mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size largestPreviewSize = map.getOutputSizes(ImageFormat.JPEG)[0];
            Log.i("LargestSize", largestPreviewSize.getWidth() + " " + largestPreviewSize.getHeight());

            setAspectRatioTextureView(largestPreviewSize.getHeight(),largestPreviewSize.getWidth());

            mImageReader = ImageReader.newInstance(largestPreviewSize.getWidth(), largestPreviewSize.getHeight(), ImageFormat.JPEG,/*maxImages*/7);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mainHandler);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mCameraManager.openCamera(mCameraId, deviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Toast.makeText(this, "카메라를 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }


    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {

            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            new SaveImageTask().execute(bitmap);


        }
    };


    private CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            try {
                takePreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Toast.makeText(com.example.cameraexample4.MainActivity.this, "카메라를 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    };


    public void takePreview() throws CameraAccessException {
        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewBuilder.addTarget(mSurfaceViewHolder.getSurface());
        mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceViewHolder.getSurface(), mImageReader.getSurface()), mSessionPreviewStateCallback, mHandler);
    }

    private CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mSession = session;

            try {

                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Toast.makeText(com.example.cameraexample4.MainActivity.this, "카메라 구성 실패", Toast.LENGTH_SHORT).show();
        }
    };

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            mSession = session;
            unlockFocus();
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            mSession = session;
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };



    public void takePicture() {

        try {
            CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);//用来设置拍照请求的request
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);


            // 화면 회전 안되게 고정시켜 놓은 상태에서는 아래 로직으로 방향을 얻을 수 없어서
            // 센서를 사용하는 것으로 변경
            //deviceRotation = getResources().getConfiguration().orientation;
            mDeviceRotation = ORIENTATIONS.get(deviceOrientation.getOrientation());
            Log.d("@@@", mDeviceRotation+"");

            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mDeviceRotation);
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            mSession.capture(mCaptureRequest, mSessionCaptureCallback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public Bitmap getRotatedBitmap(Bitmap bitmap, int degrees) throws Exception {
        if(bitmap == null) return null;
        if (degrees == 0) return bitmap;

        Matrix m = new Matrix();
        m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }



    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mSession.capture(mPreviewBuilder.build(), mSessionCaptureCallback,
                    mHandler);
            // After this, the camera will go back to the normal state of preview.
            mSession.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallback,
                    mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    //출처 - https://codeday.me/ko/qa/20190310/39556.html
    /**
     * A copy of the Android internals  insertImage method, this method populates the
     * meta data with DATE_ADDED and DATE_TAKEN. This fixes a common problem where media
     * that is inserted manually gets saved at the end of the gallery (because date is not populated).
     * @see MediaStore.Images.Media#insertImage(ContentResolver, Bitmap, String, String)
     */
    public static final String insertImage(ContentResolver cr,
                                           Bitmap source,
                                           String title,
                                           String description) {

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, title);
        values.put(MediaStore.Images.Media.DESCRIPTION, description);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        // Add the date meta data to ensure the image is added at the front of the gallery
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

        Uri url = null;
        String stringUrl = null;    /* value to be returned */

        try {
            url = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (source != null) {
                OutputStream imageOut = cr.openOutputStream(url);
                try {
                    source.compress(Bitmap.CompressFormat.JPEG, 50, imageOut);
                } finally {
                    imageOut.close();
                }

            } else {
                cr.delete(url, null, null);
                url = null;
            }
        } catch (Exception e) {
            if (url != null) {
                cr.delete(url, null, null);
                url = null;
            }
        }

        if (url != null) {
            stringUrl = url.toString();
        }

        return stringUrl;
    }


    private class SaveImageTask extends AsyncTask<Bitmap, Void, Void> {

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Toast.makeText(com.example.cameraexample4.MainActivity.this, "사진을 저장하였습니다.", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Bitmap... data) {

            Bitmap bitmap = null;
            try {
                bitmap = getRotatedBitmap(data[0], mDeviceRotation);
            } catch (Exception e) {
                e.printStackTrace();
            }

            float ratio = 0.0f;

            if (mDeviceRotation == 0 || mDeviceRotation == 180) {
                ratio = (float) bitmap.getWidth() / (float) bitmap.getHeight();
            } else {
                ratio = (float) bitmap.getHeight() / (float) bitmap.getWidth();
            }

            if (SR) {
                String MODEL_NAME = "v5_300.tflite";
                int LR_IMAGE_HEIGHT = 300;
                int LR_IMAGE_WIDTH = 300;
                int UPSCALE_FACTOR = 4;
                int SR_IMAGE_HEIGHT = LR_IMAGE_HEIGHT * UPSCALE_FACTOR;
                int SR_IMAGE_WIDTH = LR_IMAGE_WIDTH * UPSCALE_FACTOR;
                int[] lowResRGB = new int[LR_IMAGE_HEIGHT * LR_IMAGE_WIDTH];
                int[] intOutValues = new int[SR_IMAGE_HEIGHT * SR_IMAGE_WIDTH];
                Interpreter.Options options = new Interpreter.Options();
                CompatibilityList compatList = new CompatibilityList();

                Bitmap resized = Bitmap.createScaledBitmap(bitmap, LR_IMAGE_WIDTH, LR_IMAGE_HEIGHT, true);
                resized.getPixels(
                        lowResRGB, 0, LR_IMAGE_WIDTH, 0, 0, LR_IMAGE_WIDTH, LR_IMAGE_HEIGHT);

                if(compatList.isDelegateSupportedOnThisDevice()){
                    // if the device has a supported GPU, add the GPU delegate
                    GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
                    GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
                    options.addDelegate(gpuDelegate);
                } else {
                    // if the GPU is not supported, run on 4 threads
                    options.setNumThreads(4);
                }

                float[][][][] input = new float[1][LR_IMAGE_HEIGHT][LR_IMAGE_WIDTH][3];
                int j = 0;
                int k = 0;
                for (int i = 0; i < lowResRGB.length; ++i)
                {   j = i%LR_IMAGE_WIDTH;
                    k = i/LR_IMAGE_WIDTH;
                    final int val = lowResRGB[i];

                    input[0][k][j][0] = ((val >> 16) & 0xFF) ;
                    input[0][k][j][1] = (((val >> 8) & 0xFF)) ;
                    input[0][k][j][2] = (val & 0xFF) ;
                }

                float[][][][] output = new float[1][SR_IMAGE_HEIGHT][SR_IMAGE_WIDTH][3];

                try (Interpreter interpreter = new Interpreter(loadModelFile(MainActivity.this, MODEL_NAME), options)) {
                    interpreter.run(input, output);
                }catch (Exception e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < intOutValues.length; ++i)
                {
                    j = i%SR_IMAGE_WIDTH;
                    k = i/SR_IMAGE_WIDTH;

                    output[0][k][j][0] = clamp(output[0][k][j][0], (float)0.0, (float)255.0);
                    output[0][k][j][1] = clamp(output[0][k][j][1], (float)0.0, (float)255.0);
                    output[0][k][j][2] = clamp(output[0][k][j][2], (float)0.0, (float)255.0);

                    intOutValues[i] = 0xFF000000
                            | ((round(output[0][k][j][0])) << 16)
                            | ((round(output[0][k][j][1])) << 8)
                            | ((round(output[0][k][j][2])));
                }

                bitmap = Bitmap.createBitmap(intOutValues,0,SR_IMAGE_WIDTH,SR_IMAGE_WIDTH,SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
                if (mDeviceRotation == 0 || mDeviceRotation == 180) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(SR_IMAGE_WIDTH*ratio), SR_IMAGE_HEIGHT, true);
                } else {
                    bitmap = Bitmap.createScaledBitmap(bitmap, SR_IMAGE_WIDTH, Math.round(SR_IMAGE_HEIGHT*ratio), true);
                }


            } else if (LL) {
                String MODEL_NAME = "mirnet_int8.tflite";
                String MODEL_NAME2 = "v5_300.tflite";
                int LL_IMAGE_HEIGHT = 300;
                int LL_IMAGE_WIDTH = 300;
                int UPSCALE_FACTOR = 4;
                int SR_IMAGE_HEIGHT = LL_IMAGE_HEIGHT * UPSCALE_FACTOR;
                int SR_IMAGE_WIDTH = LL_IMAGE_WIDTH * UPSCALE_FACTOR;
                int[] lowLightRGB = new int[LL_IMAGE_HEIGHT * LL_IMAGE_WIDTH];
                int[] intOutValues = new int[SR_IMAGE_HEIGHT * SR_IMAGE_WIDTH];
                Interpreter.Options options = new Interpreter.Options();
                CompatibilityList compatList = new CompatibilityList();

                Bitmap resized = Bitmap.createScaledBitmap(bitmap, LL_IMAGE_HEIGHT, LL_IMAGE_WIDTH, true);
                resized.getPixels(
                        lowLightRGB, 0, LL_IMAGE_WIDTH, 0, 0, LL_IMAGE_WIDTH, LL_IMAGE_HEIGHT);

                if(compatList.isDelegateSupportedOnThisDevice()){
                    // if the device has a supported GPU, add the GPU delegate
                    GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
                    GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
                    options.addDelegate(gpuDelegate);
                } else {
                    // if the GPU is not supported, run on 4 threads
                    options.setNumThreads(4);
                }

                float[][][][] input = new float[1][LL_IMAGE_HEIGHT][LL_IMAGE_WIDTH][3];
                int j = 0;
                int k = 0;
                for (int i = 0; i < lowLightRGB.length; ++i)
                {   j = i%LL_IMAGE_WIDTH;
                    k = i/LL_IMAGE_WIDTH;
                    final int val = lowLightRGB[i];
                    input[0][k][j][0] = ((val >> 16) & 0xFF)/255.0f ;
                    input[0][k][j][1] = (((val >> 8) & 0xFF))/255.0f ;
                    input[0][k][j][2] = (val & 0xFF)/255.0f ;
                }

                float[][][][] output = new float[1][LL_IMAGE_HEIGHT][LL_IMAGE_WIDTH][3];
                float[][][][] output2 = new float[1][SR_IMAGE_HEIGHT][SR_IMAGE_WIDTH][3];

                try (Interpreter interpreter = new Interpreter(loadModelFile(MainActivity.this, MODEL_NAME), options)) {
                    interpreter.run(input, output);
                }catch (Exception e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < LL_IMAGE_WIDTH*LL_IMAGE_HEIGHT; ++i) {
                    j = i % LL_IMAGE_WIDTH;
                    k = i / LL_IMAGE_WIDTH;

                    output[0][k][j][0] = clamp(output[0][k][j][0] * 255, (float) 0.0, (float) 255.0);
                    output[0][k][j][1] = clamp(output[0][k][j][1] * 255, (float) 0.0, (float) 255.0);
                    output[0][k][j][2] = clamp(output[0][k][j][2] * 255, (float) 0.0, (float) 255.0);
                }

                try (Interpreter interpreter = new Interpreter(loadModelFile(MainActivity.this, MODEL_NAME2), options)) {
                    interpreter.run(output, output2);
                }catch (Exception e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < intOutValues.length; ++i)
                {
                    j = i%SR_IMAGE_WIDTH;
                    k = i/SR_IMAGE_WIDTH;

                    output2[0][k][j][0] = clamp(output2[0][k][j][0], (float)0.0, (float)255.0);
                    output2[0][k][j][1] = clamp(output2[0][k][j][1], (float)0.0, (float)255.0);
                    output2[0][k][j][2] = clamp(output2[0][k][j][2], (float)0.0, (float)255.0);

                    intOutValues[i] = 0xFF000000
                            | ((round(output2[0][k][j][0])) << 16)
                            | ((round(output2[0][k][j][1])) << 8)
                            | ((round(output2[0][k][j][2])));
                }

                Bitmap bicubic = Bitmap.createScaledBitmap(resized, SR_IMAGE_WIDTH, SR_IMAGE_HEIGHT, true);
                Bitmap outBitmap = Bitmap.createBitmap(intOutValues,0,SR_IMAGE_WIDTH,SR_IMAGE_WIDTH,SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
                Bitmap overBitmap = Bitmap.createBitmap(SR_IMAGE_WIDTH,SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);

                Paint paint = new Paint();
                paint.setAlpha(180);
                Canvas canvas = new Canvas(overBitmap);
                canvas.drawBitmap(outBitmap, 0, 0, paint);
                paint.setAlpha(80);
                canvas.drawBitmap(bicubic, 0, 0, paint);
                paint.setAlpha(255);
                canvas.drawBitmap(overBitmap, 0, 0, paint);
                if (mDeviceRotation == 0 || mDeviceRotation == 180) {
                    bitmap = Bitmap.createScaledBitmap(overBitmap, Math.round(SR_IMAGE_WIDTH*ratio), SR_IMAGE_HEIGHT, true);
                } else {
                    bitmap = Bitmap.createScaledBitmap(overBitmap, SR_IMAGE_WIDTH, Math.round(SR_IMAGE_HEIGHT*ratio), true);
                }

            }

            insertImage(getContentResolver(), bitmap, ""+System.currentTimeMillis(), "");

            return null;
        }

    }

    // 모델을 읽어오는 함수로, 텐서플로 라이트 홈페이지에 있다.
    // MappedByteBuffer 바이트 버퍼를 Interpreter 객체에 전달하면 모델 해석을 할 수 있다.
    private MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    // 출처 https://stackoverflow.com/a/43516672
    private void setAspectRatioTextureView(int ResolutionWidth , int ResolutionHeight )
    {
        if(ResolutionWidth > ResolutionHeight){
            int newWidth = mDSI_width;
            int newHeight = ((mDSI_width * ResolutionWidth)/ResolutionHeight);
            updateTextureViewSize(newWidth,newHeight);

        }else {
            int newWidth = mDSI_width;
            int newHeight = ((mDSI_width * ResolutionHeight)/ResolutionWidth);
            updateTextureViewSize(newWidth,newHeight);
        }

    }

    private void updateTextureViewSize(int viewWidth, int viewHeight) {
        Log.d("@@@", "TextureView Width : " + viewWidth + " TextureView Height : " + viewHeight);
        mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(viewWidth, viewHeight));
    }





}