package com.project.clientapp.Database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public interface CartDAO {

    // We only load cart by restaurant Id
    // cuz each restaurant will have a different order receipt  , different payment link so we can't make 1 cart for all
    @Query("SELECT * FROM Cart WHERE fbid=:fbid AND restaurantId=:restaurantID")
    Flowable<List<CartItem>> getAllCart(String fbid, int restaurantID);

    @Query("SELECT COUNT(*) FROM Cart WHERE fbid=:fbid AND restaurantId=:restaurantID")
    Single<Integer> countItemInCart(String fbid, int restaurantID);

    @Query("SELECT SUM(foodPrice*foodQuantity)+(foodExtraPrice*foodQuantity) FROM Cart WHERE fbid=:fbid AND restaurantId=:restaurantID")
    Single<Long> sumPrice(String fbid, int restaurantID);

    @Query("SELECT * FROM Cart WHERE foodId=:foodId AND fbid=:fbid AND restaurantId=:restaurantID")
    Single<CartItem> getItemInCart(String foodId ,String fbid, int restaurantID);

    @Insert(onConflict = OnConflictStrategy.REPLACE)// if conflict foodId , we update the info
    Completable insertOrReplaceAll(CartItem... cartItems);

    //@Insert(onConflict = OnConflictStrategy.REPLACE)
    //Single<Integer> updateCart(CartItem cart);

    @Delete
    Single<Integer> deleteCart(CartItem cart);

    @Query("DELETE FROM Cart WHERE fbid=:fbid AND restaurantId=:restaurantId")
    Single<Integer> cleanCart(String fbid , int restaurantId);

}
