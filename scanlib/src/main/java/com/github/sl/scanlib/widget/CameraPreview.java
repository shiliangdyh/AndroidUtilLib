package com.github.sl.scanlib.widget;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.sl.util.LogUtils;
import com.google.zxing.FoundPartException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.DetectorResult;

import net.sourceforge.base.base.CameraUtils;
import net.sourceforge.base.base.DisplayUtils;
import net.sourceforge.zbar.BarcodeFormat;
import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.bertsir.zbar.utils.QRUtils;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraPreview";
    private Handler autoFocusHandler = new Handler();
    private Camera camera;
    private float aspectTolerance = 0.1f;//允许的实际宽高比和理想宽高比之间的最大差值
    private boolean previewing;//相机正在预览
    private boolean surfaceCreated;
    private boolean shouldAdjustFocusArea;//自动对焦
    private ArrayList<Camera.Area> focusAreas;
    private IViewFinder viewFinderView;//扫描框以及周边黑色背景
    private List<BarcodeFormat> formats;
    private ImageScanner imageScanner;
    private boolean scanSuccess = false;

    public CameraPreview(Context context) {
        super(context);
        setupScanner();
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupScanner();
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupScanner();
    }

    public void setShouldAdjustFocusArea(boolean shouldAdjustFocusArea) {
        this.shouldAdjustFocusArea = shouldAdjustFocusArea;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LogUtils.d(TAG, "surfaceCreated: ");
        surfaceCreated = true;
        startCameraPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceCreated = false;
        stopScan();
    }

    /**
     * 开始预览
     */
    public void startCameraPreview() {
        if (camera != null) {
            try {
                previewing = true;
                setupCameraParameters();//设置相机参数
                camera.setPreviewDisplay(getHolder());//设置在当前surfaceView中进行相机预览
                camera.setDisplayOrientation(getDisplayOrientation());//设置相机预览图像的旋转角度
                camera.setOneShotPreviewCallback(this);//设置一次性的预览回调
                camera.startPreview();//开始预览
                safeAutoFocus();//自动对焦
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 尝试自动对焦
     */
    private void safeAutoFocus() {
        if (this.camera != null && this.previewing && this.surfaceCreated) {
            try {
                setupFocusAreas();
                this.camera.autoFocus(this.autoFocusCB);
            } catch (Exception var2) {
                var2.printStackTrace();
                this.scheduleAutoFocus();
            }
        }
    }

    /**
     * 设置对焦区域
     */
    private void setupFocusAreas() {
        if (!shouldAdjustFocusArea) return;

        if (camera == null) return;

        Camera.Parameters parameters = camera.getParameters();
        if (parameters.getMaxNumFocusAreas() <= 0) {
            Log.e(TAG, "不支持设置对焦区域");
            return;
        }

        if (focusAreas == null) {
            int width = 2000, height = 2000;
            if (viewFinderView == null) {
                return;
            }
            Rect framingRect = viewFinderView.getFramingRect();//获得扫码框区域
            if (framingRect == null) return;
            int viewFinderViewWidth = ((View) viewFinderView).getWidth();
            int viewFinderViewHeight = ((View) viewFinderView).getHeight();

            //1.根据ViewFinderView和2000*2000的尺寸之比，缩放对焦区域
            Rect scaledRect = new Rect(framingRect);
            scaledRect.left = scaledRect.left * width / viewFinderViewWidth;
            scaledRect.right = scaledRect.right * width / viewFinderViewWidth;
            scaledRect.top = scaledRect.top * height / viewFinderViewHeight;
            scaledRect.bottom = scaledRect.bottom * height / viewFinderViewHeight;

            //2.旋转对焦区域
            Rect rotatedRect = new Rect(scaledRect);
            int rotationCount = getRotationCount();
            if (rotationCount == 1) {//若相机图像需要顺时针旋转90度，则将扫码框逆时针旋转90度
                rotatedRect.left = scaledRect.top;
                rotatedRect.top = 2000 - scaledRect.right;
                rotatedRect.right = scaledRect.bottom;
                rotatedRect.bottom = 2000 - scaledRect.left;
            } else if (rotationCount == 2) {//若相机图像需要顺时针旋转180度,则将扫码框逆时针旋转180度
                rotatedRect.left = 2000 - scaledRect.right;
                rotatedRect.top = 2000 - scaledRect.bottom;
                rotatedRect.right = 2000 - scaledRect.left;
                rotatedRect.bottom = 2000 - scaledRect.top;
            } else if (rotationCount == 3) {//若相机图像需要顺时针旋转270度，则将扫码框逆时针旋转270度
                rotatedRect.left = 2000 - scaledRect.bottom;
                rotatedRect.top = scaledRect.left;
                rotatedRect.right = 2000 - scaledRect.top;
                rotatedRect.bottom = scaledRect.right;
            }

            //3.坐标系平移
            Rect rect = new Rect(rotatedRect.left - 1000, rotatedRect.top - 1000, rotatedRect.right - 1000, rotatedRect.bottom - 1000);

            Camera.Area area = new Camera.Area(rect, 1000);
            focusAreas = new ArrayList<>();
            focusAreas.add(area);
        }

        parameters.setFocusAreas(focusAreas);
        camera.setParameters(parameters);
    }

    /**
     * 获取（旋转角度/90）
     */
    private int getRotationCount() {
        int displayOrientation = getDisplayOrientation();
        return displayOrientation / 90;
    }

    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        //自动对焦完成时此方法被调用
        public void onAutoFocus(boolean success, Camera camera) {
            scheduleAutoFocus();//一秒之后再次自动对焦
        }
    };

    /**
     * 一秒之后尝试自动对焦
     */
    private void scheduleAutoFocus() {
        autoFocusHandler.postDelayed(new Runnable() {
            public void run() {
                safeAutoFocus();
            }
        }, 1000);
    }

    /**
     * 设置相机参数
     */
    private void setupCameraParameters() {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size optimalSize = getOptimalPreviewSize();
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            camera.setParameters(parameters);
        }
    }

    public void startScan() {
        scanSuccess = false;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            int cameraId = CameraUtils.getDefaultCameraId();
            if (camera == null) {
                camera = CameraUtils.getCameraInstance(cameraId);//打开camera
            }
            getHolder().addCallback(this);//surface生命周期的回调
        } else {//没有相机权限
            throw new RuntimeException("没有Camera权限");
        }
    }

    public void stopScan() {
        stopCameraPreview();
    }

    /**
     * 停止预览
     */
    public synchronized void stopCameraPreview() {
        if (camera != null) {
            try {
                previewing = false;
                autoFocusHandler.removeCallbacksAndMessages(null);
                getHolder().removeCallback(this);
                camera.cancelAutoFocus();
                camera.setOneShotPreviewCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 找到一个合适的previewSize（根据控件的尺寸）
     */
    private Camera.Size getOptimalPreviewSize() {
        if (camera == null) {
            return null;
        }

        //相机图像默认都是横屏(即宽>高)
        List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
        if (sizes == null) return null;
        int w, h;
        if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_LANDSCAPE) {
            w = getWidth();
            h = getHeight();
        } else {
            w = getHeight();
            h = getWidth();
        }

        double targetRatio = (double) w / h;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > aspectTolerance) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


    /**
     * 要使相机图像的方向与手机中窗口的方向一致，相机图像需要顺时针旋转的角度
     * <p>
     * 此方法由google官方提供，详见Camera类中setDisplayOrientation的方法说明
     */
    public int getDisplayOrientation() {
        if (camera == null) {
            return 0;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(0, info);

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        decodeByZxing(data);
        decodeByZbar(data);
    }

    private boolean zxingAllowAnalysis = true;
    private boolean zbarAllowAnalysis = true;
    private ExecutorService zxingExecutorService = Executors.newSingleThreadExecutor();
    private ExecutorService zbarExecutorService = Executors.newSingleThreadExecutor();
    private Rect scaledRect, rotatedRect;
    private boolean auto_zoom = false;
    private long scaleZoom;

    private void decodeByZxing(final byte[] mData) {
        if (zxingAllowAnalysis) {
            zxingAllowAnalysis = false;
            zxingExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    int width = 0;
                    int height = 0;
                    byte[] data = mData;
                    Camera.Parameters parameters = getParameters();
                    if (parameters == null) {
                        zxingAllowAnalysis = true;
                        return;
                    }
                    Camera.Size size = parameters.getPreviewSize();
                    width = size.width;
                    height = size.height;
                    String resultStr = "";

                    if (isPortrait(getContext())) {
                        data = new byte[mData.length];
                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                data[x * height + height - y - 1] = mData[x + y * width];
                            }
                        }
                        int tmp = width;
                        width = height;
                        height = tmp;
                    }
                    int previewWidth = width;
                    int previewHeight = height;

                    //根据ViewFinderView和preview的尺寸之比，缩放扫码区域
                    Rect rect = getScaledRect(previewWidth, previewHeight);
                    PlanarYUVLuminanceSource source;
                    int cropWidth = 0;
                    int cropHeight = 0;
                    if (rect != null) {
                        cropWidth = rect.width();
                        cropHeight = rect.height();
                        source = new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, cropWidth,
                                cropHeight, false);
                    } else {
                        source = new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
                    }
                    try {
                        resultStr = QRUtils.getInstance().decodeByZxing(source);
                    } catch (FoundPartException e) {
                        autoZoomIfNeed(e, cropWidth, cropHeight);
                    }
                    synchronized (CameraPreview.class) {
                        if (!TextUtils.isEmpty(resultStr) && !scanSuccess){
                            Log.d(TAG, "zxing解析成功，内容: " + resultStr);
                            scanSuccess = true;
                            scanSuccess(resultStr);
                            return;
                        }
                        if (!scanSuccess) {//还没有扫描成功
                            zxingAllowAnalysis = true;
                            getOneMoreFrame();//再获取一帧图像数据进行识别（会再次触发onPreviewFrame方法）
                        }
                    }
                }
            });
        }
    }

    /**
     * 使用zbar解析
     *
     * @param data
     */
    private void decodeByZbar(final byte[] data) {
        if (zbarAllowAnalysis) {
            zbarAllowAnalysis = false;
            zbarExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    String resultStr = "";
                    try {
                        Camera.Parameters parameters = getParameters();
                        if (parameters == null){
                            zbarAllowAnalysis = true;
                            return;
                        }
                        Camera.Size previewSize = parameters.getPreviewSize();
                        int previewWidth = previewSize.width;
                        int previewHeight = previewSize.height;

                        //根据ViewFinderView和preview的尺寸之比，缩放扫码区域
                        Rect rect = getScaledRect(previewWidth, previewHeight);

                        /*
                         * 方案一：旋转图像数据
                         */
                        //int rotationCount = getRotationCount();//相机图像需要被顺时针旋转几次（每次90度）
                        //if (rotationCount == 1 || rotationCount == 3) {//相机图像需要顺时针旋转90度或270度
                        //    //交换宽高
                        //    int tmp = previewWidth;
                        //    previewWidth = previewHeight;
                        //    previewHeight = tmp;
                        //}
                        ////旋转数据
                        //data = rotateData(data, camera);

                        /*
                         * 方案二：旋转截取区域
                         */
                        rect = getRotatedRect(previewWidth, previewHeight, rect);

                        //从preView的图像中截取扫码区域
                        Image barcode = new Image(previewWidth, previewHeight, "Y800");
                        barcode.setData(data);
                        barcode.setCrop(rect.left, rect.top, rect.width(), rect.height());

                        //使用zbar库识别扫码区域
                        int result = imageScanner.scanImage(barcode);
                        if (result != 0) {//识别成功
                            SymbolSet syms = imageScanner.getResults();
                            String symData = null;
                            for (Symbol sym : syms) {
                                // In order to retreive QR codes containing null bytes we need to
                                // use getDataBytes() rather than getData() which uses C strings.
                                // Weirdly ZBar transforms all data to UTF-8, even the data returned
                                // by getDataBytes() so we have to decode it as UTF-8.
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                    resultStr = new String(sym.getDataBytes(), StandardCharsets.UTF_8);
                                } else {
                                    resultStr = sym.getData();
                                }
                                if (!TextUtils.isEmpty(resultStr)) {
                                    Log.d(TAG, "zbar解析成功，内容: " + resultStr + " 格式：" + BarcodeFormat.getFormatById(sym.getType()));
                                    break;//识别成功一个就跳出循环
                                }
                            }
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                    synchronized (CameraPreview.class) {
                        if (!TextUtils.isEmpty(resultStr) && !scanSuccess){
                            scanSuccess = true;
                            scanSuccess(resultStr);
                            return;
                        }
                        if (!scanSuccess) {//还没有扫描成功
                            zbarAllowAnalysis = true;
                            getOneMoreFrame();//再获取一帧图像数据进行识别（会再次触发onPreviewFrame方法）
                        }
                    }
                }
            });
        }
    }

    private void scanSuccess(String resultStr) {
        stopScan();
        Log.d(TAG, "scanSuccess: " + resultStr);
    }

    /**
     * 创建ImageScanner并进行基本设置（如支持的码格式）
     */
    public void setupScanner() {
        imageScanner = new ImageScanner();

        imageScanner.setConfig(0, Config.X_DENSITY, 3);
        imageScanner.setConfig(0, Config.Y_DENSITY, 3);

        imageScanner.setConfig(Symbol.NONE, Config.ENABLE, 0);

        for (BarcodeFormat format : getFormats()) {//设置支持的码格式
            imageScanner.setConfig(format.getId(), Config.ENABLE, 1);
        }
    }

    public Rect getRotatedRect(int previewWidth, int previewHeight, Rect rect) {
        if (rotatedRect == null) {
            int rotationCount = getRotationCount();
            rotatedRect = new Rect(rect);

            if (rotationCount == 1) {//若相机图像需要顺时针旋转90度，则将扫码框逆时针旋转90度
                rotatedRect.left = rect.top;
                rotatedRect.top = previewHeight - rect.right;
                rotatedRect.right = rect.bottom;
                rotatedRect.bottom = previewHeight - rect.left;
            } else if (rotationCount == 2) {//若相机图像需要顺时针旋转180度,则将扫码框逆时针旋转180度
                rotatedRect.left = previewWidth - rect.right;
                rotatedRect.top = previewHeight - rect.bottom;
                rotatedRect.right = previewWidth - rect.left;
                rotatedRect.bottom = previewHeight - rect.top;
            } else if (rotationCount == 3) {//若相机图像需要顺时针旋转270度，则将扫码框逆时针旋转270度
                rotatedRect.left = previewWidth - rect.bottom;
                rotatedRect.top = rect.left;
                rotatedRect.right = previewWidth - rect.top;
                rotatedRect.bottom = rect.right;
            }
        }

        return rotatedRect;
    }

    /**
     * 再获取一帧图像数据进行识别（会再次触发onPreviewFrame方法）
     */
    public void getOneMoreFrame() {
        if (camera != null) {
            camera.setOneShotPreviewCallback(this);
        } else {
            Log.d(TAG, "getOneMoreFrame: " + camera);
        }
    }

    /**
     * 根据特征点自动变焦
     */
    private void autoZoomIfNeed(FoundPartException e, int cropWidth, int cropHeight) {
        if (!auto_zoom) {
            return;
        }
        List<ResultPoint> p = e.getFoundPoints();
        //这里是解析到了特征码但是不全，如果特征点有两个则执行变焦放大
        if (p.size() > 1) {
            Log.d(TAG, "解析到特征点个数-->: " + p.size());
            DetectorResult detectorResult = null;
            //计算扫描框中的二维码的宽度，两点间距离公式
            float point1X = p.get(0).getX();
            float point1Y = p.get(0).getY();
            float point2X = p.get(1).getX();
            float point2Y = p.get(1).getY();
            int len = (int) Math.sqrt(Math.pow(Math.abs(point1X - point2X), 2) + Math.pow(Math.abs(point1Y - point2Y), 2));
            Log.d(TAG, "mAnalysisTask,  len  = " + len + " ,  cropWidth  = " + cropWidth
                    + " point1X : " + point1X + "  point1Y : " + point1Y + "  point2X : " + point2X + " point2Y : " + point2Y);
            if (len < cropWidth / 4 && len > 20) {
                if (camera != null) {
                    Camera.Parameters parameters = camera.getParameters();
                    if (parameters.isZoomSupported()) {
                        cameraZoom();
                    }
                }
            }
        }
    }

    /**
     * 相机设置焦距
     */
    public synchronized void cameraZoom() {
        if (System.currentTimeMillis() - scaleZoom < 1000) {
            //限制一秒内只缩放一次
            return;
        }
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            if (!parameters.isZoomSupported()) {
                return;
            }
            int maxZoom = parameters.getMaxZoom();
            if (maxZoom == 0) {
                return;
            }
            int stepZoom = maxZoom / 10;
            int newZoom = parameters.getZoom() + stepZoom;
            if (newZoom > parameters.getMaxZoom()) {
                newZoom = maxZoom;
            }
            parameters.setZoom(newZoom);
            scaleZoom = System.currentTimeMillis();
            camera.setParameters(parameters);
        }
    }

    /**
     * 根据ViewFinderView和preview的尺寸之比，缩放扫码区域
     */
    public Rect getScaledRect(int previewWidth, int previewHeight) {
        if (scaledRect == null && viewFinderView != null) {
            Rect framingRect = viewFinderView.getFramingRect();//获得扫码框区域
            int viewFinderViewWidth = ((View) viewFinderView).getWidth();
            int viewFinderViewHeight = ((View) viewFinderView).getHeight();

            int width, height;
            if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT//竖屏使用
                    && previewHeight < previewWidth) {
                width = previewHeight;
                height = previewWidth;
            } else if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_LANDSCAPE//横屏使用
                    && previewHeight > previewWidth) {
                width = previewHeight;
                height = previewWidth;
            } else {
                width = previewWidth;
                height = previewHeight;
            }

            scaledRect = new Rect(framingRect);
            scaledRect.left = scaledRect.left * width / viewFinderViewWidth;
            scaledRect.right = scaledRect.right * width / viewFinderViewWidth;
            scaledRect.top = scaledRect.top * height / viewFinderViewHeight;
            scaledRect.bottom = scaledRect.bottom * height / viewFinderViewHeight;
        }

        return scaledRect;
    }

    private static Point getScreenResolution(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point screenResolution = new Point();
        display.getSize(screenResolution);
        return screenResolution;
    }

    /**
     * 是否为竖屏
     */
    private static boolean isPortrait(Context context) {
        Point screenResolution = getScreenResolution(context);
        return screenResolution.y > screenResolution.x;
    }

    /**
     * 设置支持的码格式
     */
    public void setFormats(@NonNull List<BarcodeFormat> formats) {
        this.formats = formats;
        setupScanner();
    }

    public Collection<BarcodeFormat> getFormats() {
        if (formats == null) {
            return BarcodeFormat.CUSTOM_FORMATS;
        }
        return formats;
    }

    private Camera.Parameters getParameters() {
        if (camera != null) {
            return camera.getParameters();
        }
        return null;
    }
}
