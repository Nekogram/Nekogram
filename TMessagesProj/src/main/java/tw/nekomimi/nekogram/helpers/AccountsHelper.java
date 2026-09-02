package tw.nekomimi.nekogram.helpers;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.Collections;

public class AccountsHelper {

    public static void fillAccountSelectorMenu(ItemOptions menu, int currentAccount, Context context, Theme.ResourcesProvider resourcesProvider, boolean adaptive) {
        LaunchActivity activity;
        if (context instanceof LaunchActivity) {
            activity = (LaunchActivity) context;
        } else {
            return;
        }
        var accountNumbers = new ArrayList<Integer>();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (PasscodeHelper.isAccountHidden(a)) continue;
            if (UserConfig.getInstance(a).isClientActivated()) {
                accountNumbers.add(a);
            }
        }
        Collections.sort(accountNumbers, (o1, o2) -> {
            var l1 = UserConfig.getInstance(o1).loginTime;
            var l2 = UserConfig.getInstance(o2).loginTime;
            return Integer.compare(l1, l2);
        });
        if (accountNumbers.isEmpty() || adaptive && accountNumbers.size() == 1) {
            return;
        }
        menu.addGap();

        var previewWindow = activity.fragmentPreviewWindow;
        var recyclerView = new RecyclerView(context) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (previewWindow != null && previewWindow.dispatchTouchEvent(ev, super::dispatchTouchEvent)) {
                    return true;
                }
                return super.dispatchTouchEvent(ev);
            }
        };
        var adapter = new AccountSelectorAdapter(accountNumbers, currentAccount, adaptive, resourcesProvider);
        adapter.setOnClickListener(v -> {
            var account = ((AccountView) v).account;
            if (currentAccount == account) return;
            menu.dismiss();
            activity.switchToAccount(account, true);
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setBackground(Theme.createRoundRectDrawable(0, dp(12), 0));
        recyclerView.setClipToOutline(true);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        var touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                adapter.swapElements(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                if (viewHolder == null) {
                    return;
                }
                viewHolder.itemView.setSelected(true);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setSelected(false);
            }
        });
        adapter.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                var account = ((AccountView) v).account;
                if (account == currentAccount || AndroidUtilities.isTablet()) {
                    var holder = recyclerView.findContainingViewHolder(v);
                    if (holder == null) return false;
                    touchHelper.startDrag(holder);
                } else {
                    var actionBarLayout = activity.actionBarLayout;
                    var fragment = new DialogsActivity(null) {
                        @Override
                        public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
                            super.onTransitionAnimationEnd(isOpen, backward);
                            if (!isOpen && backward) { // closed
                                menu.bringDimViewToFront();
                                previewWindow.hide();
                            }
                        }

                        @Override
                        public void onPreviewOpenAnimationEnd() {
                            super.onPreviewOpenAnimationEnd();
                            menu.dismiss(false);
                            activity.switchToAccount(account, true);
                            previewWindow.hide();
                        }
                    };
                    fragment.setCurrentAccount(account);
                    actionBarLayout.presentFragmentAsPreview(fragment);
                    previewWindow.show();
                }
                return true;
            }
        });
        touchHelper.attachToRecyclerView(recyclerView);
        menu.addView(recyclerView);
    }

    private static class AccountSelectorAdapter extends RecyclerView.Adapter<AccountSelectorAdapter.ViewHolder> {
        private final ArrayList<Integer> accountNumbers;
        private final int currentAccount;
        private final Theme.ResourcesProvider resourcesProvider;
        private final boolean adaptive;
        private View.OnClickListener onClickListener;
        private View.OnLongClickListener onLongClickListener;

        public AccountSelectorAdapter(ArrayList<Integer> accountNumbers, int currentAccount, boolean adaptive, Theme.ResourcesProvider resourcesProvider) {
            this.accountNumbers = accountNumbers;
            this.currentAccount = currentAccount;
            this.adaptive = adaptive;
            this.resourcesProvider = resourcesProvider;
        }

        public void setOnClickListener(View.OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
        }

        public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
            this.onLongClickListener = onLongClickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            var accountView = new AccountView(parent.getContext(), resourcesProvider);
            accountView.setOnClickListener(onClickListener);
            accountView.setOnLongClickListener(onLongClickListener);
            accountView.setLayoutParams(new RecyclerView.LayoutParams(adaptive ? ViewGroup.LayoutParams.MATCH_PARENT : dp(230), dp(48)));
            return new ViewHolder(accountView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            var account = accountNumbers.get(position);
            holder.accountView.setAccount(account, account == currentAccount);
        }

        @Override
        public int getItemCount() {
            return accountNumbers.size();
        }

        public void swapElements(int fromIndex, int toIndex) {
            if (fromIndex < 0 || toIndex < 0 || fromIndex >= accountNumbers.size() || toIndex >= accountNumbers.size()) {
                return;
            }
            final UserConfig userConfig1 = UserConfig.getInstance(accountNumbers.get(fromIndex));
            final UserConfig userConfig2 = UserConfig.getInstance(accountNumbers.get(toIndex));
            final int tempLoginTime = userConfig1.loginTime;
            userConfig1.loginTime = userConfig2.loginTime;
            userConfig2.loginTime = tempLoginTime;
            userConfig1.saveConfig(false);
            userConfig2.saveConfig(false);
            Collections.swap(accountNumbers, fromIndex, toIndex);
            notifyItemMoved(fromIndex, toIndex);
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {
            private final AccountView accountView;

            public ViewHolder(@NonNull AccountView itemView) {
                super(itemView);

                accountView = itemView;
            }
        }
    }

    private static class AccountView extends LinearLayout {

        private int account = -1;
        private boolean selected = false;

        private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final BackupImageView avatarView;
        private final AvatarDrawable avatarDrawable;
        private final TextView textView;

        public AccountView(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);

            setOrientation(LinearLayout.HORIZONTAL);
            var stateListDrawable = new StateListDrawable();
            stateListDrawable.setEnterFadeDuration(150);
            stateListDrawable.setExitFadeDuration(150);
            stateListDrawable.addState(new int[]{android.R.attr.state_selected}, new ColorDrawable(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider)));
            stateListDrawable.addState(new int[]{}, Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector, resourcesProvider), Theme.RIPPLE_MASK_ALL));
            setBackground(stateListDrawable);

            selectedPaint.setStyle(Paint.Style.STROKE);
            selectedPaint.setStrokeWidth(dp(1.33f));
            selectedPaint.setColor(Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider));
            var avatarContainer = new FrameLayout(context) {

                @Override
                protected void dispatchDraw(@NonNull Canvas canvas) {
                    if (selected) {
                        canvas.drawCircle(getWidth() / 2.0f, getHeight() / 2.0f, dp(16), selectedPaint);
                    }
                    super.dispatchDraw(canvas);
                }
            };
            addView(avatarContainer, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL, 12, 0, 0, 0));

            avatarDrawable = new AvatarDrawable(resourcesProvider);
            avatarView = new BackupImageView(context);
            avatarView.setRoundRadius(dp(16));
            avatarContainer.addView(avatarView, LayoutHelper.createLinear(32, 32, Gravity.CENTER, 1, 1, 1, 1));

            textView = new TextView(context);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
            textView.setMaxLines(2);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            addView(textView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL, 14, 0, 14, 0));

        }

        public void setAccount(int account, boolean selected) {
            this.selected = selected;
            if (selected) {
                avatarView.setScaleX(0.833f);
                avatarView.setScaleY(0.833f);
            } else {
                avatarView.setScaleX(1.0f);
                avatarView.setScaleY(1.0f);
            }

            if (this.account == account) {
                return;
            }
            this.account = account;

            var user = UserConfig.getInstance(account).getCurrentUser();

            avatarDrawable.setInfo(user);

            avatarView.getImageReceiver().setCurrentAccount(account);
            avatarView.setForUserOrChat(user, avatarDrawable);

            textView.setText(UserObject.getUserName(user));
        }
    }
}
