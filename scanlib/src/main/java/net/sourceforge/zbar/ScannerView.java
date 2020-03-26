package net.sourceforge.zbar;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.sl.scanlib.widget.IViewFinder;
import com.google.zxing.FoundPartException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.DetectorResult;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.bertsir.zbar.utils.QRUtils;
import net.sourceforge.base.base.BarcodeScannerView;

/**
 * zbar扫码视图，继承自基本扫码视图BarcodeScannerView
 * <p>
 * BarcodeScannerView内含CameraPreview（相机预览）和ViewFinderView（扫码框、阴影遮罩等）
 */
public class ScannerView extends BarcodeScannerView {
    private static final String TAG = "ZBarScannerView";
    private ImageScanner imageScanner;
    private List<BarcodeFormat> formats;
    private ResultHandler resultHandler;

    private boolean zxingAllowAnalysis = true;
    private boolean zbarAllowAnalysis = true;
    private boolean scanSuccess = false;

    private ExecutorService zxingExecutorService = Executors.newSingleThreadExecutor();
    private ExecutorService zbarExecutorService = Executors.newSingleThreadExecutor();
    private long scaleZoom;

    private boolean auto_zoom = false;

    public interface ResultHandler {
        void handleResult(String rawResult);
    }

    @Override
    public synchronized void stopCamera() {
        scanSuccess = true;
        super.stopCamera();
    }

    public void setAuto_zoom(boolean auto_zoom) {
        this.auto_zoom = auto_zoom;
    }

    @Override
    public synchronized void startCamera() {
        scanSuccess = false;
        zxingAllowAnalysis = true;
        zbarAllowAnalysis = true;
        super.startCamera();
    }

    /*
     * 加载zbar动态库
     * zbar.jar中的类会用到
     */
    static {
        System.loadLibrary("iconv");
    }

    public ScannerView(@NonNull Context context, @NonNull IViewFinder viewFinderView, @Nullable ResultHandler resultHandler) {
        super(context, viewFinderView);
        this.resultHandler = resultHandler;
        setupScanner();//创建ImageScanner（zbar扫码器）并进行基本设置（如支持的码格式）
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

    /**
     * Called as preview frames are displayed.<br/>
     * This callback is invoked on the event thread open(int) was called from.<br/>
     * (此方法与Camera.open运行于同一线程，在本项目中，就是CameraHandlerThread线程)
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (resultHandler == null) {
            return;
        }
        if (scanSuccess) {
            return;
        }
        if (zxingAllowAnalysis || zbarAllowAnalysis) {
            decodeByZxing(data);
            decodeByZbar(data);
        }

    }

    private void decodeByZxing(final byte[] mData) {
        if (zxingAllowAnalysis){
            zxingAllowAnalysis = false;
            zxingExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    int width = 0;
                    int height = 0;
                    byte[] data = mData;
                    Camera.Parameters parameters = getParameters();
                    if (parameters == null){
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
                    synchronized (ScannerView.class) {
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

    private synchronized Camera.Parameters getParameters() {
        Camera camera = getCamera();
        if (camera == null){
            return null;
        }
        return camera.getParameters();
    }

    /**
     * 根据特征点自动变焦
     */
    private void autoZoomIfNeed(FoundPartException e, int cropWidth, int cropHeight) {
        if (!auto_zoom){
            return;
        }
        List<ResultPoint> p = e.getFoundPoints();
        //这里是解析到了特征码但是不全，如果特征点有两个则执行变焦放大
        if(p.size() > 1){
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
                Camera camera = getCamera();
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
    public synchronized void cameraZoom(){
        if (System.currentTimeMillis() - scaleZoom < 1000){
            //限制一秒内只缩放一次
            return;
        }
        Camera mCamera = getCamera();
        if(mCamera != null){
            Camera.Parameters parameters = mCamera.getParameters();
            if(!parameters.isZoomSupported()){
                return;
            }
            int maxZoom = parameters.getMaxZoom();
            if(maxZoom == 0){
                return;
            }
            int stepZoom = maxZoom / 10;
            int newZoom = parameters.getZoom() + stepZoom;
            if(newZoom > parameters.getMaxZoom()){
                newZoom = maxZoom;
            }
            parameters.setZoom(newZoom);
            scaleZoom = System.currentTimeMillis();
            mCamera.setParameters(parameters);
        }
    }


    /**
     * 是否为竖屏
     */
    public static boolean isPortrait(Context context) {
        Point screenResolution = getScreenResolution(context);
        return screenResolution.y > screenResolution.x;
    }

    static Point getScreenResolution(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point screenResolution = new Point();
        display.getSize(screenResolution);
        return screenResolution;
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
                    synchronized (ScannerView.class) {
                        if (!TextUtils.isEmpty(resultStr) && !scanSuccess){
                            scanSuccess = true;
                            scanSuccess(resultStr);
                            stopCamera();
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
//        zxingExecutorService.shutdownNow();
//        zbarExecutorService.shutdownNow();
        if (resultHandler != null) {
            resultHandler.handleResult(resultStr);
        }
    }


//--------------------------------------------------------------------------------------------------

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

    /**
     * 再获取一帧图像数据进行识别（会再次触发onPreviewFrame方法）
     */
    public void getOneMoreFrame() {
        if (cameraWrapper != null) {
            cameraWrapper.camera.setOneShotPreviewCallback(this);
        }else {
            Log.d(TAG, "getOneMoreFrame: " + cameraWrapper);
        }
    }
}