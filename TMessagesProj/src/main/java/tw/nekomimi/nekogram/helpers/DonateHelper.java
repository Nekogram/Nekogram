package tw.nekomimi.nekogram.helpers;

import android.app.Activity;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DonateHelper implements PurchasesUpdatedListener {
    private static final List<String> SKUS = Arrays.asList("donate001", "donate002", "donate005", "donate010", "donate020", "donate050", "donate100");

    private static volatile DonateHelper Instance;

    private AlertDialog progressDialog;
    private final BillingClient billingClient;
    private List<SkuDetails> skuDetails;

    public static DonateHelper getInstance() {
        DonateHelper localInstance = Instance;
        if (localInstance == null) {
            synchronized (DonateHelper.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new DonateHelper();
                }
                return localInstance;
            }
        }
        return localInstance;
    }

    DonateHelper() {
        billingClient = BillingClient.newBuilder(ApplicationLoader.applicationContext)
                .setListener(this)
                .enablePendingPurchases()
                .build();
    }

    public void showDonationDialog(Activity activity) {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        progressDialog = new AlertDialog(activity, 3);
        progressDialog.showDelayed(400);
        if (billingClient.isReady()) {
            showSkuList(activity);
        } else {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingServiceDisconnected() {
                    FileLog.e("onBillingServiceDisconnected");
                }

                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        showSkuList(activity);
                    } else {
                        showErrorAlert(activity, billingResult);
                    }
                }
            });
        }
    }

    @UiThread
    private void showSkuList(Activity activity) {
        if (skuDetails != null) {
            AndroidUtilities.runOnUIThread(() -> {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                List<String> skuTitles = new ArrayList<>();
                for (int i = 0; i < skuDetails.size(); i++) {
                    skuTitles.add(skuDetails.get(i).getPrice());
                }
                String[] titles = new String[skuTitles.size()];
                skuTitles.toArray(titles);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(LocaleController.getString("Donate", R.string.Donate));
                builder.setMessage(LocaleController.getString("DonateEvilGoogle", R.string.DonateEvilGoogle));
                builder.setItems(titles,
                        (dialog, which) -> {
                            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                    .setSkuDetails(skuDetails.get(which))
                                    .build();
                            billingClient.launchBillingFlow(activity, flowParams);
                        });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                builder.show();
            });
        } else {
            querySkuList(activity);
        }
    }

    private void querySkuList(Activity activity) {
        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setSkusList(SKUS)
                .setType(BillingClient.SkuType.INAPP)
                .build();
        billingClient.querySkuDetailsAsync(params, (billingResult, list) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                skuDetails = list;
                showSkuList(activity);
            } else {
                showErrorAlert(activity, billingResult);
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
            // directly consume in-app purchase, so that people can donate multiple times
            for (Purchase purchase : list) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    ConsumeParams params = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();
                    billingClient.consumeAsync(params, (billingResult1, s) -> {
                        if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            AndroidUtilities.runOnUIThread(() -> {
                                try {
                                    Toast.makeText(ApplicationLoader.applicationContext, LocaleController.getString("DonateThankYou", R.string.DonateThankYou), Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    FileLog.e(e);
                                }
                            });
                        } else {
                            showErrorAlert(null, billingResult1);
                        }
                    });
                }
            }
        } else {
            showErrorAlert(null, billingResult);
        }
    }

    private void showErrorAlert(Activity activity, BillingResult result) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED || result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            if (activity == null || TextUtils.isEmpty(result.getDebugMessage())) {
                try {
                    Toast.makeText(ApplicationLoader.applicationContext, LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + " " + result.getResponseCode(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred));
                builder.setMessage(result.getDebugMessage());
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                builder.show();
            }
        });
    }
}
