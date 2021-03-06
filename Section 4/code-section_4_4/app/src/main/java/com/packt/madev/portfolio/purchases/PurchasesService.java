package com.packt.madev.portfolio.purchases;

import android.content.Context;
import android.util.Log;

import com.github.lukaspili.reactivebilling.ReactiveBilling;
import com.github.lukaspili.reactivebilling.model.Purchase;
import com.github.lukaspili.reactivebilling.model.PurchaseType;
import com.github.lukaspili.reactivebilling.response.GetPurchasesResponse;
import com.github.lukaspili.reactivebilling.response.PurchaseResponse;
import com.github.lukaspili.reactivebilling.response.Response;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@Singleton
public class PurchasesService {

    public static final List<GetPurchasesResponse.PurchaseResponse> EMPTY_LIST = Collections.emptyList();

    @Inject
    public PurchasesService() {
    }


    private static final String PREMIUM_SKU_ID = "android.test.purchased";

    public Observable<Boolean> monitorPurchases(Context context) {
        return RxJavaInterop.toV2Observable(
                rx.Observable.concat(
                        rx.Observable.just(false),
                        ReactiveBilling.getInstance(context)
                                .getPurchases(PurchaseType.PRODUCT, null)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .map(GetPurchasesResponse::getList)
                                .map(list -> list == null ? EMPTY_LIST : list)
                                .flatMap(rx.Observable::from)
                                .map(GetPurchasesResponse.PurchaseResponse::getProductId)
                                .filter(item -> PREMIUM_SKU_ID.equals(item))
                                .map(item -> true)
                                .firstOrDefault(false),
                        ReactiveBilling.getInstance(context).purchaseFlow()
                                .doOnNext(this::recordPurchase)
                                .map(PurchaseResponse::isSuccess)
                )
        );
    }

    public Observable<Response> purchasePremium(Context context) {
        return RxJavaInterop.toV2Observable(
                ReactiveBilling.getInstance(context)
                        .startPurchase(PREMIUM_SKU_ID, PurchaseType.PRODUCT, null, null)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
        );
    }


    private void recordPurchase(PurchaseResponse purchaseResponse) {
        if (purchaseResponse.isSuccess()) {
            Purchase purchase = purchaseResponse.getPurchase();
            Log.i("APP", "Purchase done " + purchase.getProductId());
        }
    }
}
