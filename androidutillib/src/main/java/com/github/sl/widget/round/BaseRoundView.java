package com.github.sl.widget.round;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;

import com.github.androidutillib.R;


/**
 * @package: com.mirstone.baselib.widget.round
 * @fileName: BaseRoundView
 * @data: 2018/8/14 14:43
 * @author: ShiLiang
 * @describe: 实现圆角
 */
public class BaseRoundView {
    private View view;
    private float[] radio = new float[8];   // top-left, top-right, bottom-right, bottom-left
    private Path clipPath;                 // 剪裁区域路径
    private Path clipReversalPath;        //需要裁剪的反区域
    private Paint paint;                   // 画笔
    private boolean roundAsCircle;           // 圆形
    private int strokeColor;               // 描边颜色
    private int strokeWidth;               // 描边半径
    private Region areaRegion;             // 内容区域

    public BaseRoundView(View view, AttributeSet attributeSet) {
        this.view = view;
        TypedArray ta = view.getContext().obtainStyledAttributes(attributeSet, R.styleable.fillet_view);
        roundAsCircle = ta.getBoolean(R.styleable.fillet_view_round_as_circle, false);
        strokeColor = ta.getColor(R.styleable.fillet_view_stroke_color, Color.WHITE);
        strokeWidth = ta.getDimensionPixelSize(R.styleable.fillet_view_stroke_width, 0);
        int roundCorner = ta.getDimensionPixelSize(R.styleable.fillet_view_round_corner, 0);
        int roundCornerTopLeft = ta.getDimensionPixelSize(
                R.styleable.fillet_view_round_corner_top_left, roundCorner);
        int roundCornerTopRight = ta.getDimensionPixelSize(
                R.styleable.fillet_view_round_corner_top_right, roundCorner);
        int roundCornerBottomLeft = ta.getDimensionPixelSize(
                R.styleable.fillet_view_round_corner_bottom_left, roundCorner);
        int roundCornerBottomRight = ta.getDimensionPixelSize(
                R.styleable.fillet_view_round_corner_bottom_right, roundCorner);
        radio[0] = roundCornerTopLeft;
        radio[1] = roundCornerTopLeft;
        radio[2] = roundCornerTopRight;
        radio[3] = roundCornerTopRight;
        radio[4] = roundCornerBottomRight;
        radio[5] = roundCornerBottomRight;
        radio[6] = roundCornerBottomLeft;
        radio[7] = roundCornerBottomLeft;
        clipPath = new Path();
        clipReversalPath = new Path();
        areaRegion = new Region();
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        ta.recycle();//为了保持以后使用的一致性，需要回收
    }

    public void onSizeChanged(int w, int h) {
        RectF areas = new RectF();
        areas.left = 0;
        areas.top = 0;
        areas.right = w;
        areas.bottom = h;
        clipPath.reset();
        if (roundAsCircle) {
            float d = areas.width() >= areas.height() ? areas.height() : areas.width();
            float r = d / 2;
            PointF center = new PointF(w / 2, h / 2);
            clipPath.addCircle(center.x, center.y, r, Path.Direction.CW);
            int mEdgeFix = 10;
            clipPath.moveTo(-mEdgeFix, -mEdgeFix);  // 通过空操作让Path区域占满画布
            clipPath.moveTo(w + mEdgeFix, h + mEdgeFix);
        } else {
            clipPath.addRoundRect(areas, radio, Path.Direction.CW);
        }


        Region clip = new Region((int) areas.left, (int) areas.top,
                (int) areas.right, (int) areas.bottom);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            clipReversalPath.addRect(areas, Path.Direction.CW );
            clipReversalPath.op(clipPath, Path.Op.DIFFERENCE);
            areaRegion.setPath(clipReversalPath, clip);
        }else {
            areaRegion.setPath(clipPath, clip);
        }
    }

    public void draw(Canvas canvas) {
        canvas.saveLayer(new RectF(0, 0, canvas.getWidth(), canvas.getHeight()), null, Canvas
                .ALL_SAVE_FLAG);
    }

    public void drawOff(Canvas canvas) {
        if (strokeWidth > 0) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
            paint.setStrokeWidth(strokeWidth * 2);
            paint.setColor(strokeColor);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(clipPath, paint);
        }
        //Android9.0 DST_IN 无效问题
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            canvas.drawPath(clipReversalPath, paint);
        }else {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            canvas.drawPath(clipPath, paint);
        }
        canvas.restore();
    }

    public Path getClipPath() {
        return clipPath;
    }

    private float getF() {
        return 1.0f - (float) Math
                .round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f,
                        view.getResources().getDisplayMetrics()))
                / (float) view.getWidth();
    }

    public boolean isRoundAsCircle() {
        return roundAsCircle;
    }

    public void setRoundAsCircle(boolean mRoundAsCircle) {
        this.roundAsCircle = mRoundAsCircle;
    }

    public int getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(@ColorInt int mStrokeColor) {
        this.strokeColor = mStrokeColor;
        invalidate();
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(int mStrokeWidth) {
        this.strokeWidth = mStrokeWidth;
        invalidate();
    }

    public float getRoundCornerTopLeft() {
        return radio[0];
    }

    public void setRoundCornerTopLeft(int roundCornerTopLeft) {
        radio[0] = roundCornerTopLeft;
        radio[1] = roundCornerTopLeft;
        invalidate();
    }

    public void setRoundCorner(int roundCorner) {
        radio[0] = roundCorner;
        radio[1] = roundCorner;
        radio[2] = roundCorner;
        radio[3] = roundCorner;
        radio[4] = roundCorner;
        radio[5] = roundCorner;
        radio[6] = roundCorner;
        radio[7] = roundCorner;
        invalidate();
    }

    public float getRoundCornerTopRight() {
        return radio[2];
    }

    public void setRoundCornerTopRight(int roundCornerTopRight) {
        radio[2] = roundCornerTopRight;
        radio[3] = roundCornerTopRight;
        invalidate();
    }

    public float getRoundCornerBottomLeft() {
        return radio[6];
    }

    public void setRoundCornerBottomLeft(int roundCornerBottomLeft) {
        radio[6] = roundCornerBottomLeft;
        radio[7] = roundCornerBottomLeft;
        invalidate();
    }

    public float getRoundCornerBottomRight() {
        return radio[4];
    }

    public void setRoundCornerBottomRight(int roundCornerBottomRight) {
        radio[4] = roundCornerBottomRight;
        radio[5] = roundCornerBottomRight;
        invalidate();
    }

    private void invalidate() {
        view.invalidate();
    }
}
