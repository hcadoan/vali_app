package com.example.vali_app;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import API.ResponseResult;
import API.RetrofitInterface;
import API.RetrofitServer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    EditText emailText, passwordText;
    AppCompatButton loginBtn, signupBtn;
    CheckBox cbSave;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emailText = findViewById(R.id.email);
        passwordText = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        signupBtn = findViewById(R.id.signupBtn2);
        cbSave = findViewById(R.id.cbSave);

        //checkbox rememberme
        //luu thong tin
        sharedPreferences = getSharedPreferences("SaveInfo",MODE_PRIVATE);

        //lay thong tin
        emailText.setText(sharedPreferences.getString("email",""));
        passwordText.setText(sharedPreferences.getString("password",""));
        cbSave.setChecked(sharedPreferences.getBoolean("check", false));

        Toast toast = new Toast(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();

        //api
        RetrofitServer retrofitServer = new RetrofitServer();
        RetrofitInterface retrofitInterface = retrofitServer.Retrofit();

        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signupIntent = new Intent(MainActivity.this, SignUpActivity.class);
                startActivity(signupIntent);
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailText.getText().toString();
                String password = passwordText.getText().toString();

                // Khởi tạo dialog_progressbar
                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.layout_dialog_progressbar);
                ProgressBar progressBar = dialog.findViewById(R.id.progressBar);
                dialog.setCancelable(false);

                if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password)){
                    View view1 = inflater.inflate(R.layout.layout_toast_error, (ViewGroup) findViewById(R.id.Layout_toast_2));
                    TextView tvMessege = view1.findViewById(R.id.tvMessege2);
                    tvMessege.setText("Enter every details");
                    toast.setView(view1);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.show();
                }
                else {
                    if (cbSave.isChecked()) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("check", true);
                        editor.putString("email", email);
                        editor.putString("password", password);
                        editor.commit();
                    } else {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.remove("check");
                        editor.remove("email");
                        editor.remove("password");
                        editor.commit();
                    }

//                    dialog.show();

                    HashMap<String, String> map = new HashMap<>();

                    map.put("email", email);
                    map.put("password", password);

                    Call<JsonObject> call = retrofitInterface.Login(map);

                    call.enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            Log.e("code", String.valueOf(response.code()));
                            if (response.code() == 200) {

                                JsonObject result = response.body();
                                String token = result.get("token").getAsString();

                                View view = inflater.inflate(R.layout.layout_toast_success, (ViewGroup) findViewById(R.id.Layout_toast));
                                TextView tvMessege = view.findViewById(R.id.tvMessege1);
                                tvMessege.setText("log in Successfully");
                                toast.setView(view);
                                toast.setGravity(Gravity.BOTTOM, 0, 0);
                                toast.setDuration(Toast.LENGTH_LONG);
                                toast.show();

                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("token", token);
                                editor.commit();

                                Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                                startActivity(intent);
                            } else {
                                dialog.dismiss();
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
                                } catch (IOException e){

                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            Toast.makeText(MainActivity.this, t.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

}