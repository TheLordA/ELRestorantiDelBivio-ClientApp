package com.project.clientapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.project.clientapp.Common.Common;
import com.project.clientapp.Retrofit.MyRestaurantAPI;
import com.project.clientapp.Retrofit.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class UpdateInfoActivity extends AppCompatActivity {

    MyRestaurantAPI myRestaurantAPI;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    AlertDialog dialog;

    @BindView(R.id.edt_user_name)
    EditText edt_user_name;
    @BindView(R.id.edt_user_address)
    EditText edt_user_address;

    @BindView(R.id.btn_update)
    Button btn_update;
    @BindView(R.id.toolbar)
    Toolbar toolbar;



    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_info);
        ButterKnife.bind(this);
        init();
        initView();
    }

    //override back Arrow


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home)
        {
            finish();//close this activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        toolbar.setTitle(getString(R.string.update_information));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null){

                    dialog.show();
                    Map<String,String> headers = new HashMap<>();
                    headers.put("Authorization",Common.buildJWT(Common.API_KEY));
                    compositeDisposable.add(myRestaurantAPI.updateUserInfo(headers,
                            user.getPhoneNumber(),
                            edt_user_name.getText().toString(),
                            edt_user_address.getText().toString())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(UpdateUserModel -> {
                                        if (UpdateUserModel.isSuccess())
                                        {
                                            //if user has been update just refresh again
                                            compositeDisposable.add(
                                                    myRestaurantAPI.getUser(headers)
                                                            .subscribeOn(Schedulers.io())
                                                            .observeOn(AndroidSchedulers.mainThread())
                                                            .subscribe(userModel -> {
                                                                        if (userModel.isSuccess())
                                                                        {
                                                                            Common.currentUser = userModel.getResult().get(0);
                                                                            startActivity(new Intent(UpdateInfoActivity.this,HomeActivity.class));
                                                                            finish();
                                                                        }
                                                                        else
                                                                        {
                                                                            Toast.makeText(UpdateInfoActivity.this, "[GET USER RESULT]"+userModel.getMessage(), Toast.LENGTH_SHORT).show();
                                                                        }
                                                                        dialog.dismiss();
                                                                    },
                                                                    throwable -> {
                                                                        dialog.dismiss();
                                                                        Toast.makeText(UpdateInfoActivity.this, "[GET USER]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    })
                                            );
                                        }
                                        else
                                        {
                                            dialog.dismiss();
                                            Toast.makeText(UpdateInfoActivity.this, "[UPDATE USER API RETURN]"+UpdateUserModel.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    ,throwable -> {
                                        dialog.dismiss();
                                        Toast.makeText(UpdateInfoActivity.this, "[UPDATE USER API]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    })
                    );

                }else{
                    Toast.makeText(UpdateInfoActivity.this, "Not signed In ! Please Sign In ", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(UpdateInfoActivity.this,MainActivity.class));
                    finish();
                }
            }
        });

        if (Common.currentUser !=null && !TextUtils.isEmpty(Common.currentUser.getName()))
            edt_user_name.setText(Common.currentUser.getName());
        if (Common.currentUser !=null && !TextUtils.isEmpty(Common.currentUser.getAddress()))
            edt_user_address.setText(Common.currentUser.getAddress());
    }

    private void init() {
        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(this).build();
        myRestaurantAPI = RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(MyRestaurantAPI.class);
    }
}
