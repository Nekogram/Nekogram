package tw.nekomimi.nekogram;

import org.telegram.messenger.MessageObject;

import java.util.ArrayList;

public interface ForwardContext {
    ForwardParams forwardParams = new ForwardParams();

    ArrayList<MessageObject> getForwardingMessages();

    default ForwardParams getForwardParams() {
        return forwardParams;
    }

    default void setForwardParams(boolean noquote, boolean nocaption) {
        forwardParams.noQuote = noquote;
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
    }
}
