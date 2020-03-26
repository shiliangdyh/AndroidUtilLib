package cn.bertsir.zbar.view;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.sl.scanlib.R;

import java.util.ArrayList;

import cn.bertsir.zbar.QrConfig;
import sourceforge.base.base.IViewFinder;


/**
 * Created by Bert on 2017/9/20.
 */

public class ScanView extends FrameLayout implements IViewFinder {
    private static final String TAG = "ScanView";

    private Rect framingRect;//扫码框所占区域
//    private float widthRatio = 0.86f;//扫码框宽度占view总宽度的比例
//    private float heightWidthRatio = 0.6f;//扫码框的高宽比
    private int leftOffset = -1;//扫码框相对于左边的偏移量，若为负值，则扫码框会水平居中
    private int topOffset = -1;//扫码框相对于顶部的偏移量，若为负值，则扫码框会竖直居中
    private LineView iv_scan_line;
    private TranslateAnimation animation;
    private FrameLayout fl_scan;
    private int CURRENT_TYEP = 1;
    private CornerView cnv_left_top;
    private CornerView cnv_left_bottom;
    private CornerView cnv_right_top;
    private CornerView cnv_right_bottom;
    private ArrayList<CornerView> cornerViews;
    private int line_speed = 3000;
    private boolean isOnlyCenter = true;
    private TextView tv_desc;

    public ScanView(Context context) {
        super(context);
        initView(context);
    }

    public ScanView(Context context,AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ScanView(Context context,AttributeSet attrs,int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public void setOnlyCenter(boolean onlyCenter) {
        isOnlyCenter = onlyCenter;
    }

    private void initView(Context mContext){
        View scan_view = View.inflate(mContext, R.layout.view_scan, this);

        cnv_left_top = (CornerView) scan_view.findViewById(R.id.cnv_left_top);
        cnv_left_bottom = (CornerView) scan_view.findViewById(R.id.cnv_left_bottom);
        cnv_right_top = (CornerView) scan_view.findViewById(R.id.cnv_right_top);
        cnv_right_bottom = (CornerView) scan_view.findViewById(R.id.cnv_right_bottom);
        tv_desc = ((TextView) scan_view.findViewById(R.id.tv_des));

        cornerViews = new ArrayList<>();
        cornerViews.add(cnv_left_top);
        cornerViews.add(cnv_left_bottom);
        cornerViews.add(cnv_right_top);
        cornerViews.add(cnv_right_bottom);

        iv_scan_line = (LineView) scan_view.findViewById(R.id.iv_scan_line);

        fl_scan = (FrameLayout) scan_view.findViewById(R.id.fl_scan);
        fl_scan.post(new Runnable() {
            @Override
            public void run() {
                updateFramingRect();
            }
        });

        animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation
                .RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                0.9f);
        animation.setDuration(line_speed);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
    }

    public void setLineSpeed(int speed){
        animation.setDuration(speed);
    }

    public void startScan(){
        iv_scan_line.startAnimation(animation);
    }

    public void onPause(){
        if (iv_scan_line != null) {
            iv_scan_line.clearAnimation();
            iv_scan_line.setVisibility(View.GONE);
        }
    }

    public void onResume(){
        if (iv_scan_line != null) {
            iv_scan_line.setVisibility(View.VISIBLE);
            iv_scan_line.startAnimation(animation);
        }
    }

    public void setType(int type){
        CURRENT_TYEP = type;
        LinearLayout.LayoutParams fl_params = (LinearLayout.LayoutParams) fl_scan.getLayoutParams();
        if(CURRENT_TYEP == QrConfig.SCANVIEW_TYPE_QRCODE){
//            fl_params.width = dip2px(250);
//            fl_params.height = dip2px(250);
        }else if(CURRENT_TYEP == QrConfig.SCANVIEW_TYPE_BARCODE){
            fl_params.width = dip2px(300);
            fl_params.height = dip2px(100);
        }
        fl_scan.setLayoutParams(fl_params);
    }

    public void setCornerColor(int color){
        for (int i = 0; i < cornerViews.size(); i++) {
            cornerViews.get(i).setColor(color);
        }
    }

    public void setCornerWidth(int dp){
        for (int i = 0; i < cornerViews.size(); i++) {
            cornerViews.get(i).setLineWidth(dp);
        }
    }

    public void setLineColor(int color){
        iv_scan_line.setLinecolor(color);
    }

    public int dip2px(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5);
    }

    public int getScreenWidth() {
        WindowManager wm = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);

        int width = wm.getDefaultDisplay().getWidth();
        return width;
    }

    public int getScreenHeight() {
        WindowManager wm = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);
        int height = wm.getDefaultDisplay().getHeight();
        return height;
    }

//    @Override
//    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
//        updateFramingRect();
//    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateFramingRect();
    }

    /**
     * 设置framingRect的值（扫码框所占的区域）
     */
    public synchronized void updateFramingRect() {
        Point viewSize = new Point(getWidth(), getHeight());
        if (!isOnlyCenter)
        {
            if (getWidth() != 0 && getHeight() != 0){
                framingRect = new Rect(0, 0, getWidth(), getHeight());
            }
            return;
        }
        int width, height;
        width = fl_scan.getWidth();
        height = fl_scan.getHeight();

        if (width == 0 || height == 0 ){
            return;
        }

        int left, top;
        if (leftOffset < 0) {
            left = (viewSize.x - width) / 2;//水平居中
        } else {
            left = leftOffset;
        }
        if (topOffset < 0) {
            top = (viewSize.y - height) / 2;//竖直居中
        } else {
            top = topOffset;
        }
        framingRect = new Rect(left, top, left + width, top + height);
    }


    @Override
    public Rect getFramingRect() {
        return framingRect;
    }

    public void setIsShowDesc(boolean show_des) {
        tv_desc.setVisibility(show_des ? View.VISIBLE : View.GONE);
    }

    public void setDesc(String des_text) {
        tv_desc.setText(des_text);
    }
}
