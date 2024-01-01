package com.example.vali_app;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import API.ResponseResult;
import API.RetrofitInterface;
import API.RetrofitServer;
import Adapter.ValiAdapter;
import SocketIO.SocketIO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import model.ValiResult;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity implements ValiAdapter.OnDeleteSuccessListener {

    private RecyclerView recyclerView;
    private LinearLayout addVali, logout;
    private TextView tvUsername;
    private ValiAdapter valiAdapter;
    List<ValiResult> valiList = new ArrayList<>();
    private GestureDetector gestureDetector;
    SharedPreferences sharedPreferences;
    RetrofitServer retrofitServer;
    RetrofitInterface retrofitInterface;
    String token;
    Toast toast;
    DashboardActivity dashboardActivity;
    SocketIO socketIO;
    Socket socket;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private final int FINE_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        addVali = findViewById(R.id.add_vali);
        logout = findViewById(R.id.logout);
        tvUsername = findViewById(R.id.tvUsername);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        toast = new Toast(DashboardActivity.this);
        LayoutInflater inflater = getLayoutInflater();

        //location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();

        //api
        retrofitServer = new RetrofitServer();
        retrofitInterface = retrofitServer.Retrofit();

        //socket
        socketIO = new SocketIO();
        socket = socketIO.socket(DashboardActivity.this);

        //get token
        sharedPreferences = getSharedPreferences("SaveInfo", MODE_PRIVATE);
        token = sharedPreferences.getString("token", "");

        //user name
        Call<JsonObject> callUser = retrofitInterface.GetUser("Bearer " + token);
        callUser.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.code() == 200) {
                    JsonObject result = response.body();
                    String userName = result.get("username").getAsString();
                    tvUsername.setText(userName);
                } else {
                    try {
                        ResponseResult result = new Gson().fromJson(response.errorBody().string(), ResponseResult.class);
                        // Display specific errors
                        List<String> errorMessages = result.getErrors();
                        for (String errorMessage : errorMessages) {
                            // Process each error message (e.g., show in a Toast)
                            View view = inflater.inflate(R.layout.layout_toast_error, (ViewGroup) findViewById(R.id.Layout_toast_2));
                            TextView tvMessage = view.findViewById(R.id.tvMessege2);
                            tvMessage.setText(errorMessage);
                            toast.setView(view);
                            toast.setGravity(Gravity.BOTTOM, 0, 0);
                            toast.setDuration(Toast.LENGTH_LONG);
                            toast.show();
                        }
                    } catch (IOException e) {

                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, t.getMessage(),
                        Toast.LENGTH_LONG).show();
                Log.d("error", t.getMessage());
            }
        });

        //list vali
        LoadListVali();

        // Cập nhật RecyclerView với danh sách vali
        valiAdapter = new ValiAdapter(DashboardActivity.this, valiList);
        valiAdapter.setOnDeleteSuccessListener(this);
        recyclerView.setAdapter(valiAdapter);

        //add vali moi
        addVali.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddValiDialog();
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLogoutDialog();
            }
        });
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.layout_dialog_logout, null);

        Button btnLogout = dialogView.findViewById(R.id.btnLogout);
        Button btnCancel = dialogView.findViewById(R.id.btnNo);

        builder.setView(dialogView);
        final AlertDialog dialog = builder.create();

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("token");
                editor.apply();

                Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Finish the current activity
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void LoadListVali() {
        Call<List<ValiResult>> callAllVali = retrofitInterface.GetAllVali("Bearer " + token);
        callAllVali.enqueue(new Callback<List<ValiResult>>() {
            @Override
            public void onResponse(Call<List<ValiResult>> call, Response<List<ValiResult>> response) {
                if (response.code() == 200) {
                    List<ValiResult> valiListFromApi = response.body();
                    updateRecyclerView(valiListFromApi);
                    // Iterate through the valiList and fetch coordinates for each Vali
                    for (ValiResult vali : valiListFromApi) {
                        fetchCoordinatesAndUpdate(vali);
                    }
                } else {
                    // Handle error response
                }
            }

            @Override
            public void onFailure(Call<List<ValiResult>> call, Throwable t) {

            }
        });

        socket.on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                DashboardActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = (JSONObject) args[0];

                            String suitcaseId = jsonObject.optString("suitcaseId", "");
                            double latitude = jsonObject.optDouble("latitude", 0.0);
                            double longitude = jsonObject.optDouble("longitude", 0.0);

                            String dmsLatitude = convertToDMS(latitude, true);
                            String dmsLongitude = convertToDMS(longitude, false);

                            // Find the ValiResult object in the list based on suitcaseId
                            ValiResult matchedVali = findValiResultBySuitcaseId(suitcaseId);

                            if (matchedVali != null) {
                                // Update the specific ValiResult object with real-time data
                                matchedVali.setLatitude(dmsLatitude);
                                matchedVali.setLongitude(dmsLongitude);
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
                                matchedVali.setDistance(formattedDistance);

                                // Notify the adapter of the specific item change
                                int updatedIndex = valiList.indexOf(matchedVali);
                                if (updatedIndex != -1) {
                                    valiAdapter.notifyItemChanged(updatedIndex);
                                }
                            }

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

    private void fetchCoordinatesAndUpdate(final ValiResult vali) {
        Call<JsonArray> call = retrofitInterface.GetCoordinates("Bearer " + token, vali.get_id());
        call.enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if (response.code() == 200) {
                    JsonArray result = response.body();
                    if (result != null && result.size() > 0) {
                        JsonObject lastObject = result.get(result.size() - 1).getAsJsonObject();
                        Double latitude = lastObject.get("latitude").getAsDouble();
                        Double longitude = lastObject.get("longitude").getAsDouble();

                        String dmsLatitude = convertToDMS(latitude, true);
                        String dmsLongitude = convertToDMS(longitude, false);

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

                        // Update the ValiResult object in the valiList
                        updateValiCoordinates(vali, dmsLatitude, dmsLongitude, distance, distanceUnit);
                    }
                } else {
                    try {
                        ResponseResult result = new Gson().fromJson(response.errorBody().string(), ResponseResult.class);
                        // Display specific errors
                        List<String> errorMessages = result.getErrors();
                        for (String errorMessage : errorMessages) {
                            Toast.makeText(DashboardActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (IOException e) {
                        // Handle exception
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateValiCoordinates(ValiResult vali, String latitude, String longitude, Double distance, String distanceUnit) {
        // Update latitude and longitude for the ValiResult object
        vali.setLatitude(latitude);
        vali.setLongitude(longitude);
        String formattedDistance = String.format(Locale.US, "%.2f%s", distance, distanceUnit);
        vali.setDistance(formattedDistance);

        // Notify the adapter of the specific item change
        int updatedIndex = valiList.indexOf(vali);
        if (updatedIndex != -1) {
            valiAdapter.notifyItemChanged(updatedIndex);
        }
    }

    private ValiResult findValiResultBySuitcaseId(String suitcaseId) {
        for (ValiResult valiResult : valiList) {
            if (valiResult.get_id().equals(suitcaseId)) {
                return valiResult;
            }
        }
        return null;
    }

    private void updateRecyclerView(List<ValiResult> valiListFromApi) {
        valiList.clear();
        valiList.addAll(valiListFromApi);
        valiAdapter.notifyDataSetChanged();
    }

    //dialog add
    private void showAddValiDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.layout_dialog_add, null);

        final EditText etSuitcaseName = dialogView.findViewById(R.id.etSuitcaseName);
        final EditText edtDeviceId = dialogView.findViewById(R.id.etDeviceId);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Khởi tạo dialog_progressbar
        final Dialog dialogpro = new Dialog(DashboardActivity.this);
        dialogpro.setContentView(R.layout.layout_dialog_progressbar);
        ProgressBar progressBar = dialogpro.findViewById(R.id.progressBar);
        dialogpro.setCancelable(false);

        builder.setView(dialogView);
        final AlertDialog dialog = builder.create();

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogpro.show();

                String name = etSuitcaseName.getText().toString().trim();
                String deviceId = edtDeviceId.getText().toString().trim();

                // Check if name and deviceId are not empty
                if (!name.isEmpty() && !deviceId.isEmpty()) {
                    // Call your API to add the vali
                    HashMap<String, String> map = new HashMap<>();

                    map.put("suitcaseName", name);
                    map.put("deviceId", deviceId);

                    Call<ResponseResult> callAddVali = retrofitInterface.AddVali("Bearer " + token, map);
                    callAddVali.enqueue(new Callback<ResponseResult>() {
                        @Override
                        public void onResponse(Call<ResponseResult> call, Response<ResponseResult> response) {
                            if (response.code() == 200) {
                                View view = inflater.inflate(R.layout.layout_toast_success, (ViewGroup) findViewById(R.id.Layout_toast));
                                TextView tvMessege = view.findViewById(R.id.tvMessege1);
                                tvMessege.setText("create suitcase successfully");
                                toast.setView(view);
                                toast.setGravity(Gravity.BOTTOM, 0, 0);
                                toast.setDuration(Toast.LENGTH_LONG);
                                toast.show();
                                LoadListVali();
                                dialogpro.dismiss();
                            } else {
                                dialogpro.dismiss();
                                try {
                                    ResponseResult result = new Gson().fromJson(response.errorBody().string(), ResponseResult.class);
                                    // Display specific errors
                                    List<String> errorMessages = result.getErrors();
                                    for (String errorMessage : errorMessages) {
                                        // Process each error message (e.g., show in a Toast)
                                        View view = inflater.inflate(R.layout.layout_toast_error, (ViewGroup) findViewById(R.id.Layout_toast_2));
                                        TextView tvMessage = view.findViewById(R.id.tvMessege2);
                                        tvMessage.setText(errorMessage);
                                        toast.setView(view);
                                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                                        toast.setDuration(Toast.LENGTH_LONG);
                                        toast.show();
                                    }
                                } catch (IOException e) {

                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseResult> call, Throwable t) {
                            Toast.makeText(DashboardActivity.this, t.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            Log.d("error", t.getMessage());
                        }
                    });
                    dialog.dismiss();
                } else {
                    dialogpro.dismiss();
                    Toast.makeText(DashboardActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    public void onDeleteSuccess() {
        // Reload your main activity here
        LoadListVali();
    }

    @Override
    public void onBackPressed() {
        // Check if the user is on the main dashboard screen
        // If yes, show a confirmation dialog or take any other appropriate action
        // If no, proceed with the default behavior (super.onBackPressed())

        // Replace 'YourLoginActivity.class' with the actual class of your login activity.
        if (this instanceof DashboardActivity) {
            // Do something or show a confirmation dialog
            // You might want to handle the case differently, e.g., show an exit confirmation
            // or navigate to the home screen instead of going back to the login screen.
            // Example:
            showExitConfirmationDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void showExitConfirmationDialog() {
        // Implement your exit confirmation dialog logic here
        // For example, show an AlertDialog with options to exit or cancel.
        // You can customize this according to your app's requirements.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to exit the app?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish(); // Close the app
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing or dismiss the dialog
            }
        });
        builder.show();
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }
        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null) {
                    currentLocation = location;
                    LoadListVali();
                }
            }
        });
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        if (currentLocation == null) {
            // Handle the case where currentLocation is null
            return 0.0; // or any default value
        } else {
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
    }


    // Handle the result of location permission request
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @NonNull int[] grantResults) {
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