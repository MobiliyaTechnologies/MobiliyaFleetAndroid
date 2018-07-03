package com.mobiliya.fleet.customViews;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

/**
 * Created by Kunal on 24-Feb-18.
 */

public class CustomEditTextMedium extends android.support.v7.widget.AppCompatEditText {
    public CustomEditTextMedium(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Medium.ttf"));
    }
}
