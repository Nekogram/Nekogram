package org.telegram.messenger;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.Components.Paint.PersistColorPalette;

import tw.nekomimi.nekogram.helpers.InlineBotHelper;
import tw.nekomimi.nekogram.helpers.MessageHelper;
import tw.nekomimi.nekogram.helpers.UserHelper;

public class BaseController {

    protected final int currentAccount;
    private AccountInstance parentAccountInstance;

    public BaseController(int num) {
        parentAccountInstance = AccountInstance.getInstance(num);
        currentAccount = num;
    }

    protected final AccountInstance getAccountInstance() {
        return parentAccountInstance;
    }

    protected final AppGlobalConfig getAppGlobalConfig() {
        return getMessagesController().config;
    }

    protected final MessagesController getMessagesController() {
        return parentAccountInstance.getMessagesController();
    }

    protected final ContactsController getContactsController() {
        return parentAccountInstance.getContactsController();
    }

    protected final PersistColorPalette getColorPalette() {
        return parentAccountInstance.getColorPalette();
    }

    protected final MediaDataController getMediaDataController() {
        return parentAccountInstance.getMediaDataController();
    }

    protected final ConnectionsManager getConnectionsManager() {
        return parentAccountInstance.getConnectionsManager();
    }

    protected final LocationController getLocationController() {
        return parentAccountInstance.getLocationController();
    }

    protected final NotificationsController getNotificationsController() {
        return parentAccountInstance.getNotificationsController();
    }

    protected final NotificationCenter getNotificationCenter() {
        return parentAccountInstance.getNotificationCenter();
    }

    protected final UserConfig getUserConfig() {
        return parentAccountInstance.getUserConfig();
    }

    protected final MessagesStorage getMessagesStorage() {
        return parentAccountInstance.getMessagesStorage();
    }

    protected final DownloadController getDownloadController() {
        return parentAccountInstance.getDownloadController();
    }

    protected final SendMessagesHelper getSendMessagesHelper() {
        return parentAccountInstance.getSendMessagesHelper();
    }

    protected final SecretChatHelper getSecretChatHelper() {
        return parentAccountInstance.getSecretChatHelper();
    }

    protected final StatsController getStatsController() {
        return parentAccountInstance.getStatsController();
    }

    protected final FileLoader getFileLoader() {
        return parentAccountInstance.getFileLoader();
    }

    protected final FileRefController getFileRefController() {
        return parentAccountInstance.getFileRefController();
    }

    protected final MemberRequestsController getMemberRequestsController() {
        return parentAccountInstance.getMemberRequestsController();
    }

    protected final MessageHelper getMessageHelper() {
        return parentAccountInstance.getMessageHelper();
    }

    protected final UserHelper getUserHelper() {
        return parentAccountInstance.getUserHelper();
    }

    protected final InlineBotHelper getInlineBotHelper() {
        return parentAccountInstance.getInlineBotHelper();
    }
}
