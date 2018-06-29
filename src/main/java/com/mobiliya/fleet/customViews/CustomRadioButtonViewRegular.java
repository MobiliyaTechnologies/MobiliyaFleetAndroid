package com.mobiliya.fleet.customViews;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

/**
 * Created by Kunal on 24-Feb-18.
 */

@SuppressWarnings({"ALL", "unused"})
public class CustomRadioButtonViewRegular extends android.support.v7.widget.AppCompatRadioButton {
    public CustomRadioButtonViewRegular(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Regular.ttf"));
    }
}