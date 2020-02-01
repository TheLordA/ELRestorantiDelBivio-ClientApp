package com.project.clientapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.clientapp.Common.Common;
import com.project.clientapp.Database.CartDataSource;
import com.project.clientapp.Database.CartDatabase;
import com.project.clientapp.Database.CartItem;
import com.project.clientapp.Database.LocalCartDataSource;
import com.project.clientapp.FoodDetailActivity;
import com.project.clientapp.Interface.IFoodDetailOrCartClickListener;
import com.project.clientapp.Modal.EventBus.FoodDetailEvent;
import com.project.clientapp.Modal.FavoriteOnlyId;
import com.project.clientapp.Modal.Food;
import com.project.clientapp.R;
import com.project.clientapp.Retrofit.MyRestaurantAPI;
import com.project.clientapp.Retrofit.RetrofitClient;
import com.squareup.picasso.Picasso;


import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MyFoodAdapter extends RecyclerView.Adapter<MyFoodAdapter.MyViewHolder> {

    Context context ;
    List<Food> foodList ;
    CompositeDisposable compositeDisposable ;
    CartDataSource cartDataSource;
    MyRestaurantAPI myRestaurantAPI ;

    public void onStop(){
        compositeDisposable.clear();
    }

    public MyFoodAdapter(Context context, List<Food> foodList) {
        this.context = context;
        this.foodList = foodList;
        compositeDisposable = new CompositeDisposable();
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(context).cartDAO());
        myRestaurantAPI = RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(MyRestaurantAPI.class);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
            .inflate(R.layout.layout_food,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        Picasso.get().load(foodList.get(position).getImage()).placeholder(R.drawable.app_icon).into(holder.img_food);

        holder.txt_food_name.setText(foodList.get(position).getName());
        holder.txt_food_price.setText(new StringBuilder(context.getString(R.string.money_sign)).append(foodList.get(position).getPrice()));

        //check Favorite
        if(Common.currentFavOfRestaurant != null && Common.currentFavOfRestaurant.size() > 0){
            if (Common.checkFavorite(foodList.get(position).getId())){
                holder.img_fav.setImageResource(R.drawable.ic_favorite_primary_color_24dp);
                holder.img_fav.setTag(true);
            }
            else{
                holder.img_fav.setImageResource(R.drawable.ic_favorite_border_primary_color_24dp);
                holder.img_fav.setTag(false);
            }
        }
        else{
            //default , there is no favorite
            holder.img_fav.setTag(false);
        }

        //event
        holder.img_fav.setOnClickListener(view -> {
            ImageView fav = (ImageView)view;
            if ((Boolean)fav.getTag()) {
                //if tag = true => favorite item clicked
                Map<String,String> headers = new HashMap<>();
                headers.put("Authorization",Common.buildJWT(Common.API_KEY));
                compositeDisposable.add(myRestaurantAPI.removeFavorite(headers,foodList.get(position).getId(),Common.currentRestaurant.getId())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(favoriteModel -> {
                            if (favoriteModel.isSuccess() && favoriteModel.getMessage().contains("Success")){
                                fav.setImageResource(R.drawable.ic_favorite_border_primary_color_24dp);
                                fav.setTag(false);
                                if (Common.currentFavOfRestaurant != null ){
                                    Common.removeFavorite(foodList.get(position).getId());
                                }
                            }
                        }
                        ,throwable -> {
                                    Toast.makeText(context, "[REMOVE FAV]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                }));
            }
            else {
                //if tag = true => favorite item clicked
                Map<String,String> headers = new HashMap<>();
                headers.put("Authorization",Common.buildJWT(Common.API_KEY));
                compositeDisposable.add(myRestaurantAPI.insertFavorite(headers
                                ,foodList.get(position).getId()
                                ,Common.currentRestaurant.getId()
                                ,Common.currentRestaurant.getName()
                                ,foodList.get(position).getName()
                                ,foodList.get(position).getImage()
                                ,foodList.get(position).getPrice())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(favoriteModel -> {
                                    if (favoriteModel.isSuccess() && favoriteModel.getMessage().contains("Success")){
                                        fav.setImageResource(R.drawable.ic_favorite_primary_color_24dp);
                                        fav.setTag(true);
                                        if (Common.currentFavOfRestaurant != null ){
                                            Common.currentFavOfRestaurant.add(new FavoriteOnlyId(foodList.get(position).getId()));
                                        }
                                    }
                                }
                                ,throwable -> {
                                    //Toast.makeText(context, "[REMOVE FAV]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();

                                }));
            }
        });
        holder.setListener((view, position1, isDetail) -> {
            if (isDetail){
                context.startActivity(new Intent(context, FoodDetailActivity.class));
                EventBus.getDefault().postSticky(new FoodDetailEvent(true,foodList.get(position)));
            }else{
                //cart create
                CartItem cartItem = new CartItem();
                cartItem.setFoodId(foodList.get(position).getId());
                cartItem.setFoodName(foodList.get(position).getName());
                cartItem.setFoodPrice(foodList.get(position).getPrice());
                cartItem.setFoodImage(foodList.get(position).getImage());
                cartItem.setFoodQuantity(1);
                cartItem.setUserPhone(Common.currentUser.getUserPhone());
                cartItem.setRestaurantId(Common.currentRestaurant.getId());
                cartItem.setFoodAddon("Normal");
                cartItem.setFoodSize("Normal");
                cartItem.setFoodExtraPrice(0.0);
                cartItem.setFbid(Common.currentUser.getFbid());

                compositeDisposable.add(
                        cartDataSource.insertOrReplaceAll(cartItem)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(()->{
                                    Toast.makeText(context, "ADD To Cart", Toast.LENGTH_SHORT).show();
                            }
                            ,throwable -> {
                                    Toast.makeText(context, "[ADD CART]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            })
                );
            }
        });

    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.img_food)
        ImageView img_food;
        @BindView(R.id.txt_food_name)
        TextView txt_food_name;
        @BindView(R.id.txt_food_price)
        TextView txt_food_price;
        @BindView(R.id.img_detail)
        ImageView img_detail;
        @BindView(R.id.img_cart)
        ImageView img_add_cart;
        @BindView(R.id.img_fav)
        ImageView img_fav;

        IFoodDetailOrCartClickListener listener ;

        public IFoodDetailOrCartClickListener getListener() {
            return listener;
        }

        public void setListener(IFoodDetailOrCartClickListener listener) {
            this.listener = listener;
        }

        Unbinder unbinder ;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this,itemView);

            img_detail.setOnClickListener(this);
            img_add_cart.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.img_detail){
                listener.onFoodItemClickListener(view,getAdapterPosition(),true);
            }
            else if (view.getId() == R.id.img_cart){
                listener.onFoodItemClickListener(view,getAdapterPosition(),false);
            }
        }
    }
}
