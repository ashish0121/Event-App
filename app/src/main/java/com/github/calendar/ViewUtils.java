package com.github.calendar;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.content.ContextCompat;

public class ViewUtils {

    public static int[] getCalendarColors(Context context) {
        int transparentColor = ContextCompat.getColor(context, android.R.color.transparent);
        TypedArray ta = context.getResources().obtainTypedArray(R.array.calendar_colors);
        int[] colors;
        if (ta.length() > 0) {
            colors = new int[ta.length()];
            for (int i = 0; i < ta.length(); i++) {
                colors[i] = ta.getColor(i, transparentColor);
            }
        } else {
            colors = new int[]{transparentColor};
        }
        ta.recycle();
        return colors;
    }
}
