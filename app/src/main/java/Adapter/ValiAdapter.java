package Adapter;

import static android.content.Context.MODE_PRIVATE;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vali_app.MapsActivity;
import com.example.vali_app.R;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import API.ResponseResult;
import API.RetrofitInterface;
import API.RetrofitServer;
import model.ValiResult;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ValiAdapter extends RecyclerView.Adapter<ValiAdapter.ViewHolder> {
    private List<ValiResult> valiList;
    private Context context;
    SharedPreferences sharedPreferences;
    RetrofitServer retrofitServer;
    RetrofitInterface retrofitInterface;
    Toast toast;
    LayoutInflater inflater;
    private OnDeleteSuccessListener onDeleteSuccessListener;
    public void setOnDeleteSuccessListener(OnDeleteSuccessListener listener) {
        this.onDeleteSuccessListener = listener;
    }

    public interface OnDeleteSuccessListener {
        void onDeleteSuccess();
    }

    public ValiAdapter(Context context, List<ValiResult> valiList) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.valiList = valiList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.vali_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ValiResult vali = valiList.get(position);
        holder.bindData(vali, context, position);
    }

    @Override
    public int getItemCount() {
        return valiList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Existing members
        TextView nameTextView;
        TextView tvConnect;
        ImageButton locateButton;
        Context context;
        int position;
        TextView tvLatitude, tvLongitude, tvDistance;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Existing initialization
            nameTextView = itemView.findViewById(R.id.nameTextView);
            tvConnect = itemView.findViewById(R.id.tvConnect);
            locateButton = itemView.findViewById(R.id.locateButton);
            tvLatitude = itemView.findViewById(R.id.tvLat);
            tvLongitude = itemView.findViewById(R.id.tvLong);
            tvDistance = itemView.findViewById(R.id.tvDistance);

            ImageButton menuButton = itemView.findViewById(R.id.menuButton);
            menuButton.setOnClickListener(this);
            this.context = context;
        }

        public void bindData(ValiResult vali, Context context, int position) {
            this.position = position;

            // Set data to other views
            nameTextView.setText(vali.getName());
            // Set tvConnect icon and text based on isOpenConnect
            if (vali.isOpenConnect()) {
                tvConnect.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_verified, 0, 0, 0); // Set connected icon
                tvConnect.setText("Connected");
            } else {
                tvConnect.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_unverified, 0, 0, 0); // Set disconnected icon
                tvConnect.setText("Disconnected");
            }

            tvLatitude.setText(vali.getLatitude());
            tvLongitude.setText(vali.getLongitude());
            tvDistance.setText(vali.getDistance());

            String latitude = vali.getLatitude();
            String longitude = vali.getLongitude();

            // Kiểm tra nếu latitude và longitude không phải là null và không rỗng thì mở khóa nút
            if (latitude != null && longitude != null && !latitude.isEmpty() && !longitude.isEmpty()) {
                locateButton.setEnabled(true);
                locateButton.setBackgroundResource(R.drawable.bg_icon_1);
            } else {
                locateButton.setEnabled(false);
                locateButton.setBackgroundResource(R.drawable.bg_icon_1_off);
            }
            locateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Xử lý khi nút Định vị được nhấn
                    String vali_id = vali.get_id();
                    String name = vali.getName();
                    String distance = vali.getDistance();
                    String dmsLatitude = vali.getLatitude();
                    String dmsLongitude = vali.getLongitude();
                    //api
                    retrofitServer = new RetrofitServer();
                    retrofitInterface = retrofitServer.Retrofit();
                    //get token
                    sharedPreferences = context.getSharedPreferences("SaveInfo",MODE_PRIVATE);
                    String token = sharedPreferences.getString("token","");

                    Call<JsonArray> call = retrofitInterface.GetCoordinates("Bearer "+token, vali_id);
                    call.enqueue(new Callback<JsonArray>() {
                        @Override
                        public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                            if(response.code() == 200) {
                                JsonArray result = response.body();
                                if (result != null) {
                                    int lastIndex = result.size() - 1;
                                    JsonObject lastObject = result.get(lastIndex).getAsJsonObject();
                                    Double latitude = lastObject.get("latitude").getAsDouble();
                                    Double longitude = lastObject.get("longitude").getAsDouble();
                                    //test
                                    Log.e("history_map", "latitude_history: "+latitude);
                                    Log.e("history_map", "longitude_history: "+longitude);

                                    Intent intent = new Intent(context, MapsActivity.class);
                                    intent.putExtra("latitude", latitude);
                                    intent.putExtra("longitude", longitude);
                                    intent.putExtra("dmslatitude", dmsLatitude);
                                    intent.putExtra("dmslongitude", dmsLongitude);
                                    intent.putExtra("valiId", vali_id);
                                    intent.putExtra("valiName", name);
                                    intent.putExtra("valiDistance", distance);
                                    context.startActivity(intent);
                                }
                            } else {
                                try {
                                    ResponseResult result = new Gson().fromJson(response.errorBody().string(), ResponseResult.class);
                                    // Display specific errors
                                    List<String> errorMessages = result.getErrors();
                                    for (String errorMessage : errorMessages) {
                                        Toast.makeText(context, errorMessage,
                                                Toast.LENGTH_LONG).show();
                                    }
                                } catch (IOException e){

                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonArray> call, Throwable t) {
                            Toast.makeText(context, t.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.menuButton) {
                showPopupMenu(v, getAdapterPosition());
            }
        }
    }

    private void showPopupMenu(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.inflate(R.menu.context_menu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (position != RecyclerView.NO_POSITION) {
                    switch (item.getItemId()) {
                        case R.id.edit:
                            // Xử lý khi chọn Edit
                            showEditDialog(valiList.get(position));
                            return true;
                        case R.id.delete:
                            // Xử lý khi chọn Delete
                            showDeleteDialog(valiList.get(position), position);
                            return true;
                    }
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void showEditDialog(ValiResult vali) {
        // Khởi tạo dialog_progressbar
        final Dialog dialogpro = new Dialog(context);
        dialogpro.setContentView(R.layout.layout_dialog_progressbar);
        ProgressBar progressBar = dialogpro.findViewById(R.id.progressBar);
        dialogpro.setCancelable(false);

        // Sửa lại để sử dụng context từ ViewHolder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.layout_dialog_edit, null);

        final EditText etSuitcaseName = dialogView.findViewById(R.id.etSuitcaseName);
        Button btnEdit = dialogView.findViewById(R.id.btnEdit);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel2);

        builder.setView(dialogView);
        final AlertDialog dialog = builder.create();

        etSuitcaseName.setText(vali.getName());

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogpro.show();
                String name = etSuitcaseName.getText().toString().trim();

                // Check if name and deviceId are not empty
                if (!name.isEmpty()) {
                    // Call your API
                    //api
                    retrofitServer = new RetrofitServer();
                    retrofitInterface = retrofitServer.Retrofit();
                    //get token
                    sharedPreferences = context.getSharedPreferences("SaveInfo",MODE_PRIVATE);
                    String token = sharedPreferences.getString("token","");

                    HashMap<String, String> map = new HashMap<>();

                    map.put("suitcaseName", name);

                    Call<ResponseResult> call = retrofitInterface.EditVali("Bearer " + token, vali.get_id(), map);
                    call.enqueue(new Callback<ResponseResult>() {
                        @Override
                        public void onResponse(Call<ResponseResult> call, Response<ResponseResult> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(context, "Edit suitcase successfully",
                                        Toast.LENGTH_LONG).show();

                                if (onDeleteSuccessListener != null) {
                                    onDeleteSuccessListener.onDeleteSuccess();
                                }
                                
                                dialogpro.dismiss();
                            } else {
                                dialogpro.dismiss();
                                try {
                                    ResponseResult result = new Gson().fromJson(response.errorBody().string(), ResponseResult.class);
                                    // Display specific errors
                                    List<String> errorMessages = result.getErrors();
                                    for (String errorMessage : errorMessages) {
                                        // Process each error message (e.g., show in a Toast)
                                        Toast.makeText(context, errorMessage,
                                                Toast.LENGTH_LONG).show();
                                    }
                                } catch (IOException e) {

                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseResult> call, Throwable t) {
                            Toast.makeText(context, t.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            Log.d("error", t.getMessage());
                        }
                    });
                    dialog.dismiss();
                } else {
                    Toast.makeText(context, "Please fill in field", Toast.LENGTH_SHORT).show();
                    dialogpro.dismiss();
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

    private void showDeleteDialog(ValiResult vali, int position) {
        // Sửa lại để sử dụng context từ ViewHolder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.layout_dialog_delete, null);

        Button btnDelete = dialogView.findViewById(R.id.btnDelete);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel3);

        builder.setView(dialogView);
        final AlertDialog dialog = builder.create();

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Call your delete API
                String valiId = vali.get_id();
                deleteValiApi(valiId, position);
                dialog.dismiss();
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

    private void deleteValiApi(String valiId, int position) {
        // Khởi tạo dialog_progressbar
        final Dialog dialogpro = new Dialog(context);
        dialogpro.setContentView(R.layout.layout_dialog_progressbar);
        ProgressBar progressBar = dialogpro.findViewById(R.id.progressBar);
        dialogpro.setCancelable(false);
        dialogpro.show();
        //api
        retrofitServer = new RetrofitServer();
        retrofitInterface = retrofitServer.Retrofit();
        //get token
        sharedPreferences = context.getSharedPreferences("SaveInfo",MODE_PRIVATE);
        String token = sharedPreferences.getString("token","");

        Call<ResponseResult> callDeleteVali = retrofitInterface.DeleteVali("Bearer " + token, valiId);
        callDeleteVali.enqueue(new Callback<ResponseResult>() {
            @Override
            public void onResponse(Call<ResponseResult> call, Response<ResponseResult> response) {
                if(response.isSuccessful()) {

                    // Tạo và hiển thị Toast
                    Toast.makeText(context, "Delete suitcase successfully", Toast.LENGTH_SHORT).show();

                    int deletedPosition = position; // assuming this is a method in your ViewHolder
                    removeItem(deletedPosition);

                    if (onDeleteSuccessListener != null) {
                        onDeleteSuccessListener.onDeleteSuccess();
                    }

                    dialogpro.dismiss();
                } else {
                    dialogpro.dismiss();
                }
            }

            @Override
            public void onFailure(Call<ResponseResult> call, Throwable t) {

            }
        });
    }

    public void removeItem(int position) {
        valiList.remove(position);
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }


}
