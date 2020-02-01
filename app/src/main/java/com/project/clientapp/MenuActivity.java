package com.project.clientapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ImageView;
import android.widget.Toast;

import com.flaviofaria.kenburnsview.KenBurnsView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nex3z.notificationbadge.NotificationBadge;
import com.project.clientapp.Adapter.MyCategoryAdapter;
import com.project.clientapp.Common.Common;
import com.project.clientapp.Database.CartDataSource;
import com.project.clientapp.Database.CartDatabase;
import com.project.clientapp.Database.LocalCartDataSource;
import com.project.clientapp.Modal.EventBus.MenuItemEvent;
import com.project.clientapp.Modal.EventBus.RestaurantLoadEvent;
import com.project.clientapp.Retrofit.MyRestaurantAPI;
import com.project.clientapp.Retrofit.RetrofitClient;
import com.project.clientapp.Utils.SpaceItemDecoration;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import dmax.dialog.SpotsDialog;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MenuActivity extends AppCompatActivity {

    @BindView(R.id.img_restaurant)
    KenBurnsView img_restaurant;
    @BindView(R.id.recycler_category)
    RecyclerView recycler_category;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.fab)
    FloatingActionButton btn_cart;
    @BindView(R.id.badge)
    NotificationBadge badge;

    MyRestaurantAPI myRestaurantAPI ;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    AlertDialog dialog ;

    MyCategoryAdapter adapter;
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
        setContentView(R.layout.activity_menu);

        init();
        initView();

        countCartByRestaurant();
        LoadFavoriteByRestaurant();
    }

    private void LoadFavoriteByRestaurant() {
        Map<String,String> headers = new HashMap<>();
        headers.put("Authorization",Common.buildJWT(Common.API_KEY));
        compositeDisposable.add(myRestaurantAPI.getFavoriteByRestaurant(headers,Common.currentRestaurant.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(favoriteOnlyIdModel ->{
                    if (favoriteOnlyIdModel.isSuccess()){
                        if (favoriteOnlyIdModel.getResult() != null && favoriteOnlyIdModel.getResult().size() > 0){

                            Common.currentFavOfRestaurant = favoriteOnlyIdModel.getResult();
                        }
                        else {

                            Common.currentFavOfRestaurant = new ArrayList<>();
                        }
                    }
                    else{
                        //Toast.makeText(this, "[GET FAVORITE]"+favoriteOnlyIdModel.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                }
                ,throwable -> {
                    Toast.makeText(this, "[GET FAVORITE]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                })
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        countCartByRestaurant();
    }

    private void countCartByRestaurant() {
        cartDataSource.countItemInCart(Common.currentUser.getFbid(),Common.currentRestaurant.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Integer integer) {
                        badge.setText(String.valueOf(integer));
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MenuActivity.this, "[COUNT CART]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initView() {
        ButterKnife.bind(this);

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_item_from_left);

        btn_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this,CartListActivity.class));
            }
        });

        GridLayoutManager layoutManager = new GridLayoutManager(this,2);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter != null){
                    switch (adapter.getItemViewType(position)){
                        case Common.DEFAULT_COLUMN_TYPE :return 1;
                        case Common.FULL_WIDTH_COLUMN: return 2;
                        default: return -1 ;
                    }
                }
                else
                    return -1;
            }
        });
        recycler_category.setLayoutManager(layoutManager);
        recycler_category.addItemDecoration(new SpaceItemDecoration(8));
    }

    private void init() {
        dialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();
        myRestaurantAPI = RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(MyRestaurantAPI.class);
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(this).cartDAO());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /* Register EVENT BUS */

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    //Listen EventBus
    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void LoadMenuByRestaurant(MenuItemEvent  event){
        if (event.isSuccess()){
            Picasso.get().load(event.getRestaurant().getImage()).into(img_restaurant);
            toolbar.setTitle(event.getRestaurant().getName());

            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            Map<String,String> headers = new HashMap<>();
            headers.put("Authorization",Common.buildJWT(Common.API_KEY));
            // request catygory by restaurant Id
            compositeDisposable.add(
                    myRestaurantAPI.getCategories(headers,event.getRestaurant().getId())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(menuModel -> {
                                        adapter = new MyCategoryAdapter(MenuActivity.this,menuModel.getResult());
                                        recycler_category.setAdapter(adapter);
                                        recycler_category.setLayoutAnimation(layoutAnimationController);
                                    }
                            ,throwable -> {
                                        Toast.makeText(this, "[GET CATEGORY]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    })
            );

        }
        else{

        }
        dialog.dismiss();
    }

}
