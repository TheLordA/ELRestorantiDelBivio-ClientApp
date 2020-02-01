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
import com.project.clientapp.FoodListActivity;
import com.project.clientapp.Interface.IOnRecyclerViewClickListener;
import com.project.clientapp.MenuActivity;
import com.project.clientapp.Modal.Category;
import com.project.clientapp.Modal.EventBus.FoodListEvent;
import com.project.clientapp.R;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyCategoryAdapter extends RecyclerView.Adapter<MyCategoryAdapter.MyViewHolder> {

    Context context;
    List<Category> categorylist ;

    public MyCategoryAdapter(Context context, List<Category> categorylist) {
        this.context = context;
        this.categorylist = categorylist;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.layout_category,parent,false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        Picasso.get().load(categorylist.get(position).getImage()).into(holder.img_category);
        holder.txt_category.setText(new StringBuilder(categorylist.get(position).getName()));

        // to avoid crash we should implement
        holder.setListener((view, position1) -> {
            // send StickyPost event to foodlist Activity
            EventBus.getDefault().postSticky(new FoodListEvent(true,categorylist.get(position)));
            context.startActivity(new Intent(context, FoodListActivity.class));
        });
    }

    @Override
    public int getItemCount() {
        return categorylist.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.img_category)
        ImageView img_category;
        @BindView(R.id.txt_category)
        TextView txt_category;

        IOnRecyclerViewClickListener listener;

        public void setListener(IOnRecyclerViewClickListener listener) {
            this.listener = listener;
        }

        Unbinder unbinder ;
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

    @Override
    public int getItemViewType(int position) {
        if(categorylist.size()==1){
            return Common.DEFAULT_COLUMN_TYPE;
        }
        else{
            if(categorylist.size() % 2 == 0 ){
                return Common.DEFAULT_COLUMN_TYPE;
            }
            else{
                return (position>1 && position == categorylist.size()-1) ? Common.FULL_WIDTH_COLUMN:Common.DEFAULT_COLUMN_TYPE;
            }
        }
    }
}
