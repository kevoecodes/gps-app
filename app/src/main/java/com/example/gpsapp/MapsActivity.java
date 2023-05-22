package com.example.gpsapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Marker destinationMarker;
    private Polyline routePolyline;
   //    LatLng dit = new LatLng(-6.853640, 39.273686);
    LatLng destination_location;
    LatLng currentLatLng;

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private FusedLocationProviderClient fusedLocationClient;

    // Create a PlacesClient instance
    PlacesClient placesClient;
    String apiKey = "AIzaSyDQz72mL0bI2Li-VJ2AAyFl78sB4UbQIMk";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        ImageButton currentLocationButton = findViewById(R.id.currentLocationButton);
        ImageButton directionsButton = findViewById(R.id.directionsButton);
        currentLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationPermissions();
            }
        });

        directionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(destination_location);
                checkLocationPermissions();
                if (destination_location == null) {
                    Toast.makeText(MapsActivity.this, "Search a place first", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(MapsActivity.this, "Drawing route to the destination", Toast.LENGTH_SHORT).show();
                    drawRoute(currentLatLng, destination_location);
                }
            }
        });

        AutoCompleteTextView searchEditText = findViewById(R.id.searchField);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Call performPlaceAutocomplete when the text changes
                performPlaceAutocomplete(s.toString(), searchEditText);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        Places.initialize(getApplicationContext(), apiKey); // Replace YOUR_API_KEY with your actual API key
        placesClient = Places.createClient(this);

        loadMap();
    }

    private void performPlaceAutocomplete(String query, AutoCompleteTextView searchEditText) {
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query)
                .setTypeFilter(TypeFilter.ADDRESS)
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
            List<AutocompletePrediction> predictions = response.getAutocompletePredictions();

            // Create a list of suggestion strings
            List<String> suggestionList = new ArrayList<>();
            for (AutocompletePrediction prediction : predictions) {
                suggestionList.add(prediction.getFullText(null).toString());
            }

            // Update the autocomplete dropdown suggestions
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestionList);
            searchEditText.setAdapter(adapter);
            adapter.notifyDataSetChanged();

            // Set a marker on the map for the selected location when text is submitted
            searchEditText.setOnItemClickListener((parent, view, position, id) -> {
                String selectedPlace = (String) parent.getItemAtPosition(position);
                Geocoder geocoder = new Geocoder(this);
                try {
                    List<Address> addresses = geocoder.getFromLocationName(selectedPlace, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        destination_location = new LatLng(address.getLatitude(), address.getLongitude());
                        mMap.addMarker(new MarkerOptions().position(destination_location).title(selectedPlace));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destination_location, 15));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }).addOnFailureListener((exception) -> {
            // Handle any errors that occur during autocomplete request
        });
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    private void getCurrentLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    // Create marker options with the custom icon
                    int innerColor = Color.parseColor("#80adf0");  // Replace with your desired outer color
                    int outerColor = Color.parseColor("#f8f1ff"); // Replace with your desired inner color
                    int markerRadius = 24; // Adjust the radius as needed for the rounded shape
                    Bitmap markerBitmap = createLayeredBitmap(outerColor, innerColor, markerRadius);
                    BitmapDescriptor markerIcon = BitmapDescriptorFactory.fromBitmap(markerBitmap);

                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(currentLatLng)
                            .title("Current Location")
                            .icon(markerIcon);

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
                    mMap.addMarker(markerOptions);
                } else {
                    // Handle the case when location is null
                }
            }
        });
    }

    private void loadMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng loc_1 = new LatLng(-6.801439, 39.234056);
        LatLng loc_2 = new LatLng(-6.817461, 39.282645);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(loc_1, 12);
        mMap.moveCamera(cameraUpdate);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                // Handle the case when location permissions are denied
            }
        }
    }

    private void drawRoute(LatLng origin, LatLng destination) {
        // Clear previous route and markers
        if (routePolyline != null)
            routePolyline.remove();
        if (destinationMarker != null)
            destinationMarker.remove();

        // Add destination marker
        destinationMarker = mMap.addMarker(new MarkerOptions().position(destination).title("Destination"));
        String apiKey = "AIzaSyDQz72mL0bI2Li-VJ2AAyFl78sB4UbQIMk";
        // Prepare the API request URL
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&key=" + apiKey;

        // Create a JsonObjectRequest for the API request
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Parse the response and extract the route polyline
                        List<LatLng> points = parseRoutePoints(response);

                        // Draw the route polyline on the map
                        if (points != null && !points.isEmpty()) {
                            PolylineOptions polylineOptions = new PolylineOptions()
                                    .addAll(points)
                                    .color(Color.BLUE) // Set your desired polyline color
                                    .width(8f); // Set your desired polyline width
                            routePolyline = mMap.addPolyline(polylineOptions);

                            // Adjust camera bounds to include both origin and destination
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            builder.include(origin);
                            builder.include(destination);
                            LatLngBounds bounds = builder.build();
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100);
                            mMap.moveCamera(cameraUpdate);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                    }
                });

        // Add the request to the Volley request queue
        Volley.newRequestQueue(this).add(request);
    }

    private List<LatLng> parseRoutePoints(JSONObject response) {
        List<LatLng> points = new ArrayList<>();

        try {
            JSONArray routesArray = response.getJSONArray("routes");
            if (routesArray.length() > 0) {
                JSONObject route = routesArray.getJSONObject(0);
                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String encodedPoints = overviewPolyline.getString("points");
                points = decodePolyline(encodedPoints);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return points;
    }

    private List<LatLng> decodePolyline(String encodedPoints) {
        List<LatLng> decodedPoints = new ArrayList<>();
        int index = 0;
        int lat = 0, lng = 0;

        while (index < encodedPoints.length()) {
            int b, shift = 0, result = 0;
            do {
                b = encodedPoints.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encodedPoints.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            double latitude = lat / 1e5;
            double longitude = lng / 1e5;
            LatLng point = new LatLng(latitude, longitude);
            decodedPoints.add(point);
        }

        return decodedPoints;
    }

    private Bitmap createLayeredBitmap(int outerColor, int innerColor, int radius) {
        int shadowColor = Color.parseColor("#55000000");  // Semi-transparent black shadow color

        Bitmap bitmap = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Create outer circle with drop shadow
        Paint outerPaint = new Paint();
        outerPaint.setAntiAlias(true);
        outerPaint.setColor(outerColor);
        outerPaint.setShadowLayer(radius / 8, 0, 0, shadowColor);  // Apply drop shadow

        RectF outerRectF = new RectF(0, 0, radius * 2, radius * 2);
        canvas.drawOval(outerRectF, outerPaint);

        // Create inner circle
        Paint innerPaint = new Paint();
        innerPaint.setAntiAlias(true);
        innerPaint.setColor(innerColor);

        RectF innerRectF = new RectF(radius / 4, radius / 4, radius * 7 / 4, radius * 7 / 4);
        canvas.drawOval(innerRectF, innerPaint);

        return bitmap;
    }


}
