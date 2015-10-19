package pt.castro.finder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by lourenco on 29/09/15.
 */
public class FindPlaces extends AppCompatActivity implements LocationListener {

    private static final String PLACES_KEY = "AIzaSyCrBhaAOq5EMeR2QK9m2PtXSODRcO3cUCw";

    private static final int MIN_VARIATION_DISTANCE = 100;

    private double mLatitude = 0;
    private double mLongitude = 0;

    private TreeMap<String, PlaceObject> mPlacesMap;
    private LinearLayout mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_view);
        mRootView = (LinearLayout) findViewById(R.id.root_view);
        buildPlacesData();
        startLocation();
        startShimmer();
    }

    private void startShimmer() {
        final ShimmerTextView shimmerTextView = (ShimmerTextView) findViewById(R.id.shimmer_tv);
        shimmerTextView.setVisibility(View.VISIBLE);
        Shimmer shimmer = new Shimmer();
        shimmer.start(shimmerTextView);
    }

    private void buildPlacesData() {
        final String[] types = getResources().getStringArray(R.array.place_type);
        final String[] names = getResources().getStringArray(R.array.place_type_name);
        final TypedArray drawables = getResources().obtainTypedArray(R.array.background_images);
        mPlacesMap = new TreeMap<>();
        for (int x = 0; x < types.length; x++) {
            String name = names[x];
            int drawableId = drawables.getResourceId(x, -1);
            if (drawableId == -1) {
                Log.e(getClass().getSimpleName(), "Failed retrieving drawable for " + name);
                continue;
            }
            mPlacesMap.put(names[x], new PlaceObject(types[x], name, drawableId));
        }
        drawables.recycle();
    }

    private void startLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission
                    .ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }
        }
        mLatitude = 0;
        mLongitude = 0;
        final Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            onLocationChanged(location);
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 20000, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 20000, 0, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                startShimmer();
                startLocation();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private static String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        try {
            final URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            inputStream = urlConnection.getInputStream();
            final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            final StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
            Log.d("Downloading", e.toString());
        } finally {
            assert inputStream != null;
            inputStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    @Override
    public void onLocationChanged(Location location) {
        float[] results = new float[1];
        Location.distanceBetween(mLatitude, mLongitude, location.getLatitude(), location.getLongitude(), results);
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        if (results[0] > MIN_VARIATION_DISTANCE) {
            getNearestPlaces();
        }
    }

    protected void getNearestPlaces() {
        final PlacesTask placesTask = new PlacesTask(mPlacesMap);
        placesTask.execute();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /**
     * A class, to download Google Places
     */
    private class PlacesTask extends AsyncTask<Void, Void, Void> {

        private TreeMap<String, PlaceObject> placesMap;

        public PlacesTask(final TreeMap<String, PlaceObject> placesMap) {
            this.placesMap = placesMap;
        }

        @Override
        protected final Void doInBackground(Void[] url) {
            try {
                for (PlaceObject placeObject : placesMap.values()) {
                    String data = downloadUrl("https://maps.googleapis.com/maps/api/place/nearbysearch/json?"
                            + "location=" + mLatitude + "," + mLongitude + "&types=" + placeObject.type
                            + "&sensor=true" + "&rankby=distance" + "&key=" + PLACES_KEY);
                    PlaceJSONParser placeJsonParser = new PlaceJSONParser();
                    try {
                        JSONObject jObject = new JSONObject(data);
                        placeObject.data = placeJsonParser.parse(jObject).get(0);
                    } catch (Exception e) {
                        Log.d("Exception", e.toString());
                    }
                }
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            for (PlaceObject placeObject : placesMap.values()) {
                if (placeObject.data != null) {
                    buildRow(mRootView, placeObject.data, placeObject.name);
                }
            }
            final ShimmerTextView shimmerTextView = (ShimmerTextView) findViewById(R.id.shimmer_tv);
            shimmerTextView.setVisibility(View.GONE);
        }
    }

    private LinearLayout buildRow(ViewGroup root, HashMap<String, String> place, String type) {
        int drawable = mPlacesMap.get(type).drawableId;
        LinearLayout row = (LinearLayout) mRootView.findViewById(drawable);
        if (row == null) {
            row = (LinearLayout) getLayoutInflater().inflate(R.layout.row, mRootView, false);
            row.setId(drawable);
            root.addView(row);
        }

        final TextView topText = (TextView) row.findViewById(R.id.top_text);
        topText.setText(type);

        String name = place.get("place_name");
        final TextView bottomText = (TextView) row.findViewById(R.id.bottom_text);
        bottomText.setText(name);

        float[] results = new float[1];
        Location.distanceBetween(mLatitude, mLongitude, Float.parseFloat(place.get("lat")),
                Float.parseFloat(place.get("lng")), results);

        final TextView distanceText = (TextView) row.findViewById(R.id.distance);
        distanceText.setText(Integer.toString(((int) results[0])) + "m");

        final Uri gmmIntentUri = Uri.parse("google.navigation:q=" + place.get("lat") + "," + place.get("lng"));
        final Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        final View directionsButton = row.findViewById(R.id.directions_button);
        directionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mapIntent);
            }
        });

        final ImageView background = (ImageView) row.findViewById(R.id.background);
        background.setImageResource(drawable);

        final Uri gmmIntentUri2 = Uri.parse("geo:0,0?z=10&q=" + type);
        final Intent mapIntent2 = new Intent(Intent.ACTION_VIEW, gmmIntentUri2);
        mapIntent.setPackage("com.google.android.apps.maps");
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mapIntent2);
            }
        });
        return row;
    }
}
