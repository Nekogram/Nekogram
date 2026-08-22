package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.TelegramQRCodeWriter;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TextHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import tw.nekomimi.nekogram.helpers.remote.ConfigHelper;

public class NekoDonateActivity extends BaseNekoSettingsActivity implements PurchasesUpdatedListener {
    private static final List<String> SKUS = Arrays.asList("donate001", "donate002", "donate005", "donate010", "donate020", "donate050", "donate100");
    private final List<ConfigHelper.Crypto> cryptos = ConfigHelper.getCryptos();

    private final int donateRow = 100;
    private final int cryptoRow = 200;

    private BillingClient billingClient;
    private List<ProductDetails> productDetails;

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
                    billingClient.queryProductDetailsAsync(params, (queryResult, list) -> {
                        if (queryResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
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
        if (cryptos != null && !cryptos.isEmpty()) {
            items.add(UItem.asHeader(LocaleController.getString(R.string.Cryptocurrency)));
            var cryptoId = 0;
            for (var crypto : cryptos) {
                items.add(TextDetailSettingsCellFactory.of(cryptoRow + cryptoId++, String.format("%s (%s)", crypto.currency, crypto.chain), crypto.address));
            }
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
        if (id >= donateRow && id < cryptoRow) {
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
        } else if (id >= cryptoRow) {
            ConfigHelper.Crypto crypto = cryptos.get(id - cryptoRow);
            QRCodeBottomSheet.showForCrypto(this, crypto);
        }
    }

    @Override
    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        var id = item.id;
        if (id >= cryptoRow) {
            ConfigHelper.Crypto crypto = cryptos.get(id - cryptoRow);
            ItemOptions.makeOptions(this, view)
                    .setScrimViewBackground(listView.getClipBackground(view))
                    .add(R.drawable.msg_qrcode, LocaleController.getString(R.string.GetQRCode), () -> QRCodeBottomSheet.showForCrypto(this, crypto))
                    .add(R.drawable.msg_copy, LocaleController.getString(R.string.Copy), () -> {
                        AndroidUtilities.addToClipboard(crypto.address);
                        BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
                    })
                    .setMinWidth(190)
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

    public static class QRCodeBottomSheet extends BottomSheet {

        private QRCodeBottomSheet(Context context, ConfigHelper.Crypto crypto, Theme.ResourcesProvider resourcesProvider) {
            super(context, false, resourcesProvider);

            fixNavigationBar(getThemedColor(Theme.key_windowBackgroundGray));
            setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));

            var linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            var titleView = TextHelper.makeTextView(context, 20, Theme.key_windowBackgroundWhiteBlackText, true);
            titleView.setText(crypto.currency);
            linearLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 0));

            var subTitleView = TextHelper.makeTextView(context, 14, Theme.key_windowBackgroundWhiteGrayText, false);
            subTitleView.setText(crypto.chain);
            linearLayout.addView(subTitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 6));

            var imageContainer = new LinearLayout(context);
            imageContainer.setOrientation(LinearLayout.VERTICAL);
            imageContainer.setGravity(Gravity.CENTER_HORIZONTAL);
            imageContainer.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
            imageContainer.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_windowBackgroundWhite)));
            linearLayout.addView(imageContainer, LayoutHelper.createLinear(220 + 16, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 18, 0, 18, 0));

            var imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            imageView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight(), AndroidUtilities.dp(12));
                }
            });
            imageView.setClipToOutline(true);
            imageView.setImageBitmap(createQR(crypto.address));
            imageContainer.addView(imageView, LayoutHelper.createLinear(220, 220));

            View.OnClickListener copy = (v) -> {
                AndroidUtilities.addToClipboard(crypto.address);
                getBulletinFactory().createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
            };

            var addressView = TextHelper.makeTextView(context, 12, Theme.key_windowBackgroundWhiteGrayText, false);
            addressView.setText(crypto.address);
            addressView.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
            addressView.setBackground(Theme.createRadSelectorDrawable(getThemedColor(Theme.key_listSelector), 8, 8));
            addressView.setOnClickListener(copy);
            imageContainer.addView(addressView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));

            var copyView = new ButtonWithCounterView(context, true, resourcesProvider).setRound();
            copyView.setText(LocaleController.getString(R.string.Copy), false);
            copyView.setOnClickListener(copy);
            linearLayout.addView(copyView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 16, 16, 16, 16));

            var scrollView = new ScrollView(context);
            scrollView.addView(linearLayout);
            setCustomView(scrollView);
        }

        private Bitmap createQR(String key) {
            try {
                HashMap<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                hints.put(EncodeHintType.MARGIN, 0);
                var writer = new TelegramQRCodeWriter();
                return writer.encode(key, 768, 768, hints, null, 1.0f, 0xffffffff, 0xff000000, false);
            } catch (Exception e) {
                FileLog.e(e);
            }
            return null;
        }

        public static void showForCrypto(BaseFragment fragment, ConfigHelper.Crypto crypto) {
            new QRCodeBottomSheet(fragment.getParentActivity(), crypto, fragment.getResourceProvider()).show();
        }
    }
}
