package com.project.clientapp;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.project.clientapp.Adapter.MyRestaurantAdapter;
import com.project.clientapp.Adapter.RestaurantSliderAdapter;
import com.project.clientapp.Common.Common;
import com.project.clientapp.Modal.EventBus.RestaurantLoadEvent;
import com.project.clientapp.Modal.Restaurant;
import com.project.clientapp.Retrofit.MyRestaurantAPI;
import com.project.clientapp.Retrofit.RetrofitClient;
import com.project.clientapp.Services.PicassoImageLoadingService;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import ss.com.bannerslider.Slider;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    TextView txt_user_name,txt_user_phone;

    @BindView(R.id.banner_slider)
    Slider banner_slider;
    @BindView(R.id.recycler_restaurant)
    RecyclerView recycler_restaurant;


    MyRestaurantAPI myRestaurantAPI ;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    android.app.AlertDialog dialog ;

    LayoutAnimationController layoutAnimationController;


    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,drawer,toolbar,R.string.navigation_drawer_open,R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);

        txt_user_name=(TextView)headerView.findViewById(R.id.txt_user_name);
        txt_user_phone=(TextView)headerView.findViewById(R.id.txt_user_phone);

        txt_user_name.setText(Common.currentUser.getName());
        txt_user_phone.setText(Common.currentUser.getUserPhone());

        init();
        initView();
        LoadRestaurant();

    }

    private void LoadRestaurant() {
        dialog.show();
        Map<String,String> headers = new HashMap<>();
        headers.put("Authorization",Common.buildJWT(Common.API_KEY));
        compositeDisposable.add(
                myRestaurantAPI.getRestaurant(headers)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(restaurantModel -> {
                            //We Use EventBus to send local events
                            EventBus.getDefault().post(new RestaurantLoadEvent(true,restaurantModel.getResult()));
                    }
                    ,throwable -> {
                        EventBus.getDefault().post(new RestaurantLoadEvent(false,throwable.getMessage()));
                    })
        );

    }

    private void initView() {
        ButterKnife.bind(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recycler_restaurant.setLayoutManager(layoutManager);
        recycler_restaurant.addItemDecoration(new DividerItemDecoration(this,layoutManager.getOrientation()));

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_item_from_left);
    }

    private void init() {
        dialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();
        myRestaurantAPI = RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(MyRestaurantAPI.class);

        Slider.init(new PicassoImageLoadingService());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings)
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item){
        int id= item.getItemId();
        if (id == R.id.nav_log_out) {
            signOut();
        }
        else if (id == R.id.nav_nearby){
            startActivity(new Intent(HomeActivity.this, NearByRestaurantActivity.class));
        }
        else if (id == R.id.nav_order_history){
            startActivity(new Intent(HomeActivity.this,ViewOrderActivity.class));
        }
        else if (id == R.id.nav_update_info){
            startActivity(new Intent(HomeActivity.this,UpdateInfoActivity.class));
        }
        else if (id == R.id.nav_fav){
            startActivity(new Intent(HomeActivity.this,FavoriteActivity.class));
        }


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void signOut() {
            // alert dialog to confirm
            AlertDialog confirmDialog = new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Do You Really Want to Sign Out ?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("OK", (dialog, which) -> {

                    Common.currentUser = null ;
                    Common.currentRestaurant = null ;

                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(HomeActivity.this,MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }).create();

            confirmDialog.show();
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
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void processRestaurantLoadEvent(RestaurantLoadEvent event){
        if (event.isSuccess()){
            displayBanner(event.getRestaurantList());
            displayRestaurant(event.getRestaurantList());
        }
        else{
            Toast.makeText(this, "[RESTAURANT LOAD]"+event.getMessage(), Toast.LENGTH_SHORT).show();
        }
        dialog.dismiss();
    }

    private void displayRestaurant(List<Restaurant> restaurantList) {
        MyRestaurantAdapter adapter = new MyRestaurantAdapter(this,restaurantList);
        recycler_restaurant.setAdapter(adapter);
        recycler_restaurant.setLayoutAnimation(layoutAnimationController);
    }

    private void displayBanner(List<Restaurant> restaurantList) {
        banner_slider.setAdapter(new RestaurantSliderAdapter(restaurantList));
    }
}
