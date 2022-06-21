package tw.nekomimi.nekogram;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.LayoutHelper;

@SuppressLint({"ClickableViewAccessibility", "ViewConstructor"})
public class SendOptionsMenuLayout extends LinearLayout {
    private final Theme.ResourcesProvider resourcesProvider;
    private ActionBarPopupWindow sendPopupWindow;
    private boolean returnSendersNames;
    private ActionBarMenuSubItem showCaptionView;
    private ActionBarMenuSubItem hideCaptionView;

    public SendOptionsMenuLayout(Context parentActivity, ForwardContext forwardContext, boolean showSchedule, boolean showNotify, boolean darkTheme, Delegate delegate, Theme.ResourcesProvider resourcesProvider) {
        super(parentActivity);
        setOrientation(VERTICAL);

        this.resourcesProvider = resourcesProvider;
        var forwardParams = forwardContext.getForwardParams();
        if (forwardContext.getForwardingMessages() != null) {
            Paint paint = new Paint();
            paint.setColor(Theme.getColor(Theme.key_divider));
            View dividerView = new View(getContext()) {

                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(2, MeasureSpec.EXACTLY));
                }

                @Override
                protected void onDraw(Canvas canvas) {
                    canvas.drawRect(getPaddingLeft(), getPaddingTop(), getMeasuredWidth() - getPaddingRight(), getMeasuredHeight() - getPaddingBottom(), paint);
                }
            };
            ActionBarPopupWindow.ActionBarPopupWindowLayout sendPopupLayout1 = new ActionBarPopupWindow.ActionBarPopupWindowLayout(parentActivity, resourcesProvider) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    if (dividerView.getParent() != null) {
                        dividerView.setVisibility(View.GONE);
                        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                        dividerView.getLayoutParams().width = getMeasuredWidth();
                        dividerView.setVisibility(View.VISIBLE);
                    }
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }
            };
            if (darkTheme) {
                sendPopupLayout1.setBackgroundColor(getThemedColor(Theme.key_voipgroup_inviteMembersBackground));
            }
            sendPopupLayout1.setAnimationEnabled(false);
            sendPopupLayout1.setOnTouchListener(new View.OnTouchListener() {
                private final android.graphics.Rect popupRect = new android.graphics.Rect();

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        if (sendPopupWindow != null && sendPopupWindow.isShowing()) {
                            v.getHitRect(popupRect);
                            if (!popupRect.contains((int) event.getX(), (int) event.getY())) {
                                sendPopupWindow.dismiss();
                            }
                        }
                    }
                    return false;
                }
            });
            sendPopupLayout1.setDispatchKeyEventListener(keyEvent -> {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0 && sendPopupWindow != null && sendPopupWindow.isShowing()) {
                    sendPopupWindow.dismiss();
                }
            });
            sendPopupLayout1.setShownFromBottom(false);

            ActionBarMenuSubItem showSendersNameView = new ActionBarMenuSubItem(getContext(), true, true, false, resourcesProvider);
            if (darkTheme) {
                showSendersNameView.setTextColor(getThemedColor(Theme.key_voipgroup_nameText));
            }
            sendPopupLayout1.addView(showSendersNameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
            showSendersNameView.setTextAndIcon(LocaleController.getString("ShowSendersName", R.string.ShowSendersName), 0);
            showSendersNameView.setChecked(!forwardParams.noQuote);

            ActionBarMenuSubItem hideSendersNameView = new ActionBarMenuSubItem(getContext(), true, false, true, resourcesProvider);
            if (darkTheme) {
                hideSendersNameView.setTextColor(getThemedColor(Theme.key_voipgroup_nameText));
            }
            sendPopupLayout1.addView(hideSendersNameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
            hideSendersNameView.setTextAndIcon(LocaleController.getString("HideSendersName", R.string.HideSendersName), 0);
            hideSendersNameView.setChecked(forwardParams.noQuote);
            showSendersNameView.setOnClickListener(e -> {
                if (forwardParams.noQuote) {
                    returnSendersNames = false;
                    showSendersNameView.setChecked(true);
                    hideSendersNameView.setChecked(false);
                    if (showCaptionView != null) {
                        showCaptionView.setChecked(true);
                        hideCaptionView.setChecked(false);
                    }
                    forwardParams.noQuote = false;
                    forwardParams.noCaption = false;
                }
            });
            hideSendersNameView.setOnClickListener(e -> {
                if (!forwardParams.noQuote) {
                    returnSendersNames = false;
                    showSendersNameView.setChecked(false);
                    hideSendersNameView.setChecked(true);
                    forwardParams.noQuote = true;
                }
            });

            boolean hasCaption = false;
            for (MessageObject message : forwardContext.getForwardingMessages()) {
                if (!TextUtils.isEmpty(message.caption)) {
                    hasCaption = true;
                    break;
                }
            }

            if (hasCaption) {
                sendPopupLayout1.addView(dividerView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

                showCaptionView = new ActionBarMenuSubItem(getContext(), true, false, false, resourcesProvider);
                if (darkTheme) {
                    showCaptionView.setTextColor(getThemedColor(Theme.key_voipgroup_nameText));
                }
                sendPopupLayout1.addView(showCaptionView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
                showCaptionView.setTextAndIcon(LocaleController.getString("ShowCaption", R.string.ShowCaption), 0);
                showCaptionView.setChecked(!forwardParams.noCaption);

                hideCaptionView = new ActionBarMenuSubItem(getContext(), true, false, true, resourcesProvider);
                if (darkTheme) {
                    hideCaptionView.setTextColor(getThemedColor(Theme.key_voipgroup_nameText));
                }
                sendPopupLayout1.addView(hideCaptionView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
                hideCaptionView.setTextAndIcon(LocaleController.getString("HideCaption", R.string.HideCaption), 0);
                hideCaptionView.setChecked(forwardParams.noCaption);
                showCaptionView.setOnClickListener(e -> {
                    if (forwardParams.noCaption) {
                        if (returnSendersNames) {
                            forwardParams.noQuote = false;
                        }
                        returnSendersNames = false;
                        showCaptionView.setChecked(true);
                        hideCaptionView.setChecked(false);
                        showSendersNameView.setChecked(!forwardParams.noQuote);
                        hideSendersNameView.setChecked(forwardParams.noQuote);
                        forwardParams.noCaption = false;
                    }
                });
                hideCaptionView.setOnClickListener(e -> {
                    if (!forwardParams.noCaption) {
                        showCaptionView.setChecked(false);
                        hideCaptionView.setChecked(true);
                        showSendersNameView.setChecked(false);
                        hideSendersNameView.setChecked(true);
                        if (!forwardParams.noQuote) {
                            forwardParams.noQuote = true;
                            returnSendersNames = true;
                        }
                        forwardParams.noCaption = true;
                    }
                });
            }
            sendPopupLayout1.setupRadialSelectors(getThemedColor(darkTheme ? Theme.key_voipgroup_listSelector : Theme.key_dialogButtonSelector));

            addView(sendPopupLayout1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, -8));
        }

        ActionBarPopupWindow.ActionBarPopupWindowLayout sendPopupLayout2 = new ActionBarPopupWindow.ActionBarPopupWindowLayout(parentActivity, resourcesProvider);
        if (darkTheme) {
            sendPopupLayout2.setBackgroundColor(Theme.getColor(Theme.key_voipgroup_inviteMembersBackground));
        }
        sendPopupLayout2.setAnimationEnabled(false);
        sendPopupLayout2.setOnTouchListener(new View.OnTouchListener() {

            private final android.graphics.Rect popupRect = new android.graphics.Rect();

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (sendPopupWindow != null && sendPopupWindow.isShowing()) {
                        v.getHitRect(popupRect);
                        if (!popupRect.contains((int) event.getX(), (int) event.getY())) {
                            sendPopupWindow.dismiss();
                        }
                    }
                }
                return false;
            }
        });
        sendPopupLayout2.setDispatchKeyEventListener(keyEvent -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0 && sendPopupWindow != null && sendPopupWindow.isShowing()) {
                sendPopupWindow.dismiss();
            }
        });
        sendPopupLayout2.setShownFromBottom(false);

        if (showSchedule) {
            ActionBarMenuSubItem scheduleButton = new ActionBarMenuSubItem(getContext(), true, !showNotify, resourcesProvider);
            if (darkTheme) {
                scheduleButton.setTextColor(getThemedColor(Theme.key_voipgroup_nameText));
                scheduleButton.setIconColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
            }
            scheduleButton.setTextAndIcon(LocaleController.getString("ScheduleMessage", R.string.ScheduleMessage), R.drawable.msg_schedule);
            scheduleButton.setMinimumWidth(AndroidUtilities.dp(196));
            scheduleButton.setOnClickListener(v -> {
                if (sendPopupWindow != null && sendPopupWindow.isShowing()) {
                    sendPopupWindow.dismiss();
                }
                AlertsCreator.createScheduleDatePickerDialog(parentActivity, 0, (notify, scheduleDate) -> {
                    forwardParams.notify = notify;
                    forwardParams.scheduleDate = scheduleDate;
                    delegate.sendMessage();
                }, resourcesProvider);
            });
            sendPopupLayout2.addView(scheduleButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        }
        if (showNotify) {
            ActionBarMenuSubItem sendWithoutSoundButton = new ActionBarMenuSubItem(getContext(), !showSchedule, true, resourcesProvider);
            if (darkTheme) {
                sendWithoutSoundButton.setTextColor(getThemedColor(Theme.key_voipgroup_nameText));
                sendWithoutSoundButton.setIconColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
            }
            sendWithoutSoundButton.setTextAndIcon(LocaleController.getString("SendWithoutSound", R.string.SendWithoutSound), R.drawable.input_notify_off);
            sendWithoutSoundButton.setMinimumWidth(AndroidUtilities.dp(196));
            sendWithoutSoundButton.setOnClickListener(v -> {
                if (sendPopupWindow != null && sendPopupWindow.isShowing()) {
                    sendPopupWindow.dismiss();
                }
                forwardParams.notify = false;
                delegate.sendMessage();
            });
            sendPopupLayout2.addView(sendWithoutSoundButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        }
        ActionBarMenuSubItem sendMessage = new ActionBarMenuSubItem(getContext(), true, true, resourcesProvider);
        if (darkTheme) {
            sendMessage.setTextColor(getThemedColor(Theme.key_voipgroup_nameText));
            sendMessage.setIconColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
        }
        sendMessage.setTextAndIcon(LocaleController.getString("SendMessage", R.string.SendMessage), R.drawable.msg_send);
        sendMessage.setMinimumWidth(AndroidUtilities.dp(196));
        sendPopupLayout2.addView(sendMessage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        sendMessage.setOnClickListener(v -> {
            if (sendPopupWindow != null && sendPopupWindow.isShowing()) {
                sendPopupWindow.dismiss();
            }
            delegate.sendMessage();
        });
        sendPopupLayout2.setupRadialSelectors(getThemedColor(darkTheme ? Theme.key_voipgroup_listSelector : Theme.key_dialogButtonSelector));

        addView(sendPopupLayout2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    public void setSendPopupWindow(ActionBarPopupWindow sendPopupWindow) {
        this.sendPopupWindow = sendPopupWindow;
    }

    public interface Delegate {
        void sendMessage();
    }

    private int getThemedColor(String key) {
        Integer color = resourcesProvider != null ? resourcesProvider.getColor(key) : null;
        return color != null ? color : Theme.getColor(key);
    }
}
