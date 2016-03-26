package com.example.raman.sunshine;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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
import java.util.ArrayList;
import java.util.Date;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_forecast_fragment, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            FetchWeatherTask task = new FetchWeatherTask();
            task.execute("delhi");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        FetchWeatherTask mTask = new FetchWeatherTask();
        mTask.execute("delhi");
        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {

            String[] weatherData = null;

            if (params.length == 0) {
                return null;
            }

            String forecastJson = getJSONfromCloud(params[0]);

            try {
                weatherData = getWeatherDataFromJSON(forecastJson,14);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return weatherData;
        }

        @Override
        protected void onPostExecute(String[] s) {
            super.onPostExecute(s);
            ArrayAdapter<String> mForecastAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.tv_list_item, s);
            ListView forecastLV = (ListView)getActivity().findViewById(R.id.lv_forecast);
            forecastLV.setAdapter(mForecastAdapter);
        }
    }

    private String getJSONfromCloud(String param) {
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;

        String appidKey = "c6c69047c622fc3067a2755bbb8a3b71";
        String forecastJsonStr = null;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "appid";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, param)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM, appidKey)
                    .build();

            URL url = new URL(builtUri.toString());

            Log.v("Built URI ", builtUri.toString());
            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            forecastJsonStr = buffer.toString();
            Log.v("Forecast JSON String", forecastJsonStr);
        } catch (IOException e) {
            Log.e("PlaceholderFragment", "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("PlaceholderFragment", "Error closing stream", e);
                }
            }
        }
        return forecastJsonStr;
    }

    public String[] getWeatherDataFromJSON(String forecastJsonStr, int numOfDays) throws JSONException{
        String[] resultStr = new String[numOfDays];

        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";
        final String OWM_DATE = "dt";

        JSONObject forecastJSON = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJSON.getJSONArray(OWM_LIST);

        for(int i = 0; i<weatherArray.length() ; i++){

            String description;
            String highAndLow;
            JSONObject dayForecast = weatherArray.getJSONObject(i);
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            long dateTime = dayForecast.getLong(OWM_DATE);

            Date time  =  new Date((long)dateTime * 1000);

            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd");
            String formattedDate = sdf.format(time);

            highAndLow = formatHighLows(high,low);
            resultStr[i] = formattedDate + " - " + description + " - " + highAndLow;
        }

//        for (String s : resultStr) {
//            Log.v("Checking Result String", "Forecast entry: " + s);
//        }

        return resultStr;
    }

    public double getMaxTempForDay(String str, int dayIndex) throws JSONException {
        JSONObject weather = new JSONObject(str);
        JSONArray days = weather.getJSONArray("list");
        JSONObject dayInfo = days.getJSONObject(dayIndex);
        JSONObject tempInfo = dayInfo.getJSONObject("temp");
        return tempInfo.getDouble("max");
    }

    public double getMinTempForDay(String str, int dayIndex) throws JSONException {
        JSONObject weather = new JSONObject(str);
        JSONArray days = weather.getJSONArray("list");
        JSONObject dayInfo = days.getJSONObject(dayIndex);
        JSONObject tempInfo = dayInfo.getJSONObject("temp");
        return tempInfo.getDouble("min");
    }

    public String getWeatherMain(String str, int dayIndex) throws JSONException {
        JSONObject weather = new JSONObject(str);
        JSONArray days = weather.getJSONArray("list");
        JSONObject dayInfo = days.getJSONObject(dayIndex);
        JSONArray dayWeatherArray = dayInfo.getJSONArray("weather");
        JSONObject dayWeather = dayWeatherArray.getJSONObject(0);
        return dayWeather.getString("main");
    }

    public String getWeatherDescription(String str, int dayIndex) throws JSONException {
        JSONObject weather = new JSONObject(str);
        JSONArray days = weather.getJSONArray("list");
        JSONObject dayInfo = days.getJSONObject(dayIndex);
        JSONArray dayWeatherArray = dayInfo.getJSONArray("weather");
        JSONObject dayWeather = dayWeatherArray.getJSONObject(0);
        return dayWeather.getString("description");
    }

    public String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }
}
