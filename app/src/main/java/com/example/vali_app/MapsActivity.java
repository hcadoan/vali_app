package com.example.vali_app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.vali_app.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

import SocketIO.SocketIO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final int FINE_PERMISSION_CODE = 1;
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    ArrayList markerPoints = new ArrayList();

    SharedPreferences sharedPreferences;
    Double latitude;  // Initialize with default values
    Double longitude;
    String vali_id;

    SocketIO socketIO;
    Socket socket;

    TextView tvname, tvLat, tvLong, tvDistance;
    String distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tvname = findViewById(R.id.nameTextView);
        tvLat = findViewById(R.id.tvLat);
        tvLong = findViewById(R.id.tvLong);
        tvDistance = findViewById(R.id.tvDistance);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();

        //get id
        Intent intent = getIntent();
        vali_id = intent.getStringExtra("valiId");
        String name = intent.getStringExtra("valiName");
        distance = intent.getStringExtra("valiDistance");
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);
        String dmslatitude = intent.getStringExtra("dmslatitude");
        String dmslongitude = intent.getStringExtra("dmslongitude");


        tvname.setText(name);
        tvLat.setText(dmslatitude);
        tvLong.setText(dmslongitude);
        tvDistance.setText(distance);

        //socket
        socketIO = new SocketIO();
        socket = socketIO.socket(MapsActivity.this);

        socket.on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                MapsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = (JSONObject) args[0];

                            String suitcaseId = jsonObject.optString("suitcaseId", "");

                            // Check if suitcaseId matches vali_id
                            if (suitcaseId.equals(vali_id)) {
                                // If it matches, get the lat and long values
                                latitude = jsonObject.optDouble("latitude", 0.0);
                                longitude = jsonObject.optDouble("longitude", 0.0);

                                String dmsLatitude = convertToDMS(latitude, true);
                                String dmsLongitude = convertToDMS(longitude, false);
                                // Update the marker on the map
                                updateMapMarker(latitude, longitude);

                                Double distance;
                                String distanceUnit;
                                if(currentLocation != null) {
                                    distance = calculateDistance(currentLocation.getLatitude(), currentLocation.getLongitude(), latitude, longitude);
                                    if (distance >= 1) {
                                        // Khi khoảng cách lớn hơn hoặc bằng 1 km, hiển thị trong đơn vị ki-lô-mét
                                        distanceUnit = " km";
                                    } else {
                                        // Khi khoảng cách nhỏ hơn 1 km, hiển thị trong đơn vị mét
                                        distanceUnit = " m";
                                        distance = distance * 1000;
                                    }
                                } else {
                                    distance = 0.0;
                                    distanceUnit = " km";
                                }

                                String formattedDistance = String.format(Locale.US, "%.2f%s", distance, distanceUnit);

                                tvLat.setText(dmsLatitude);
                                tvLong.setText(dmsLongitude);
                                tvDistance.setText(formattedDistance);
                            }
                            Log.d("MapsRealtime", "lat: " + latitude.toString());
                            Log.d("MapsRealtime", "long: " + longitude.toString());

                            // Log details for debugging
                            Log.d("MapsRealtime", "Received message: " + jsonObject.toString());

                        } catch (ClassCastException e) {
                            // Log error details for unexpected message type
                            Log.e("SocketMessage", "Unexpected message type: " + args[0].getClass().getSimpleName(), e);
                        }
                    }
                });
            }
        });
    }

    private static String convertToDMS(double coordinate, boolean isLatitude) {
        char direction = isLatitude ? (coordinate >= 0 ? 'N' : 'S') : (coordinate >= 0 ? 'E' : 'W');
        coordinate = Math.abs(coordinate);

        int degrees = (int) coordinate;
        double minutesAndSeconds = (coordinate - degrees) * 60;

        int minutes = (int) minutesAndSeconds;
        double seconds = (minutesAndSeconds - minutes) * 60;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat decimalFormat = new DecimalFormat("00.000", symbols);

        String formattedSeconds = decimalFormat.format(seconds);

        return String.format("%d°%02d'%s\"%s", degrees, minutes, formattedSeconds, direction);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula for distance calculation
        double R = 6371; // Radius of the Earth in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Distance in kilometers
        return R * c;
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null) {
                    currentLocation = location;

                    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(MapsActivity.this);

                }
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker
//        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
//        test
        LatLng endLatLng = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(endLatLng).title("Your location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(endLatLng));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(endLatLng, 15.0f));
    }

    private void updateMapMarker(double lat, double lng) {
        if (mMap != null) {
            LatLng endLatLng = new LatLng(lat, lng);
            mMap.clear();  // Clear existing markers
            mMap.addMarker(new MarkerOptions().position(endLatLng).title("Your location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(endLatLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(endLatLng, 15.0f));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == FINE_PERMISSION_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "location permission is denied, please allow the permission", Toast.LENGTH_SHORT).show();
            }
        }
    }
}