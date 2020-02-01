package com.project.clientapp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.clientapp.Common.Common;
import com.project.clientapp.Interface.ILoadMore;
import com.project.clientapp.Modal.Order;
import com.project.clientapp.R;

import java.text.SimpleDateFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyOrderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_LOADING = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    Context context;
    List<Order> orderList;
    SimpleDateFormat simpleDateFormat;
    RecyclerView recyclerView;
    ILoadMore loadMore;

    boolean isLoading = false;
    int totalItemCount = 0, lastVisibleItem = 0, visibleThreshold = 10;

    public MyOrderAdapter(Context context, List<Order> orderList, RecyclerView recyclerView) {
        this.context = context;
        this.orderList = orderList;
        this.recyclerView = recyclerView;
        simpleDateFormat = new SimpleDateFormat("MM/DD/YYYY");

        //Init
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = linearLayoutManager.getItemCount();
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                if (!isLoading && totalItemCount <= lastVisibleItem + visibleThreshold) {
                    if (loadMore != null)
                        loadMore.onLoadMore();
                    isLoading = true;
                }
            }
        });
    }

    public void setLoaded() {
        isLoading = false;
    }

    public void addItem(List<Order> addedItems) {

        int startInsertedIndex = orderList.size();
        orderList.addAll(addedItems);
        notifyItemInserted(startInsertedIndex);

    }

    public void setLoadMore(ILoadMore loadMore) {
        this.loadMore = loadMore;
    }

    @Override
    public int getItemViewType(int position) {
        if (orderList.get(position) == null) // if we meet 'null' value in list , we will understand that's the loading state
            return VIEW_TYPE_LOADING;
        else
            return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View itemView ;
        if (viewType == VIEW_TYPE_ITEM){
            itemView = (LayoutInflater.from(context).inflate(R.layout.layout_order, parent, false));
            return new MyViewHolder(itemView);

        }
        else{
            itemView = (LayoutInflater.from(context).inflate(R.layout.layout_loading_item, parent, false));
            return new MyLoadingHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MyViewHolder){
            MyViewHolder myViewHolder = (MyViewHolder)holder;

            myViewHolder.txt_num_of_item.setText(new StringBuilder("Num of Items : ").append(orderList.get(position).getNumOfItem()));
            myViewHolder.txt_order_address.setText(orderList.get(position).getOrderAddress());
            myViewHolder.txt_order_phone.setText(orderList.get(position).getOrderPhone());
            myViewHolder.txt_order_date.setText(new StringBuilder(simpleDateFormat.format(orderList.get(position).getOrderDate())));
            myViewHolder.txt_order_number.setText(new StringBuilder("Order Number: #").append(orderList.get(position).getOrderId()));
            myViewHolder.txt_order_status.setText(Common.convertStatusToString(orderList.get(position).getOrderStatus()));
            myViewHolder.txt_order_total_price.setText(new StringBuilder(context.getString(R.string.money_sign)).append(orderList.get(position).getTotalPrice()));

            if (orderList.get(position).isCod()) {
                myViewHolder.txt_payment_method.setText(new StringBuilder("Cash On Delivery"));
            } else {
                myViewHolder.txt_payment_method.setText(new StringBuilder("Trans ID : ").append(orderList.get(position).getTransactionId()));
            }
        }
        else if (holder instanceof MyLoadingHolder){
            MyLoadingHolder myViewHolder = (MyLoadingHolder)holder;

            myViewHolder.progressBar.setIndeterminate(true);
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.txt_order_number)
        TextView txt_order_number;
        @BindView(R.id.txt_order_status)
        TextView txt_order_status;
        @BindView(R.id.txt_order_address)
        TextView txt_order_address;
        @BindView(R.id.txt_order_phone)
        TextView txt_order_phone;
        @BindView(R.id.txt_order_date)
        TextView txt_order_date;
        @BindView(R.id.txt_order_total_price)
        TextView txt_order_total_price;
        @BindView(R.id.txt_num_of_item)
        TextView txt_num_of_item;
        @BindView(R.id.txt_payment_method)
        TextView txt_payment_method;

        Unbinder unbinder;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
        }
    }

    public class MyLoadingHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.progress_bar)
        ProgressBar progressBar;


        Unbinder unbinder;

        public MyLoadingHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
        }
    }
}
