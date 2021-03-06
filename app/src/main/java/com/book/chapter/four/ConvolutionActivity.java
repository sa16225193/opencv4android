package com.book.chapter.four;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

import gloomyfish.opencvdemo.ImageSelectUtils;
import gloomyfish.opencvdemo.R;

public class ConvolutionActivity extends AppCompatActivity implements View.OnClickListener {
    private int REQUEST_CAPTURE_IMAGE = 1;
    private String TAG = "ConvolutionActivity";
    private Uri fileUri;
    private File tempFile;
    private int option;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convolution);
        Button selectBtn = (Button)this.findViewById(R.id.select_image_btn);
        Button processBtn = (Button)this.findViewById(R.id.convolution_btn);
        selectBtn.setOnClickListener(this);
        processBtn.setOnClickListener(this);
        tempFile = new File(getExternalFilesDir("img"), System.currentTimeMillis() + ".jpg");
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.select_image_btn:
                pickUpImage();
                break;
            case R.id.convolution_btn:
                blurImage(0);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.blur:
                option = 0;
                break;
            case R.id.GaussianBlur:
                option = 1;
                break;
            case R.id.medianBlur:
                option = 2;
                break;
            case R.id.dilate:
                option = 3;
                break;
            case R.id.erode:
                option = 4;
                break;
            case R.id.bilateralFilter:
                option = 5;
                break;
            case R.id.pyrMeanShiftFiltering:
                option = 6;
                break;
            case R.id.customFilter:
                option = 7;
                break;
            case R.id.morphologyDemo:
                option = 8;
                break;
            case R.id.thresholdDemo:
                option = 9;
                break;
            case R.id.adpThresholdDemo:
                option = 10;
                break;
            default:
                option = 0;
                break;
        }
        blurImage(option);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_operation, menu);
        return true;
    }

    private void blurImage(int type) {
        // read image
        Mat src = new Mat();
        if (fileUri == null) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.lena);
            Utils.bitmapToMat(bitmap, src);
        } else {
            src = Imgcodecs.imread(fileUri.getPath());
        }
        if (src.empty()) {
            return;
        }
        Mat dst = new Mat(src.rows(), src.cols(), CvType.CV_8UC3);
        if(type == 0) {//均值模糊
            Imgproc.blur(src, dst, new Size(5, 5), new Point(-1, -1), Core.BORDER_DEFAULT);
        } else if(type == 1) {//高斯模糊
            Imgproc.GaussianBlur(src, dst, new Size(0, 0), 15);
        }else if(type == 2) {//中值模糊
            Imgproc.medianBlur(src, dst, 5);
        } else if(type == 3) {//膨胀（最大值模糊）
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
            Imgproc.dilate(src, dst, kernel);
        }else if(type == 4) {//腐蚀(最小值模糊)
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
            Imgproc.erode(src, dst, kernel);
        }else if(type == 5) {//高斯双边滤波
            Imgproc.bilateralFilter(src, dst, 0, 150, 15);
        }else if(type == 6) {//均值迁移滤波
            Imgproc.pyrMeanShiftFiltering(src, dst, 10, 50);
        } else if(type == 7) {//自定义滤波
            customFilter(src, dst, 0);
        } else if(type == 8) {//形态学操作
            morphologyDemo(src, dst, 0);
        } else if(type == 9) {//阈值化与阈值
            thresholdDemo(src, dst);
        }else if(type == 10) {//自适应阈值
            adpThresholdDemo(src, dst);
        }

        // 转换为Bitmap，显示
        Bitmap bm = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Mat result = new Mat();
        Imgproc.cvtColor(dst, result, Imgproc.COLOR_BGR2RGBA);
        Utils.matToBitmap(result, bm);

        // show
        ImageView iv = (ImageView)this.findViewById(R.id.chapter4_imageView);
        iv.setImageBitmap(bm);

        // release memory
        src.release();
        dst.release();
        result.release();
    }

    /**
     * 灰度图像阈值化
     *
     * @param src
     * @param dst
     */
    private void thresholdDemo(Mat src, Mat dst) {
        int t = 127;
        int maxValue = 255;
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        //三角阈值法灰度图像阈值化
        // Imgproc.threshold(gray, dst, t, maxValue, Imgproc.THRESH_BINARY | Imgproc.THRESH_TRIANGLE);
        //OTSU灰度图像阈值化
        Imgproc.threshold(gray, dst, t, maxValue, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        gray.release();
    }

    /**
     * 自适应阈值
     *
     * @param src
     * @param dst
     */
    private void adpThresholdDemo(Mat src, Mat dst) {
        int t = 127;
        int maxValue = 255;
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        //C均值
//        Imgproc.adaptiveThreshold(src, dst, 255,
//                Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 10);
        //高斯C均值
        Imgproc.adaptiveThreshold(src, dst, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 15, 10);
        gray.release();
    }

    /**
     * 形态学操作
     * @param src
     * @param dst
     * @param option
     */
    private void morphologyDemo(Mat src, Mat dst, int option) {

        // 创建结构元素
        Mat k = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT, new Size(15, 15), new Point(-1, -1));

        // 形态学操作
        switch (option) {
            case 0: // 膨胀(局部最大值替换中心像素)
                Imgproc.morphologyEx(src, dst, Imgproc.MORPH_DILATE, k);
                break;
            case 1: // 腐蚀(局部最小值替换中心像素，先腐蚀后膨胀，可以对图像进行降噪)
                Imgproc.morphologyEx(src, dst, Imgproc.MORPH_ERODE, k);
                break;
            case 2: // 开操作 =（腐蚀操作 + 膨胀操作）
                Imgproc.morphologyEx(src, dst, Imgproc.MORPH_OPEN, k);
                break;
            case 3: // 闭操作 = （膨胀操作 + 腐蚀操作）
                Imgproc.morphologyEx(src, dst, Imgproc.MORPH_CLOSE, k);
                break;
            case 4: // 黑帽 = src - 开操作   黑帽与顶帽操作用于在灰度图像或者显微镜上分离比较暗或者明亮的斑点
                Imgproc.morphologyEx(src, dst, Imgproc.MORPH_BLACKHAT, k);
                break;
            case 5: // 顶帽 = 闭操作 - src
                Imgproc.morphologyEx(src, dst, Imgproc.MORPH_TOPHAT, k);
                break;
            case 6: // 基本梯度 = 膨胀操作 - 腐蚀操作
                Imgproc.morphologyEx(src, dst, Imgproc.MORPH_GRADIENT, k);
                break;
            default:
                break;
        }
    }

    /**
     * 自定义滤波
     * @param src
     * @param dst
     * @param type
     */
    private void customFilter(Mat src, Mat dst, int type) {
        if(type == 1) { //自定义3*3的模糊卷积核
            Mat k = new Mat(3, 3, CvType.CV_32FC1);
            float[] data = new float[]{1.0f/9.0f,1.0f/9.0f,1.0f/9.0f,
                    1.0f/9.0f, 1.0f/9.0f, 1.0f/9.0f,
                    1.0f/9.0f, 1.0f/9.0f, 1.0f/9.0f};
            k.put(0, 0, data);
        } else if(type == 2) {  //自定义3*3的近似高斯模糊卷积核
            Mat k = new Mat(3, 3, CvType.CV_32FC1);
            float[] data = new float[]{0,1.0f/8.0f,0,
                                        1.0f/8.0f, 0.5f, 1.0f/8.0f,
                                        0, 1.0f/8.0f, 0};
            k.put(0, 0, data);
        } else if(type == 3) {  //Robot算子计算图像梯度
            Mat kx = new Mat(3, 3, CvType.CV_32FC1);
            Mat ky = new Mat(3, 3, CvType.CV_32FC1);

            float[] robert_x = new float[]{-1,0,0,1};//X方向梯度算子
            kx.put(0, 0, robert_x);

            float[] robert_y = new float[]{0,1,-1,0};//Y方向梯度算子
            ky.put(0, 0, robert_y);

            Imgproc.filter2D(src, dst, -1, kx);
            Imgproc.filter2D(src, dst, -1, ky);
        } else if (type == 4) { //锐化算子
            Mat k = new Mat(3, 3, CvType.CV_32FC1);
            float[] data = new float[]{0,-1,0,-1,5,-1,0,-1,0};//锐化算子一
            k.put(0,0, data);
            Imgproc.filter2D(src, dst, -1, k);
            float[] data2 = new float[]{-1,-1,-1,-1,9,-1,-1,-1,-1};//锐化算子二：强化锐化算子八领域
            k.put(0,0,data2);
            Imgproc.filter2D(src, dst, -1,k);
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

        ImageView imageView = (ImageView) this.findViewById(R.id.chapter4_imageView);
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
        imageView.setImageBitmap(bitmap);
    }
}
