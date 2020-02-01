package com.project.clientapp;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Toast;

import com.project.clientapp.Adapter.MyOrderAdapter;
import com.project.clientapp.Common.Common;
import com.project.clientapp.Interface.ILoadMore;
import com.project.clientapp.Modal.Order;
import com.project.clientapp.Retrofit.MyRestaurantAPI;
import com.project.clientapp.Retrofit.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ViewOrderActivity extends AppCompatActivity implements ILoadMore {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.recycler_view_order)
    RecyclerView recycler_view_order;

    MyRestaurantAPI myRestaurantAPI ;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    AlertDialog dialog ;

    MyOrderAdapter adapter;
    List<Order> orderList;

    int maxData = 0 ;

    LayoutAnimationController layoutAnimationController;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_order);

        init();
        initView();

        //getAllOrder();

        getMaxOrder();
    }

    private void getMaxOrder() {
        dialog.show();
        Map<String,String> headers = new HashMap<>();
        headers.put("Authorization",Common.buildJWT(Common.API_KEY));
        compositeDisposable.add(myRestaurantAPI.getMaxOrder(headers)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(maxOrderModel -> {

                            if (maxOrderModel.isSuccess()){
                                maxData = maxOrderModel.getResult().get(0).getMaxRowNum();
                                dialog.dismiss();

                                getAllOrder(0,10);
                            }
                        }
                        ,throwable -> {
                            dialog.dismiss();
                            Toast.makeText(this, "[GET ORDER]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        })
        );
    }

    private void getAllOrder(int from , int to) {
        dialog.show();
        Map<String,String> headers = new HashMap<>();
        headers.put("Authorization",Common.buildJWT(Common.API_KEY));
        compositeDisposable.add(myRestaurantAPI.getOrder(headers,from,to)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(orderModel -> {

                if (orderModel.isSuccess()){
                    if (orderModel.getResult().size() > 0){
                        //Create Adapter
                        orderList = new ArrayList<>();
                        orderList = (orderModel.getResult());
                        adapter = new MyOrderAdapter(this,orderList,recycler_view_order);
                        adapter.setLoadMore(this);
                        recycler_view_order.setAdapter(adapter);
                        recycler_view_order.setLayoutAnimation(layoutAnimationController);
                    }
                    else{
                        orderList.remove(orderList.size()-1); // here we remove null object after the loading is done , if not loading view still available
                        orderList = orderModel.getResult();
                        adapter.addItem(orderList);
                    }
                    dialog.dismiss();
                }

                    }
            ,throwable -> {
                dialog.dismiss();
                Toast.makeText(this, "[GET ORDER]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
            })
        );
    }

    private void initView() {
        ButterKnife.bind(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recycler_view_order.setLayoutManager(layoutManager);
        recycler_view_order.addItemDecoration(new DividerItemDecoration(this,layoutManager.getOrientation()));

        toolbar.setTitle(getString(R.string.place_order));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_item_from_left);

    }

    private void init() {
        dialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();
        myRestaurantAPI = RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(MyRestaurantAPI.class);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoadMore() {
        // when loadmore being called
        // first , we will check data count with max data
        if (adapter.getItemCount() < maxData){
            // add null object to the list in order to tell adapter to show loading state
            orderList.add(null);
            adapter.notifyItemInserted(orderList.size()-1);
            getAllOrder(adapter.getItemCount()+1,adapter.getItemCount()+10);

            adapter.notifyDataSetChanged();
            adapter.setLoaded();
        }
        else{
            Toast.makeText(this, "Max DATA To Load", Toast.LENGTH_SHORT).show();
        }
    }
}
