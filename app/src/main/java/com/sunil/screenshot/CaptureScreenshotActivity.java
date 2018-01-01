package com.sunil.screenshot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

/**
 * Created by admin on 1/1/18.
 */

public class CaptureScreenshotActivity extends Activity {

    private static final int SCREEN_SHOT = 0;
    private static final String TAG = "TAG";

    MediaProjection mediaProjection;
    MediaProjectionManager projectionManager;
    VirtualDisplay virtualDisplay;
    int mResultCode;
    Intent mData;
    ImageReader imageReader;

    int width,displayWidth;
    int height,displayHeight;
    int dpi;

    String imageName;
    Bitmap bitmap;
    DisplayMetrics metrics ;
//    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_screenshot);
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        width = metric.widthPixels;
        height = metric.heightPixels;
        dpi = metric.densityDpi;

//        imageView = (ImageView) findViewById(R.id.image);
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        //StartScreenShot(imageView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivityForResult(projectionManager.createScreenCaptureIntent(),
                    SCREEN_SHOT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SCREEN_SHOT){
            if(resultCode == RESULT_OK){
                mResultCode = resultCode;
                mData = data;
                setUpMediaProjection();
                setUpVirtualDisplay();
                startCapture();
            }
        }
    }

    private void startCapture() {
        SystemClock.sleep(1000);
        imageName = System.currentTimeMillis() + ".png";
        Image image = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            image = imageReader.acquireNextImage();
        }
        if (image == null) {
            Log.e(TAG, "image is null.");
            return;
        }
        int width = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            image.close();
        }

        if (bitmap != null) {

            createImage(bitmap);
        }
    }

    private void setUpVirtualDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaProjection.createVirtualDisplay("ScreenShot",
                    width,height,dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    imageReader.getSurface(),null,null);
        }
    }

    private void setUpMediaProjection(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaProjection = projectionManager.getMediaProjection(mResultCode,mData);
        }
    }

    public void createImage(Bitmap bmp) {

        Display display= getWindowManager().getDefaultDisplay();
        Point size = new Point();

        display.getSize(size);

        final int mWidth = size.x;
        final int mHeight = size.y;

        Bitmap newBitmap = Bitmap.createBitmap(bmp, 0, 0, mWidth, mHeight);

        Bitmap resizedbitmap = newBitmap ;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        resizedbitmap.compress(Bitmap.CompressFormat.JPEG, 50, bytes);

        File file = new File(Environment.getExternalStorageDirectory().toString()+"/ScreenShot/Test");
        //create storage directories, if they don't exist
        file.mkdirs();

        //Save the path as a string value
            String filePath = file.toString() + "/capturedscreen.jpg";
            Log.e("File path",filePath);

        try {
            Log.e("path", file.getAbsolutePath() + "=" + file.getCanonicalPath());
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(filePath);
            outputStream.write(bytes.toByteArray());
            outputStream.close();
            stopProjection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopProjection() {
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        this.finish();
    }
}
