package com.come.live.who.Purchase;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.come.live.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.android.billingclient.api.BillingClient.SkuType.INAPP;

public class Prchase_MainActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    public static final String PREF_FILE = "MyPref";
    //note add unique product ids
    //use same id for preference key
    private static ArrayList<String> purchaseItemIDs = new ArrayList() {{
        add("c1");
        add("c2");
        add("c3");
    }};
    private static ArrayList<String> purchaseItemDisplay = new ArrayList();
    ArrayAdapter<String> arrayAdapter;
    ListView listView;

    private BillingClient billingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.purchase_activity_main);
        listView = (ListView) findViewById(R.id.listview);
        // Establish connection to billing client
        //check purchase status from google play store cache on every app start
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases().setListener(this).build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Purchase.PurchasesResult queryPurchase = billingClient.queryPurchases(INAPP);
                    List<Purchase> queryPurchases = queryPurchase.getPurchasesList();
                    if (queryPurchases != null && queryPurchases.size() > 0) {
                        handlePurchases(queryPurchases);
                    }

                }
            }

            @Override
            public void onBillingServiceDisconnected() {
            }
        });


        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, purchaseItemDisplay);
        listView.setAdapter(arrayAdapter);
        notifyList();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                //initiate purchase on selected consume item click
                //check if service is already connected
                if (billingClient.isReady()) {
                    Toast.makeText(Prchase_MainActivity.this, "Ready", Toast.LENGTH_SHORT).show();
                    initiatePurchase(purchaseItemIDs.get(position));
                }
                //else reconnect service
                else {
                    Toast.makeText(Prchase_MainActivity.this, "Reconnect", Toast.LENGTH_SHORT).show();
                    billingClient = BillingClient
                            .newBuilder(Prchase_MainActivity.this)
                            .enablePendingPurchases()
                            .setListener(Prchase_MainActivity.this)
                            .build();
                    billingClient.startConnection(new BillingClientStateListener() {
                        @Override
                        public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                Toast.makeText(getApplicationContext(), "init " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
                                initiatePurchase(purchaseItemIDs.get(position));
                            } else {
                                Toast.makeText(getApplicationContext(), "Error " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onBillingServiceDisconnected() {
                        }
                    });
                }


            }
        });
    }

    private void notifyList() {
        purchaseItemDisplay.clear();
        for (String p : purchaseItemIDs) {
            purchaseItemDisplay.add(p + " consumed " + getPurchaseCountValueFromPref(p) + " time(s)");
        }
        arrayAdapter.notifyDataSetChanged();
    }

    private SharedPreferences getPreferenceObject() {
        return getApplicationContext().getSharedPreferences(PREF_FILE, 0);
    }

    private SharedPreferences.Editor getPreferenceEditObject() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(PREF_FILE, 0);
        return pref.edit();
    }

    private int getPurchaseCountValueFromPref(String PURCHASE_KEY) {
        return getPreferenceObject().getInt(PURCHASE_KEY, 0);
    }

    private void savePurchaseCountValueToPref(String PURCHASE_KEY, int value) {
        getPreferenceEditObject().putInt(PURCHASE_KEY, value).commit();
    }


    private void initiatePurchase(final String PRODUCT_ID) {
        List<String> skuList = new ArrayList<>();
        skuList.add(PRODUCT_ID);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(INAPP);
        billingClient.querySkuDetailsAsync(params.build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(@NonNull BillingResult billingResult,
                                                     List<SkuDetails> skuDetailsList) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            if (skuDetailsList != null && skuDetailsList.size() > 0) {
                                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                        .setSkuDetails(skuDetailsList.get(0))
                                        .build();
                                billingClient.launchBillingFlow(Prchase_MainActivity.this, flowParams);
                            } else {
                                //try to add item/product id "c1" "c2" "c3" inside managed product in google play console
                                Toast.makeText(getApplicationContext(), "Purchase Item " + PRODUCT_ID + " not Found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    " Error " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        //if item newly purchased
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases);
        }
        //if item already purchased then check and reflect changes
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Purchase.PurchasesResult queryAlreadyPurchasesResult = billingClient.queryPurchases(INAPP);
            List<Purchase> alreadyPurchases = queryAlreadyPurchasesResult.getPurchasesList();
            if (alreadyPurchases != null) {
                handlePurchases(alreadyPurchases);
            }
        }
        //if purchase cancelled
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(getApplicationContext(), "Purchase Canceled", Toast.LENGTH_SHORT).show();
        }
        // Handle any other error msgs
        else {
            Toast.makeText(getApplicationContext(), "Error " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void handlePurchases(List<Purchase> purchases) {
        for (Purchase purchase : purchases) {
            final int index = purchaseItemIDs.indexOf(purchase.getSkus());
            //purchase found
            if (index > -1) {
                //if item is purchased
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
                        // Invalid purchase
                        // show error to user
                        Toast.makeText(getApplicationContext(), "Error : Invalid Purchase", Toast.LENGTH_SHORT).show();
                        continue;//skip current iteration only because other items in purchase list must be checked if present
                    }
                    // else purchase is valid
                    //if item is purchased and not consumed
                    if (!purchase.isAcknowledged()) {
                        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                        billingClient.consumeAsync(consumeParams, new ConsumeResponseListener() {
                            @Override
                            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    int consumeCountValue = getPurchaseCountValueFromPref(purchaseItemIDs.get(index)) + 1;
                                    savePurchaseCountValueToPref(purchaseItemIDs.get(index), consumeCountValue);
                                    Toast.makeText(getApplicationContext(), "Item " + purchaseItemIDs.get(index) + "Consumed", Toast.LENGTH_SHORT).show();
                                    notifyList();
                                }
                            }
                        });
                    }
                }
                //if purchase is pending
                else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                    Toast.makeText(getApplicationContext(),
                            purchaseItemIDs.get(index) + " Purchase is Pending. Please complete Transaction", Toast.LENGTH_SHORT).show();
                }
                //if purchase is refunded or unknown
                else if (purchase.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                    Toast.makeText(getApplicationContext(), purchaseItemIDs.get(index) + " Purchase Status Unknown", Toast.LENGTH_SHORT).show();
                }
            }

        }

    }


    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     * <p>Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     * </p>
     */
    private boolean verifyValidSignature(String signedData, String signature) {
        try {
            //for old playconsole
            // To get key go to Developer Console > Select your app > Development Tools > Services & APIs.
            //for new play console
            //To get key go to Developer Console > Select your app > Monetize > Monetization setup

            String base64Key = "Add your key here";
            return Security.verifyPurchase(base64Key, signedData, signature);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }

}



