package com.github.calendar.weather;

import android.Manifest;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.Toast;

import java.io.IOException;
import java.util.Calendar;

import com.github.calendar.BuildConfig;
import com.github.calendar.CalendarUtils;
import com.github.calendar.R;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class WeatherService extends IntentService {

    public static final String PREF_WEATHER_TODAY = "weatherToday";
    public static final String PREF_WEATHER_TOMORROW = "weatherTomorrow";
    public static final String PREF_WEATHER_ENABLED = "weatherEnabled";
    public static final String TAG = WeatherService.class.getName();
    private static final int[] HOUR_INDICES = new int[]{8, 14, 20};
    private static final String SEPARATOR = "|";
    private static final String EXTRA_ACTIVE = "extra:active";

    private ForecastIOService mForecastService;

    public static WeatherPojo getSyncedWeather(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String[] today =unpack(sp.getString(PREF_WEATHER_TODAY, null)),
                tomorrow = unpack(sp.getString(PREF_WEATHER_TOMORROW, null));
        if (today == null || tomorrow == null) {
            Toast.makeText(context, R.string.updating_weather, Toast.LENGTH_SHORT).show();
            context.startService(new Intent(context, WeatherService.class)
                    .putExtra(EXTRA_ACTIVE, true));
            return null;
        } else {
            return new WeatherPojo(today, tomorrow);
        }
    }

    private static String pack(ForecastIOService.Forecast forecast) {
        if (forecast == null || forecast.hourly == null ||
                forecast.hourly.data == null || forecast.hourly.data.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < HOUR_INDICES.length; i++) {
            int hourIndex = HOUR_INDICES[i];
            if (hourIndex >= forecast.hourly.data.length ||
                    forecast.hourly.data[hourIndex] == null) {
                sb.append(SEPARATOR);
            } else {
                sb.append(forecast.hourly.data[hourIndex].icon)
                        .append(SEPARATOR)
                        .append(forecast.hourly.data[hourIndex].temperature);
            }
            if (i < HOUR_INDICES.length - 1) {
                sb.append(SEPARATOR);
            }
        }
        return sb.toString();
    }

    private static String[] unpack(String string) {
        if (TextUtils.isEmpty(string)) {
            return null;
        }
        return string.split("\\" + WeatherService.SEPARATOR, -1);
    }

    public WeatherService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        cancelScheduledAlarm();
        boolean enabled = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(PREF_WEATHER_ENABLED, false);
        if (!enabled) {
            persist(null, PREF_WEATHER_TODAY);
            persist(null, PREF_WEATHER_TOMORROW);
            return;
        }
        Location location = getLocation();
        if (location == null && intent.getBooleanExtra(EXTRA_ACTIVE, false)) {
            notifyLocationError();
        }
        long todaySeconds = CalendarUtils.today() / DateUtils.SECOND_IN_MILLIS,
                tomorrowSeconds = todaySeconds + DateUtils.DAY_IN_MILLIS / DateUtils.SECOND_IN_MILLIS;
        persist(fetchForecast(location, todaySeconds), PREF_WEATHER_TODAY);
        persist(fetchForecast(location, tomorrowSeconds), PREF_WEATHER_TOMORROW);
        scheduleAlarm();
    }

    private void cancelScheduledAlarm() {
        PendingIntent alarmIntent;
        if ((alarmIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(this, WeatherAlarmReceiver.class), PendingIntent.FLAG_NO_CREATE)) != null) {
            ((AlarmManager) getSystemService(ALARM_SERVICE)).cancel(alarmIntent);
        }
    }

    private void scheduleAlarm() {
        ((AlarmManager) getSystemService(ALARM_SERVICE)).set(AlarmManager.RTC_WAKEUP,
                Calendar.getInstance().getTimeInMillis() + AlarmManager.INTERVAL_DAY,
                PendingIntent.getBroadcast(this, 0,
                        new Intent(this, WeatherAlarmReceiver.class), 0));
    }

    private void notifyLocationError() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(WeatherService.this, R.string.error_location,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected Location getLocation() {
        Location location = null;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            location = ((LocationManager) getSystemService(LOCATION_SERVICE))
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (location == null && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            location = ((LocationManager) getSystemService(LOCATION_SERVICE))
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        return location;
    }

    protected ForecastIOService getForecastService() {
        if (mForecastService == null) {
            mForecastService = new Retrofit.Builder()
                    .baseUrl(ForecastIOService.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ForecastIOService.class);
        }
        return mForecastService;
    }

    private ForecastIOService.Forecast fetchForecast(Location location, long timeSeconds) {
        if (location == null) {
            return null;
        }
        try {
            return getForecastService()
                    .forecast(location.getLatitude(), location.getLongitude(), timeSeconds)
                    .execute()
                    .body();
        } catch (IOException e) {
            return null;
        }
    }

    private void persist(ForecastIOService.Forecast forecast, String preferenceKey) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(preferenceKey, pack(forecast))
                .apply();
    }

    interface ForecastIOService {
        String BASE_URL = "https://api.forecast.io/";

        @GET("forecast/" + BuildConfig.FORECAST_IO_API_KEY +
                "/{latitude},{longitude},{time}?exclude=currently,daily,flags")
        Call<Forecast> forecast(@Path("latitude") double latitude,
                                @Path("longitude") double longitude,
                                @Path("time") long timeSeconds);

        class Forecast {
            Hourly hourly;
        }

        class Hourly {
            DataPoint[] data;
        }

        class DataPoint {
            String icon;
            float temperature;
        }
    }
}
