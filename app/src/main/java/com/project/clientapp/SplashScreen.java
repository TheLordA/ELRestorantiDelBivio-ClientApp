package com.project.clientapp;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.project.clientapp.Common.Common;
import com.project.clientapp.Retrofit.MyRestaurantAPI;
import com.project.clientapp.Retrofit.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class SplashScreen extends AppCompatActivity {

    MyRestaurantAPI myRestaurantAPI ;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    AlertDialog dialog ;

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        //GET TOKEN
                        FirebaseInstanceId.getInstance()
                                .getInstanceId()
                                .addOnFailureListener(e -> Toast.makeText(SplashScreen.this, "[GET TOKEN]"+e.getMessage(), Toast.LENGTH_SHORT).show())
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()){

                                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                        if (user != null ){

                                            Paper.book().write(Common.REMEMBER_FBID,user.getUid());
                                            Log.i("UID : ",Common.REMEMBER_FBID);
                                            dialog.show();
                                            compositeDisposable.add(myRestaurantAPI.getKey(user.getUid())
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(getKeyModel -> {
                                                        if (getKeyModel.isSuccess()){
                                                            //store JWT value
                                                            Common.API_KEY=getKeyModel.getToken();
                                                            Log.i("API_KEY",Common.API_KEY);
                                                            //After we have account, we will get fbid and update Token
                                                            Map<String,String> headers = new HashMap<>();

                                                            headers.put("Authorization",Common.buildJWT(Common.API_KEY));

                                                            System.out.println(headers);
                                                            Log.i("JWT Built",Common.buildJWT(Common.API_KEY));
                                                            Log.i("Token",task.getResult().getToken());

                                                            compositeDisposable.add(myRestaurantAPI.updateTokenToServer(headers,task.getResult().getToken())
                                                                    .subscribeOn(Schedulers.io())
                                                                    .observeOn(AndroidSchedulers.mainThread())
                                                                    .subscribe(tokenModel -> {
                                                                        if (!task.isSuccessful())
                                                                            Toast.makeText(SplashScreen.this, "[UPDATE TOKEN ERROR]"+tokenModel.getMessage(), Toast.LENGTH_SHORT).show();
                                                                        compositeDisposable.add(myRestaurantAPI.getUser(headers)
                                                                                .subscribeOn(Schedulers.io())
                                                                                .observeOn(AndroidSchedulers.mainThread())
                                                                                .subscribe(userModel -> {
                                                                                            if (userModel.isSuccess()) // check user availability in database
                                                                                            {
                                                                                                Common.currentUser = userModel.getResult().get(0);
                                                                                                Log.i("",Common.currentUser.getFbid());
                                                                                                Intent intent = new Intent(SplashScreen.this,HomeActivity.class);
                                                                                                startActivity(intent);
                                                                                                finish();
                                                                                            }
                                                                                            else // if the user isn't available in the database , redirect to UpdateInfo to register
                                                                                            {
                                                                                                Intent intent = new Intent(SplashScreen.this,UpdateInfoActivity.class);
                                                                                                startActivity(intent);
                                                                                                finish();
                                                                                            }
                                                                                            dialog.dismiss();
                                                                                        }
                                                                                        ,throwable -> {
                                                                                            dialog.dismiss();
                                                                                            Toast.makeText(SplashScreen.this, "[GET USER API]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                        })
                                                                        );

                                                                    },throwable -> {
                                                                        Toast.makeText(SplashScreen.this, "[UPDATE TOKEN]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    })
                                                            );
                                                        }
                                                        else{
                                                            dialog.dismiss();
                                                            Toast.makeText(SplashScreen.this, ""+getKeyModel.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                    ,throwable -> {
                                                        dialog.dismiss();
                                                        Toast.makeText(SplashScreen.this, "Cannot Get JWT", Toast.LENGTH_SHORT).show();
                                                    })
                                            );

                                        }
                                        else{
                                            Toast.makeText(SplashScreen.this, "Not signed In ! Please Sign In ", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SplashScreen.this,MainActivity.class));
                                            finish();
                                        }
                                    }
                                });
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(SplashScreen.this,"You must accept that permission to use our app",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
    }

    private void init() {
        Paper.init(this);
        dialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();
        myRestaurantAPI = RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(MyRestaurantAPI.class);
    }

}
