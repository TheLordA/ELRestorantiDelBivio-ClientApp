package com.project.clientapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.flaviofaria.kenburnsview.KenBurnsView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.project.clientapp.Adapter.MyAddonAdapter;
import com.project.clientapp.Common.Common;
import com.project.clientapp.Database.CartDataSource;
import com.project.clientapp.Database.CartDatabase;
import com.project.clientapp.Database.CartItem;
import com.project.clientapp.Database.LocalCartDataSource;
import com.project.clientapp.Modal.EventBus.AddOnEventChange;
import com.project.clientapp.Modal.EventBus.AddonLoadEvent;
import com.project.clientapp.Modal.EventBus.FoodDetailEvent;
import com.project.clientapp.Modal.EventBus.SizeLoadEvent;
import com.project.clientapp.Modal.Food;
import com.project.clientapp.Modal.Size;
import com.project.clientapp.Retrofit.MyRestaurantAPI;
import com.project.clientapp.Retrofit.RetrofitClient;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class FoodDetailActivity extends AppCompatActivity {

    @BindView(R.id.fab_add_to_cart)
    FloatingActionButton fab_add_to_cart;
    @BindView(R.id.btn_view_cart)
    Button btn_view_cart;
    @BindView(R.id.txt_money)
    TextView txt_money;
    @BindView(R.id.rdi_group_size)
    RadioGroup rdi_group_size;
    @BindView(R.id.recycler_addon)
    RecyclerView recycler_addon;
    @BindView(R.id.txt_description)
    TextView txt_description;
    @BindView(R.id.img_food_detail)
    KenBurnsView img_food_detail;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    MyRestaurantAPI myRestaurantAPI;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    AlertDialog dialog;
    CartDataSource cartDataSource;

    Food selectedFood;
    Double originalPrice;
    private double sizePrice = 0.0;
    private String sizeSelected;
    private double addonPrice = 0.0;
    private double extraPrice;

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_detail);

        init();
        initView();

    }

    private void init() {
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(this).cartDAO());
        dialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();
        myRestaurantAPI = RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(MyRestaurantAPI.class);

    }

    private void initView() {

        ButterKnife.bind(this);

        fab_add_to_cart.setOnClickListener(v -> {
            //cart create
            CartItem cartItem = new CartItem();
            cartItem.setFoodId(selectedFood.getId());
            cartItem.setFoodName(selectedFood.getName());
            cartItem.setFoodPrice(selectedFood.getPrice());
            cartItem.setFoodImage(selectedFood.getImage());
            cartItem.setFoodQuantity(1);
            cartItem.setUserPhone(Common.currentUser.getUserPhone());
            cartItem.setRestaurantId(Common.currentRestaurant.getId());
            cartItem.setFoodAddon(new Gson().toJson(Common.addonList));
            cartItem.setFoodSize(sizeSelected);
            cartItem.setFoodExtraPrice(extraPrice);
            cartItem.setFbid(Common.currentUser.getFbid());

            compositeDisposable.add(
                    cartDataSource.insertOrReplaceAll(cartItem)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(() -> {
                                        Toast.makeText(FoodDetailActivity.this, "ADD To Cart", Toast.LENGTH_SHORT).show();
                                    }
                                    , throwable -> {
                                        Toast.makeText(FoodDetailActivity.this, "[ADD CART]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    })
            );

        });

        btn_view_cart.setOnClickListener(v -> {
            startActivity(new Intent(FoodDetailActivity.this, CartListActivity.class));
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //EventBus
    @Override
    protected void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void displayFoodDetail(FoodDetailEvent event) {
        if (event.isSuccess()) {
            toolbar.setTitle(event.getFood().getImage());
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            selectedFood = event.getFood();
            originalPrice = event.getFood().getPrice();

            txt_money.setText(String.valueOf(originalPrice));
            txt_description.setText(event.getFood().getDescription());
            Picasso.get().load(event.getFood().getImage()).into(img_food_detail);

            if (event.getFood().isSize() && event.getFood().isAddon()) {
                //Load size and addon from server
                dialog.show();
                Map<String,String> headers = new HashMap<>();
                headers.put("Authorization",Common.buildJWT(Common.API_KEY));
                compositeDisposable.add(
                        myRestaurantAPI.getSizeOfFood(headers, event.getFood().getId())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(sizeModel -> {
                                            //Send Local EventBus
                                            EventBus.getDefault().post(new SizeLoadEvent(true, sizeModel.getResult()));

                                            //Load addon after loding Size
                                            dialog.show();
                                            compositeDisposable.add(
                                                    myRestaurantAPI.getAddonOfFood(headers, event.getFood().getId())
                                                            .subscribeOn(Schedulers.io())
                                                            .observeOn(AndroidSchedulers.mainThread())
                                                            .subscribe(addonModel -> {
                                                                        dialog.dismiss();
                                                                        //Send Local EventBus
                                                                        EventBus.getDefault().post(new AddonLoadEvent(true, addonModel.getResult()));
                                                                    }
                                                                    , throwable -> {
                                                                        dialog.dismiss();
                                                                        Toast.makeText(this, "[LOAD ADDON]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    })
                                            );

                                        }
                                        , throwable -> {
                                            dialog.dismiss();
                                            Toast.makeText(this, "[LOAD SIZE]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                        })
                );
            } else {
                if (event.getFood().isAddon()) { //if food have addon only
                    dialog.show();
                    Map<String,String> headers = new HashMap<>();
                    headers.put("Authorization",Common.buildJWT(Common.API_KEY));
                    compositeDisposable.add(
                            myRestaurantAPI.getAddonOfFood(headers, event.getFood().getId())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(addonModel -> {
                                                dialog.dismiss();
                                                //Send Local EventBus
                                                EventBus.getDefault().post(new AddonLoadEvent(true, addonModel.getResult()));
                                            }
                                            , throwable -> {
                                                dialog.dismiss();
                                                Toast.makeText(this, "[LOAD ADDON]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                            })
                    );
                }
                if (event.getFood().isSize()) { // if food have size only
                    dialog.show();
                    Map<String,String> headers = new HashMap<>();
                    headers.put("Authorization",Common.buildJWT(Common.API_KEY));
                    compositeDisposable.add(
                            myRestaurantAPI.getSizeOfFood(headers, event.getFood().getId())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(sizeModel -> {
                                                //Send Local EventBus
                                                EventBus.getDefault().post(new SizeLoadEvent(true, sizeModel.getResult()));
                                            }
                                            , throwable -> {
                                                dialog.dismiss();
                                                Toast.makeText(this, "[LOAD SIZE]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                            })
                    );
                }
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void displaySize(SizeLoadEvent event) {
        if (event.isSuccess()) {
            // Create Radio Button base on sizelist length
            for (Size size : event.getSizeList()) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean b) {
                        if (b)
                            sizePrice = size.getExtraPrice();
                        calculatePrice();
                        sizeSelected = size.getDescription();
                    }
                });

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.1f);
                radioButton.setLayoutParams(params);
                radioButton.setText(size.getDescription());
                radioButton.setTag(size.getExtraPrice());

                rdi_group_size.addView(radioButton);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void displayAddon(AddonLoadEvent event) {
        if (event.isSuccess()) {

            recycler_addon.setHasFixedSize(true);
            recycler_addon.setLayoutManager(new LinearLayoutManager(this));
            recycler_addon.setAdapter(new MyAddonAdapter(FoodDetailActivity.this, event.getAddonList()));
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void priceChange(AddOnEventChange eventChange) {
        if (eventChange.isAdd())
            addonPrice += eventChange.getAddon().getExtraPrice();
        else
            addonPrice -= eventChange.getAddon().getExtraPrice();
        calculatePrice();
    }

    private void calculatePrice() {

        extraPrice = 0.0;
        double newPrice;

        extraPrice += sizePrice;
        extraPrice += addonPrice;

        newPrice = originalPrice + extraPrice;

        txt_money.setText(String.valueOf(newPrice));
    }
}
