package com.project.clientapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ImageView;
import android.widget.Toast;

import com.flaviofaria.kenburnsview.KenBurnsView;
import com.project.clientapp.Adapter.MyFoodAdapter;
import com.project.clientapp.Common.Common;
import com.project.clientapp.Modal.Category;
import com.project.clientapp.Modal.EventBus.FoodListEvent;
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

public class FoodListActivity extends AppCompatActivity {

    MyRestaurantAPI myRestaurantAPI ;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    AlertDialog dialog ;

    @BindView(R.id.img_category)
    KenBurnsView img_category;
    @BindView(R.id.recycler_food_list)
    RecyclerView recycler_food_list;
    @BindView(R.id.toolbar)
    Toolbar toolbar ;

    MyFoodAdapter adapter,searchAdapter;
    private Category selectedCategory;

    LayoutAnimationController layoutAnimationController;


    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        if (adapter != null){
            adapter.onStop();
        }
        if (searchAdapter != null){
            searchAdapter.onStop();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search,menu);

        MenuItem menuItem = menu.findItem(R.id.search);

        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView)menuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        //Event
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                startSearchFood(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        menuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                //Restore to original adapter when use close search
                recycler_food_list.setAdapter(adapter);
                return true;
            }
        });

        return true;
    }

    private void startSearchFood(String query) {
        dialog.show();
        Map<String,String> headers = new HashMap<>();
        headers.put("Authorization",Common.buildJWT(Common.API_KEY));
        compositeDisposable.add(myRestaurantAPI.searchFood(headers,query,selectedCategory.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(foodModel -> {
                    if(foodModel.isSuccess()) {
                        searchAdapter = new MyFoodAdapter(FoodListActivity.this,foodModel.getResult());
                        recycler_food_list.setAdapter(adapter);
                        recycler_food_list.setLayoutAnimation(layoutAnimationController);
                    }
                    else{
                        if (foodModel.getMessage().contains("Empty")){
                            recycler_food_list.setAdapter(null);
                            recycler_food_list.setLayoutAnimation(layoutAnimationController);
                            Toast.makeText(this, "Not Found", Toast.LENGTH_SHORT).show();
                        }
                    }
                        }
                ,throwable -> {
                    dialog.dismiss();
                    Toast.makeText(this, "[SEARCH FOOD]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        }));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        init();

        initView();

    }

    private void initView() {

        ButterKnife.bind(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recycler_food_list.setLayoutManager(layoutManager);
        recycler_food_list.addItemDecoration(new DividerItemDecoration(this,layoutManager.getOrientation()));

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

    //Event Bus
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

    ///Listen EventBus
    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void LoadFoodListByCategory(FoodListEvent event){

        if (event.isSuccess()){

            selectedCategory = event.getCategory();

            Picasso.get().load(event.getCategory().getImage()).into(img_category);
            toolbar.setTitle(event.getCategory().getName());

            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            dialog.show();
            Map<String,String> headers = new HashMap<>();
            headers.put("Authorization",Common.buildJWT(Common.API_KEY));
            compositeDisposable.add(
                    myRestaurantAPI.getFoodOfMenu(headers,event.getCategory().getId())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(foodModel -> {
                                    if (foodModel.isSuccess()){
                                        MyFoodAdapter adapter = new MyFoodAdapter(this,foodModel.getResult());
                                        recycler_food_list.setAdapter(adapter);
                                        recycler_food_list.setLayoutAnimation(layoutAnimationController);
                                    }
                                    else{
                                        Toast.makeText(this, "[GET FOOD RESULT]"+foodModel.getMessage(), Toast.LENGTH_SHORT).show();
                                    }

                                    dialog.dismiss();

                                    }
                                    ,throwable -> {
                                        dialog.dismiss();
                                        Toast.makeText(this, "[GET FOOD]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    })
            );

        }
        else{

        }
        dialog.dismiss();
    }
}
