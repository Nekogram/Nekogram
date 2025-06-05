package tw.nekomimi.nekogram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LaunchActivity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import tw.nekomimi.nekogram.helpers.remote.ConfigHelper;

public class NekoDonateActivity extends BaseNekoSettingsActivity implements PurchasesUpdatedListener {
    private static final List<String> SKUS = Arrays.asList("donate001", "donate002", "donate005", "donate010", "donate020", "donate050", "donate100");
    private final List<ConfigHelper.Crypto> cryptos = ConfigHelper.getCryptos();

    private int donateRow;
    private int placeHolderRow;
    private int donate2Row;

    private int cryptoRow;
    private int crypto2Row;

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
                                    if (listAdapter != null) {
                                        productDetails = list;
                                        updateRows();
                                        listAdapter.notifyItemChanged(donateRow + 1);
                                        listAdapter.notifyItemRangeInserted(donateRow + 2, productDetails.size() - 1);
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
            if (productDetails != null && productDetails.size() > position - donateRow - 1) {
                var productDetailsParamsList =
                        ImmutableList.of(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails.get(position - donateRow - 1))
                                        .build()
                        );
                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build();
                billingClient.launchBillingFlow(getParentActivity(), flowParams);
            }
        } else if (position > cryptoRow && position < crypto2Row) {
            ConfigHelper.Crypto crypto = cryptos.get(position - cryptoRow - 1);
            QRCodeBottomSheet.showForCrypto(this, crypto);
        }
    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        if (position > cryptoRow && position < crypto2Row) {
            ConfigHelper.Crypto crypto = cryptos.get(position - cryptoRow - 1);
            ItemOptions.makeOptions(this, view)
                    .setScrimViewBackground(new ColorDrawable(getThemedColor(Theme.key_windowBackgroundWhite)))
                    .add(R.drawable.msg_qrcode, LocaleController.getString(R.string.GetQRCode), () -> QRCodeBottomSheet.showForCrypto(this, crypto))
                    .add(R.drawable.msg_copy, LocaleController.getString(R.string.Copy), () -> {
                        AndroidUtilities.addToClipboard(crypto.address);
                        BulletinFactory.of(NekoDonateActivity.this).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
                    })
                    .setMinWidth(190)
                    .show();
            return true;
        }
        return super.onItemLongClick(view, position, x, y);
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.Donate);
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();

        billingClient.endConnection();
    }

    @Override
    protected void updateRows() {
        rowCount = 0;

        if (cryptos == null || cryptos.isEmpty()) {
            cryptoRow = -1;
            crypto2Row = -1;
        } else {
            cryptoRow = rowCount++;
            rowCount += cryptos.size();
            crypto2Row = rowCount++;
        }

        donateRow = rowCount++;
        if (productDetails == null || productDetails.isEmpty()) {
            placeHolderRow = rowCount++;
        } else {
            placeHolderRow = -1;
            rowCount += productDetails.size();
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

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial, boolean divider) {
            switch (holder.getItemViewType()) {
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position > donateRow && position < donate2Row) {
                        if (productDetails != null) {
                            if (productDetails.size() > position - donateRow - 1) {
                                var product = productDetails.get(position - donateRow - 1);
                                var details = product.getOneTimePurchaseOfferDetails();
                                textCell.setText(details != null ? details.getFormattedPrice() : product.getName(), divider);
                            }
                        } else {
                            textCell.setText(LocaleController.getString(R.string.Loading), divider);
                        }
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == donateRow) {
                        headerCell.setText(LocaleController.getString(R.string.GooglePlay));
                    } else if (position == cryptoRow) {
                        headerCell.setText(LocaleController.getString(R.string.Cryptocurrency));
                    }
                    break;
                }
                case TYPE_DETAIL_SETTINGS: {
                    TextDetailSettingsCell cell = (TextDetailSettingsCell) holder.itemView;
                    if (position > cryptoRow && position < crypto2Row) {
                        ConfigHelper.Crypto crypto = cryptos.get(position - cryptoRow - 1);
                        cell.setTextAndValue(String.format("%s (%s)", crypto.currency, crypto.chain), crypto.address, divider);
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
                return productDetails != null;
            } else {
                return super.isEnabled(holder);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == donate2Row || position == crypto2Row) {
                return TYPE_SHADOW;
            } else if (position == donateRow || position == cryptoRow) {
                return TYPE_HEADER;
            } else if (position > cryptoRow && position < crypto2Row) {
                return TYPE_DETAIL_SETTINGS;
            } else if (position == placeHolderRow) {
                return TYPE_FLICKER;
            }
            return TYPE_SETTINGS;
        }
    }

    public static class QRCodeBottomSheet extends BottomSheet {

        private QRCodeBottomSheet(Context context, ConfigHelper.Crypto crypto, Theme.ResourcesProvider resourcesProvider) {
            super(context, false, resourcesProvider);

            fixNavigationBar();
            setBackgroundColor(getThemedColor(Theme.key_dialogBackground));

            var linearLayout = new LinearLayout(context);
            linearLayout.setPadding(dp(12), dp(12), dp(12), dp(12));
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            var titleView = new TextView(context);
            titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            titleView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
            titleView.setText(crypto.currency);
            linearLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

            var subTitleView = new TextView(context);
            subTitleView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
            subTitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            subTitleView.setText(crypto.chain);
            linearLayout.addView(subTitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 8));

            var imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            imageView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight(), dp(12));
                }
            });
            imageView.setClipToOutline(true);
            imageView.setImageBitmap(createQR(crypto.address));
            linearLayout.addView(imageView, LayoutHelper.createLinear(220, 220, Gravity.CENTER_HORIZONTAL, 18, 0, 18, 0));

            View.OnClickListener copy = (v) -> {
                AndroidUtilities.addToClipboard(crypto.address);
                BulletinFactory.of(container, resourcesProvider).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
            };

            var addressView = new TextView(context);
            addressView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            addressView.setText(crypto.address);
            addressView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
            addressView.setPadding(dp(4), dp(4), dp(4), dp(4));
            addressView.setBackground(Theme.createRadSelectorDrawable(getThemedColor(Theme.key_listSelector), 8, 8));
            addressView.setOnClickListener(copy);
            linearLayout.addView(addressView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 4));

            var horizontalView = new LinearLayout(context);
            horizontalView.setOrientation(LinearLayout.HORIZONTAL);

            var copyView = new TextView(context);
            copyView.setTextColor(getThemedColor(Theme.key_featuredStickers_buttonText));
            copyView.setGravity(Gravity.CENTER);
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            spannableStringBuilder.append("..").setSpan(new ColoredImageSpan(ContextCompat.getDrawable(context, R.drawable.msg_copy_filled)), 0, 1, 0);
            spannableStringBuilder.setSpan(new DialogCell.FixedWidthSpan(dp(6)), 1, 2, 0);
            spannableStringBuilder.append(LocaleController.getString(R.string.Copy));
            copyView.setText(spannableStringBuilder);
            copyView.setPadding(dp(8), 0, dp(8), 0);
            copyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            copyView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
            copyView.setSingleLine(true);
            copyView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(8), getThemedColor(Theme.key_featuredStickers_addButton), getThemedColor(Theme.key_featuredStickers_addButtonPressed)));
            copyView.setOnClickListener(copy);
            horizontalView.addView(copyView, LayoutHelper.createLinear(0, 42, 1f, 0, 4, 0, 4, 0));

            var shareView = new TextView(context);
            shareView.setTextColor(getThemedColor(Theme.key_featuredStickers_buttonText));
            shareView.setGravity(Gravity.CENTER);
            spannableStringBuilder = new SpannableStringBuilder();
            spannableStringBuilder.append("..").setSpan(new ColoredImageSpan(ContextCompat.getDrawable(context, R.drawable.msg_share_filled)), 0, 1, 0);
            spannableStringBuilder.setSpan(new DialogCell.FixedWidthSpan(dp(6)), 1, 2, 0);
            spannableStringBuilder.append(LocaleController.getString(R.string.ShareFile));
            shareView.setText(spannableStringBuilder);
            shareView.setPadding(dp(8), 0, dp(8), 0);
            shareView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            shareView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
            shareView.setSingleLine(true);
            shareView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(8), getThemedColor(Theme.key_featuredStickers_addButton), getThemedColor(Theme.key_featuredStickers_addButtonPressed)));
            shareView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, crypto.address);
                try {
                    context.startActivity(Intent.createChooser(intent, LocaleController.getString(R.string.ShareFile)));
                } catch (ActivityNotFoundException e) {
                    FileLog.e(e);
                }
            });
            horizontalView.addView(shareView, LayoutHelper.createLinear(0, 42, 1f, 4, 0, 4, 0));

            linearLayout.addView(horizontalView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM, 0, 8, 0, 0));

            var scrollView = new ScrollView(context);
            scrollView.addView(linearLayout);
            setCustomView(scrollView);
        }

        private Bitmap createQR(String key) {
            try {
                HashMap<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                hints.put(EncodeHintType.MARGIN, 0);
                var writer = new QRCodeWriter();
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
