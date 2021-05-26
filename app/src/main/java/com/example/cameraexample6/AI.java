package com.example.cameraexample6;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import static androidx.core.math.MathUtils.clamp;
import static java.lang.Math.round;

public class AI {

    int mDeviceRotation;    // 0도, 90도, 180도, 270도 핸드폰 회전
    Bitmap bitmap;          // 처리할 이미지
    Activity activity;      // MainActivity
    boolean horizontal;     // 가로일때 True, 세로일때 False

    public AI(Activity activity, Bitmap bitmap, int mDeviceRotation){       // 생성자
        this.activity = activity;
        this.bitmap = bitmap;
        this.mDeviceRotation = mDeviceRotation;
        if (mDeviceRotation == 0 || mDeviceRotation == 180) {           // 핸드폰이 가로일때
            this.horizontal = true;
        } else {                                                        // 핸드폰이 세로일때
            this.horizontal = false;
        }
    }



    public Bitmap Super_Resolution() {                  // 초해상도 작업
        String MODEL_NAME;
        int LR_IMAGE_HEIGHT;
        int LR_IMAGE_WIDTH;
        int UPSCALE_FACTOR;
        int SR_IMAGE_HEIGHT;
        int SR_IMAGE_WIDTH;
        int[] lowResRGB;
        int[] intOutValues;

        if (horizontal){
            MODEL_NAME = "SR_horizontal.tflite";
            LR_IMAGE_HEIGHT = 480;
            LR_IMAGE_WIDTH = 640;
            UPSCALE_FACTOR = 4;
            SR_IMAGE_HEIGHT = LR_IMAGE_HEIGHT * UPSCALE_FACTOR;
            SR_IMAGE_WIDTH = LR_IMAGE_WIDTH * UPSCALE_FACTOR;
            lowResRGB = new int[LR_IMAGE_HEIGHT * LR_IMAGE_WIDTH];
            intOutValues = new int[SR_IMAGE_HEIGHT * SR_IMAGE_WIDTH];

        } else {
            MODEL_NAME = "SR_vertical.tflite";
            LR_IMAGE_HEIGHT = 640;
            LR_IMAGE_WIDTH = 480;
            UPSCALE_FACTOR = 4;
            SR_IMAGE_HEIGHT = LR_IMAGE_HEIGHT * UPSCALE_FACTOR;
            SR_IMAGE_WIDTH = LR_IMAGE_WIDTH * UPSCALE_FACTOR;
            lowResRGB = new int[LR_IMAGE_HEIGHT * LR_IMAGE_WIDTH];
            intOutValues = new int[SR_IMAGE_HEIGHT * SR_IMAGE_WIDTH];
        }

        Interpreter.Options options = new Interpreter.Options();
        CompatibilityList compatList = new CompatibilityList();

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, LR_IMAGE_WIDTH, LR_IMAGE_HEIGHT, true);      // 300x300으로 Resize
        resized.getPixels(                                                                                  // bitmap 객체 에서 1차원 배열으로
                lowResRGB, 0, LR_IMAGE_WIDTH, 0, 0, LR_IMAGE_WIDTH, LR_IMAGE_HEIGHT);

        if(compatList.isDelegateSupportedOnThisDevice()){                                       // GPU 사용 가능한지 확인
            // if the device has a supported GPU, add the GPU delegate
            GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
            GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
            options.addDelegate(gpuDelegate);
        } else {
            // if the GPU is not supported, run on 4 threads
            options.setNumThreads(4);                                       // GPU 사용 불가시 CPU 4쓰레드로 동작
        }

        float[][][][] input = new float[1][LR_IMAGE_HEIGHT][LR_IMAGE_WIDTH][3];     // input 4차원 배열 생성
        int j = 0;
        int k = 0;
        for (int i = 0; i < lowResRGB.length; ++i)              // 1차원 배열을 4차원 배열으로 변경
        {   j = i%LR_IMAGE_WIDTH;                       // 가로
            k = i/LR_IMAGE_WIDTH;                       // 세로
            final int val = lowResRGB[i];

            input[0][k][j][0] = ((val >> 16) & 0xFF) ;      //RED
            input[0][k][j][1] = (((val >> 8) & 0xFF)) ;     //GREEN
            input[0][k][j][2] = (val & 0xFF) ;              //BLUE
        }

        float[][][][] output = new float[1][SR_IMAGE_HEIGHT][SR_IMAGE_WIDTH][3];       // output 4차원 배열 생성

        try (Interpreter interpreter = new Interpreter(loadModelFile(activity, MODEL_NAME), options)) { // TFLITE 파일 불러오기
            interpreter.run(input, output);                     // 실제 인공지능 연산
        }catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < intOutValues.length; ++i)       // 4차원 배열을 1차원 배열으로 변경
        {
            j = i%SR_IMAGE_WIDTH;                   // 가로
            k = i/SR_IMAGE_WIDTH;                   // 세로

            output[0][k][j][0] = clamp(output[0][k][j][0], (float)0.0, (float)255.0);       // 값 0~255 사이로 맞추기
            output[0][k][j][1] = clamp(output[0][k][j][1], (float)0.0, (float)255.0);
            output[0][k][j][2] = clamp(output[0][k][j][2], (float)0.0, (float)255.0);

            intOutValues[i] = 0xFF000000
                    | ((round(output[0][k][j][0])) << 16)       //RED
                    | ((round(output[0][k][j][1])) << 8)        //GREEN
                    | ((round(output[0][k][j][2])));            //BLUE
        }

        bitmap = Bitmap.createBitmap(intOutValues,0,SR_IMAGE_WIDTH,SR_IMAGE_WIDTH,SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);    // 1차원 배열을 Bitmap 객체로 변경

        return bitmap;

    }

    public Bitmap Low_Light() {                     // 저조도 향상 작업
        String MODEL_NAME;
        String MODEL_NAME2;
        int LL_IMAGE_HEIGHT;
        int LL_IMAGE_WIDTH;
        int UPSCALE_FACTOR;
        int SR_IMAGE_HEIGHT;
        int SR_IMAGE_WIDTH;
        int[] lowLightRGB;
        int[] intOutValues;

        if (horizontal){
            MODEL_NAME = "LL_horizontal.tflite";
            MODEL_NAME2 = "SR_horizontal.tflite";
            LL_IMAGE_HEIGHT = 480;
            LL_IMAGE_WIDTH = 640;
            UPSCALE_FACTOR = 4;
            SR_IMAGE_HEIGHT = LL_IMAGE_HEIGHT * UPSCALE_FACTOR;
            SR_IMAGE_WIDTH = LL_IMAGE_WIDTH * UPSCALE_FACTOR;
            lowLightRGB = new int[LL_IMAGE_HEIGHT * LL_IMAGE_WIDTH];
            intOutValues = new int[SR_IMAGE_HEIGHT * SR_IMAGE_WIDTH];

        } else {
            MODEL_NAME = "LL_vertical.tflite";
            MODEL_NAME2 = "SR_vertical.tflite";
            LL_IMAGE_HEIGHT = 640;
            LL_IMAGE_WIDTH = 480;
            UPSCALE_FACTOR = 4;
            SR_IMAGE_HEIGHT = LL_IMAGE_HEIGHT * UPSCALE_FACTOR;
            SR_IMAGE_WIDTH = LL_IMAGE_WIDTH * UPSCALE_FACTOR;
            lowLightRGB = new int[LL_IMAGE_HEIGHT * LL_IMAGE_WIDTH];
            intOutValues = new int[SR_IMAGE_HEIGHT * SR_IMAGE_WIDTH];
        }

        Interpreter.Options options = new Interpreter.Options();
        CompatibilityList compatList = new CompatibilityList();

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, LL_IMAGE_WIDTH, LL_IMAGE_HEIGHT, true);      // 300x300으로 Resize
        resized.getPixels(                                                                                  // bitmap 객체 에서 1차원 배열으로
                lowLightRGB, 0, LL_IMAGE_WIDTH, 0, 0, LL_IMAGE_WIDTH, LL_IMAGE_HEIGHT);

        if(compatList.isDelegateSupportedOnThisDevice()){                       // GPU 사용 가능한지 확인
            // if the device has a supported GPU, add the GPU delegate
            GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
            GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
            options.addDelegate(gpuDelegate);
        } else {
            // if the GPU is not supported, run on 4 threads
            options.setNumThreads(4);                                           // GPU 사용 불가시 CPU 4쓰레드로 동작
        }

        float[][][][] input = new float[1][LL_IMAGE_HEIGHT][LL_IMAGE_WIDTH][3];             // input 4차원 배열 생성
        int j = 0;
        int k = 0;
        for (int i = 0; i < lowLightRGB.length; ++i)            // 1차원 배열을 4차원 배열으로 변경
        {   j = i%LL_IMAGE_WIDTH;                       // 가로
            k = i/LL_IMAGE_WIDTH;                       // 세로
            final int val = lowLightRGB[i];
            input[0][k][j][0] = ((val >> 16) & 0xFF)/255.0f ;       //RED       // 값 0~1 사이로 맞추기
            input[0][k][j][1] = (((val >> 8) & 0xFF))/255.0f ;      //GREEN
            input[0][k][j][2] = (val & 0xFF)/255.0f ;               //BLUE
        }

        float[][][][] output = new float[1][LL_IMAGE_HEIGHT][LL_IMAGE_WIDTH][3];        // 저조도 output 4차원 배열 생성
        float[][][][] output2 = new float[1][SR_IMAGE_HEIGHT][SR_IMAGE_WIDTH][3];       // 초해상도 output 4차원 배열 생성

        try (Interpreter interpreter = new Interpreter(loadModelFile(activity, MODEL_NAME), options)) {     // 저조도 TFLITE 파일 불러오기
            interpreter.run(input, output);                         // 실제 인공지능 연산
        }catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < LL_IMAGE_WIDTH*LL_IMAGE_HEIGHT; ++i) {      // 결과물 후처리
            j = i % LL_IMAGE_WIDTH;
            k = i / LL_IMAGE_WIDTH;

            output[0][k][j][0] = clamp(output[0][k][j][0] * 255, (float) 0.0, (float) 255.0);   // 값 0~255 사이로 맞추기
            output[0][k][j][1] = clamp(output[0][k][j][1] * 255, (float) 0.0, (float) 255.0);
            output[0][k][j][2] = clamp(output[0][k][j][2] * 255, (float) 0.0, (float) 255.0);
        }

        try (Interpreter interpreter = new Interpreter(loadModelFile(activity, MODEL_NAME2), options)) {    // 초해상도 TFLITE 파일 불러오기
            interpreter.run(output, output2);                          // 실제 인공지능 연산
        }catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < intOutValues.length; ++i)       // 4차원 배열을 1차원 배열으로 변경
        {
            j = i%SR_IMAGE_WIDTH;                   // 가로
            k = i/SR_IMAGE_WIDTH;                   // 세로

            output2[0][k][j][0] = clamp(output2[0][k][j][0], (float)0.0, (float)255.0);     // 값 0~255 사이로 맞추기
            output2[0][k][j][1] = clamp(output2[0][k][j][1], (float)0.0, (float)255.0);
            output2[0][k][j][2] = clamp(output2[0][k][j][2], (float)0.0, (float)255.0);

            intOutValues[i] = 0xFF000000
                    | ((round(output2[0][k][j][0])) << 16)      //RED
                    | ((round(output2[0][k][j][1])) << 8)       //GREEN
                    | ((round(output2[0][k][j][2])));           //BLUE
        }

        Bitmap bicubic = Bitmap.createScaledBitmap(bitmap, SR_IMAGE_WIDTH, SR_IMAGE_HEIGHT, true);     // 원래 이미지를 고전적인 방법으로 늘리기
        Bitmap outBitmap = Bitmap.createBitmap(intOutValues,0,SR_IMAGE_WIDTH,SR_IMAGE_WIDTH,SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);      // 인공지능 결과물을 Bitmap으로 변경
        Bitmap overBitmap = Bitmap.createBitmap(SR_IMAGE_WIDTH,SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);       // 최종 결과물이 될 도화지

        Paint paint = new Paint();              // 이 작업을 하는 이유는 저조도 향상 모델이 완전 어두운 사진만 잘 밝혀주고 역광사진은 밝은부분이 너무 밝게 나오기에 그 현상을 완화하기위한 작업
        paint.setAlpha(180);                                    // 투명도 180으로 설정
        Canvas canvas = new Canvas(overBitmap);
        canvas.drawBitmap(outBitmap, 0, 0, paint);      // 인공지능 결과물을 투명도 180으로 색칠
        paint.setAlpha(80);                                     // 투명도 80으로 설정
        canvas.drawBitmap(bicubic, 0, 0, paint);        // 원래 이미지 늘린걸 투명도 80으로 색칠
        paint.setAlpha(255);                                    // 투명도 255(완전 불투명)로 설정
        canvas.drawBitmap(overBitmap, 0, 0, paint);     // 이미지 2개 합친걸 다시 덮어쓰기

        return overBitmap;

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
}