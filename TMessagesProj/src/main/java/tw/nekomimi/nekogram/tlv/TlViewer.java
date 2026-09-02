package tw.nekomimi.nekogram.tlv;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.OutputSerializedData;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.AlertsCreator;

public class TlViewer {

    // serialized message without attach path
    public static class CleanSerializedData extends SerializedData {
        public CleanSerializedData(int size) {
            super(size);
        }
    }

    public static void openTlViewer(BaseFragment fragment, TLObject... object) {
        openTlViewer(fragment, new TLObject() {
            @Override
            public void serializeToStream(OutputSerializedData stream) {
                stream.writeInt32(0x1cb5c415);
                stream.writeInt32(object.length);
                for (var obj : object) {
                    obj.serializeToStream(stream);
                }
            }
        });
    }

    public static void openTlViewer(BaseFragment fragment, TLObject object) {
        try {
            var data = new CleanSerializedData(object.getObjectSize());
            object.serializeToStream(data);
            var readData = new SerializedData(data.toByteArray());
            var jsonElement = TlBinaryReader.deserializeObject(TlReaders.READERS, readData);
            var jsonFragment = new JsonActivity(jsonElement);
            fragment.presentFragment(jsonFragment);
            readData.cleanup();
            data.cleanup();
        } catch (Exception e) {
            FileLog.e(e);
            AlertsCreator.showSimpleAlert(fragment,
                    LocaleController.getString(R.string.AppName),
                    LocaleController.getString(R.string.ErrorOccurred) + "\n" + e.getLocalizedMessage());
        }
    }
}
