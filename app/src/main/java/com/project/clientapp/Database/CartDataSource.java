package com.project.clientapp.Database;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

public interface CartDataSource {

    Flowable<List<CartItem>> getAllCart(String userPhone, int restaurantID);

    Single<Integer> countItemInCart(String userPhone, int restaurantID);

    Single<Long> sumPrice(String userPhone, int restaurantID);

    Single<CartItem> getItemInCart(String foodId ,String userPhone, int restaurantID);

    Completable insertOrReplaceAll(CartItem... cartItems);

    //Single<Integer> updateCart(CartItem cart);

    Single<Integer> deleteCart(CartItem cart);

    Single<Integer> cleanCart(String userPhone , int restaurantId);
}
