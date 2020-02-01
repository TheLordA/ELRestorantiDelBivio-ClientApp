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
import com.project.clientapp.FoodDetailActivity;
import com.project.clientapp.Interface.IOnRecyclerViewClickListener;
import com.project.clientapp.Modal.EventBus.FoodDetailEvent;
import com.project.clientapp.Modal.Favorite;
import com.project.clientapp.Modal.Restaurant;
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
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;

public class MyFavoriteAdapter extends RecyclerView.Adapter<MyFavoriteAdapter.MyViewHolder> {

    Context context;
    List<Favorite> favoriteList;
    CompositeDisposable compositeDisposable;
    MyRestaurantAPI myRestaurantAPI;

    public MyFavoriteAdapter(Context context, List<Favorite> favoriteList) {
        this.context = context;
        this.favoriteList = favoriteList;
        compositeDisposable = new CompositeDisposable();
        myRestaurantAPI = RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(MyRestaurantAPI.class);
    }

    public void onDestroy() {
        compositeDisposable.clear();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.layout_favorite_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        Picasso.get().load(favoriteList.get(position).getFoodImage()).into(holder.img_food);
        holder.txt_food_name.setText(favoriteList.get(position).getFoodName());
        holder.txt_food_price.setText(new StringBuilder(context.getString(R.string.money_sign)).append(favoriteList.get(position).getPrice()));
        holder.txt_restaurant_name.setText(favoriteList.get(position).getRestaurantName());

        //Event
        holder.setListener((view, position1) -> {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", Common.buildJWT(Common.API_KEY));
            compositeDisposable.add(myRestaurantAPI.getFoodById(headers, favoriteList.get(position).getFoodId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(foodModel -> {
                                if (foodModel.isSuccess()) {
                                    //When user click to add to favorite an item , just start FoodDetailActivity
                                    context.startActivity(new Intent(context, FoodDetailActivity.class));
                                    if (Common.currentRestaurant == null) {
                                        compositeDisposable.add(myRestaurantAPI.getRestaurantById(headers, String.valueOf(favoriteList.get(position).getRestaurantId()))
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(restaurantModel -> {
                                                            if (restaurantModel.isSuccess()) {
                                                                Common.currentRestaurant = restaurantModel.getResult().get(0);
                                                                EventBus.getDefault().postSticky(new FoodDetailEvent(true, foodModel.getResult().get(0)));

                                                            } else {
                                                                Toast.makeText(context, "" + restaurantModel.getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                        , throwable -> {
                                                            Toast.makeText(context, "[GET RESTAURANT BY ID]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                        })
                                        );
                                    } else {
                                        EventBus.getDefault().postSticky(new FoodDetailEvent(true, foodModel.getResult().get(0)));
                                    }
                                } else {
                                    Toast.makeText(context, "[GET FOOD BY ID RESULT]" + foodModel.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            , throwable -> {
                                Toast.makeText(context, "[GET FOOD BY ID]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            })
            );
        });
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.img_food)
        ImageView img_food;
        @BindView(R.id.txt_food_name)
        TextView txt_food_name;
        @BindView(R.id.txt_food_price)
        TextView txt_food_price;
        @BindView(R.id.txt_restaurant_name)
        TextView txt_restaurant_name;

        Unbinder unbinder;

        IOnRecyclerViewClickListener listener;

        public void setListener(IOnRecyclerViewClickListener listener) {
            this.listener = listener;
        }

        public MyViewHolder(@NonNull View itemView, Unbinder unbinder) {
            super(itemView);
            this.unbinder = unbinder;
        }

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            unbinder = ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onClick(view, getAdapterPosition());
        }
    }
}
