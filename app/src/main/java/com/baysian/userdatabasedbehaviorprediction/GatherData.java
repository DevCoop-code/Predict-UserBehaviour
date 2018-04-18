package com.baysian.userdatabasedbehaviorprediction;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class GatherData extends Service
{
    private static final String TAG = "GatherData";

    Timer timer;
    int i=0;

    LocationManager locationManager;
    double latitude = 0;
    double longitude = 0;

    SensorManager sensorManager;
    int accelerometerSensor;
    float xAxis;
    float yAxis;
    float zAxis;

    String region = null;

    String gpsDataURL = null;

    SharedData sharedData;

    String Active = null;
    /*
    Mock Data, Hwajeon-Dong Location
     */
    double mock_latitude = 37.600800;
    double mock_longitude = 126.864873;

    float large_accel = 0;

    public GatherData()
    {

    }

    @Override
    public IBinder onBind(Intent intent)
    {
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public void onCreate()
    {
        sharedData = new SharedData();
        if(sharedData.GPS_FLAG)
        {
            locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2, locationListener);
        }

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometerSensor = Sensor.TYPE_ACCELEROMETER;
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(accelerometerSensor), SensorManager.SENSOR_DELAY_NORMAL);

        Log.d(TAG,"onCreate()");
    }

    @Override
    public void onDestroy()
    {
        Toast.makeText(this, "서비스가 중지되었습니다.", Toast.LENGTH_LONG).show();
        Log.d(TAG,"onDestroy()");
        timer.cancel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand()");

        timer = new Timer();
        timer.schedule(adTask, 0, 600000);
        return super.onStartCommand(intent, flags, startId);
    }

    TimerTask adTask = new TimerTask()
    {
        @Override
        public void run()
        {
            i++;
            /*SharedData sharedData = new SharedData();
            sharedData.num = i;
            sharedData.sharedLatitude = latitude;
            sharedData.sharedLongitude = longitude;
            */
            URLTask urlTask = new URLTask();
            Log.v(TAG,"Timer = " + String.valueOf(sharedData.num));
            gpsDataURL = new String();
            gpsDataURL = String.valueOf(latitude)+","+String.valueOf(longitude);
            urlTask.execute(gpsDataURL);
        }
    };

    public class URLTask extends AsyncTask<String, Void, String[]>
    {
        private final String LOG_TAG = "URLTask";

        private String[] getAddressDataFromJson(String addressJsonStr) throws JSONException
        {
            final String OWN_RESULTS    = "results";
            final String OWN_ADDRESS    = "address_components";
            final String OWN_NAME       = "short_name";

            JSONObject addressJson = new JSONObject(addressJsonStr);
            JSONArray addressArray = addressJson.getJSONArray(OWN_RESULTS);

            JSONObject nowAddress = addressArray.getJSONObject(0);

            JSONArray addressObject = nowAddress.getJSONArray(OWN_ADDRESS);

            String[] resultStr = new String[addressObject.length()];

            for(int i=0; i<addressObject.length(); i++)
            {
                JSONObject useAddress = addressObject.getJSONObject(i);
                String premise = useAddress.getString(OWN_NAME);
                resultStr[i] = premise;
            }
            //Log.v(TAG,resultStr[1]);
            return resultStr;
        }

        @Override
        protected String[] doInBackground(String... params)
        {
            if(params.length == 0)
            {
                return null;
            }
            HttpURLConnection urlConnection = null, con = null;
            BufferedReader reader = null;

            String addressJsonStr = null;

            String Api_key = "AIzaSyBoYK4eWlDZsqBRIKbfLmRbm72NlCZ85gw";

            /*
            Reverse GEO-CODING
             */
            try
            {
                final String ADDRESS_BASE_URL = "https://maps.googleapis.com/maps/api/geocode/json?";
                final String GEO_PARAM = "latlng";
                final String APIKEY_PARAM = "key";

                Uri builtUri = Uri.parse(ADDRESS_BASE_URL).buildUpon()
                        .appendQueryParameter(GEO_PARAM, params[0])
                        .appendQueryParameter(APIKEY_PARAM, Api_key)
                        .build();
                URL url = new URL(builtUri.toString());

                con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("GET");
                con.connect();

                InputStream inputStream = con.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if(inputStream == null)
                    return null;
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while( (line = reader.readLine()) != null)
                    buffer.append(line + "\n");

                if(buffer.length() == 0)
                    return null;
                addressJsonStr = buffer.toString();
                //Log.v(LOG_TAG,addressJsonStr);

            }
            catch(IOException e)
            {
                Log.e(TAG,"Error+ReverseGeo",e);
                return null;
            }
            finally
            {
                if(con != null)
                    con.disconnect();
                if(reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch(final IOException e)
                    {
                        Log.e(TAG,"Error closing stream", e);
                    }
                }
            }

            /*
            Send Data to Server
             */
            try
            {
                SharedData sharedData = new SharedData();

                String[] result = null;
                String day = null;
                String hour = null;

                /*
                Location JSONParsing Part
                 */
                if(latitude != 0 && longitude != 0)
                    result = getAddressDataFromJson(addressJsonStr);

                Time dayTime = new Time();
                dayTime.setToNow();

                int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

                long dateTime;
                dateTime = dayTime.setJulianDay(julianStartDay);
                SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE");
                day = shortenedDateFormat.format(dateTime);

                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat hourDateFormat = new SimpleDateFormat("kk.mm");
                hour = hourDateFormat.format(date);

                /*
                if(sharedData.GPS_FLAG)
                {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2, locationListener);
                }
                */

                //가속도계 값 가장 큰 것 찾기
                if(xAxis > yAxis)
                    large_accel = xAxis;
                else
                    large_accel = yAxis;

                if(-1<large_accel && large_accel<1 )
                    Active = new String("stop");
                else
                    Active = new String("active");

                final String BASE_URL = "https://javaserver-cooddy.c9users.io/longitude="+region+"&time="+hour+"&day="+day+"&accel="+String.valueOf(large_accel);

                URL url = new URL(BASE_URL);

                urlConnection = (HttpURLConnection)url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                urlConnection.getInputStream();

                return result;
            }
            catch(JSONException e)
            {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            catch(IOException e)
            {
                Log.e(TAG,"Error",e);
                return null;
            }
            finally
            {
                if(urlConnection != null)
                    urlConnection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result)
        {
            if(result != null)
            {
                region = result[1];
            }
        }
    }

    LocationListener locationListener = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location location)
        {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle)
        {

        }

        @Override
        public void onProviderEnabled(String s)
        {

        }

        @Override
        public void onProviderDisabled(String s)
        {

        }
    };

    final SensorEventListener sensorEventListener = new SensorEventListener()
    {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent)
        {
            if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            {
                xAxis = sensorEvent.values[0];
                yAxis = sensorEvent.values[1];
                zAxis = sensorEvent.values[2];
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i)
        {

        }
    };
}
