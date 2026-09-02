package tw.nekomimi.nekogram.forward;

import android.view.View;

import androidx.collection.LongSparseArray;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ShareAlert;
import org.telegram.ui.Components.UndoView;

import java.util.ArrayList;

public interface ForwardContext {
    ForwardParams forwardParams = new ForwardParams();

    ArrayList<MessageObject> getForwardingMessages();

    default boolean forceShowScheduleAndSound() {
        return false;
    }

    default ForwardParams getForwardParams() {
        return forwardParams;
    }

    default void openShareAlert(BaseFragment parentFragment, ChatActivity parentChatActivity, Runnable callback) {
        var context = parentFragment.getContext();
        if (context == null) {
            return;
        }
        parentFragment.showDialog(new ShareAlert(context, parentChatActivity, getForwardingMessages(), null, null, false, null, null, false, false, false, forwardParams.noQuote, forwardParams.noCaption, null, parentFragment.getResourceProvider()) {
            @Override
            public void dismissInternal() {
                super.dismissInternal();
                if (parentChatActivity != null) {
                    AndroidUtilities.requestAdjustResize(parentChatActivity.getParentActivity(), parentChatActivity.getClassGuid());
                    if (parentChatActivity.getChatActivityEnterView().getVisibility() == View.VISIBLE) {
                        parentChatActivity.getFragmentView().requestLayout();
                    }
                }
            }

            @Override
            protected void onSend(LongSparseArray<TLRPC.Dialog> dids, int count, TLRPC.TL_forumTopic topic, boolean showToast) {
                if (showToast && parentChatActivity != null) {
                    AndroidUtilities.runOnUIThread(() -> {
                        var undoView = parentChatActivity.getUndoView();
                        if (undoView == null) {
                            return;
                        }
                        if (dids.size() == 1) {
                            if (dids.valueAt(0).id != parentFragment.getUserConfig().getClientUserId() || !BulletinFactory.of(parentFragment).showForwardedBulletinWithTag(dids.valueAt(0).id, count)) {
                                undoView.showWithAction(dids.valueAt(0).id, UndoView.ACTION_FWD_MESSAGES, count, topic, null, null);
                            }
                        } else {
                            undoView.showWithAction(0, UndoView.ACTION_FWD_MESSAGES, count, dids.size(), null, null);
                        }
                    }, 0);
                }
                callback.run();
            }
        });
        if (parentChatActivity != null) {
            AndroidUtilities.setAdjustResizeToNothing(parentChatActivity.getParentActivity(), parentChatActivity.getClassGuid());
            parentChatActivity.getFragmentView().requestLayout();
        }
    }

    default void setForwardParams(boolean noquote, boolean nocaption) {
        forwardParams.noQuote = noquote || nocaption;
        forwardParams.noCaption = nocaption;
        forwardParams.notify = true;
        forwardParams.scheduleDate = 0;
    }

    default void setForwardParams(boolean noquote) {
        forwardParams.noQuote = noquote;
        forwardParams.noCaption = false;
        forwardParams.notify = true;
        forwardParams.scheduleDate = 0;
    }

    class ForwardParams {
        public boolean noQuote = false;
        public boolean noCaption = false;
        public boolean notify = true;
        public int scheduleDate = 0;
        public int scheduleRepeatPeriod = 0;
    }
}
