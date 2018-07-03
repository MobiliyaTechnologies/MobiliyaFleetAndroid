package com.mobiliya.fleet.customViews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import com.mobiliya.fleet.R;

public class GradientTextView extends android.support.v7.widget.AppCompatTextView {

    public GradientTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Black.ttf"));
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        //Setting the gradient if layout is changed
        if (changed) {
            getPaint().setShader(new LinearGradient(0, 0, getWidth(), getHeight(),
                    ContextCompat.getColor(getContext(), R.color.dashboard_header_start_color),
                    ContextCompat.getColor(getContext(), R.color.dashboard_header_end_color),
                    Shader.TileMode.REPEAT));
        }
    }
}
