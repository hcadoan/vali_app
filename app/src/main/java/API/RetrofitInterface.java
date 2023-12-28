package API;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;

import model.ValiResult;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface RetrofitInterface {

    @POST("api/users/login")
    Call<JsonObject> Login(@Body HashMap<String, String> map);

    @POST("api/users/register")
    Call<ResponseResult> SignUp(@Body HashMap<String, String> map);

    @GET("api/users/profile")
    Call<JsonObject> GetUser(@Header("Authorization") String token);

    @POST("api/suitcases")
    Call<ResponseResult> AddVali(@Header("Authorization") String token, @Body HashMap<String, String> map);

    @GET("api/suitcases")
    Call<List<ValiResult>> GetAllVali(@Header("Authorization") String token);

    @DELETE("api/suitcases/{id}")
    Call<ResponseResult> DeleteVali(@Header("Authorization") String token, @Path("id") String valiId);

    @PUT("api/suitcases/{id}")
    Call<ResponseResult> EditVali(@Header("Authorization") String token, @Path("id") String valiId, @Body HashMap<String, String> map);

    @GET("api/suitcases/{id}/coordinates")
    Call<JsonArray> GetCoordinates(@Header("Authorization") String token, @Path("id") String valiId);
}
