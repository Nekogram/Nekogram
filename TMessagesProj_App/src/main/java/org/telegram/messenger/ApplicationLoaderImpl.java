package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import org.telegram.messenger.regular.BuildConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.UpdateAppAlertDialog;
import org.telegram.ui.Components.UpdateButton;
import org.telegram.ui.Components.UpdateLayout;
import org.telegram.ui.IUpdateButton;
import org.telegram.ui.IUpdateLayout;

public class ApplicationLoaderImpl extends ApplicationLoader {
    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }

    @Override
    protected boolean isStandalone() {
        return "release".equals(org.telegram.messenger.BuildConfig.BUILD_TYPE) || "debug".equals(org.telegram.messenger.BuildConfig.BUILD_TYPE);
    }

    @Override
    public boolean showUpdateAppPopup(Context context, TLRPC.TL_help_appUpdate update, int account) {
        try {
            (new UpdateAppAlertDialog(context, update, account)).show();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return true;
    }

    @Override
    public IUpdateLayout takeUpdateLayout(Activity activity, ViewGroup sideMenu, ViewGroup sideMenuContainer) {
        return new UpdateLayout(activity, sideMenu, sideMenuContainer);
    }

    @Override
    public IUpdateButton takeUpdateButton(Context context) {
        return new UpdateButton(context);
    }
}
