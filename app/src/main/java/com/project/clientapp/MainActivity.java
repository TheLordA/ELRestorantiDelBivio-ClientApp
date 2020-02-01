package com.project.clientapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.project.clientapp.Common.Common;
import com.project.clientapp.Retrofit.MyRestaurantAPI;
import com.project.clientapp.Retrofit.RetrofitClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    MyRestaurantAPI myRestaurantAPI ;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    AlertDialog dialog ;

    private List<AuthUI.IdpConfig> providers ;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;

    private static final int APP_REQUEST_CODE = 1234;
    @BindView(R.id.btn_sign_in)
    Button btn_sign_in;
    @OnClick(R.id.btn_sign_in)

    void loginuser() {

        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setIsSmartLockEnabled(false)
                .setAvailableProviders(providers).build(),APP_REQUEST_CODE);
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_REQUEST_CODE){

            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode==RESULT_OK)
            {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();//call Listener OnChange
            }
            else
            {
                Toast.makeText(this, "Failed to sign In", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        init();
    }

    private void init() {

        Paper.init(this);

        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build());
        firebaseAuth = FirebaseAuth.getInstance();
        listener = firebaseAuth1 -> {
            FirebaseUser user = firebaseAuth1.getCurrentUser();
            if (user!=null) // user already logged
            {
                dialog.show();

                Paper.book().write(Common.REMEMBER_FBID,user.getUid());

                FirebaseInstanceId.getInstance()
                        .getInstanceId()
                        .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "[GET TOKEN]"+e.getMessage(), Toast.LENGTH_SHORT).show())
                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        Map<String,String> headers = new HashMap<>();
                        headers.put("Authorization",Common.buildJWT(Common.API_KEY));

                        System.out.println("Main :"+headers);

                        compositeDisposable.add(myRestaurantAPI.updateTokenToServer(headers,task.getResult().getToken())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(tokenModel -> {
                                    if (!tokenModel.isSuccess())
                                        Toast.makeText(MainActivity.this, "[UPDATE TOKEN]" + tokenModel.getMessage(), Toast.LENGTH_SHORT).show();

                                    compositeDisposable.add(myRestaurantAPI.getUser(headers)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(userModel -> {
                                                if (userModel.isSuccess()) // if user already in database
                                                {
                                                    Common.currentUser = userModel.getResult().get(0);
                                                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                } else // if the user isn't available in the database , redirect to UpdateInfo to register
                                                {
                                                    Intent intent = new Intent(MainActivity.this, UpdateInfoActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                }
                                                dialog.dismiss();
                                            }, throwable -> {
                                                dialog.dismiss();
                                                Toast.makeText(MainActivity.this, "[GET USER]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                            })
                                    );
                                },throwable -> {
                                    dialog.dismiss();
                                    Toast.makeText(MainActivity.this, "[GET USER]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                })
                        );
                        /*try {
                            wait(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }*/
                    }
                });

                dialog.show();

            }
            else
                {
                    loginuser();
                }
        };
        dialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();
        myRestaurantAPI = RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(MyRestaurantAPI.class);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (listener !=null && firebaseAuth !=null)
            firebaseAuth.addAuthStateListener(listener);
    }

    @Override
    protected void onStop() {
        if (listener !=null && firebaseAuth !=null)
            firebaseAuth.removeAuthStateListener(listener);
        super.onStop();
    }
}
