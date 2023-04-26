package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
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
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.LaunchActivity;

import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.helpers.remote.ConfigHelper;

@SuppressWarnings("deprecation")
public class NekoDonateActivity extends BaseNekoSettingsActivity implements PurchasesUpdatedListener {
    private static final List<String> SKUS = Arrays.asList("donate001", "donate002", "donate005", "donate010", "donate020", "donate050", "donate100");

    private int buyMeACoffeeRow;
    private int buyMeACoffee2Row;

    private int donateRow;
    private int placeHolderRow;
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
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
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
                                        updateRows();
                                        listAdapter.notifyItemChanged(donateRow + 1);
                                        listAdapter.notifyItemRangeInserted(donateRow + 2, skuDetails.size() - 1);
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
        if (position > donateRow && position < donate2Row) {
            if (skuDetails != null && skuDetails.size() > position - donateRow - 1) {
                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetails.get(position - donateRow - 1))
                        .build();
                billingClient.launchBillingFlow(getParentActivity(), flowParams);
            }
        } else if (position == buyMeACoffeeRow) {
            Browser.openUrl(getParentActivity(), "https://www.buymeacoffee.com/nekogram");
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

        if (ConfigHelper.getCoffee()) {
            buyMeACoffeeRow = rowCount++;
            buyMeACoffee2Row = rowCount++;
        } else {
            buyMeACoffeeRow = -1;
            buyMeACoffee2Row = -1;
        }

        donateRow = rowCount++;
        if (skuDetails == null || skuDetails.isEmpty()) {
            placeHolderRow = rowCount++;
        } else {
            placeHolderRow = -1;
            rowCount += skuDetails.size();
        }
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
                            AndroidUtilities.runOnUIThread(() -> {
                                BulletinFactory.of(this).createErrorBulletin(LocaleController.getString("DonateThankYou", R.string.DonateThankYou)).show();
                                try {
                                    fragmentView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                                } catch (Exception ignored) {
                                }
                                if (getParentActivity() instanceof LaunchActivity) {
                                    ((LaunchActivity) getParentActivity()).getFireworksOverlay().start();
                                }
                            });
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
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position > donateRow && position < donate2Row) {
                        if (skuDetails != null) {
                            if (skuDetails.size() > position - donateRow - 1) {
                                textCell.setText(skuDetails.get(position - donateRow - 1).getPrice(), position + 1 != donate2Row);
                            }
                        } else {
                            textCell.setText(LocaleController.getString("Loading", R.string.Loading), position + 1 != donate2Row);
                        }
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == donateRow) {
                        headerCell.setText(LocaleController.getString("GooglePlay", R.string.GooglePlay));
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == donate2Row) {
                        cell.setText(LocaleController.getString("DonateEvilGoogle", R.string.DonateEvilGoogle));
                    }
                    break;
                }
                case TYPE_FLICKER: {
                    FlickerLoadingView flickerLoadingView = (FlickerLoadingView) holder.itemView;
                    flickerLoadingView.setViewType(FlickerLoadingView.TEXT_SETTINGS_TYPE);
                    flickerLoadingView.setIsSingleCell(true);
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            if (type == 2) {
                return skuDetails != null;
            } else {
                return super.isEnabled(holder);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == buyMeACoffee2Row) {
                return TYPE_SHADOW;
            } else if (position == donateRow) {
                return TYPE_HEADER;
            } else if (position == donate2Row) {
                return TYPE_INFO_PRIVACY;
            } else if (position == buyMeACoffeeRow) {
                return TYPE_BUYMEACOFFEE;
            } else if (position == placeHolderRow) {
                return TYPE_FLICKER;
            }
            return TYPE_SETTINGS;
        }
    }
}
