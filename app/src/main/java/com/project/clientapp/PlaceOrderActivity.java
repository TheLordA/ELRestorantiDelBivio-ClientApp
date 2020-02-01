package com.project.clientapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.google.gson.Gson;
import com.project.clientapp.Common.Common;
import com.project.clientapp.Database.CartDataSource;
import com.project.clientapp.Database.CartDatabase;
import com.project.clientapp.Database.LocalCartDataSource;
import com.project.clientapp.Modal.EventBus.SendTotalCostEvent;
import com.project.clientapp.Modal.FCMSendData;
import com.project.clientapp.Retrofit.BraintreeAPI;
import com.project.clientapp.Retrofit.FCMService;
import com.project.clientapp.Retrofit.MyRestaurantAPI;
import com.project.clientapp.Retrofit.RetrofitBraintreeClient;
import com.project.clientapp.Retrofit.RetrofitClient;
import com.project.clientapp.Retrofit.RetrofitFCMClient;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.w3c.dom.Text;

import java.net.Inet4Address;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import dmax.dialog.SpotsDialog;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PlaceOrderActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    private static final int REQUEST_BRAINTREE_CODE = 7777;

    @BindView(R.id.edt_date)
    EditText edt_date;
    @BindView(R.id.txt_total_cash)
    TextView txt_total_cash;
    @BindView(R.id.txt_user_phone)
    TextView txt_user_phone;
    @BindView(R.id.txt_user_address)
    TextView txt_user_address;
    @BindView(R.id.txt_new_address)
    TextView txt_new_address;
    @BindView(R.id.btn_add_new_address)
    Button btn_add_new_address;
    @BindView(R.id.ckb_default_address)
    CheckBox ckb_default_address;
    @BindView(R.id.rdi_cod)
    RadioButton rdi_cod;
    @BindView(R.id.rdi_online_payment)
    RadioButton rdi_online_payment;
    @BindView(R.id.btn_proceed)
    Button btn_proceed;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    FCMService fcmService ;
    MyRestaurantAPI myRestaurantAPI;
    BraintreeAPI braintreeAPI;
    AlertDialog dialog;
    CartDataSource cartDataSource;
    CompositeDisposable compositeDisposable = new CompositeDisposable();

    boolean isSelectedDate = false, isAddNewAddress = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);

        init();
        initView();

    }

    private void initView() {
        ButterKnife.bind(this);

        txt_user_phone.setText(Common.currentUser.getUserPhone());
        txt_user_address.setText(Common.currentUser.getAddress());

        toolbar.setTitle(getString(R.string.place_order));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        btn_add_new_address.setOnClickListener(view -> {

            isAddNewAddress = true;
            ckb_default_address.setChecked(false);
            View layout_add_new_address = LayoutInflater.from(PlaceOrderActivity.this)
                    .inflate(R.layout.layout_add_new_address, null);
            EditText edt_add_new_address = (EditText) layout_add_new_address.findViewById(R.id.edt_add_new_adress);

            edt_add_new_address.setText(txt_new_address.getText().toString());

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(PlaceOrderActivity.this)
                    .setTitle("Add New Address")
                    .setView(layout_add_new_address)
                    .setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss())
                    .setPositiveButton("ADD", (dialogInterface, i) -> txt_new_address.setText(edt_add_new_address.getText().toString()));

            androidx.appcompat.app.AlertDialog addNewAddressDialog = builder.create();
            addNewAddressDialog.show();
        });

        edt_date.setOnClickListener(view -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dpd = DatePickerDialog.newInstance(PlaceOrderActivity.this,
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH));

            dpd.show(getSupportFragmentManager(), "DatePickerDialog");
        });

        btn_proceed.setOnClickListener(view -> {

            if (!isSelectedDate) {
                Toast.makeText(PlaceOrderActivity.this, "Please Select A Date", Toast.LENGTH_SHORT).show();
                return;
            } else {
                String dateString = edt_date.getText().toString();
                DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                try {
                    Date orderDate = df.parse(dateString);
                    // get current date to compare with
                    Calendar calender = Calendar.getInstance();
                    Date currentDate = df.parse(df.format(calender.getTime()));
                    if (!DateUtils.isToday(orderDate.getTime())) {
                        if (orderDate.before(currentDate)) {
                            Toast.makeText(PlaceOrderActivity.this, "Please Choose a Correct Date ( Curent or + )", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }


                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if (!isAddNewAddress) {
                if (!ckb_default_address.isChecked()) {
                    Toast.makeText(PlaceOrderActivity.this, "Please Choose Default Address or Set a New One .", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (rdi_cod.isChecked()) {

                getOrderNumber(false);
            } else if (rdi_online_payment.isChecked()) {

                // Online Payment Process
                getOrderNumber(true);
            }

        });

    }

    private void getOrderNumber(boolean isOnlinePayment) {
        dialog.show();
        if (!isOnlinePayment) {
            String address = ckb_default_address.isChecked() ? txt_user_address.getText().toString() : txt_new_address.getText().toString();
            compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getFbid(), Common.currentRestaurant.getId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(cartItems -> {
                                //Get Order Number From Server
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Authorization", Common.buildJWT(Common.API_KEY));
                                compositeDisposable.add(
                                        myRestaurantAPI.createOrder(headers,
                                                Common.currentUser.getUserPhone(),
                                                Common.currentUser.getName(),
                                                address,
                                                edt_date.getText().toString(),
                                                Common.currentRestaurant.getId(),
                                                "NONE",
                                                true,
                                                Double.valueOf(txt_total_cash.getText().toString()),
                                                cartItems.size())
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(createOrderModel -> {
                                                            if (createOrderModel.isSuccess()) {
                                                                //After having order number , we will update all item of this order to orderDetail
                                                                //first , select cart items
                                                                compositeDisposable.add(
                                                                        myRestaurantAPI.updateOrder(
                                                                                headers,
                                                                                String.valueOf(createOrderModel.getResult().get(0).getOrderNumber()),
                                                                                new Gson().toJson(cartItems))
                                                                                .subscribeOn(Schedulers.io())
                                                                                .observeOn(AndroidSchedulers.mainThread())
                                                                                .subscribe(updateOrderModel -> {
                                                                                            if (updateOrderModel.isSuccess()) {
                                                                                                //after we update item , we will clear cart and show message success
                                                                                                cartDataSource.cleanCart(Common.currentUser.getFbid(),
                                                                                                        Common.currentRestaurant.getId())
                                                                                                        .subscribeOn(Schedulers.io())
                                                                                                        .observeOn(AndroidSchedulers.mainThread())
                                                                                                        .subscribe(new SingleObserver<Integer>() {
                                                                                                            @Override
                                                                                                            public void onSubscribe(Disposable d) {

                                                                                                            }

                                                                                                            @Override
                                                                                                            public void onSuccess(Integer integer) {
                                                                                                                //Create Notification
                                                                                                                Map<String,String> dataSend = new HashMap<>();
                                                                                                                dataSend.put(Common.NOTIF_TITLE,"New Order");
                                                                                                                dataSend.put(Common.NOTIF_CONTENT,"You Have New Order"+createOrderModel.getResult().get(0).getOrderNumber());

                                                                                                                FCMSendData sendData = new FCMSendData(Common.createTopicSender(Common.getTopicChannel(Common.currentRestaurant.getId())),dataSend);
                                                                                                                compositeDisposable.add(fcmService.sendNotification(sendData)
                                                                                                                .subscribeOn(Schedulers.io())
                                                                                                                .observeOn(AndroidSchedulers.mainThread())
                                                                                                                .subscribe(fcmResponse -> {
                                                                                                                            Toast.makeText(PlaceOrderActivity.this, "Order PLaced", Toast.LENGTH_SHORT).show();
                                                                                                                            Intent homeActivity = new Intent(PlaceOrderActivity.this, HomeActivity.class);
                                                                                                                            homeActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                                                                                            startActivity(homeActivity);
                                                                                                                            finish();
                                                                                                                }
                                                                                                                ,throwable -> {
                                                                                                                            Toast.makeText(PlaceOrderActivity.this, "Order PLaced But Can't Be Send To Server", Toast.LENGTH_SHORT).show();
                                                                                                                            Intent homeActivity = new Intent(PlaceOrderActivity.this, HomeActivity.class);
                                                                                                                            homeActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                                                                                            startActivity(homeActivity);
                                                                                                                            finish();
                                                                                                                }
                                                                                                                ));
                                                                                                            }

                                                                                                            @Override
                                                                                                            public void onError(Throwable e) {
                                                                                                                Toast.makeText(PlaceOrderActivity.this, "[CLEAR CART]" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                                            }
                                                                                                        });
                                                                                            }
                                                                                            if (dialog.isShowing())
                                                                                                dialog.dismiss();
                                                                                        },
                                                                                        throwable -> {
                                                                                            dialog.dismiss();
                                                                                            //Toast.makeText(this, "[UPDATE ODER]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                        })
                                                                );
                                                            } else {
                                                                dialog.dismiss();
                                                                Toast.makeText(this, "[CREATE ORDER]" + createOrderModel.getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        },
                                                        throwable -> {
                                                            dialog.dismiss();
                                                            Toast.makeText(this, "[CREATE ODER]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                        })
                                );
                            },
                            throwable -> {
                                Toast.makeText(this, "[GET ALL CART]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            })
            );
        }
        else {
            //if payment is online
            //first , get Token
            compositeDisposable.add(braintreeAPI.getToken()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(braintreeToken -> {
                                if (braintreeToken.isSuccess()) {
                                    DropInRequest dropInRequest = new DropInRequest().clientToken(braintreeToken.getClientToken());
                                    startActivityForResult(dropInRequest.getIntent(PlaceOrderActivity.this), REQUEST_BRAINTREE_CODE);
                                } else {
                                    Toast.makeText(this, "Cannot get Token", Toast.LENGTH_SHORT).show();
                                }
                                dialog.dismiss();
                            }
                            , throwable -> {
                                dialog.dismiss();
                                Toast.makeText(PlaceOrderActivity.this, "[GET TOKEN]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            })
            );
        }

    }

    private void init() {
        fcmService = RetrofitFCMClient.getInstance().create(FCMService.class);
        dialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();
        myRestaurantAPI = RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(MyRestaurantAPI.class);
        braintreeAPI = RetrofitBraintreeClient.getInstance(Common.currentRestaurant.getPaymentUrl()).create(BraintreeAPI.class);
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(this).cartDAO());
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        isSelectedDate = true;

        edt_date.setText(new StringBuilder("").append(monthOfYear + 1).append("/").append(dayOfMonth).append("/").append(year));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BRAINTREE_CODE) {
            if (resultCode == RESULT_OK) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                PaymentMethodNonce nonce = result.getPaymentMethodNonce();

                //After having nonce , we are done the payment with our API
                if (!TextUtils.isEmpty(txt_total_cash.getText().toString())) {

                    String amount = txt_total_cash.getText().toString();
                    if (!dialog.isShowing())
                        dialog.show();

                    String address = ckb_default_address.isChecked() ? txt_user_address.getText().toString() : txt_new_address.getText().toString();

                    compositeDisposable.add(braintreeAPI.submitPayment(amount, nonce.getNonce())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(braintreeTransaction -> {
                                        if (braintreeTransaction.isSuccess()) {
                                            if (!dialog.isShowing())
                                                dialog.show();
                                            //After we have transaction , we make order just like the COD
                                            compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getFbid(), Common.currentRestaurant.getId())
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(cartItems -> {
                                                                //Get Order Number From Server
                                                                Map<String, String> headers = new HashMap<>();
                                                                headers.put("Authorization", Common.buildJWT(Common.API_KEY));
                                                                compositeDisposable.add(
                                                                        myRestaurantAPI.createOrder(headers,
                                                                                Common.currentUser.getUserPhone(),
                                                                                Common.currentUser.getName(),
                                                                                address,
                                                                                edt_date.getText().toString(),
                                                                                Common.currentRestaurant.getId(),
                                                                                braintreeTransaction.getTransaction().getId(),
                                                                                false,
                                                                                Double.valueOf(amount),
                                                                                cartItems.size())
                                                                                .subscribeOn(Schedulers.io())
                                                                                .observeOn(AndroidSchedulers.mainThread())
                                                                                .subscribe(createOrderModel -> {
                                                                                            if (createOrderModel.isSuccess()) {
                                                                                                //After having order number , we will update all item of this order to orderDetail
                                                                                                //first , select cart items
                                                                                                compositeDisposable.add(
                                                                                                        myRestaurantAPI.updateOrder(headers,
                                                                                                                String.valueOf(createOrderModel.getResult().get(0).getOrderNumber()),
                                                                                                                new Gson().toJson(cartItems))
                                                                                                                .subscribeOn(Schedulers.io())
                                                                                                                .observeOn(AndroidSchedulers.mainThread())
                                                                                                                .subscribe(updateOrderModel -> {
                                                                                                                            if (updateOrderModel.isSuccess()) {
                                                                                                                                //after we update item , we will clear cart and show message success
                                                                                                                                cartDataSource.cleanCart(Common.currentUser.getFbid(),
                                                                                                                                        Common.currentRestaurant.getId())
                                                                                                                                        .subscribeOn(Schedulers.io())
                                                                                                                                        .observeOn(AndroidSchedulers.mainThread())
                                                                                                                                        .subscribe(new SingleObserver<Integer>() {
                                                                                                                                            @Override
                                                                                                                                            public void onSubscribe(Disposable d) {

                                                                                                                                            }

                                                                                                                                            @Override
                                                                                                                                            public void onSuccess(Integer integer) {
                                                                                                                                                //Create Notification
                                                                                                                                                Map<String,String> dataSend = new HashMap<>();
                                                                                                                                                dataSend.put(Common.NOTIF_TITLE,"New Order");
                                                                                                                                                dataSend.put(Common.NOTIF_CONTENT,"You Have New Order"+createOrderModel.getResult().get(0).getOrderNumber());

                                                                                                                                                FCMSendData sendData = new FCMSendData(Common.createTopicSender(Common.getTopicChannel(Common.currentRestaurant.getId())),dataSend);
                                                                                                                                                compositeDisposable.add(fcmService.sendNotification(sendData)
                                                                                                                                                        .subscribeOn(Schedulers.io())
                                                                                                                                                        .observeOn(AndroidSchedulers.mainThread())
                                                                                                                                                        .subscribe(fcmResponse -> {
                                                                                                                                                                    Toast.makeText(PlaceOrderActivity.this, "Order PLaced", Toast.LENGTH_SHORT).show();
                                                                                                                                                                    Intent homeActivity = new Intent(PlaceOrderActivity.this, HomeActivity.class);
                                                                                                                                                                    homeActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                                                                                                                                    startActivity(homeActivity);
                                                                                                                                                                    finish();
                                                                                                                                                                }
                                                                                                                                                                ,throwable -> {
                                                                                                                                                                    Toast.makeText(PlaceOrderActivity.this, "Order PLaced But Can't Be Send To Server", Toast.LENGTH_SHORT).show();
                                                                                                                                                                    Intent homeActivity = new Intent(PlaceOrderActivity.this, HomeActivity.class);
                                                                                                                                                                    homeActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                                                                                                                                    startActivity(homeActivity);
                                                                                                                                                                    finish();
                                                                                                                                                                }
                                                                                                                                                        ));
                                                                                                                                            }

                                                                                                                                            @Override
                                                                                                                                            public void onError(Throwable e) {
                                                                                                                                                Toast.makeText(PlaceOrderActivity.this, "[CLEAR CART]" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                                                                            }
                                                                                                                                        });
                                                                                                                            }
                                                                                                                            if (dialog.isShowing())
                                                                                                                                dialog.dismiss();
                                                                                                                        },
                                                                                                                        throwable -> {
                                                                                                                            dialog.dismiss();
                                                                                                                            //Toast.makeText(this, "[UPDATE ODER]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                                                        })
                                                                                                );
                                                                                            } else {
                                                                                                dialog.dismiss();
                                                                                                Toast.makeText(PlaceOrderActivity.this, "[CREATE ORDER]" + createOrderModel.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                            }
                                                                                        },
                                                                                        throwable -> {
                                                                                            dialog.dismiss();
                                                                                            Toast.makeText(PlaceOrderActivity.this, "[CREATE ODER]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                        })
                                                                );
                                                            },
                                                            throwable -> {
                                                                Toast.makeText(PlaceOrderActivity.this, "[GET ALL CART]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                            })
                                            );
                                        } else {
                                            dialog.dismiss();
                                            Toast.makeText(PlaceOrderActivity.this, "Transaction Failed", Toast.LENGTH_SHORT).show();
                                        }
                                        dialog.dismiss();
                                    }
                                    , throwable -> {
                                        if (dialog.isShowing())
                                            dialog.dismiss();
                                        Toast.makeText(PlaceOrderActivity.this, "[SUBMIT PAYEMENT]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    })

                    );

                }
            }
        }
    }

    //EventBus

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

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void setTotalCash(SendTotalCostEvent event) {
        txt_total_cash.setText(String.valueOf(event.getCash()));
    }
}
