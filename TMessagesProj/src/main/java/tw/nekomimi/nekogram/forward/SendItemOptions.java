package tw.nekomimi.nekogram.forward;

import android.view.View;
import android.view.ViewGroup;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.ItemOptions;

public class SendItemOptions {
    private boolean returnSendersNames;
    private ActionBarMenuSubItem showCaptionView;
    private ActionBarMenuSubItem hideCaptionView;
    private final ItemOptions itemOptions;

    public SendItemOptions(ViewGroup containerView, View anchorView, ForwardContext forwardContext, boolean showSchedule, boolean showNotify, Runnable callback, Theme.ResourcesProvider resourcesProvider) {
        this(containerView, anchorView, forwardContext, showSchedule, showNotify, -1, callback, resourcesProvider);
    }

    public SendItemOptions(ViewGroup containerView, View anchorView, ForwardContext forwardContext, boolean showSchedule, boolean showNotify, long dialogId, Runnable callback, Theme.ResourcesProvider resourcesProvider) {
        itemOptions = ItemOptions.makeOptions(containerView, resourcesProvider, anchorView);

        var forwardParams = forwardContext.getForwardParams();
        if (forwardContext.getForwardingMessages() != null) {
            var showSendersNameView = itemOptions.addChecked();
            showSendersNameView.setTextAndIcon(LocaleController.getString(R.string.ShowSendersName), 0);
            showSendersNameView.setChecked(!forwardParams.noQuote);

            var hideSendersNameView = itemOptions.addChecked();
            hideSendersNameView.setTextAndIcon(LocaleController.getString(R.string.HideSendersName), 0);
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

            boolean hasCaption = ForwardItem.hasCaption(forwardContext.getForwardingMessages());

            if (hasCaption) {
                itemOptions.addGap();

                showCaptionView = itemOptions.addChecked();
                showCaptionView.setTextAndIcon(LocaleController.getString(R.string.ShowCaption), 0);
                showCaptionView.setChecked(!forwardParams.noCaption);

                hideCaptionView = itemOptions.addChecked();
                hideCaptionView.setTextAndIcon(LocaleController.getString(R.string.HideCaption), 0);
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

            itemOptions.addGap();
        }

        itemOptions.addIf(showNotify, R.drawable.input_notify_off, LocaleController.getString(R.string.SendWithoutSound), () -> {
            forwardParams.notify = false;
            callback.run();
        });
        itemOptions.addIf(showSchedule, R.drawable.msg_calendar2, LocaleController.getString(R.string.ScheduleMessage), () ->
                AlertsCreator.createScheduleDatePickerDialog(containerView.getContext(), dialogId, (notify, scheduleDate, scheduleRepeatPeriod) -> {
                    forwardParams.notify = notify;
                    forwardParams.scheduleDate = scheduleDate;
                    forwardParams.scheduleRepeatPeriod = scheduleRepeatPeriod;
                    callback.run();
                }, resourcesProvider));
        itemOptions.add(R.drawable.msg_send, LocaleController.getString(R.string.SendMessage), callback);
    }

    public void show() {
        itemOptions.show();
    }
}
