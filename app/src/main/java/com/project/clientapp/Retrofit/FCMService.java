package com.project.clientapp.Retrofit;

import com.project.clientapp.Modal.FCMResponse;
import com.project.clientapp.Modal.FCMSendData;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface FCMService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key={"Your FCM key Goes here"}"
    })
    @POST("fcm/send")
    Observable<FCMResponse> sendNotification(@Body FCMSendData body);
}
