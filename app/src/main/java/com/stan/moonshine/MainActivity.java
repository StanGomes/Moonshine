package com.stan.moonshine;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private CurrentWeather mCurrentWeather;
    @BindView(R.id.time_textView) TextView mTimeView;
    @BindView(R.id.temp_textView) TextView mTempView;
    @BindView(R.id.icon_imageView) ImageView mIconView;
    @BindView(R.id.windspeed_value) TextView mWindspeedView;
    @BindView(R.id.precip_textView) TextView mPrecipView;
    @BindView(R.id.summary_textView) TextView mSummaryView;
    @BindView(R.id.refresh_imageView) ImageView mRefreshView;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        final double latitude = 45.3167088;
        final double longitude = -75.83175890000001;

        mRefreshView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast(latitude,longitude);
            }
        });

        getForecast(latitude,longitude);
    }

    private void getForecast(double latitude, double longitude) {
        Resources resources = getResources();
        String api = resources.getString(R.string.api_key);
        String forecastUrl = "https://api.darksky.net/forecast/"+ api +"/"+ latitude +","+longitude+"?units=si";

        if (isNetworkAvailable()) {

            toggleVisibility();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleVisibility();
                        }
                    });
                    showErrorDialog();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleVisibility();
                        }
                    });
                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mCurrentWeather = getCurrentDetails(jsonData);
                            //runOnUiThread is used to switch from the background async thread to the main ui thread to update the views.
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateViews();
                                }
                            });
                        } else {
                            showErrorDialog();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                    catch (JSONException e){
                        Log.e(TAG,"Exception caught ", e);
                    }
                }
            });
        }
        else {
            Toast.makeText(this,"Network is unavailable!",Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleVisibility() {
        if (mProgressBar.getVisibility()== View.INVISIBLE){
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshView.setVisibility(View.INVISIBLE);
        }
        else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshView.setVisibility(View.VISIBLE);
        }

    }

    private void updateViews() {
        mTempView.setText(mCurrentWeather.getTemp() +"");
        mTimeView.setText("The weather at "+mCurrentWeather.getFormattedTime()+" is");
        //mTimeView.setText(String.format(mCurrentWeather.getFormattedTime(), R.string.time_string));
        mWindspeedView.setText(mCurrentWeather.getWindSpeed()+" km/h");
        mPrecipView.setText(mCurrentWeather.getPrecipChance()+"%");
        mSummaryView.setText(mCurrentWeather.getSummary());

        Drawable drawable = getResources().getDrawable(mCurrentWeather.getIconId());
        mIconView.setImageDrawable(drawable);

    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {

        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        Log.i(TAG,"From JSON: "+timezone);
        JSONObject currently = forecast.getJSONObject("currently");

        CurrentWeather currentWeather = new CurrentWeather();

        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setWindSpeed(currently.getDouble("windSpeed"));
        currentWeather.setTemp(currently.getDouble("temperature"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));

        currentWeather.setTimeZone(timezone);

        Log.d(TAG, currentWeather.getFormattedTime());

        return currentWeather;


    }

    private boolean isNetworkAvailable() {

        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (netInfo != null && netInfo.isConnected()){

            isAvailable = true;

        }
        return isAvailable;

    }

    private void showErrorDialog() {

        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(),"error_dialog");
    }
}
