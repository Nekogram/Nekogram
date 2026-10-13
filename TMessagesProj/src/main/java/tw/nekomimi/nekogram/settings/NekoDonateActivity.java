package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.common.collect.ImmutableList;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BotWebViewVibrationEffect;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.Wallet.WalletRecipientView;
import org.telegram.ui.Wallet.WalletSendEnterAmount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import tw.nekomimi.nekogram.helpers.remote.ConfigHelper;

public class NekoDonateActivity extends BaseNekoSettingsActivity implements PurchasesUpdatedListener {
    private static final List<String> SKUS = Arrays.asList("donate001", "donate002", "donate005", "donate010", "donate020", "donate050", "donate100");

    private final int sendGramsRow = rowId++;
    private final int donateRow = 100;

    private BillingClient billingClient;
    private List<ProductDetails> productDetails;
    private final String tonAddress = ConfigHelper.getTonAddress();

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        billingClient = BillingClient.newBuilder(ApplicationLoader.applicationContext)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();

        billingClient.endConnection();
    }

    private void showErrorAlert(BillingResult result) {
        if (getParentActivity() == null || result.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED || result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            if (TextUtils.isEmpty(result.getDebugMessage())) {
                BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.ErrorOccurred) + ": " + result.getResponseCode()).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                builder.setTitle(LocaleController.getString(R.string.ErrorOccurred));
                builder.setMessage(result.getDebugMessage());
                builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
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
                    var productList =
                            SKUS.stream().map(s -> QueryProductDetailsParams.Product.newBuilder()
                                            .setProductId(s)
                                            .setProductType(BillingClient.ProductType.INAPP)
                                            .build())
                                    .collect(Collectors.toList());
                    var params = QueryProductDetailsParams.newBuilder()
                            .setProductList(productList)
                            .build();
                    billingClient.queryProductDetailsAsync(params, (queryResult, result) -> {
                        if (queryResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            var list = result.getProductDetailsList();
                            if (!list.isEmpty()) {
                                AndroidUtilities.runOnUIThread(() -> {
                                    productDetails = list;
                                    if (listView != null) {
                                        listView.adapter.update(true);
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
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (!TextUtils.isEmpty(tonAddress)) {
            items.add(WalletAddressCellFactory.of(sendGramsRow, tonAddress));
            items.add(UItem.asShadow(null));
        }
        items.add(UItem.asHeader(LocaleController.getString(R.string.GooglePlay)));
        if (productDetails != null && !productDetails.isEmpty()) {
            for (int i = 0; i < productDetails.size(); i++) {
                var product = productDetails.get(i);
                var details = product.getOneTimePurchaseOfferDetails();
                items.add(TextSettingsCellFactory.of(donateRow + i, details != null ? details.getFormattedPrice() : product.getName()));
            }
        } else {
            items.add(UItem.asFlicker(1, FlickerLoadingView.TEXT_SETTINGS_TYPE));
        }
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        var id = item.id;
        if (id == sendGramsRow) {
            if (getMessagesController().config.walletAvailable.get()) {
                presentFragment(new WalletSendEnterAmount(tonAddress));
            } else {
                AndroidUtilities.addToClipboard(tonAddress);
                BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.WalletAddressCopiedBulletin)).show();
            }
        } else if (id >= donateRow) {
            if (productDetails != null && productDetails.size() > id - donateRow) {
                var productDetailsParamsList =
                        ImmutableList.of(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails.get(id - donateRow))
                                        .build()
                        );
                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build();
                billingClient.launchBillingFlow(getParentActivity(), flowParams);
            }
        }
    }

    @Override
    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        var id = item.id;
        if (id == sendGramsRow) {
            ItemOptions.makeOptions(this, view)
                    .setScrimViewBackground(listView.getClipBackground(view))
                    .addIf(getMessagesController().config.walletAvailable.get(), R.drawable.ic_gram, LocaleController.getString(R.string.WalletSendGrams), () -> {
                        presentFragment(new WalletSendEnterAmount(tonAddress));
                    })
                    .add(R.drawable.msg_copy, LocaleController.getString(R.string.WalletCopyAddress), () -> {
                        AndroidUtilities.addToClipboard(tonAddress);
                        BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.WalletAddressCopiedBulletin)).show();
                    })
                    .show();
            return true;
        }
        return super.onItemLongClick(item, view, position, x, y);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.Donate);
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
                                BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.DonateThankYou)).show();
                                BotWebViewVibrationEffect.NOTIFICATION_SUCCESS.vibrate();
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

    private static class WalletAddressCell extends FrameLayout {
        private final WalletRecipientView addressInfoLayout;


        public WalletAddressCell(@NonNull Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            addressInfoLayout = new WalletRecipientView(context, resourcesProvider, null, null, true);
            addressInfoLayout.setOnClickListener(null);
            addView(addressInfoLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 64));
        }

        public void setAddress(String address) {
            addressInfoLayout.setRecipient(null, address);
        }
    }

    private static class WalletAddressCellFactory extends UItem.UItemFactory<WalletAddressCell> {
        static {
            setup(new WalletAddressCellFactory());
        }

        @Override
        public WalletAddressCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            var frame = new WalletAddressCell(context, resourcesProvider);
            frame.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return frame;
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            var recipientView = (WalletAddressCell) view;
            recipientView.setAddress(item.text.toString());
        }

        public static UItem of(int id, CharSequence title) {
            var item = UItem.ofFactory(WalletAddressCellFactory.class);
            item.id = id;
            item.text = title;
            return item;
        }
    }
}
