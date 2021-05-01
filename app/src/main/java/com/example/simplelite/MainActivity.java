package com.example.simplelite;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;         // 핵심 모듈
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import android.graphics.Bitmap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
//import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import static androidx.core.math.MathUtils.clamp;
import static java.lang.Math.round;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SuperResolution";
    private static final String MODEL_NAME = "v5_300.tflite";
    private static final String MODEL_NAME2 = "mirnet_int8.tflite";
    private static final int LR_IMAGE_HEIGHT = 300;
    private static final int LR_IMAGE_WIDTH = 300;
    private static final int UPSCALE_FACTOR = 4;
    private static final int SR_IMAGE_HEIGHT = LR_IMAGE_HEIGHT * UPSCALE_FACTOR;
    private static final int SR_IMAGE_WIDTH = LR_IMAGE_WIDTH * UPSCALE_FACTOR;
    private static final int LL_IMAGE_HEIGHT = 300;
    private static final int LL_IMAGE_WIDTH = 300;
    private static final String LR_IMG_1 = "landscape.jpg";
    private static final String LL_IMG_1 = "low-light.png";
    public Bitmap bitmap1;
    public Bitmap outBitmap;
    public Bitmap resized ;
    public Bitmap bicubic;
    public int[] lowResRGB = new int[LR_IMAGE_HEIGHT * LR_IMAGE_WIDTH];
    public int[] lowLightRGB = new int[LL_IMAGE_HEIGHT * LL_IMAGE_WIDTH];
    public int[]  intOutValues = new int[SR_IMAGE_HEIGHT * SR_IMAGE_WIDTH];

    public Interpreter.Options options = new Interpreter.Options();
    public CompatibilityList compatList = new CompatibilityList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AssetManager assetManager = getAssets();

        // TextView 객체 얻기
        final ImageView imageView = findViewById(R.id.imageView);
        final ImageView imageView2 = findViewById(R.id.imageView2);
        final ImageView imageView3 = findViewById(R.id.imageView3);


        // R.id.button_1 : 첫 번째 버튼을 가리키는 id
        // setOnClickListener : 버튼이 눌렸을 때 호출될 함수 설정
        findViewById(R.id.button_1).setOnClickListener(new View.OnClickListener() {
            // 리스너의 기능 중에서 클릭(single touch) 사용
            @Override
            public void onClick(View v) {
                try {
                    InputStream inputStream1 = assetManager.open(LR_IMG_1);
                    bitmap1 = BitmapFactory.decodeStream(inputStream1);
                    resized = Bitmap.createScaledBitmap(bitmap1, LR_IMAGE_WIDTH, LR_IMAGE_HEIGHT, true);
                    Log.d(TAG, "onCreate:fsdafsadfsafasfdsfas " + resized.getWidth() + resized.getHeight());

                    resized.getPixels(
                            lowResRGB, 0, LR_IMAGE_WIDTH, 0, 0, LR_IMAGE_WIDTH, LR_IMAGE_HEIGHT);
                    Log.d(TAG, "onCreate: asdasf" + lowResRGB.length);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to open an low resolution image");
                }

                imageView.setImageBitmap(resized);

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
                    //Log.d(TAG, String.format("onClick: value : %x", val));
//            inputs[i * 3 + 0] = ((val >> 16) & 0xFF)/255.0f ;
//            inputs[i * 3 + 1] = (((val >> 8) & 0xFF))/255.0f ;
//            inputs[i * 3 + 2] = (val & 0xFF) /255.0f ;
                    //input[i * 3 + 0] = ((val >> 16) & 0xFF) ;
                    //input[i * 3 + 1] = (((val >> 8) & 0xFF)) ;
                    //input[i * 3 + 2] = (val & 0xFF)  ;
                    input[0][k][j][0] = ((val >> 16) & 0xFF) ;
                    input[0][k][j][1] = (((val >> 8) & 0xFF)) ;
                    input[0][k][j][2] = (val & 0xFF) ;

                    //Log.d(TAG, "onClick: value" + input[0][k][j][0] + input[0][k][j][1] + input[0][k][j][2]);

                }



                // input : 텐서플로 모델의 placeholder에 전달할 데이터(3)
                // output: 텐서플로 모델로부터 결과를 넘겨받을 배열. 덮어쓰기 때문에 초기값은 의미없다.
                //int[] input = new int[]{3};
                float[][][][] output = new float[1][SR_IMAGE_HEIGHT][SR_IMAGE_WIDTH][3];

                //output = input;

                // 1번 모델을 해석할 인터프리터 생성
                //Interpreter tflite = getTfliteInterpreter(MODEL_NAME);

                try (Interpreter interpreter = new Interpreter(loadModelFile(MainActivity.this, MODEL_NAME), options)) {
                    interpreter.run(input, output);
                }catch (Exception e) {
                    e.printStackTrace();
                }

                // 모델 구동.
                // 정확하게는 from_session 함수의 output_tensors 매개변수에 전달된 연산 호출
                //tflite.run(input, output);

                Log.d(TAG, "onCreate: asdasf" + output.length);

                for (int i = 0; i < intOutValues.length; ++i)
                {
                    j = i%SR_IMAGE_WIDTH;
                    k = i/SR_IMAGE_WIDTH;

                    output[0][k][j][0] = clamp(output[0][k][j][0], (float)0.0, (float)255.0);
                    output[0][k][j][1] = clamp(output[0][k][j][1], (float)0.0, (float)255.0);
                    output[0][k][j][2] = clamp(output[0][k][j][2], (float)0.0, (float)255.0);

//            intOutValues[i] = 0xFF000000
//                            | (((int) (outputs[i * 3] * 255)) << 16)
//                            | (((int) (outputs[i * 3 + 1] * 255)) << 8)
//                            | ((int) (outputs[i * 3 + 2] * 255));
                    intOutValues[i] = 0xFF000000
                            | ((round(output[0][k][j][0])) << 16)
                            | ((round(output[0][k][j][1])) << 8)
                            | ((round(output[0][k][j][2])));
//            System.out.println( "r:" + String.valueOf(outputs[i * 3]) +";  g:" +String.valueOf((outputs[i * 3 + 1])
//                    +";  b:" + String.valueOf(outputs[i * 3 + 2])));
                    //Log.d(TAG, "onClick: value" + output[0][k][j][0] + output[0][k][j][1] + output[0][k][j][2]);
                }


                outBitmap = Bitmap.createBitmap(intOutValues,0,SR_IMAGE_WIDTH,SR_IMAGE_WIDTH,SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);

                // 출력을 배열에 저장하기 때문에 0번째 요소를 가져와서 문자열로 변환
                bicubic = Bitmap.createScaledBitmap(resized, SR_IMAGE_WIDTH, SR_IMAGE_HEIGHT, true);
                imageView2.setImageBitmap(bicubic);
                imageView3.setImageBitmap(outBitmap);
            }
        });

        findViewById(R.id.button_2).setOnClickListener(new View.OnClickListener() {
            // 리스너의 기능 중에서 클릭(single touch) 사용
            @Override
            public void onClick(View v) {
                try {
                    InputStream inputStream1 = assetManager.open(LL_IMG_1);
                    bitmap1 = BitmapFactory.decodeStream(inputStream1);
                    resized = Bitmap.createScaledBitmap(bitmap1, LL_IMAGE_WIDTH, LL_IMAGE_HEIGHT, true);
                    Log.d(TAG, "onCreate:fsdafsadfsafasfdsfas " + resized.getWidth() + resized.getHeight());

                    resized.getPixels(
                            lowLightRGB, 0, LL_IMAGE_WIDTH, 0, 0, LL_IMAGE_WIDTH, LL_IMAGE_HEIGHT);
                    Log.d(TAG, "onCreate: asdasf" + lowLightRGB.length);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to open an low resolution image");
                }

                imageView.setImageBitmap(resized);

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
                    //Log.d(TAG, String.format("onClick: value : %x", val));
//            inputs[i * 3 + 0] = ((val >> 16) & 0xFF)/255.0f ;
//            inputs[i * 3 + 1] = (((val >> 8) & 0xFF))/255.0f ;
//            inputs[i * 3 + 2] = (val & 0xFF) /255.0f ;
                    //input[i * 3 + 0] = ((val >> 16) & 0xFF) ;
                    //input[i * 3 + 1] = (((val >> 8) & 0xFF)) ;
                    //input[i * 3 + 2] = (val & 0xFF)  ;
                    input[0][k][j][0] = ((val >> 16) & 0xFF)/255.0f ;
                    input[0][k][j][1] = (((val >> 8) & 0xFF))/255.0f ;
                    input[0][k][j][2] = (val & 0xFF)/255.0f ;

                    //Log.d(TAG, "onClick: value" + input[0][k][j][0] + input[0][k][j][1] + input[0][k][j][2]);

                }



                // input : 텐서플로 모델의 placeholder에 전달할 데이터(3)
                // output: 텐서플로 모델로부터 결과를 넘겨받을 배열. 덮어쓰기 때문에 초기값은 의미없다.
                //int[] input = new int[]{3};
                float[][][][] output = new float[1][LL_IMAGE_HEIGHT][LL_IMAGE_WIDTH][3];
                float[][][][] output2 = new float[1][SR_IMAGE_HEIGHT][SR_IMAGE_WIDTH][3];

                //output = input;

                // 1번 모델을 해석할 인터프리터 생성
                //Interpreter tflite = getTfliteInterpreter(MODEL_NAME);

                try (Interpreter interpreter = new Interpreter(loadModelFile(MainActivity.this, MODEL_NAME2), options)) {
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

                try (Interpreter interpreter = new Interpreter(loadModelFile(MainActivity.this, MODEL_NAME), options)) {
                    interpreter.run(output, output2);
                }catch (Exception e) {
                    e.printStackTrace();
                }


                // 모델 구동.
                // 정확하게는 from_session 함수의 output_tensors 매개변수에 전달된 연산 호출
                //tflite.run(input, output);

                Log.d(TAG, "onCreate: asdasf" + output.length);

                for (int i = 0; i < intOutValues.length; ++i)
                {
                    j = i%SR_IMAGE_WIDTH;
                    k = i/SR_IMAGE_WIDTH;

                    output2[0][k][j][0] = clamp(output2[0][k][j][0], (float)0.0, (float)255.0);
                    output2[0][k][j][1] = clamp(output2[0][k][j][1], (float)0.0, (float)255.0);
                    output2[0][k][j][2] = clamp(output2[0][k][j][2], (float)0.0, (float)255.0);

//            intOutValues[i] = 0xFF000000
//                            | (((int) (outputs[i * 3] * 255)) << 16)
//                            | (((int) (outputs[i * 3 + 1] * 255)) << 8)
//                            | ((int) (outputs[i * 3 + 2] * 255));
                    intOutValues[i] = 0xFF000000
                            | ((round(output2[0][k][j][0])) << 16)
                            | ((round(output2[0][k][j][1])) << 8)
                            | ((round(output2[0][k][j][2])));
//            System.out.println( "r:" + String.valueOf(outputs[i * 3]) +";  g:" +String.valueOf((outputs[i * 3 + 1])
//                    +";  b:" + String.valueOf(outputs[i * 3 + 2])));
                    //Log.d(TAG, "onClick: value" + output[0][k][j][0] + output[0][k][j][1] + output[0][k][j][2]);
                }


                outBitmap = Bitmap.createBitmap(intOutValues,0,SR_IMAGE_WIDTH,SR_IMAGE_WIDTH,SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);

                // 출력을 배열에 저장하기 때문에 0번째 요소를 가져와서 문자열로 변환
                bicubic = Bitmap.createScaledBitmap(resized, SR_IMAGE_WIDTH, SR_IMAGE_HEIGHT, true);
                imageView2.setImageBitmap(bicubic);
                Bitmap overBitmap = Bitmap.createBitmap(SR_IMAGE_WIDTH,SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
                Paint paint = new Paint();
                paint.setAlpha(180);
                Canvas canvas = new Canvas(overBitmap);
                canvas.drawBitmap(outBitmap, 0, 0, paint);
                paint.setAlpha(80);
                canvas.drawBitmap(bicubic, 0, 0, paint);
                paint.setAlpha(255);
                canvas.drawBitmap(overBitmap, 0, 0, paint);
                imageView3.setImageBitmap(overBitmap);
            }
        });

    }

    // 모델 파일 인터프리터를 생성하는 공통 함수
    // loadModelFile 함수에 예외가 포함되어 있기 때문에 반드시 try, catch 블록이 필요하다.
    private Interpreter getTfliteInterpreter(String modelPath) {
        try {
            return new Interpreter(loadModelFile(MainActivity.this, modelPath));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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