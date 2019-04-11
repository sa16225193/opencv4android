package com.book.chapter.three;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import gloomyfish.opencvdemo.ImageSelectUtils;
import gloomyfish.opencvdemo.R;

public class MatOperationsActivity extends AppCompatActivity implements View.OnClickListener {
    private int REQUEST_CAPTURE_IMAGE = 1;
    private String TAG = "DEMO-OpenCV";
    private Uri fileUri;
    private File tempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mat_operations);
        Button selectBtn = (Button)this.findViewById(R.id.select_image_btn);
        Button processBtn = (Button)this.findViewById(R.id.operation_btn);
        selectBtn.setOnClickListener(this);
        processBtn.setOnClickListener(this);

        tempFile = new File(getExternalFilesDir("img"), System.currentTimeMillis() + ".jpg");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_operation_mat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.blendMat:
                blendMat(1.5, 0.5);
                break;
            case R.id.meanAndDev:
                meanAndDev();
                break;
            case R.id.normAndAbs:
                normAndAbs();
                break;
            case R.id.logicOperator:
                logicOperator();
                break;
            case R.id.adjustBrightAndContrast:
                adjustBrightAndContrast(50, 1.0f);
                break;
            case R.id.matArithmeticDemo:
                matArithmeticDemo();
                break;
            case R.id.channelsAndPixels:
                channelsAndPixels();
                break;
            case R.id.readAndWritePixels:
                readAndWritePixels();
            default:
                blendMat(1.5, 0.5);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 调整亮度与对比度
     * @param alpha
     * @param gamma
     */
    public void blendMat(double alpha, double gamma) {
        // 加载图像
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

        // 创建纯黑色图像
        Mat black = Mat.zeros(src.size(), src.type());
        Mat dst = new Mat();

        // 像素混合 - 基于权重
        Core.addWeighted(src, alpha, black, 1.0-alpha, gamma, dst);

        // 转换为Bitmap，显示
        Bitmap bm = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Mat result = new Mat();
        Imgproc.cvtColor(dst, result, Imgproc.COLOR_BGR2RGBA);
        Utils.matToBitmap(result, bm);

        // show
        ImageView iv = (ImageView)this.findViewById(R.id.chapter3_imageView);
        iv.setImageBitmap(bm);

    }

    /**
     * 根据均值进行二值分割
     */
    public void meanAndDev() {
        // 加载图像
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
        // 转为灰度图像
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // 计算均值与标准方差（标准方差越小,说明图像各个像素的差异越小,图像本身携带的有效信息越少。）
        MatOfDouble means = new MatOfDouble();
        MatOfDouble stddevs = new MatOfDouble();
        Core.meanStdDev(gray, means, stddevs);

        // 显示均值与标准方差
        double[] mean = means.toArray();
        double[] stddev = stddevs.toArray();
        Log.i(TAG, "gray image means : " + mean[0]);
        Log.i(TAG, "gray image stddev : " + stddev[0]);

        // 读取像素数组
        int width = gray.cols();
        int height = gray.rows();
        byte[] data = new byte[width*height];
        gray.get(0, 0, data);
        int pv = 0;

        // 根据均值，二值分割
        int t = (int)mean[0];
        for(int i=0; i<data.length; i++) {
            pv = data[i]&0xff;
            if(pv > t) {
                data[i] = (byte)255;
            } else {
                data[i] = (byte)0;
            }
        }
        gray.put(0, 0, data);

        Bitmap bm = Bitmap.createBitmap(gray.cols(), gray.rows(), Bitmap.Config.ARGB_8888);
        Mat dst = new Mat();
        Imgproc.cvtColor(gray, dst, Imgproc.COLOR_GRAY2RGBA);
        Utils.matToBitmap(dst, bm);
        ImageView iv = (ImageView)this.findViewById(R.id.chapter3_imageView);
        iv.setImageBitmap(bm);
        dst.release();  //释放Mat对象
        gray.release();
        src.release();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.select_image_btn:
                pickUpImage();
                break;
            case R.id.operation_btn:
                blendMat(1.5, 0.5);
                break;
            default:
                break;
        }
    }

    /**
     * 归一化
     */
    public void normAndAbs() {
        // 创建随机浮点数图像
        Mat src = Mat.zeros(400, 400, CvType.CV_32FC3);
        float[] data = new float[400*400*3];
        Random random = new Random();
        for(int i=0; i<data.length; i++) {
            data[i] = (float)random.nextGaussian();
        }
        src.put(0, 0, data);

        // 归一化值到0～255之间
        Mat dst = new Mat();
        Core.normalize(src, dst, 0, 255, Core.NORM_MINMAX, -1, new Mat());


        // 类型转换
        Mat dst8u = new Mat();
        dst.convertTo(dst8u, CvType.CV_8UC3);

        // 转换为Bitmap，显示
        Bitmap bm = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Mat result = new Mat();
        Imgproc.cvtColor(dst8u, result, Imgproc.COLOR_BGR2RGBA);
        Utils.matToBitmap(result, bm);

        // show
        ImageView iv = (ImageView)this.findViewById(R.id.chapter3_imageView);
        iv.setImageBitmap(bm);
    }

    /**
     * Mat的逻辑操作
     */
    public void logicOperator() {
        //创建图像
        Mat src1 = Mat.zeros(400, 400, CvType.CV_8UC3);
        Mat src2 = new Mat(400, 400, CvType.CV_8UC3);
        src2.setTo(new Scalar(255, 255, 255));  //设置为白色图像

        //ROI区域定义
        Rect rect = new Rect();
        rect.x=100;
        rect.y=100;
        rect.width = 200;
        rect.height = 200;
        Imgproc.rectangle(src1, rect.tl(), rect.br(), new Scalar(0, 255, 0), -1);

        //矩形
        rect.x=10;
        rect.y=10;
        Imgproc.rectangle(src2, rect.tl(), rect.br(), new Scalar(255, 255, 0), -1);

        //逻辑运算
        Mat dst1 = new Mat();
        Mat dst2 = new Mat();
        Mat dst3 = new Mat();
        Core.bitwise_and(src1, src2, dst1);
        Core.bitwise_or(src1, src2, dst2);
        Core.bitwise_xor(src1, src2, dst3);

        //输出结果
        Mat dst = Mat.zeros(400, 1200, CvType.CV_8UC3);
        rect.x=0;
        rect.y=0;
        rect.width=400;
        rect.height=400;
        dst1.copyTo(dst.submat(rect));
        rect.x=400;
        dst2.copyTo(dst.submat(rect));
        rect.x=800;
        dst3.copyTo(dst.submat(rect));

        //释放内存
        dst1.release();
        dst2.release();
        dst3.release();

        // 转换为Bitmap，显示
        Bitmap bm = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Mat result = new Mat();
        Imgproc.cvtColor(dst, result, Imgproc.COLOR_BGR2RGBA);
        Utils.matToBitmap(result, bm);

        // show
        ImageView iv = (ImageView)this.findViewById(R.id.chapter3_imageView);
        iv.setImageBitmap(bm);

    }

    /**
     *  调整亮度与对比度
     * @param b - brightness
     * @param c - contrast
     */
    public void adjustBrightAndContrast(int b, float c) {
        // 输入图像src1
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

        // 调整亮度
        Mat dst1 = new Mat();
        Core.add(src, new Scalar(b, b, b), dst1);

        // 调整对比度
//        Mat dst2 = new Mat();
//        Core.multiply(dst1, new Scalar(c, c, c), dst2);

        // 转换为Bitmap，显示
        Bitmap bm = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Mat result = new Mat();
        Imgproc.cvtColor(dst1, result, Imgproc.COLOR_BGR2RGBA);
        Utils.matToBitmap(result, bm);

        // show
        ImageView iv = (ImageView)this.findViewById(R.id.chapter3_imageView);
        iv.setImageBitmap(bm);
    }

    public void matArithmeticDemo() {
        // 输入图像src1
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
        // 输入图像src2
        Mat moon = Mat.zeros(src.rows(), src.cols(), src.type());
        int cx = src.cols() - 60;
        int cy = 60;
        Imgproc.circle(moon, new Point(cx, cy), 50, new Scalar(90,95,234), -1, 8, 0);

        // 加法运算
        Mat dst = new Mat();
        Core.add(src, moon, dst);

        Bitmap bm = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Mat result = new Mat();
        Imgproc.cvtColor(dst, result, Imgproc.COLOR_BGR2RGBA);
        Utils.matToBitmap(result, bm);
        ImageView iv = (ImageView)this.findViewById(R.id.chapter3_imageView);
        iv.setImageBitmap(bm);
    }

    /**
     * 通道分离与合并
     */
    public void channelsAndPixels() {
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

        List<Mat> mv = new ArrayList<>();
        Core.split(src, mv);
        for(Mat m : mv) {
            int pv = 0;
            int channels = m.channels();
            int width = m.cols();
            int height = m.rows();
            byte[] data = new byte[channels*width*height];
            m.get(0, 0, data);
            for(int i=0; i<data.length; i++) {
                pv = data[i]&0xff;
                pv = 255-pv;
                data[i] = (byte)pv;
            }
            m.put(0, 0, data);
        }
        Core.merge(mv, src);

        Bitmap bm = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Mat dst = new Mat();
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2RGBA);
        Utils.matToBitmap(dst, bm);
        ImageView iv = (ImageView)this.findViewById(R.id.chapter3_imageView);
        iv.setImageBitmap(bm);
        dst.release();
        src.release();
    }

    /**
     * 读取并修改像素
     */
    public void readAndWritePixels() {
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
        int channels = src.channels();
        int width = src.cols();
        int height = src.rows();

//        // each row data  //每次读取一行像素
//        byte[] data = new byte[channels*width];
//        // loop
//        int b=0, g=0, r=0;
//        int pv = 0;
//        for(int row=0; row<height; row++) {
//            src.get(row, 0, data);
//            for(int col=0; col<data.length; col++) {
//                // 读取
//                pv = data[col]&0xff;
//                // 修改
//                pv = 255 - pv;
//                data[col] = (byte)pv;
//            }
//            // 写入
//            src.put(row, 0, data);
//        }

        // all pixels
        //读取全部像素
        int pv = 0;
        byte[] data = new byte[channels*width*height];
        src.get(0, 0, data);
        for(int i=0; i<data.length; i++) {
            pv = data[i]&0xff;
            pv = 255-pv;
            data[i] = (byte)pv;
        }
        src.put(0, 0, data);

        Bitmap bm = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Mat dst = new Mat();
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2RGBA);
        Utils.matToBitmap(dst, bm);
        ImageView iv = (ImageView)this.findViewById(R.id.chapter3_imageView);
        iv.setImageBitmap(bm);
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

        ImageView imageView = (ImageView)this.findViewById(R.id.chapter3_imageView);
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
