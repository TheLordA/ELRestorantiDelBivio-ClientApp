package com.project.clientapp.Services;

import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.project.clientapp.Common.Common;
import com.project.clientapp.Modal.TokenModel;
import com.project.clientapp.Retrofit.MyRestaurantAPI;
import com.project.clientapp.Retrofit.RetrofitClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import io.paperdb.Paper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    MyRestaurantAPI myRestaurantAPI;
    CompositeDisposable compositeDisposable;

    @Override
    public void onCreate() {
        super.onCreate();
        myRestaurantAPI= RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(MyRestaurantAPI.class);
        compositeDisposable = new CompositeDisposable();
        Paper.init(this);
    }

    @Override
    public void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    public void onNewToken(@NonNull String newToken) {
        super.onNewToken(newToken);
        //Here we wil update Token
        // to do so we need FBID
        //but this is a service we can't access Common,CurrentUser cuz it's null
        //so , we need save signed FBID by Paper and retrieve it when we need it

        String fbid = Paper.book().read(Common.REMEMBER_FBID);
        String apiKey = Paper.book().read(Common.API_KEY_TAG);
        Map<String,String> headers = new HashMap<>();
        headers.put("Authorization",Common.buildJWT(Common.API_KEY));
        compositeDisposable.add(myRestaurantAPI.updateTokenToServer(headers,newToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tokenModel -> {
                    //DO nothing
                        }
                ,throwable -> {
                            Toast.makeText(this, "[REFRESH TOKEN]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        })
        );
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        //Get Notification object from FCM
        //we want to retirive notification while app is killed , so we must use data payload
        Map<String,String> dataRecv = remoteMessage.getData();
        if (dataRecv == null){
            Common.ShowNotification(this, new Random().nextInt(),dataRecv.get(Common.NOTIF_TITLE),dataRecv.get(Common.NOTIF_CONTENT),null);
        }
    }
}
