package com.book.chapter.six;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.xfeatures2d.SIFT;
import org.opencv.xfeatures2d.SURF;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import gloomyfish.opencvdemo.ImageSelectUtils;
import gloomyfish.opencvdemo.R;

public class Feature2dMainActivity extends AppCompatActivity implements View.OnClickListener{
    private int REQUEST_CAPTURE_IMAGE = 1;
    private String TAG = "DEMO-OpenCV";
    private CascadeClassifier faceDetector;
    private Uri fileUri;
    private File tempFile;
    private int option;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feature2d_main);
        Button selectBtn = (Button)this.findViewById(R.id.select_image_btn);
        Button processBtn = (Button)this.findViewById(R.id.extract_feature_btn);
        selectBtn.setOnClickListener(this);
        processBtn.setOnClickListener(this);
        try {
            initFaceDetector();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        tempFile = new File(getExternalFilesDir("img"), System.currentTimeMillis() + ".jpg");
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.select_image_btn:
                pickUpImage();
                break;
            case R.id.extract_feature_btn:
                extractFeatureImage(7);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.harrisCornerDemo:
                option = 0;
                break;
            case R.id.shiTomasicornerDemo:
                option = 1;
                break;
            case R.id.surfDemo:
                option = 2;
                break;
            case R.id.siftDemo:
                option = 3;
                break;
            case R.id.detectorDemo:
                option = 4;
                break;
            case R.id.descriptorDemo:
                option = 5;
                break;
            case R.id.findKnownObject:
                option = 6;
                break;
            case R.id.faceDetectionDemo:
                option = 7;
                break;
            default:
                option = 0;
                break;
        }
        extractFeatureImage(7);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detector, menu);
        return true;
    }

    private void initFaceDetector() throws IOException {
        InputStream input = getResources().openRawResource(R.raw.lbpcascade_frontalface);
        File cascadeDir = this.getDir("cascade", Context.MODE_PRIVATE);
        File file = new File(cascadeDir.getAbsoluteFile(), "lbpcascade_frontalface.xml");
        FileOutputStream output = new FileOutputStream(file);
        byte[] buff = new byte[1024];
        int len = 0;
        while((len = input.read(buff)) != -1) {
            output.write(buff, 0, len);
        }
        input.close();
        output.close();
        faceDetector = new CascadeClassifier(file.getAbsolutePath());
        file.delete();
        cascadeDir.delete();
    }

    private void extractFeatureImage(int section) {
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
        Mat dst = new Mat();
        if(section == 0) {
            harrisCornerDemo(src, dst);
        } else if(section == 1) {
            shiTomasicornerDemo(src, dst);
        } else if(section == 2) {
            surfDemo(src, dst);
        } else if(section == 3){
            siftDemo(src, dst);
        } else if(section == 4) {
            detectorDemo(src, dst, 1);
        } else if(section == 5) {
            descriptorDemo(src, dst);
        } else if(section == 6) {
            findKnownObject(src, dst);
        } else if(section == 7) {
            faceDetectionDemo(src, dst);
        }

        // 转换为Bitmap，显示
        Bitmap bm = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Mat result = new Mat();
        Imgproc.cvtColor(dst, result, Imgproc.COLOR_BGR2RGBA);
        Utils.matToBitmap(result, bm);

        // show
        ImageView iv = (ImageView)this.findViewById(R.id.chapter6_imageView);
        iv.setImageBitmap(bm);

        // release memory
        src.release();
        dst.release();
        result.release();
    }

    private void faceDetectionDemo(Mat src, Mat dst) {
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGRA2GRAY);
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(gray, faces, 1.1, 3, 0, new Size(50, 50), new Size());
        List<Rect> faceList = faces.toList();
        src.copyTo(dst);
        if(faceList.size() > 0) {
            for(Rect rect : faceList) {
                Imgproc.rectangle(dst, rect.tl(), rect.br(), new Scalar(0,0,255), 2, 8, 0);
            }
        }
        gray.release();
    }

    private void findKnownObject(Mat src, Mat dst) {
        String boxFile = fileUri.getPath().replaceAll("box_in_scene", "box");
        Mat boxImage = Imgcodecs.imread(boxFile, Imgcodecs.IMREAD_GRAYSCALE);
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);


        SURF surf_detector = SURF.create(400, 4, 3, false, false);
        MatOfKeyPoint keyPoints_box = new MatOfKeyPoint();
        MatOfKeyPoint keyPoints_scene = new MatOfKeyPoint();

        // 特征检测-关键点
        surf_detector.detect(boxImage, keyPoints_box);
        surf_detector.detect(gray, keyPoints_scene);

        // 获取描述子
        Mat descriptor_box = new Mat();
        Mat descriptor_scene = new Mat();
        surf_detector.compute(boxImage, keyPoints_box, descriptor_box);
        surf_detector.compute(gray, keyPoints_scene, descriptor_scene);

        // 匹配
        MatOfDMatch matches = new MatOfDMatch();
        DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        descriptorMatcher.match(descriptor_box, descriptor_scene, matches);

        // find min max distance
        DMatch[] dm_arrays = matches.toArray();
        double max_dist = 0; double min_dist = 100;
        for(int i=0; i<descriptor_box.rows(); i++) {
            double dist = dm_arrays[i].distance;
            max_dist = Math.max(dist, max_dist);
            min_dist = Math.min(dist, min_dist);
        }
        Log.i("Find Known Object", "max distance : " + max_dist);
        Log.i("Find Known Object", "min distance : " + min_dist);

        ArrayList<DMatch> goodMatches = new ArrayList<DMatch>();
        double t = 3.0*min_dist;
        for(int i=0; i<descriptor_box.rows(); i++) {
            if(dm_arrays[i].distance <= t) {
                goodMatches.add(dm_arrays[i]);
            }
        }
        Features2d.drawMatches(boxImage, keyPoints_box, gray, keyPoints_scene, new MatOfDMatch(goodMatches.toArray(new DMatch[0])),
                dst, Scalar.all(-1), Scalar.all(-1), new MatOfByte(),Features2d.NOT_DRAW_SINGLE_POINTS);

        // 得到匹配程度高的关键点对
        Point[] boxes = new Point[goodMatches.size()];
        Point[] scenes = new Point[goodMatches.size()];
        KeyPoint[] kp_boxes = keyPoints_box.toArray();
        KeyPoint[] kp_scenes = keyPoints_scene.toArray();
        for(int i=0; i<goodMatches.size(); i++) {
            boxes[i] = (kp_boxes[goodMatches.get(i).queryIdx].pt);
            scenes[i] = (kp_scenes[goodMatches.get(i).trainIdx].pt);
        }

        // 寻找位置
        Mat H = Calib3d.findHomography(new MatOfPoint2f(boxes), new MatOfPoint2f(scenes), Calib3d.RANSAC, 3);
        Mat obj_corners = new Mat(4,1,CvType.CV_32FC2);
        Mat scene_corners = new Mat(4,1,CvType.CV_32FC2);
        obj_corners.put(0, 0, new double[] {0,0});
        obj_corners.put(1, 0, new double[] {boxImage.cols(),0});
        obj_corners.put(2, 0, new double[] {boxImage.cols(),boxImage.rows()});
        obj_corners.put(3, 0, new double[] {0,boxImage.rows()});
        Core.perspectiveTransform(obj_corners, scene_corners, H);

        // 绘制直线，矩形外接框
        Imgproc.line(dst, new Point(scene_corners.get(0,0)[0]+boxImage.cols(), scene_corners.get(0,0)[1]),
                    new Point(scene_corners.get(1,0)[0] + boxImage.cols(), scene_corners.get(1,0)[1]),
                new Scalar(0, 255, 0),4);

        Imgproc.line(dst, new Point(scene_corners.get(1,0)[0]+boxImage.cols(), scene_corners.get(1,0)[1]),
                new Point(scene_corners.get(2,0)[0]+boxImage.cols(), scene_corners.get(2,0)[1]),
                new Scalar(0, 255, 0),4);

        Imgproc.line(dst, new Point(scene_corners.get(2,0)[0]+boxImage.cols(), scene_corners.get(2,0)[1]),
                new Point(scene_corners.get(3,0)[0]+boxImage.cols(), scene_corners.get(3,0)[1]),
                new Scalar(0, 255, 0),4);

        Imgproc.line(dst, new Point(scene_corners.get(3,0)[0]+boxImage.cols(), scene_corners.get(3,0)[1]),
                new Point(scene_corners.get(0,0)[0]+boxImage.cols(), scene_corners.get(0,0)[1]),
                new Scalar(0, 255, 0),4);


        // 释放内存
        keyPoints_box.release();
        keyPoints_scene.release();

        descriptor_box.release();
        descriptor_scene.release();
        matches.release();
    }

    private void descriptorDemo(Mat src, Mat dst) {
        String boxFile = fileUri.getPath().replaceAll("box_in_scene", "box");
        Mat boxImage = Imgcodecs.imread(boxFile);
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.AKAZE);
        DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.AKAZE);

        // 关键点检测
        MatOfKeyPoint keyPoints_box = new MatOfKeyPoint();
        MatOfKeyPoint keyPoints_scene = new MatOfKeyPoint();
        detector.detect(boxImage, keyPoints_box);
        detector.detect(src, keyPoints_scene);

        // 描述子生成
        Mat descriptor_box = new Mat();
        Mat descriptor_scene = new Mat();
        descriptorExtractor.compute(boxImage, keyPoints_box, descriptor_box);
        descriptorExtractor.compute(src, keyPoints_scene, descriptor_scene);

        // 特征匹配
        MatOfDMatch matches = new MatOfDMatch();
        DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        descriptorMatcher.match(descriptor_box, descriptor_scene, matches);
        Features2d.drawMatches(boxImage, keyPoints_box, src, keyPoints_scene, matches, dst);

        // 释放内存
        keyPoints_box.release();
        keyPoints_scene.release();

        descriptor_box.release();
        descriptor_scene.release();
        matches.release();
    }

    /**
     * 特征检测
     *
     * @param src
     * @param dst
     * @param type
     */
    private void detectorDemo(Mat src, Mat dst, int type) {
        FeatureDetector detector = null;
        if(type == 1) {
            detector = FeatureDetector.create(FeatureDetector.ORB);//ORB检测器
        } else if(type == 2) {
            detector = FeatureDetector.create(FeatureDetector.BRISK);//BRISK检测器
        } else if(type == 3) {
            detector = FeatureDetector.create(FeatureDetector.FAST);//FAST检测器
        } else if(type == 4){
            detector = FeatureDetector.create(FeatureDetector.AKAZE);//AKAZE检测器
        } else {
            detector = FeatureDetector.create(FeatureDetector.HARRIS);//HARRIS检测器
        }
        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
        detector.detect(src, keyPoints);
        Features2d.drawKeypoints(src, keyPoints, dst);
    }

    /**
     * SIFT特征检测
     * 主要用在图像比对、对象与场景识别等领域，该方法与其他的特征提取方法相比有更高的准确性、特征抗干扰
     *
     * @param src
     * @param dst
     */
    private void siftDemo(Mat src, Mat dst) {
        String boxFile = fileUri.getPath().replaceAll("box_in_scene", "box");
        Mat boxImage = Imgcodecs.imread(boxFile);

        SIFT sift_detector = SIFT.create();
        MatOfKeyPoint keyPoints_box = new MatOfKeyPoint();
        MatOfKeyPoint keyPoints_scene = new MatOfKeyPoint();

        // 特征检测-关键点
        sift_detector.detect(boxImage, keyPoints_box);
        sift_detector.detect(src, keyPoints_scene);

        // 获取描述子
        Mat descriptor_txt = new Mat();
        Mat descriptor_scene = new Mat();
        sift_detector.compute(boxImage, keyPoints_box, descriptor_txt);
        sift_detector.compute(src, keyPoints_scene, descriptor_scene);

        // 匹配
        MatOfDMatch matches = new MatOfDMatch();
        DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_SL2);
        descriptorMatcher.match(descriptor_txt, descriptor_scene, matches);
        Features2d.drawMatches(boxImage, keyPoints_box, src, keyPoints_scene, matches, dst);

        // 释放内存
        keyPoints_box.release();
        keyPoints_scene.release();

        descriptor_txt.release();
        descriptor_scene.release();
        matches.release();
    }

    private void surfDemo(Mat src, Mat dst) {
        String textFile = fileUri.getPath().replaceAll("box_in_scene", "box");
        Mat textImage = Imgcodecs.imread(textFile);

        SURF surf_detector = SURF.create(100, 4, 3, false, false);
        MatOfKeyPoint keyPoints_txt = new MatOfKeyPoint();
        MatOfKeyPoint keyPoints_scene = new MatOfKeyPoint();

        // 特征检测-关键点
        surf_detector.detect(textImage, keyPoints_txt);
        surf_detector.detect(src, keyPoints_scene);

        // 获取描述子
        Mat descriptor_txt = new Mat();
        Mat descriptor_scene = new Mat();
        surf_detector.compute(textImage, keyPoints_txt, descriptor_txt);
        surf_detector.compute(src, keyPoints_scene, descriptor_scene);

        // 匹配
        MatOfDMatch matches = new MatOfDMatch();
        DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_SL2);
        descriptorMatcher.match(descriptor_txt, descriptor_scene, matches);
        Features2d.drawMatches(textImage, keyPoints_txt, src, keyPoints_scene, matches, dst);

        // 释放内存
        keyPoints_txt.release();
        keyPoints_scene.release();

        descriptor_txt.release();
        descriptor_scene.release();
        matches.release();
    }

    /**
     * Shi-Tomasi角点检测
     *
     * @param src
     * @param dst
     */
    private void shiTomasicornerDemo(Mat src, Mat dst) {
        // 变量定义
        double k = 0.04;
        int blockSize = 3;
        double qualityLevel= 0.01;
        boolean useHarrisCorner = false;

        // 角点检测
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        MatOfPoint corners = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(gray, corners, 100, qualityLevel, 10, new Mat(), blockSize, useHarrisCorner, k);

        // 绘制角点
        dst.create(src.size(), src.type());
        src.copyTo(dst);
        Point[] points = corners.toArray();
        for(int i=0; i<points.length; i++) {
            Imgproc.circle(dst, points[i], 5,  new Scalar(0, 0, 255), 2, 8, 0);
        }
        gray.release();
    }

    /**
     * Harris角点检测
     *
     * @param src
     * @param dst
     */
    private void harrisCornerDemo(Mat src, Mat dst) {
        // 定义阈值T
        int threshold = 100;
        Mat gray = new Mat();
        Mat response = new Mat();
        Mat response_norm = new Mat();

        // 角点检测
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);    //彩色图像转为单通道灰色图像
        Imgproc.cornerHarris(gray, response, 2, 3, 0.04);   //Harris角点检测计算各个像素点上角点相应值
        Core.normalize(response, response_norm, 0, 255, Core.NORM_MINMAX, CvType.CV_32F);   //过滤result超过阈值的像素点

        // 绘制角点
        dst.create(src.size(), src.type());
        src.copyTo(dst);
        float[] data = new float[1];
        for(int j=0; j<response_norm.rows(); j++ )
        {
            for(int i=0; i<response_norm.cols(); i++ )
            {
                response_norm.get(j, i, data);
                if((int)data[0] > 100)
                {
                    Imgproc.circle(dst, new Point(i, j), 5,  new Scalar(0, 0, 255), 2, 8, 0);
                    Log.i("Harris Corner", "find corner point...");
                }
            }
        }
        gray.release();
        response.release();
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

        ImageView imageView = (ImageView) this.findViewById(R.id.chapter6_imageView);
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
