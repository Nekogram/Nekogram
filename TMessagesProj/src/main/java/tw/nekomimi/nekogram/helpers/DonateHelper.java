package tw.nekomimi.nekogram.helpers;

import android.app.Activity;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DonateHelper implements BillingClientStateListener, SkuDetailsResponseListener, PurchasesUpdatedListener, ConsumeResponseListener {
    private final Activity activity;
    private AlertDialog progressDialog;
    private BillingClient billingClient;

    public DonateHelper(Activity activity) {
        this.activity = activity;
    }

    private void startConnection() {
        billingClient = BillingClient.newBuilder(activity)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        billingClient.startConnection(this);
    }

    public void showDonationDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        progressDialog = new AlertDialog(activity, 3);
        progressDialog.showDelayed(400);
        startConnection();
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        activity.runOnUiThread(() -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                List<String> skuList = Arrays.asList("donate001", "donate002", "donate005", "donate010", "donate020", "donate050", "donate100");
                SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                billingClient.querySkuDetailsAsync(params.build(), this);
            } else {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                showErrorAlert(billingResult.getResponseCode(), billingResult.getDebugMessage());
            }
        });
    }

    @Override
    public void onBillingServiceDisconnected() {
        startConnection();
    }

    @Override
    public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> list) {
        activity.runOnUiThread(() -> {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                List<String> skuTitles = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    skuTitles.add(list.get(i).getPrice());
                }
                String[] titles = new String[skuTitles.size()];
                skuTitles.toArray(titles);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(LocaleController.getString("Donate", R.string.Donate));
                builder.setMessage(LocaleController.getString("DonateEvilGoogle", R.string.DonateEvilGoogle));
                builder.setItems(titles,
                        (dialog, which) -> {
                            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                    .setSkuDetails(list.get(which))
                                    .build();
                            billingClient.launchBillingFlow(activity, flowParams);
                        });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                builder.show();
            } else {
                showErrorAlert(billingResult.getResponseCode(), billingResult.getDebugMessage());
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
        activity.runOnUiThread(() -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                // directly consume in-app purchase, so that people can donate multiple times
                for (Purchase purchase : list) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        billingClient.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(), this);
                    }
                }
            } else {
                showErrorAlert(billingResult.getResponseCode(), billingResult.getDebugMessage());
            }
        });
    }

    @Override
    public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String s) {
        activity.runOnUiThread(() -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                try {
                    Toast.makeText(activity, LocaleController.getString("DonateThankYou", R.string.DonateThankYou), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            } else {
                showErrorAlert(billingResult.getResponseCode(), billingResult.getDebugMessage());
            }
        });
    }

    private void showErrorAlert(int errorCode, String errorMessage) {
        if (errorCode == BillingClient.BillingResponseCode.USER_CANCELED || errorCode == BillingClient.BillingResponseCode.OK) {
            return;
        }
        if (TextUtils.isEmpty(errorMessage)) {
            try {
                Toast.makeText(activity, LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + " " + errorCode, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                FileLog.e(e);
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred));
            builder.setMessage(errorMessage);
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
            builder.show();
        }
    }
}
