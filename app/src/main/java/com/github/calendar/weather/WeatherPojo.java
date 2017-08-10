package com.github.calendar.weather;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;

import java.util.HashMap;

import com.github.calendar.R;

public class WeatherPojo {

    private static final int SPEC_SIZE = 6;

    public DayInfo today = null;
    public DayInfo tomorrow = null;

    public WeatherPojo(String[] todayData, String[] tomorrowData) {
        if (todayData.length == SPEC_SIZE) {
            today = new DayInfo(todayData);
        }
        if (tomorrowData.length == SPEC_SIZE) {
            tomorrow = new DayInfo(tomorrowData);
        }
    }

    public static class DayInfo {

        public final WeatherInfo morning = new WeatherInfo();
        public final WeatherInfo afternoon = new WeatherInfo();
        public final WeatherInfo night = new WeatherInfo();

        DayInfo(String[] specs) {
            int index = 0;
            morning.icon = specs[index++];
            morning.temperature = toTemperature(specs[index++]);
            afternoon.icon = specs[index++];
            afternoon.temperature = toTemperature(specs[index++]);
            night.icon = specs[index++];
            night.temperature = toTemperature(specs[index]);
        }

        private Float toTemperature(String temperature) {
            if (TextUtils.isEmpty(temperature)) {
                return null;
            }
            try {
                return Float.valueOf(temperature);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    public static class WeatherInfo {
        private static final HashMap<String, Integer> ICON_MAP = new HashMap<>();
        private static final String ICON_CLEAR_DAY = "clear-day";
        private static final String ICON_CLEAR_NIGHT = "clear-night";
        private static final String ICON_CLOUDY = "cloudy";
        private static final String ICON_RAIN = "rain";
        private static final String ICON_SNOW = "snow";
        private static final String ICON_WIND = "wind";
        private static final String ICON_PARTLY_CLOUDY_DAY = "partly-cloudy-day";
        private static final String ICON_PARTLY_CLOUDY_NIGHT = "partly-cloudy-night";

        static {
            ICON_MAP.put(ICON_CLEAR_DAY, R.drawable.icon_clear_day);
            ICON_MAP.put(ICON_CLEAR_NIGHT, R.drawable.icon_clear_night);
            ICON_MAP.put(ICON_CLOUDY, R.drawable.icon_cloudy);
            ICON_MAP.put(ICON_PARTLY_CLOUDY_DAY, R.drawable.icon_partly_cloudy_day);
            ICON_MAP.put(ICON_PARTLY_CLOUDY_NIGHT, R.drawable.icon_partly_cloudy_night);
            ICON_MAP.put(ICON_RAIN, R.drawable.icon_rain);
            ICON_MAP.put(ICON_SNOW, R.drawable.icon_snow);
            ICON_MAP.put(ICON_WIND, R.drawable.icon_wind);
        }
        String icon;
        public Float temperature;

        public Drawable getIcon(Context context, int tint) {
            if (TextUtils.isEmpty(icon)) {
                return null;
            }
            int drawableResId = R.drawable.icon_cloudy;
            if (ICON_MAP.containsKey(icon)) {
                drawableResId = ICON_MAP.get(icon);
            }
            Drawable drawable = ContextCompat.getDrawable(context, drawableResId);
            drawable = DrawableCompat.wrap(drawable);
            //DrawableCompat.setTint(drawable, tint);
            return drawable;
        }
    }
}
