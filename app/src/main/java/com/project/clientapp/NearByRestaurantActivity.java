package com.project.clientapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.project.clientapp.Common.Common;
import com.project.clientapp.Modal.EventBus.MenuItemEvent;
import com.project.clientapp.Modal.Restaurant;
import com.project.clientapp.Retrofit.MyRestaurantAPI;
import com.project.clientapp.Retrofit.RetrofitClient;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class NearByRestaurantActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    MyRestaurantAPI myRestaurantAPI ;
    CompositeDisposable compositeDisposable = new CompositeDisposable();

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    AlertDialog dialog ;

    LocationRequest locationRequest ;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location currentLocation;

    Marker userMarker ;

    boolean isFirsLoad = false ;

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_near_by_restaurant);

        init();
        initView();

    }

    private void initView() {
        ButterKnife.bind(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        toolbar.setTitle(getString(R.string.nearby_restaurant));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void init() {
        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(this).build();
        myRestaurantAPI = RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(MyRestaurantAPI.class);

        buildLocationRequest();
        buildLocationCallBack();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());
        // it may check for permission automatically , we wilml live that comment here to refer where should it be done
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();
                addMarkerAndMoveCamera(locationResult.getLastLocation());

                if (!isFirsLoad){
                    isFirsLoad = !isFirsLoad;
                    requestNearbyRestaurant(locationResult.getLastLocation().getLatitude(),locationResult.getLastLocation().getLongitude(),10);
                }
            }
        };

    }

    private void requestNearbyRestaurant(double latitude, double longitude, int distance) {
        dialog.show();
        Map<String,String> headers = new HashMap<>();
        headers.put("Authorization",Common.buildJWT(Common.API_KEY));
        compositeDisposable.add(myRestaurantAPI.getNearByRestaurant(headers,latitude,longitude,distance)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(restaurantModel -> {
                    if (restaurantModel.isSuccess()){
                        addRestaurantMarker(restaurantModel.getResult());
                    }
                    else{
                        Toast.makeText(this, ""+restaurantModel.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();

                },throwable -> {
                    dialog.dismiss();
                    Toast.makeText(this, "[NEARBY RESTAURANT]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                })
        );
    }

    private void addRestaurantMarker(List<Restaurant> restaurantList) {
        for (Restaurant restaurant:restaurantList){
            mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurant_marker))
            .position(new LatLng(restaurant.getLat(),restaurant.getLng()))
            .snippet(restaurant.getAddress())
            .title(new StringBuilder().append(restaurant.getId()).append(".").append(restaurant.getName()).toString())
            );
        }
    }

    private void addMarkerAndMoveCamera(Location lastLocation) {
        if(userMarker != null)
            userMarker.remove();

        LatLng userLatLng = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
        userMarker = mMap.addMarker(new MarkerOptions().position(userLatLng).title(Common.currentUser.getName()));
        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(userLatLng,17);
        mMap.animateCamera(yourLocation);
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,R.raw.map_style));
            if (!success)
                Log.e("ERROR_MAP","Load style error");
        } catch (Resources.NotFoundException e) {
            Log.e("ERROR_MAP","Resource not found ");
        }

        mMap.setOnInfoWindowClickListener(marker -> {
            String id = marker.getTitle().substring(0,marker.getTitle().indexOf("."));
            if (!TextUtils.isEmpty(id)){
                Map<String,String> headers = new HashMap<>();
                headers.put("Authorization",Common.buildJWT(Common.API_KEY));
                compositeDisposable.add(myRestaurantAPI.getRestaurantById(headers,id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(restaurantByIdModel -> {
                            if (restaurantByIdModel.isSuccess()){
                                Common.currentRestaurant = restaurantByIdModel.getResult().get(0);
                                EventBus.getDefault().postSticky(new MenuItemEvent(true,Common.currentRestaurant));
                                startActivity(new Intent(NearByRestaurantActivity.this,MenuActivity.class));
                                finish();
                            }else{
                                Toast.makeText(this, ""+restaurantByIdModel.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                ,throwable -> {
                            Toast.makeText(this, "[GET RESTAYRANT BY ID]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        })
                );
            }
        });
    }
}
