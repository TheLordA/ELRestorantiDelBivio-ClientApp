package com.project.clientapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.EventLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.clientapp.Common.Common;
import com.project.clientapp.Interface.IOnRecyclerViewClickListener;
import com.project.clientapp.MenuActivity;
import com.project.clientapp.Modal.EventBus.MenuItemEvent;
import com.project.clientapp.Modal.Restaurant;
import com.project.clientapp.R;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyRestaurantAdapter extends RecyclerView.Adapter<MyRestaurantAdapter.MyViewHolder> {

    Context context;
    List<Restaurant> restaurantList;

    public MyRestaurantAdapter(Context context, List<Restaurant> restaurantList) {
        this.context = context;
        this.restaurantList = restaurantList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.layout_restaurant,parent,false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Picasso.get().load(restaurantList.get(position).getImage()).into(holder.img_restaurant);
        holder.txt_restaurant_name.setText(new StringBuilder(restaurantList.get(position).getName()));
        holder.txt_restaurant_address.setText(new StringBuilder(restaurantList.get(position).getAddress()));

        // to avoid crash we should implement
        holder.setListener((view, position1) -> {

            Common.currentRestaurant =restaurantList.get(position);
            //Here  we use postSticky means that this event  will be listen from other activity
            //difference is 'post'
            EventBus.getDefault().postSticky(new MenuItemEvent(true,restaurantList.get(position)));
            context.startActivity(new Intent(context, MenuActivity.class));
        });
    }

    @Override
    public int getItemCount() {
        return restaurantList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.txt_restaurant_name)
        TextView txt_restaurant_name;
        @BindView(R.id.txt_restaurant_address)
        TextView txt_restaurant_address;
        @BindView(R.id.img_restaurant)
        ImageView img_restaurant ;

        IOnRecyclerViewClickListener listener;

        public void setListener(IOnRecyclerViewClickListener listener) {
            this.listener = listener;
        }

        Unbinder unbinder;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this,itemView);

            itemView.setOnClickListener(this);
        }


        @Override
        public void onClick(View view) {
            listener.onClick(view,getAdapterPosition());
        }
    }
}
