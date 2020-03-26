package com.github.sl.widget.round;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

/**
 * @package: com.mirstone.baselib.widget.round
 * @fileName: RoundLinearLayout
 * @data: 2018/8/14 14:52
 * @author: ShiLiang
 * @describe:
 */
public class RoundLinearLayout extends LinearLayout {
    private BaseRoundView baseFilletView;

    public RoundLinearLayout(Context context) {
        super(context);
        init(null);
    }

    public RoundLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public RoundLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        baseFilletView = new BaseRoundView(this, attrs);
    }

    @Override
    public void draw(Canvas canvas) {
        baseFilletView.draw(canvas);
        super.draw(canvas);
        baseFilletView.drawOff(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        baseFilletView.onSizeChanged(w, h);
    }
}
