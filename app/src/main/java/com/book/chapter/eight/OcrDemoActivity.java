package com.book.chapter.eight;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import gloomyfish.opencvdemo.ImageSelectUtils;
import gloomyfish.opencvdemo.R;

public class OcrDemoActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String DEFAULT_LANGUAGE = "eng";
    private String TAG = "OcrDemoActivity";
    private int REQUEST_CAPTURE_IMAGE = 1;
    private int option;
    private Uri fileUri;
    private TessBaseAPI baseApi;
    private File tempFile;
    private ImageView ivPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_demo);
        Button selectBtn = (Button) this.findViewById(R.id.select_image_btn);
        Button ocrRecogBtn = (Button) this.findViewById(R.id.ocr_recognize_btn);
        ivPhoto = findViewById(R.id.chapter8_imageView);
        selectBtn.setOnClickListener(this);
        ocrRecogBtn.setOnClickListener(this);
        option = getIntent().getIntExtra("TYPE", 0);
        tempFile = new File(getExternalFilesDir("img"), System.currentTimeMillis() + ".jpg");

        try {
            if (option == 2) {
                initNumberTessBaseAPI();
                this.setTitle("身份证号码识别演示");
                ivPhoto.setImageResource(R.drawable.mockid);
            } else if (option == 3) {
                this.setTitle("偏斜校正演示");
                ocrRecogBtn.setText("校正");
                ivPhoto.setImageResource(R.drawable.jiaozheng);
            } else {
                initTextTessBaseAPI();
                this.setTitle("Tesseract OCR文本识别演示");
                ivPhoto.setImageResource(R.drawable.sample_text);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.select_image_btn:
                pickUpImage();
                break;
            case R.id.ocr_recognize_btn:
                if (option == 2) {
                    recognizeCardId();
                } else if (option == 3) {
                    deSkewTextImage();
                } else {
                    recognizeTextImage();
                }
                break;
            default:
                break;
        }
    }

    private void deSkewTextImage() {
        Mat src = new Mat();
        src = initMat(src);
        if (src.empty()) {
            return;
        }
        Mat dst = new Mat();
        CardNumberROIFinder.deSkewText(src, dst);

        // 转换为Bitmap，显示
        Bitmap bm = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, bm);

        // show
        ivPhoto.setImageBitmap(bm);

        // 释放内存
        dst.release();
        src.release();
    }

    private Mat initMat(Mat src) {
        if (fileUri == null) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.jiaozheng);
            Utils.bitmapToMat(bitmap, src);
        } else {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(fileUri.getPath(), options);
            int w = options.outWidth;
            int h = options.outHeight;
            int inSample = 1;
            if (w > 2000 || h > 2000) {
                while (Math.max(w / inSample, h / inSample) > 1000) {
                    inSample *= 2;
                }
            }
            options.inJustDecodeBounds = false;
            options.inSampleSize = inSample;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(), options);
//            src = Imgcodecs.imread(fileUri.getPath());
            Utils.bitmapToMat(bitmap, src);
        }
        return src;
    }


    private void initTextTessBaseAPI() throws IOException {
        baseApi = new TessBaseAPI();
        String datapath = Environment.getExternalStorageDirectory() + "/tesseract/";
        File dir = new File(datapath + "tessdata/");
        dir.mkdirs();
        InputStream input = getResources().openRawResource(R.raw.eng);
        File file = new File(dir, "eng.traineddata");
        FileOutputStream output = new FileOutputStream(file);
        byte[] buff = new byte[1024];
        int len = 0;
        while ((len = input.read(buff)) != -1) {
            output.write(buff, 0, len);
        }
        input.close();
        output.close();
        boolean success = baseApi.init(datapath, "eng");
        if (success) {
            Log.i(TAG, "load Tesseract OCR Engine successfully...");
        } else {
            Log.i(TAG, "WARNING:could not initialize Tesseract data...");
        }
    }

    private void initNumberTessBaseAPI() throws IOException {
        baseApi = new TessBaseAPI();
        String datapath = Environment.getExternalStorageDirectory() + "/tesseract/";
        File dir = new File(datapath + "tessdata/");
        dir.mkdirs();
        InputStream input = getResources().openRawResource(R.raw.nums);
        File file = new File(dir, "nums.traineddata");
        FileOutputStream output = new FileOutputStream(file);
        byte[] buff = new byte[1024];
        int len = 0;
        while ((len = input.read(buff)) != -1) {
            output.write(buff, 0, len);
        }
        input.close();
        output.close();
        boolean success = baseApi.init(datapath, "nums");
        if (success) {
            Log.i(TAG, "load Tesseract OCR Engine successfully...");
        } else {
            Log.i(TAG, "WARNING:could not initialize Tesseract data...");
        }
    }

    private void recognizeCardId() {
        Bitmap template = BitmapFactory.decodeResource(getResources(), R.drawable.card_template);
        Bitmap cardImage;
        if (fileUri == null) {
            cardImage = BitmapFactory.decodeResource(getResources(), R.drawable.mockid);
        } else {
            cardImage = BitmapFactory.decodeFile(fileUri.getPath());
        }
        Bitmap temp = CardNumberROIFinder.extractNumberROI(cardImage.copy(Bitmap.Config.ARGB_8888, true), template);
        baseApi.setImage(temp);
        String myIdNumber = baseApi.getUTF8Text();
        TextView txtView = findViewById(R.id.text_result_id);
        txtView.setText("身份证号码为:" + myIdNumber);
        ivPhoto.setImageBitmap(temp);

    }

    private void recognizeTextImage() {
        Bitmap bmp;
        if (fileUri == null) {
            bmp = BitmapFactory.decodeResource(getResources(), R.drawable.sample_text);
        } else {
            bmp = BitmapFactory.decodeFile(fileUri.getPath());
        }
        baseApi.setImage(bmp);
        String recognizedText = baseApi.getUTF8Text();
        TextView txtView = findViewById(R.id.text_result_id);
        if (!recognizedText.isEmpty()) {
            txtView.append("识别结果:\n" + recognizedText);
        }
    }

    private void pickUpImage() {
        Intent selectIntent = new Intent(Intent.ACTION_PICK);
        selectIntent.setType("image/*");
        if (selectIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(selectIntent, REQUEST_CAPTURE_IMAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        if (requestCode == REQUEST_CAPTURE_IMAGE) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                File f = new File(ImageSelectUtils.getRealPath(uri, getApplicationContext()));
                fileUri = Uri.fromFile(f);
            } else {
                fileUri = Uri.fromFile(tempFile);
            }
        }

        displaySelectedImage();
    }

    private void displaySelectedImage() {

        if (fileUri == null) return;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileUri.getPath(), options);
        int w = options.outWidth;
        int h = options.outHeight;
        int inSample = 1;
        if (w > 1000 || h > 1000) {
            while (Math.max(w / inSample, h / inSample) > 1000) {
                inSample *= 2;
            }
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath());
        ivPhoto.setImageBitmap(bitmap);
    }
}
