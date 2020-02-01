package com.project.clientapp.Common;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.firebase.messaging.RemoteMessage;
import com.project.clientapp.Modal.Addon;
import com.project.clientapp.Modal.Favorite;
import com.project.clientapp.Modal.FavoriteOnlyId;
import com.project.clientapp.Modal.Restaurant;
import com.project.clientapp.Modal.User;
import com.project.clientapp.R;
import com.project.clientapp.Retrofit.FCMService;
import com.project.clientapp.Retrofit.RetrofitClient;
import com.project.clientapp.Services.MyFirebaseMessagingService;

import java.util.AbstractQueue;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Common {

    public static final String API_RESTAURANT_ENDPOINT = ""; // The Server endpoint goes Here
    public static String API_KEY ="" ; // we will set it as a hard core but after we will secure it

    public static final int DEFAULT_COLUMN_TYPE = 0;
    public static final int FULL_WIDTH_COLUMN = 1;

    public static String REMEMBER_FBID ="" ;
    public static final String API_KEY_TAG = "API_KEY_TAG";
    public static final String NOTIF_TITLE = "Title" ;
    public static final String NOTIF_CONTENT = "Content";

    public static User currentUser;
    public static Restaurant currentRestaurant;
    public static Set<Addon> addonList = new HashSet<>();
    public static List<FavoriteOnlyId> currentFavOfRestaurant;

    public static FCMService getFCMService(){
        return RetrofitClient.getInstance("https://fcm.googleapis.com/").create(FCMService.class);
    }

    public static boolean checkFavorite(int id) {
        boolean result = false ;
        for(FavoriteOnlyId item : currentFavOfRestaurant)
            if (item.getFoodId() ==id){
                result= true ;
            }
        return result;
    }

    public static void removeFavorite(int id ) {

        for(FavoriteOnlyId item : currentFavOfRestaurant)
            if (item.getFoodId() ==id){
                currentFavOfRestaurant.remove(item) ;
            }

    }

    public static String convertStatusToString(int orderStatus) {
        switch (orderStatus) {
            case 0:
                return "Placed";
            case 1 :
                return "Shipping";
            case 2 :
                return " Shipped";
            case -1 :
                return "Cancelled";
            default:
                return "Cancelled";
        }
    }

    public static void ShowNotification(Context context, int notifId, String title, String body, Intent intent) {
        PendingIntent pendingIntent = null ;
        if (intent != null)
            pendingIntent = PendingIntent.getActivity(context,notifId,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        String NOTIFICATION_CHANNEL_ID = "my_restaurant_client_app";
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "My Restaurant Notification",NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("Restaurant Client App");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationChannel.enableVibration(true);

            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID);

        builder.setContentTitle(title).setContentText(body)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.drawable.app_icon));

        if (pendingIntent != null)
            builder.setContentIntent(pendingIntent);
        Notification mNotification = builder.build();

        notificationManager.notify(notifId,mNotification);

    }

    public static String buildJWT(String apiKey) {
        return new StringBuilder("Bearer").append(" ").append(apiKey).toString();
    }

    public static String getTopicChannel(int id) {
        return new StringBuilder("Restaurant_").append(id).toString();
    }

    public static String createTopicSender(String topicChannel) {
        return new StringBuilder("/topics/").append(topicChannel).toString();
    }
}
