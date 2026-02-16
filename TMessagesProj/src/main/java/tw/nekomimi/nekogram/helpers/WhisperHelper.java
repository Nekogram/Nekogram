package tw.nekomimi.nekogram.helpers;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.text.TextUtils;
import android.util.Base64;
import android.util.TypedValue;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BotWebViewVibrationEffect;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import tw.nekomimi.nekogram.NekoConfig;

public class WhisperHelper {
    private static OkHttpClient okHttpClient;
    private static final Gson gson = new Gson();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static boolean useWorkersAi(int account) {
        return NekoConfig.transcribeProvider == NekoConfig.TRANSCRIBE_WORKERSAI || (!UserConfig.getInstance(account).isPremium() && NekoConfig.transcribeProvider == NekoConfig.TRANSCRIBE_AUTO);
    }

    public static void showErrorDialog(Exception e) {
        var fragment = LaunchActivity.getSafeLastFragment();
        var message = e.getLocalizedMessage();
        if (!BulletinFactory.canShowBulletin(fragment) || message == null) {
            return;
        }
        if (message.length() > 45) {
            AlertsCreator.showSimpleAlert(fragment, LocaleController.getString(R.string.ErrorOccurred), e.getMessage());
        } else {
            BulletinFactory.of(fragment).createErrorBulletin(message).show();
        }
    }

    public static void showCfCredentialsDialog(BaseFragment fragment) {
        var resourcesProvider = fragment.getResourceProvider();
        var context = fragment.getParentActivity();
        var builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString(R.string.CloudflareCredentials));
        builder.setMessage(AndroidUtilities.replaceSingleTag(LocaleController.getString(R.string.CloudflareCredentialsDialog),
                -1,
                AndroidUtilities.REPLACING_TAG_TYPE_LINKBOLD,
                () -> Browser.openUrl(context, "https://nekogram.app/cloudflare-credentials"),
                resourcesProvider));
        builder.setCustomViewOffset(0);

        var ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        var editTextAccountId = new EditTextBoldCursor(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
            }
        };
        editTextAccountId.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        editTextAccountId.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        editTextAccountId.setText(NekoConfig.cfAccountID);
        editTextAccountId.setHintText(LocaleController.getString(R.string.CloudflareAccountID));
        editTextAccountId.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        editTextAccountId.setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider));
        editTextAccountId.setSingleLine(true);
        editTextAccountId.setFocusable(true);
        editTextAccountId.setTransformHintToHeader(true);
        editTextAccountId.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField, resourcesProvider), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, resourcesProvider), Theme.getColor(Theme.key_text_RedRegular, resourcesProvider));
        editTextAccountId.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        editTextAccountId.setBackground(null);
        editTextAccountId.requestFocus();
        editTextAccountId.setPadding(0, 0, 0, 0);
        ll.addView(editTextAccountId, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, 0, 24, 0, 24, 0));

        var editTextApiToken = new EditTextBoldCursor(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
            }
        };
        editTextApiToken.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        editTextApiToken.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        editTextApiToken.setText(NekoConfig.cfApiToken);
        editTextApiToken.setHintText(LocaleController.getString(R.string.CloudflareAPIToken));
        editTextApiToken.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        editTextApiToken.setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider));
        editTextApiToken.setSingleLine(true);
        editTextApiToken.setFocusable(true);
        editTextApiToken.setTransformHintToHeader(true);
        editTextApiToken.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField, resourcesProvider), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, resourcesProvider), Theme.getColor(Theme.key_text_RedRegular, resourcesProvider));
        editTextApiToken.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editTextApiToken.setBackground(null);
        editTextApiToken.requestFocus();
        editTextApiToken.setPadding(0, 0, 0, 0);
        ll.addView(editTextApiToken, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, 0, 24, 0, 24, 0));

        builder.setView(ll);
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        var dialog = builder.create();
        fragment.showDialog(dialog);
        var button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (button != null) {
            button.setOnClickListener(v -> {
                var accountId = editTextAccountId.getText();
                if (!TextUtils.isEmpty(accountId) && accountId.length() != 32) {
                    AndroidUtilities.shakeViewSpring(editTextAccountId, -6);
                    BotWebViewVibrationEffect.APP_ERROR.vibrate();
                    return;
                }
                var apiToken = editTextApiToken.getText();
                if (!TextUtils.isEmpty(apiToken) && apiToken.length() != 40) {
                    AndroidUtilities.shakeViewSpring(editTextApiToken, -6);
                    BotWebViewVibrationEffect.APP_ERROR.vibrate();
                    return;
                }
                NekoConfig.setCfAccountID(accountId == null ? "" : accountId.toString());
                NekoConfig.setCfApiToken(apiToken == null ? "" : apiToken.toString());
                dialog.dismiss();
            });
        }
    }

    private static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            var builder = new OkHttpClient.Builder();
            builder.connectTimeout(120, TimeUnit.SECONDS);
            builder.readTimeout(120, TimeUnit.SECONDS);
            builder.writeTimeout(120, TimeUnit.SECONDS);
            okHttpClient = builder.build();
        }
        return okHttpClient;
    }

    private static void extractAudio(String inputFilePath, String outputFilePath) throws IOException {
        var extractor = new MediaExtractor();
        extractor.setDataSource(inputFilePath);

        MediaFormat audioFormat = null;
        int audioTrackIndex = -1;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            var format = extractor.getTrackFormat(i);
            var mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                audioFormat = format;
                audioTrackIndex = i;
                break;
            }
        }

        if (audioFormat == null) {
            throw new IOException("No audio track found in " + inputFilePath);
        }

        var muxer = new MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        var trackIndex = muxer.addTrack(audioFormat);
        muxer.start();

        extractor.selectTrack(audioTrackIndex);

        var bufferInfo = new MediaCodec.BufferInfo();
        var buffer = ByteBuffer.allocate(65536);

        while (true) {
            var sampleSize = extractor.readSampleData(buffer, 0);
            if (sampleSize < 0) {
                break;
            }

            bufferInfo.offset = 0;
            bufferInfo.size = sampleSize;
            bufferInfo.presentationTimeUs = extractor.getSampleTime();
            bufferInfo.flags = 0;

            muxer.writeSampleData(trackIndex, buffer, bufferInfo);
            extractor.advance();
        }

        muxer.stop();
        muxer.release();
        extractor.release();
    }

    public static void requestWorkersAi(String path, boolean video, BiConsumer<String, Exception> callback) {
        if (TextUtils.isEmpty(NekoConfig.cfAccountID) || TextUtils.isEmpty(NekoConfig.cfApiToken)) {
            callback.accept(null, new Exception(LocaleController.getString(R.string.CloudflareCredentialsNotSet)));
            return;
        }
        executorService.submit(() -> {
            File audioPath;
            if (video) {
                var audioFile = new File(path + ".m4a");
                try {
                    extractAudio(path, audioFile.getAbsolutePath());
                } catch (IOException e) {
                    FileLog.e(e);
                }
                audioPath = audioFile.exists() ? audioFile : new File(path);
            } else {
                audioPath = new File(path);
            }
            byte[] audio;
            try {
                audio = Files.readAllBytes(audioPath.toPath());
            } catch (IOException e) {
                callback.accept(null, e);
                return;
            }
            var payload = new WhisperRequest();
            payload.audio = Base64.encodeToString(audio, Base64.NO_WRAP);
            payload.vadFilter = false;
            var client = getOkHttpClient();
            var request = new Request.Builder()
                    .url("https://api.cloudflare.com/client/v4/accounts/" + NekoConfig.cfAccountID + "/ai/run/@cf/openai/whisper-large-v3-turbo")
                    .header("Authorization", "Bearer " + NekoConfig.cfApiToken)
                    .post(RequestBody.create(gson.toJson(payload), MediaType.get("application/json")));
            try (var response = client.newCall(request.build()).execute()) {
                var body = response.body().string();
                var whisperResponse = gson.fromJson(body, WhisperResponse.class);
                if (whisperResponse.success && whisperResponse.result != null) {
                    callback.accept(whisperResponse.result.text, null);
                } else {
                    var errors = whisperResponse.errors;
                    callback.accept(null, new Exception(errors.size() == 1 ? errors.get(0).message : errors.toString()));
                }
            } catch (Exception e) {
                callback.accept(null, e);
            }
        });
    }

    public static class WhisperRequest {
        @SerializedName("audio")
        @Expose
        public String audio;
        @SerializedName("vad_filter")
        @Expose
        public Boolean vadFilter;
    }

    public static class Result {
        @SerializedName("text")
        @Expose
        public String text;
    }

    public static class WhisperResponse {
        @SerializedName("result")
        @Expose
        public Result result;
        @SerializedName("success")
        @Expose
        public Boolean success;
        @SerializedName("errors")
        @Expose
        public List<Error> errors;
    }

    public static class Error {
        @SerializedName("message")
        @Expose
        public String message;

        @NonNull
        @Override
        public String toString() {
            return message;
        }
    }
}
