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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import API.ResponseResult;
import API.RetrofitInterface;
import API.RetrofitServer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity {

    EditText nameText, emailText, passwordText, password_2Text;
    AppCompatButton signupBtn, loginBtn;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        nameText = findViewById(R.id.username);
        emailText = findViewById(R.id.email);
        passwordText = findViewById(R.id.password);
        password_2Text = findViewById(R.id.password_2);
        signupBtn = findViewById(R.id.signupBtn);
        loginBtn = findViewById(R.id.loginBtn2);

        Toast toast = new Toast(SignUpActivity.this);
        LayoutInflater inflater = getLayoutInflater();

        //api
        RetrofitServer retrofitServer = new RetrofitServer();
        RetrofitInterface retrofitInterface = retrofitServer.Retrofit();

        // chuyen sang trang dang nhap
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signupIntent = new Intent(SignUpActivity.this, MainActivity.class);
                startActivity(signupIntent);
            }
        });

        //dang ky tai khoan
        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = nameText.getText().toString();
                String email = emailText.getText().toString();
                String password = passwordText.getText().toString();
                String password_2 = password_2Text.getText().toString();

                // Khởi tạo dialog_progressbar
                final Dialog dialog = new Dialog(SignUpActivity.this);
                dialog.setContentView(R.layout.layout_dialog_progressbar);
                ProgressBar progressBar = dialog.findViewById(R.id.progressBar);
                dialog.setCancelable(false);

                //kiem tra empty
                if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(name) || TextUtils.isEmpty(password_2)){
                    View view1 = inflater.inflate(R.layout.layout_toast_error, (ViewGroup) findViewById(R.id.Layout_toast_2));
                    TextView tvMessege = view1.findViewById(R.id.tvMessege2);
                    tvMessege.setText("Enter every details");
                    toast.setView(view1);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.show();
                }
                else if (!password.equals(password_2)) {
                    View view1 = inflater.inflate(R.layout.layout_toast_error, (ViewGroup) findViewById(R.id.Layout_toast_2));
                    TextView tvMessege = view1.findViewById(R.id.tvMessege2);
                    tvMessege.setText("password not match");
                    toast.setView(view1);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.show();
                }
                else {
                    dialog.show();

                    HashMap<String, String> map = new HashMap<>();

                    map.put("username", name);
                    map.put("email", email);
                    map.put("password", password);

                    Call<ResponseResult> callSignup = retrofitInterface.SignUp(map);

                    callSignup.enqueue(new Callback<ResponseResult>() {
                        @Override
                        public void onResponse(Call<ResponseResult> call, Response<ResponseResult> response) {
                            Log.d("response", String.valueOf(response));
                            if (response.code() == 200) {

                                View view = inflater.inflate(R.layout.layout_toast_success, (ViewGroup) findViewById(R.id.Layout_toast));
                                TextView tvMessege = view.findViewById(R.id.tvMessege1);
                                tvMessege.setText("Sign up successfully");
                                toast.setView(view);
                                toast.setGravity(Gravity.BOTTOM, 0, 0);
                                toast.setDuration(Toast.LENGTH_LONG);
                                toast.show();

                                Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                startActivity(intent);
                                dialog.dismiss();

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
                        public void onFailure(Call<ResponseResult> call, Throwable t) {
                            Toast.makeText(SignUpActivity.this, t.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            Log.d("error", t.getMessage());
                        }
                    });
                }
            }
        });
    }
}