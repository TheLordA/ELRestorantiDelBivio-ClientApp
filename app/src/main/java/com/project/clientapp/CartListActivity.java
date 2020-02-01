package com.project.clientapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.project.clientapp.Adapter.MyCartAdapter;
import com.project.clientapp.Common.Common;
import com.project.clientapp.Database.CartDataSource;
import com.project.clientapp.Database.CartDatabase;
import com.project.clientapp.Database.LocalCartDataSource;
import com.project.clientapp.Modal.EventBus.CalculatePriceEvent;
import com.project.clientapp.Modal.EventBus.SendTotalCostEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CartListActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.recycler_cart)
    RecyclerView recycler_cart;
    @BindView(R.id.txt_final_cost)
    TextView txt_final_cost;
    @BindView(R.id.btn_order)
    Button btn_order;

    CompositeDisposable compositeDisposable = new CompositeDisposable();
    CartDataSource cartDataSource ;

    LayoutAnimationController layoutAnimationController;


    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_list);

        init();
        initView();

        getAllItemInCart();

    }

    private void getAllItemInCart() {
        compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getFbid(),Common.currentRestaurant.getId())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(cartItems -> {
                    if (cartItems.isEmpty()){
                        btn_order.setText(R.string.empty_cart);
                        btn_order.setEnabled(false);
                        btn_order.setBackgroundResource(android.R.color.darker_gray);

                        MyCartAdapter adapter = new MyCartAdapter(CartListActivity.this,cartItems);
                        recycler_cart.setAdapter(adapter);

                        calculateCartTotalPrice();
                    }
                    else{
                        btn_order.setText(R.string.place_order);
                        btn_order.setEnabled(true);
                        btn_order.setBackgroundResource(R.color.colorPrimary);
                    }
                }
                ,throwable -> {

                })
        );
    }

    private void calculateCartTotalPrice() {
        cartDataSource.sumPrice(Common.currentUser.getFbid(),Common.currentRestaurant.getId())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SingleObserver<Long>() {
                @Override
                public void onSubscribe(Disposable d) {

                }

                @Override
                public void onSuccess(Long aLong) {
                    if (aLong <= 0) {
                        btn_order.setText(getString(R.string.empty_cart));
                        btn_order.setEnabled(false);
                        btn_order.setBackgroundResource(android.R.color.darker_gray);
                    }
                    else{
                        btn_order.setText(getString(R.string.place_order));
                        btn_order.setEnabled(true);
                        btn_order.setBackgroundResource(R.color.colorPrimary);
                    }

                    txt_final_cost.setText(String.valueOf(aLong));
                }

                @Override
                public void onError(Throwable e) {
                    if (e.getMessage().contains("Query returned empty"))
                        txt_final_cost.setText("0");
                    else
                        Toast.makeText(CartListActivity.this, "[SUM CART]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        ButterKnife.bind(this);

        toolbar.setTitle(getString(R.string.cart));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        recycler_cart.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recycler_cart.setLayoutManager(layoutManager);
        recycler_cart.addItemDecoration(new DividerItemDecoration(this,layoutManager.getOrientation()));
        recycler_cart.setLayoutAnimation(layoutAnimationController);

        btn_order.setOnClickListener(view -> {
            EventBus.getDefault().postSticky(new SendTotalCostEvent(txt_final_cost.getText().toString()));
            startActivity(new Intent(CartListActivity.this,PlaceOrderActivity.class));

        });

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_item_from_left);

    }

    private void init() {

        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(this).cartDAO());
    }

    //EventBus


    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Subscribe(sticky = true ,threadMode = ThreadMode.MAIN)
    public void calculatePrice(CalculatePriceEvent event){
        if (event != null)
            calculateCartTotalPrice();
    }
}
