package com.project.clientapp.Retrofit;

import com.project.clientapp.Modal.AddonModel;
import com.project.clientapp.Modal.CreateOrderModel;
import com.project.clientapp.Modal.FavoriteModel;
import com.project.clientapp.Modal.FavoriteOnlyId;
import com.project.clientapp.Modal.FavoriteOnlyIdModel;
import com.project.clientapp.Modal.FoodModel;
import com.project.clientapp.Modal.GetKeyModel;
import com.project.clientapp.Modal.MaxOrderModel;
import com.project.clientapp.Modal.MenuModel;
import com.project.clientapp.Modal.OrderModel;
import com.project.clientapp.Modal.RestaurantModel;
import com.project.clientapp.Modal.SizeModel;
import com.project.clientapp.Modal.TokenModel;
import com.project.clientapp.Modal.UpdateOrderModel;
import com.project.clientapp.Modal.UpdateUserModel;
import com.project.clientapp.Modal.UserModel;

import java.util.Map;

import io.reactivex.Observable;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface MyRestaurantAPI {

    // GET KEY
    @GET("getkey")
    Observable<GetKeyModel> getKey(@Query("fbid") String fbid);

    // GET
    @GET("user")
    Observable<UserModel> getUser(@HeaderMap Map<String,String> headers);

    @GET("restaurant")
    Observable<RestaurantModel> getRestaurant(@HeaderMap Map<String,String> headers);

    @GET("restaurantById")
    Observable<RestaurantModel> getRestaurantById(@HeaderMap Map<String,String> headers, @Query("restaurantId") String restaurantId);

    @GET("nearbyrestaurant")
    Observable<RestaurantModel> getNearByRestaurant(@HeaderMap Map<String,String> headers,@Query("lat") Double lat,@Query("lng") Double lng,@Query("distance") int distance);

    @GET("menu")
    Observable<MenuModel> getCategories(@HeaderMap Map<String,String> headers, @Query("restaurantId") int restaurantId);

    @GET("food")
    Observable<FoodModel> getFoodOfMenu(@HeaderMap Map<String,String> headers, @Query("menuId") int menuId);

    @GET("foodById")
    Observable<FoodModel> getFoodById(@HeaderMap Map<String,String> headers, @Query("foodId") int foodId);

    @GET("searchFood")
    Observable<FoodModel> searchFood(@HeaderMap Map<String,String> headers, @Query("foodName") String foodName, @Query("menuId") int menuId);

    @GET("size")
    Observable<SizeModel> getSizeOfFood(@HeaderMap Map<String,String> headers, @Query("foodId") int foodId);

    @GET("addon")
    Observable<AddonModel> getAddonOfFood(@HeaderMap Map<String,String> headers, @Query("foodId") int foodId);

    @GET("favorite")
    Observable<FavoriteModel> getFavoriteByUser(@HeaderMap Map<String,String> headers);

    @GET("favoriteByRestaurant")
    Observable<FavoriteOnlyIdModel> getFavoriteByRestaurant(@HeaderMap Map<String,String> headers, @Query("restaurantId") int restaurantId);

    @GET("order")
    Observable<OrderModel> getOrder(@HeaderMap Map<String,String> headers,@Query("from") int from,@Query("to") int to);

    @GET("maxorder")
    Observable<MaxOrderModel> getMaxOrder(@HeaderMap Map<String,String> headers);

    @GET("token")
    Observable<TokenModel> getToken(@HeaderMap Map<String,String> headers);

    //POST

    @POST("user")
    @FormUrlEncoded
    Observable<UpdateUserModel> updateUserInfo(@HeaderMap Map<String,String> headers,
                                               @Field("userPhone") String userPhone,
                                               @Field("userName") String userName,
                                               @Field("userAddress") String userAddress);

    @POST("token")
    @FormUrlEncoded
    Observable<TokenModel> updateTokenToServer(@HeaderMap Map<String,String> headers,
                                          @Field("token") String token);

    @POST("favorite")
    @FormUrlEncoded
    Observable<FavoriteModel> insertFavorite(@HeaderMap Map<String,String> headers,
                                             @Field("foodId") int foodId,
                                             @Field("restaurantId") int restaurantId,
                                             @Field("restaurantName") String restaurantName,
                                             @Field("foodName") String foodName,
                                             @Field("foodImage") String foodImage,
                                             @Field("price") double price);

    @POST("createOrder")
    @FormUrlEncoded
    Observable<CreateOrderModel> createOrder(@HeaderMap Map<String,String> headers,
                                             @Field("orderPhone") String orderPhone,
                                             @Field("orderName") String orderName,
                                             @Field("orderAddress") String orderAddress,
                                             @Field("orderDate") String orderDate,
                                             @Field("restaurantId") int restaurantId,
                                             @Field("transactionId") String transactionId,
                                             @Field("cod") boolean cod,
                                             @Field("totalPrice") Double totalPrice,
                                             @Field("numOfItem") int numOfItem);

    @POST("updateOrder")
    @FormUrlEncoded
    Observable<UpdateOrderModel> updateOrder(@HeaderMap Map<String,String> headers,
                                             @Field("orderId") String orderId,
                                             @Field("orderDetail") String orderDetail);

    //DELETE
    @DELETE("favorite")
    Observable<FavoriteModel> removeFavorite(@HeaderMap Map<String,String> headers,
                                             @Field("foodId") int foodId,
                                             @Field("restaurantId") int restaurantId);
}
