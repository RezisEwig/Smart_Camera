package com.example.cameraexample6;
//참고
//https://www.c-sharpcorner.com/UploadFile/9e8439/how-to-make-a-custom-camera-ion-android/
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.sql.Timestamp;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.ContextThemeWrapper;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import static android.os.Environment.DIRECTORY_PICTURES;
import static android.provider.MediaStore.Images.Media.insertImage;

public class MainActivity extends Activity implements Callback, OnClickListener {

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    //private Button flipCamera;
   // private Button flashCameraButton;
    //private Button captureImage;

    private ImageView take_photo;
    private ImageView flashButton;
    private ImageView cameraChangeButton;


    private int cameraId;
    private boolean flashmode = false;
    private int rotation;
    private int mDeviceRotation;
    private DeviceOrientation deviceOrientation;

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



        // camera surface view created
        cameraId = CameraInfo.CAMERA_FACING_BACK;


        cameraChangeButton = findViewById(R.id.cameraChangeButton);
        flashButton = findViewById(R.id.flashButton);
        take_photo = findViewById(R.id.take_photo);



        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        cameraChangeButton.setOnClickListener(this);
        take_photo.setOnClickListener(this);
        flashButton.setOnClickListener(this);
        deviceOrientation = new DeviceOrientation();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Camera.getNumberOfCameras() > 1) {
            cameraChangeButton.setVisibility(View.VISIBLE);
        }
        if (!getBaseContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH)) {
            flashButton.setVisibility(View.GONE);
        }

        SeekBar ZoomseekBar = findViewById(R.id.zoom);



        ZoomseekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Camera.Parameters p = camera.getParameters();
                int maxZoom = p.getMaxZoom();
                int zoom = p.getZoom();

                if (p.isZoomSupported()) {
                    zoom += 10;
                    if (zoom > maxZoom) {
                        zoom -= 10;
                    }
                    p.setZoom(progress);
                }

                camera.setParameters(p);

                try {
                    camera.setPreviewDisplay(surfaceHolder);
                } catch (Exception e) { }
                camera.startPreview();

            }



            @Override

            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override

            public void onStopTrackingTouch(SeekBar seekBar) { }

        });




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

/*
//수동으로 포커스 조절
        //autofocus 참고 https://argc.tistory.com/244
        ConstraintLayout layoutBackground = findViewById(R.id.background);
        layoutBackground.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                take_photo.setEnabled(false);
                camera.autoFocus(myAutoFocusCallback);
            }});
*/

        }

/*
        Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                take_photo.setEnabled(true);
            }};

*/




    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!openCamera(CameraInfo.CAMERA_FACING_BACK)) {
            alertCameraDialog();
        }

        //set camera to continually auto-focus
        //오토포커스 참고 http://edu.popcornware.net/pop%EC%95%88%EB%93%9C%EB%A1%9C%EC%9D%B4%EB%93%9C-%EC%B9%B4%EB%A9%94%EB%9D%BC-%EC%98%A4%ED%86%A0%ED%8F%AC%EC%BB%A4%EC%8A%A4-%EC%84%A4%EC%A0%95/
        Camera.Parameters params = camera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(params);

        try {
            camera.setPreviewDisplay(holder);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        Log.e("G","001 surfaceCreated onCreadCamera");



    }

    private boolean openCamera(int id) {
        boolean result = false;
        cameraId = id;
        releaseCamera();
        try {
            camera = Camera.open(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (camera != null) {
            try {
                setUpCamera(camera);
                camera.setErrorCallback(new ErrorCallback() {

                    @Override
                    public void onError(int error, Camera camera) {

                    }
                });
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                result = true;
            } catch (IOException e) {
                e.printStackTrace();
                result = false;
                releaseCamera();
            }
        }
        return result;
    }

    private void setUpCamera(Camera c) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;

            default:
                break;
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            // frontFacing
            rotation = (info.orientation + degree) % 330;
            rotation = (360 - rotation) % 360;
        } else {
            // Back-facing
            rotation = (info.orientation - degree + 360) % 360;
        }
        c.setDisplayOrientation(rotation);
        Camera.Parameters params = c.getParameters();

        showFlashButton(params);

        List<String> focusModes = params.getSupportedFlashModes();
        if (focusModes != null) {
            if (focusModes
                    .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFlashMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
        }

        params.setRotation(rotation);
    }

    private void showFlashButton(Parameters params) {
        boolean showFlash = (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH) && params.getFlashMode() != null)
                && params.getSupportedFlashModes() != null
                && params.getSupportedFocusModes().size() > 1;
        flashButton.setVisibility(showFlash ? View.VISIBLE
                : View.INVISIBLE);

    }

    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.setErrorCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("error", e.toString());
            camera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.flashButton:
                flashOnButton();
                break;
            case R.id.cameraChangeButton:
                flipCamera();
                break;
            case R.id.take_photo:
                takeImage();
                break;

            default:
                break;
        }

    }


    private void takeImage() {
        camera.takePicture(null, null, new PictureCallback() {

            private File imageFile;


            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {
                    // convert byte array into bitmap
                    Bitmap loadedImage = null;
                    Bitmap rotatedBitmap = null;
                    loadedImage = BitmapFactory.decodeByteArray(data, 0,
                            data.length);


                    // rotate Image
                    Matrix rotateMatrix = new Matrix();
                    rotateMatrix.postRotate(rotation);
                    rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0,
                            loadedImage.getWidth(), loadedImage.getHeight(),
                            rotateMatrix, false);
                    String state = Environment.getExternalStorageState();
                    File folder = null;
                    if (state.contains(Environment.MEDIA_MOUNTED)) {
                        folder = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES)+"/testtest");

                    } else {
                        folder = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES) + "/testtest");

                    }

                    boolean success = true;
                    if (!folder.exists()) {
                        success = folder.mkdirs();
                    }

                    if (success) {
                        java.util.Date date = new java.util.Date();
                        imageFile = new File(folder.getAbsolutePath()
                                + File.separator
                                + new Timestamp(date.getTime()).toString()
                                + "Image.jpg");


                        mDeviceRotation = ORIENTATIONS.get(deviceOrientation.getOrientation());//넣어보기
                        Log.d("@@@", mDeviceRotation+"");//넣어보기
                        new SaveImageTask().execute(loadedImage); //넣어보기
                        //imageFile.createNewFile();

                    } else {
                        Toast.makeText(getBaseContext(), "Image Not saved",
                                Toast.LENGTH_SHORT).show();
;
                        return;
                    }

                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

                    // save image into gallery
                    rotatedBitmap.compress(CompressFormat.JPEG, 100, ostream);

                    FileOutputStream fout = new FileOutputStream(imageFile);
                    fout.write(ostream.toByteArray());
                    fout.close();
                    ContentValues values = new ContentValues();

                    values.put(Images.Media.DATE_TAKEN,
                            System.currentTimeMillis());
                    values.put(Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.MediaColumns.DATA,
                            imageFile.getAbsolutePath());

                    MainActivity.this.getContentResolver().insert(
                            Images.Media.EXTERNAL_CONTENT_URI, values);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                camera.startPreview();

            }
        });
    }

    private void flipCamera() {
        int id = (cameraId == CameraInfo.CAMERA_FACING_BACK ? CameraInfo.CAMERA_FACING_FRONT
                : CameraInfo.CAMERA_FACING_BACK);
        if (!openCamera(id)) {
            alertCameraDialog();
        }
    }

    private void alertCameraDialog() {
        AlertDialog.Builder dialog = createAlert(MainActivity.this,
                "Camera info", "error to open camera");
        dialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        });

        dialog.show();
    }

    private Builder createAlert(Context context, String title, String message) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(
                new ContextThemeWrapper(context,
                        android.R.style.Theme_Holo_Light_Dialog));
        dialog.setIcon(R.drawable.ic_launcher_background);
        if (title != null)
            dialog.setTitle(title);
        else
            dialog.setTitle("Information");
        dialog.setMessage(message);
        dialog.setCancelable(false);
        return dialog;

    }

    int flashInt=0; //flash 버튼 이미지를 바꾸기 위한 변수
    private void flashOnButton() {
        if (camera != null) {
            try {
                Parameters param = camera.getParameters();
                param.setFlashMode(!flashmode ? Parameters.FLASH_MODE_TORCH
                        : Parameters.FLASH_MODE_OFF);
                camera.setParameters(param);
                flashmode = !flashmode;
                flashInt = 1 - flashInt;
                if(flashInt == 1){ //flash를 켰을때
                    flashButton.setImageResource(R.drawable.image_flash_on);
                }
                else{//flash를 껐을때
                    flashButton.setImageResource(R.drawable.image_flash_off);
                }


            } catch (Exception e) {
                // TODO: handle exception
            }

        }
    }

    private class SaveImageTask extends AsyncTask<Bitmap, Void, Void> {

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Toast.makeText(com.example.cameraexample6.MainActivity.this, "사진을 저장하였습니다.", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Bitmap... data) {

            Bitmap bitmap = null;
            try {

                bitmap = getRotatedBitmap(data[0], mDeviceRotation); //넣어보기
            } catch (Exception e) {
                e.printStackTrace();
            }

            AI ai = new AI(MainActivity.this, bitmap, mDeviceRotation);  // AI 객체 생성

            if (SR) {
                bitmap = ai.Super_Resolution();             // 초해상도 작업 진행
            } else if (LL) {
                bitmap = ai.Low_Light();                    // 저조도 작업 진행
            }

            insertImage(getContentResolver(), bitmap, ""+System.currentTimeMillis(), "");


            return null;
        }

    }

    public Bitmap getRotatedBitmap(Bitmap bitmap, int degrees) throws Exception {
        if(bitmap == null) return null;
        if (degrees == 0) return bitmap;

        Matrix m = new Matrix();
        m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }


}