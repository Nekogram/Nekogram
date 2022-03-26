package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

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
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;

import java.util.Arrays;
import java.util.List;

public class NekoDonateActivity extends BaseNekoSettingsActivity implements PurchasesUpdatedListener {
    private static final List<String> SKUS = Arrays.asList("donate001", "donate002", "donate005", "donate010", "donate020", "donate050", "donate100");

    private int donateRow;
    private int donate2Row;

    private BillingClient billingClient;
    private List<SkuDetails> skuDetails;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        billingClient = BillingClient.newBuilder(ApplicationLoader.applicationContext)
                .setListener(this)
                .enablePendingPurchases()
                .build();

        return true;
    }

    private void showErrorAlert(BillingResult result) {
        if (getParentActivity() == null || result.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED || result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            if (TextUtils.isEmpty(result.getDebugMessage())) {
                BulletinFactory.of(this).createErrorBulletin(LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + ": " + result.getResponseCode()).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred));
                builder.setMessage(result.getDebugMessage());
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                showDialog(builder.create());
            }
        });
    }

    @Override
    public View createView(Context context) {
        View fragmentView = super.createView(context);

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {

            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    SkuDetailsParams params = SkuDetailsParams.newBuilder()
                            .setSkusList(SKUS)
                            .setType(BillingClient.SkuType.INAPP)
                            .build();
                    billingClient.querySkuDetailsAsync(params, (queryResult, list) -> {
                        if (queryResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            if (list != null && list.size() > 0) {
                                AndroidUtilities.runOnUIThread(() -> {
                                    if (listAdapter != null) {
                                        skuDetails = list;
                                        listAdapter.notifyItemRangeChanged(donateRow, 7);
                                    }
                                });
                            }
                        } else {
                            showErrorAlert(queryResult);
                        }
                    });
                } else {
                    showErrorAlert(billingResult);
                }
            }
        });

        return fragmentView;
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position >= donateRow && position < donate2Row) {
            if (skuDetails != null && skuDetails.size() > position - donateRow) {
                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetails.get(position - donateRow))
                        .build();
                billingClient.launchBillingFlow(getParentActivity(), flowParams);
            }
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString("Donate", R.string.Donate);
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();

        billingClient.endConnection();
    }

    @Override
    protected void updateRows() {
        rowCount = 0;

        donateRow = rowCount++;
        rowCount += 6;

        donate2Row = rowCount++;
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
            for (Purchase purchase : list) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    ConsumeParams params = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();
                    billingClient.consumeAsync(params, (billingResult1, s) -> {
                        if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            AndroidUtilities.runOnUIThread(() -> BulletinFactory.of(this).createErrorBulletin(LocaleController.getString("DonateThankYou", R.string.DonateThankYou)).show());
                        } else {
                            showErrorAlert(billingResult1);
                        }
                    });
                }
            }
        } else {
            showErrorAlert(billingResult);
        }
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 2: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position >= donateRow && position < donate2Row) {
                        if (skuDetails != null) {
                            if (skuDetails.size() > position - donateRow) {
                                textCell.setText(skuDetails.get(position - donateRow).getPrice(), position + 1 != donate2Row);
                            }
                        } else {
                            textCell.setText(LocaleController.getString("Loading", R.string.Loading), position + 1 != donate2Row);
                        }
                    }
                    break;
                }
                case 7: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    if (position == donate2Row) {
                        cell.setText(LocaleController.getString("DonateEvilGoogle", R.string.DonateEvilGoogle));
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return skuDetails != null && type == 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == donate2Row) {
                return 7;
            }
            return 2;
        }
    }
}
