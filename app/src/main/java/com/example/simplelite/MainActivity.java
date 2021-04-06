package com.example.simplelite;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;         // 핵심 모듈
import android.graphics.Bitmap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
//import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SuperResolution";
    private static final String MODEL_NAME = "v2.1_300.tflite";
    private static final int LR_IMAGE_HEIGHT = 300;
    private static final int LR_IMAGE_WIDTH = 300;
    private static final int UPSCALE_FACTOR = 4;
    private static final int SR_IMAGE_HEIGHT = LR_IMAGE_HEIGHT * UPSCALE_FACTOR;
    private static final int SR_IMAGE_WIDTH = LR_IMAGE_WIDTH * UPSCALE_FACTOR;
    private static final String LR_IMG_1 = "500.jpg";
    public Bitmap bitmap1;
    public Bitmap outBitmap;
    public Bitmap resized ;
    public int[] lowResRGB = new int[LR_IMAGE_HEIGHT * LR_IMAGE_WIDTH];
    private  int[]  intOutValues = new int[SR_IMAGE_HEIGHT * SR_IMAGE_WIDTH];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AssetManager assetManager = getAssets();
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



        // xml 파일에 정의된 TextView 객체 얻기
        final TextView tv_output = findViewById(R.id.tv_output);
        final ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(resized);

        // R.id.button_1 : 첫 번째 버튼을 가리키는 id
        // setOnClickListener : 버튼이 눌렸을 때 호출될 함수 설정
        findViewById(R.id.button_1).setOnClickListener(new View.OnClickListener() {
            // 리스너의 기능 중에서 클릭(single touch) 사용
            @Override
            public void onClick(View v) {
                float[][][][] input = new float[1][LR_IMAGE_HEIGHT][LR_IMAGE_WIDTH][3];
                int j = 0;
                int k = 0;
                for (int i = 0; i < lowResRGB.length; ++i)
                {   j = i%LR_IMAGE_WIDTH;
                    k = i/LR_IMAGE_WIDTH;
                    final int val = lowResRGB[i];
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
                float[][][][] output = new float[1][SR_IMAGE_HEIGHT][SR_IMAGE_WIDTH][3];    // 15 = 3 * 5, out = x * 5

                //output = input;

                // 1번 모델을 해석할 인터프리터 생성
                //Interpreter tflite = getTfliteInterpreter(MODEL_NAME);

                try (Interpreter interpreter = new Interpreter(loadModelFile(MainActivity.this, MODEL_NAME))) {
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
//            intOutValues[i] = 0xFF000000
//                            | (((int) (outputs[i * 3] * 255)) << 16)
//                            | (((int) (outputs[i * 3 + 1] * 255)) << 8)
//                            | ((int) (outputs[i * 3 + 2] * 255));
                    intOutValues[i] = 0xFF000000
                            | (((int) (output[0][k][j][0])) << 16)
                            | (((int) (output[0][k][j][1])) << 8)
                            | ((int) (output[0][k][j][2]));
//            System.out.println( "r:" + String.valueOf(outputs[i * 3]) +";  g:" +String.valueOf((outputs[i * 3 + 1])
//                    +";  b:" + String.valueOf(outputs[i * 3 + 2])));
                    //Log.d(TAG, "onClick: value" + output[0][k][j][0] + output[0][k][j][1] + output[0][k][j][2]);
                }


                outBitmap = Bitmap.createBitmap(intOutValues,0,SR_IMAGE_WIDTH,SR_IMAGE_WIDTH,SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);

                // 출력을 배열에 저장하기 때문에 0번째 요소를 가져와서 문자열로 변환
                tv_output.setText(String.valueOf(output[0]));
                imageView.setImageBitmap(outBitmap);
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