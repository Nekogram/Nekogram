package tw.nekomimi.nekogram.helpers.remote;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.jaredrummler.truetypeparser.TTFFile;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.EmojiData;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.AbstractSerializedData;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Bulletin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import tw.nekomimi.nekogram.Extra;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.UnzipHelper;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class EmojiHelper extends BaseRemoteHelper implements NotificationCenter.NotificationCenterDelegate {
    private static final String EMOJI_TAG = "emoji" + 15;
    private static final String EMOJI_FONT_AOSP = "NotoColorEmoji.ttf";
    private static final int EMOJI_COUNT;
    private static final String EMOJI_PACKS_FILE_DIR;
    private static final Runnable invalidateUiRunnable = () -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.emojiLoaded);
    private static final String[] previewEmojis = {
            "\uD83D\uDE00",
            "\uD83D\uDE09",
            "\uD83D\uDE14",
            "\uD83D\uDE28"
    };
    private static volatile EmojiHelper Instance;
    private static int currentAccount = UserConfig.selectedAccount;
    private static TextPaint textPaint;

    private final HashMap<String, Typeface> typefaceCache = new HashMap<>();
    private final ArrayList<EmojiPackBase> emojiPacksInfo = new ArrayList<>();
    private final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekoemojis", Context.MODE_PRIVATE);
    private final HashMap<String, Pair<EmojiPackInfo, Boolean[]>> loadingEmojiPacks = new HashMap<>();
    private final ArrayList<EmojiPackLoadListener> listeners = new ArrayList<>();

    private String emojiPack;
    private Typeface systemEmojiTypeface;
    private Bitmap systemEmojiPreview;
    private boolean loadSystemEmojiFailed = false;
    private boolean loadingPack = false;
    private String pendingDeleteEmojiPackId;
    private Bulletin emojiPackBulletin;

    static {
        var files = ApplicationLoader.applicationContext.getExternalFilesDir(null);
        if (files != null) {
            EMOJI_PACKS_FILE_DIR = files.getAbsolutePath() + "/emojis/";
        } else {
            EMOJI_PACKS_FILE_DIR = ApplicationLoader.applicationContext.getFilesDir().getAbsolutePath() + "/emojis/";
        }
        int count = 0;
        for (String[] data : EmojiData.data) {
            count += data.length;
        }
        EMOJI_COUNT = count;
    }

    private EmojiHelper() {
        checkAccount();
        emojiPack = preferences.getString("emoji_pack", "");
    }

    public static EmojiHelper getInstance() {
        EmojiHelper localInstance = Instance;
        if (localInstance == null) {
            synchronized (EmojiHelper.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new EmojiHelper();
                }
                return localInstance;
            }
        }
        return localInstance;
    }

    public static File getSystemEmojiFontPath() {
        try (var br = new BufferedReader(new FileReader("/system/etc/fonts.xml"))) {
            String line;
            var ignored = false;
            while ((line = br.readLine()) != null) {
                var trimmed = line.trim();
                if (trimmed.startsWith("<family") && trimmed.contains("ignore=\"true\"")) {
                    ignored = true;
                } else if (trimmed.startsWith("</family>")) {
                    ignored = false;
                } else if (trimmed.startsWith("<font") && !ignored) {
                    var start = trimmed.indexOf(">");
                    var end = trimmed.indexOf("<", 1);
                    if (start > 0 && end > 0) {
                        var font = trimmed.substring(start + 1, end);
                        if (font.toLowerCase().contains("emoji")) {
                            File file = new File("/system/fonts/" + font);
                            if (file.exists()) {
                                FileLog.d("emoji font file fonts.xml = " + font);
                                return file;
                            }
                        }
                    }
                }
            }
            br.close();

            var fileAOSP = new File("/system/fonts/" + EMOJI_FONT_AOSP);
            if (fileAOSP.exists()) {
                return fileAOSP;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    private static ArrayList<File> getAllEmojis() {
        ArrayList<File> emojis = new ArrayList<>();
        File emojiDir = new File(EMOJI_PACKS_FILE_DIR);
        if (emojiDir.exists()) {
            File[] files = emojiDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        emojis.add(file);
                    }
                }
            }
        }
        return emojis;
    }

    private static long calculateFolderSize(File directory) {
        long length = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    length += file.length();
                } else {
                    length += calculateFolderSize(file);
                }
            }
        }
        return length;
    }

    public static boolean isValidCustomPack(File file) {
        String packName = file.getName();
        int lastIndexOf = packName.lastIndexOf("_v");
        if (lastIndexOf == -1) {
            return false;
        }
        packName = packName.substring(0, lastIndexOf);
        return new File(file, packName + ".ttf").exists() && new File(file, "preview.png").exists();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deleteFolder(File input) {
        File[] files = input.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        input.delete();
    }

    public static File getEmojiDir(String emojiID, int version) {
        return new File(EMOJI_PACKS_FILE_DIR + emojiID + "_v" + version);
    }

    public static void mkDirs() {
        File emojiDir = new File(EMOJI_PACKS_FILE_DIR);
        if (!emojiDir.exists()) {
            emojiDir.mkdirs();
        }
    }

    public static boolean isValidEmojiPack(File path) {
        if (path == null) {
            return false;
        }
        try {
            Typeface typeface;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                typeface = new Typeface.Builder(path)
                        .build();
            } else {
                typeface = Typeface.createFromFile(path);
            }
            return typeface != null && !typeface.equals(Typeface.DEFAULT);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void drawEmojiFont(Canvas canvas, int x, int y, Typeface typeface, String emoji, int emojiSize) {
        int fontSize = (int) (emojiSize * 0.85f);
        Rect areaRect = new Rect(0, 0, emojiSize, emojiSize);
        if (textPaint == null) {
            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextAlign(Paint.Align.CENTER);
        }
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(fontSize);
        Rect textRect = new Rect();
        textPaint.getTextBounds(emoji, 0, emoji.length(), textRect);
        canvas.drawText(emoji, areaRect.centerX() + x, -textRect.top + y, textPaint);
    }

    public Long getEmojiSize() {
        return getAllEmojis().stream()
                .filter(file -> !file.getName().startsWith(emojiPack))
                .filter(file -> !isValidCustomPack(file))
                .map(EmojiHelper::calculateFolderSize)
                .reduce(0L, Long::sum);
    }

    public void deleteAll() {
        getAllEmojis().stream()
                .filter(file -> !file.getName().startsWith(emojiPack))
                .filter(file -> !isValidCustomPack(file))
                .forEach(EmojiHelper::deleteFolder);
    }

    public String getEmojiPack() {
        return emojiPack;
    }

    public void setEmojiPack(String pack) {
        setEmojiPack(pack, true);
    }

    public void setEmojiPack(String pack, boolean manually) {
        emojiPack = pack;
        preferences.edit().putString("emoji_pack", pack).apply();
        if (manually && NekoConfig.useSystemEmoji) {
            NekoConfig.toggleUseSystemEmoji();
        }
    }

    public void addListener(EmojiPackLoadListener listener) {
        listeners.add(listener);
    }

    public void removeListener(EmojiPackLoadListener listener) {
        listeners.remove(listener);
    }

    public Typeface getCurrentTypeface() {
        if (NekoConfig.useSystemEmoji) {
            return getSystemEmojiTypeface();
        } else {
            return getSelectedTypeface();
        }
    }

    public Typeface getSystemEmojiTypeface() {
        if (!loadSystemEmojiFailed && systemEmojiTypeface == null) {
            var font = getSystemEmojiFontPath();
            if (font != null) {
                systemEmojiTypeface = Typeface.createFromFile(font);
            }
            if (systemEmojiTypeface == null) {
                loadSystemEmojiFailed = true;
            }
        }
        return systemEmojiTypeface;
    }

    private Typeface getSelectedTypeface() {
        EmojiPackBase pack = getEmojiCustomPacksInfo()
                .stream()
                .filter(emojiPackInfo -> emojiPackInfo.packId.equals(emojiPack))
                .findFirst()
                .orElse(null);
        if (pack == null) {
            return null;
        }
        Typeface typeface;
        if (!typefaceCache.containsKey(pack.packId)) {
            File emojiFile = new File(pack.fileLocation);
            if (!emojiFile.exists()) {
                return null;
            }
            typefaceCache.put(pack.packId, typeface = Typeface.createFromFile(emojiFile));
        } else {
            typeface = typefaceCache.get(pack.packId);
        }
        return typeface;
    }

    public String getSelectedPackName() {
        if (NekoConfig.useSystemEmoji) return "System";
        return emojiPacksInfo
                .stream()
                .filter(e -> {
                    if (e instanceof EmojiPackInfo) {
                        return isPackInstalled((EmojiPackInfo) e);
                    }
                    return true;
                })
                .filter(emojiPackInfo -> Objects.equals(emojiPackInfo.packId, emojiPack))
                .findFirst()
                .map(e -> e.packName)
                .orElse("Apple");
    }

    public String getSelectedEmojiPackId() {
        return getAllEmojis()
                .stream()
                .map(File::getName)
                .anyMatch(name -> name.startsWith(emojiPack) || name.endsWith(emojiPack))
                ? emojiPack : "default";
    }

    public void loadEmojisInfo(EmojiPacksLoadedListener listener) {
        if (loadingPack) {
            return;
        }
        loadingPack = true;
        emojiPacksInfo.clear();
        loadCustomEmojiPacks();
        loadEmojiPackInfo();
        getInstance().load((res, error) -> AndroidUtilities.runOnUIThread(() -> {
            loadingPack = false;
            listener.emojiPacksLoaded(error);
        }));
    }

    public void loadEmojiPackInfo() {
        String list = preferences.getString("emoji_packs_v3", "");
        if (!TextUtils.isEmpty(list)) {
            byte[] bytes = Base64.decode(list, Base64.DEFAULT);
            SerializedData data = new SerializedData(bytes);
            int count = data.readInt32(false);
            for (int a = 0; a < count; a++) {
                emojiPacksInfo.add(EmojiPackInfo.deserialize(data));
            }
            data.cleanup();
        }
    }

    public ArrayList<EmojiPackBase> getEmojiPacks() {
        return emojiPacksInfo;
    }

    public ArrayList<EmojiPackInfo> getEmojiPacksInfo() {
        return emojiPacksInfo.stream()
                .filter(e -> e instanceof EmojiPackInfo)
                .map(e -> (EmojiPackInfo) e)
                .filter(e -> e.getFileDocument() != null && e.getPreviewDocument() != null)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<EmojiPackBase> getEmojiCustomPacksInfo() {
        return emojiPacksInfo.stream()
                .filter(e -> !(e instanceof EmojiPackInfo))
                .filter(e -> !e.getPackId().equals(pendingDeleteEmojiPackId))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public boolean isInstalledOldVersion(String emojiID, int version) {
        return getAllVersions(emojiID, version).size() > 0;
    }

    public boolean isInstalledOffline(String emojiID) {
        return getAllVersions(emojiID, -1).size() > 0;
    }

    public ArrayList<File> getAllVersions(String emojiID) {
        return getAllVersions(emojiID, -1);
    }

    public File getCurrentEmojiPackOffline() {
        return getAllVersions(emojiPack)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public ArrayList<File> getAllVersions(String emojiID, int version) {
        return getAllEmojis().stream()
                .filter(file -> file.getName().startsWith(emojiID))
                .filter(file -> version == -1 || !file.getName().endsWith("_v" + version))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void deleteOldVersions(EmojiPackInfo pack) {
        for (File oldVersion : getAllVersions(pack.getPackId(), pack.getPackVersion())) {
            deleteFolder(oldVersion);
        }
    }

    public boolean isPackDownloaded(EmojiPackInfo pack) {
        return EmojiHelper.getEmojiDir(pack.getPackId(), pack.getPackVersion()).exists();
    }

    public boolean isPackInstalled(EmojiPackInfo pack) {
        var dir = EmojiHelper.getEmojiDir(pack.getPackId(), pack.getPackVersion());
        if (!dir.exists()) {
            return false;
        }
        var list = dir.list();
        return list != null && list.length >= EMOJI_COUNT;
    }

    public EmojiPackBase getCurrentEmojiPackInfo() {
        var selected = getSelectedEmojiPackId();
        return emojiPacksInfo.stream()
                .filter(emojiPackInfo -> emojiPackInfo != null && emojiPackInfo.packId.equals(selected))
                .findFirst()
                .orElse(null);
    }

    public EmojiPackInfo getEmojiPackInfo(String emojiPackId) {
        return emojiPacksInfo.stream()
                .filter(emojiPackInfo -> emojiPackInfo instanceof EmojiPackInfo)
                .filter(emojiPackInfo -> emojiPackInfo.packId.equals(emojiPackId))
                .map(emojiPackInfo -> (EmojiPackInfo) emojiPackInfo)
                .findFirst()
                .orElse(null);
    }

    public boolean isEmojiPackDownloading(EmojiPackInfo pack) {
        return FileLoader.getInstance(currentAccount).isLoadingFile(FileLoader.getAttachFileName(pack.getFileDocument()));
    }

    public void installDownloadedEmoji(EmojiPackInfo pack, boolean update) {
        var emojiDir = EmojiHelper.getEmojiDir(pack.packId, pack.packVersion);
        emojiDir.mkdir();
        UnzipHelper.unzip(pack.fileLocation, emojiDir, () -> {
            if (isPackInstalled(pack)) {
                if (update) {
                    EmojiHelper.getInstance().deleteOldVersions(pack);
                } else {
                    EmojiHelper.getInstance().setEmojiPack(pack.getPackId());
                }
                reloadEmoji();
            }
            callProgressChanged(pack, true, 100, pack.fileSize);
            loadingEmojiPacks.remove(pack.fileLocation);
        });
        callProgressChanged(pack, false, 100, pack.fileSize);
    }

    public EmojiPackBase installEmoji(File emojiFile) throws Exception {
        return installEmoji(emojiFile, true);
    }

    public EmojiPackBase installEmoji(File emojiFile, boolean checkInstallation) throws IOException, NoSuchAlgorithmException {
        String fontName = emojiFile.getName();
        int dotIndex = fontName.lastIndexOf('.');
        if (dotIndex != -1) {
            fontName = fontName.substring(0, dotIndex);
        }

        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileInputStream inputStream = new FileInputStream(emojiFile)) {
            byte[] dataBytes = new byte[4 * 1024];
            int nread;
            while ((nread = inputStream.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
        }
        byte[] mdBytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte mdByte : mdBytes) {
            sb.append(Integer.toString((mdByte & 0xff) + 0x100, 16).substring(1));
        }

        try (InputStream inputStream = new FileInputStream(emojiFile)) {
            String tmpFontName = TTFFile.open(inputStream).getFullName();
            if (tmpFontName != null) {
                fontName = tmpFontName;
            }
        } catch (IOException e) {
            FileLog.e(e);
        }
        File emojiDir = new File(EMOJI_PACKS_FILE_DIR + fontName + "_v" + sb);
        boolean isAlreadyInstalled = getAllEmojis().stream()
                .filter(EmojiHelper::isValidCustomPack)
                .anyMatch(file -> file.getName().endsWith(sb.toString()));
        if (isAlreadyInstalled) {
            if (checkInstallation) {
                return null;
            } else {
                EmojiPackBase emojiPackBase = new EmojiPackBase();
                emojiPackBase.loadFromFile(emojiDir);
                return emojiPackBase;
            }
        }
        emojiDir.mkdirs();
        File emojiFont = new File(emojiDir, fontName + ".ttf");
        try (FileInputStream inputStream = new FileInputStream(emojiFile)) {
            AndroidUtilities.copyFile(inputStream, emojiFont);
        }
        Typeface typeface = Typeface.createFromFile(emojiFont);
        Bitmap bitmap = drawPreviewBitmap(typeface);
        File emojiPreview = new File(emojiDir, "preview.png");
        try (FileOutputStream outputStream = new FileOutputStream(emojiPreview)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        }
        EmojiPackBase emojiPackBase = new EmojiPackBase();
        emojiPackBase.loadFromFile(emojiDir);
        emojiPacksInfo.add(emojiPackBase);
        return emojiPackBase;
    }

    private Bitmap drawPreviewBitmap(Typeface typeface) {
        int emojiSize = 73;
        Bitmap bitmap = Bitmap.createBitmap(emojiSize * 2, emojiSize * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                int xPos = x * emojiSize;
                int yPos = y * emojiSize;
                String emoji = previewEmojis[x + y * 2];
                EmojiHelper.drawEmojiFont(
                        canvas,
                        xPos,
                        yPos,
                        typeface,
                        emoji,
                        emojiSize
                );
            }
        }
        return bitmap;
    }

    public Bitmap getSystemEmojiPreview() {
        if (systemEmojiPreview == null) {
            systemEmojiPreview = drawPreviewBitmap(getSystemEmojiTypeface());
        }
        return systemEmojiPreview;
    }

    private void loadCustomEmojiPacks() {
        getAllEmojis().stream()
                .filter(EmojiHelper::isValidCustomPack)
                .sorted(Comparator.comparingLong(File::lastModified))
                .map(file -> {
                    EmojiPackBase emojiPackBase = new EmojiPackBase();
                    emojiPackBase.loadFromFile(file);
                    return emojiPackBase;
                })
                .forEach(emojiPacksInfo::add);
    }

    public boolean isSelectedCustomEmojiPack() {
        return getAllEmojis().stream()
                .filter(EmojiHelper::isValidCustomPack)
                .anyMatch(file -> file.getName().endsWith(emojiPack));
    }

    public void cancelableDelete(BaseFragment fragment, EmojiPackBase emojiPackBase, OnBulletinAction onUndoBulletinAction) {
        if (emojiPackBulletin != null && pendingDeleteEmojiPackId != null) {
            AlertDialog progressDialog = new AlertDialog(fragment.getParentActivity(), 3);
            emojiPackBulletin.hide(false, 0);
            new Thread() {
                @Override
                public void run() {
                    do {
                        SystemClock.sleep(50);
                    } while (pendingDeleteEmojiPackId != null);
                    AndroidUtilities.runOnUIThread(() -> {
                        progressDialog.dismiss();
                        cancelableDelete(fragment, emojiPackBase, onUndoBulletinAction);
                    });
                }
            }.start();
            progressDialog.setCanCancel(false);
            progressDialog.showDelayed(150);
            return;
        }
        pendingDeleteEmojiPackId = emojiPackBase.getPackId();
        onUndoBulletinAction.onPreStart();
        boolean wasSelected = emojiPackBase.getPackId().equals(emojiPack);
        if (wasSelected) {
            EmojiHelper.getInstance().setEmojiPack("default", false);
        }
        EmojiSetBulletinLayout bulletinLayout = new EmojiSetBulletinLayout(
                fragment.getParentActivity(),
                LocaleController.getString("EmojiSetRemoved", R.string.EmojiSetRemoved),
                LocaleController.formatString("EmojiSetRemovedInfo", R.string.EmojiSetRemovedInfo, emojiPackBase.getPackName()),
                emojiPackBase,
                null
        );
        Bulletin.UndoButton undoButton = new Bulletin.UndoButton(fragment.getParentActivity(), false).setUndoAction(() -> {
            if (wasSelected) {
                EmojiHelper.getInstance().setEmojiPack(pendingDeleteEmojiPackId, false);
            }
            pendingDeleteEmojiPackId = null;
            onUndoBulletinAction.onUndo();
        }).setDelayedAction(() -> new Thread() {
            @Override
            public void run() {
                deleteEmojiPack(emojiPackBase);
                reloadEmoji();
                pendingDeleteEmojiPackId = null;
            }
        }.start());
        bulletinLayout.setButton(undoButton);
        emojiPackBulletin = Bulletin.make(fragment, bulletinLayout, Bulletin.DURATION_LONG).show();
    }

    public void deleteEmojiPack(EmojiPackBase emojiPackBase) {
        File emojiDir = new File(emojiPackBase.getFileLocation()).getParentFile();
        if (emojiDir != null && emojiDir.exists()) {
            File[] files = emojiDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            emojiDir.delete();
        }
        emojiPacksInfo.remove(emojiPackBase);
        if (emojiPackBase.getPackId().equals(emojiPack)) {
            EmojiHelper.getInstance().setEmojiPack("default", false);
        }
    }

    @Override
    protected void onError(String text, Delegate delegate) {
        delegate.onTLResponse(null, text);
    }

    @Override
    protected String getTag() {
        return EMOJI_TAG;
    }

    private void checkAccount() {
        AndroidUtilities.runOnUIThread(() -> {
            if (currentAccount != UserConfig.selectedAccount) {
                NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoaded);
                NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoadProgressChanged);
                NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoadFailed);
            }
            currentAccount = UserConfig.selectedAccount;
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoadProgressChanged);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoadFailed);
        });
    }

    private void getNewVersionMessagesCallback(Delegate delegate, ArrayList<EmojiPackInfo> packs, TLObject response) {
        if (response != null) {
            var res = (TLRPC.messages_Messages) response;
            getMessagesController().removeDeletedMessagesFromArray(Extra.UPDATE_CHANNEL_ID, res.messages);
            var documents = new HashMap<Integer, TLRPC.Document>();
            for (var message : res.messages) {
                if (message.media == null || message.media.document == null) {
                    continue;
                }
                documents.put(message.id, message.media.document);
            }

            SerializedData serializedData = new SerializedData();
            serializedData.writeInt32(packs.size());
            for (EmojiPackInfo pack : packs) {
                TLRPC.Document file = documents.get(pack.fileId);
                if (file != null) {
                    pack.flags |= 1;
                    pack.fileDocument = file;
                    pack.fileSize = file.size;
                    pack.fileLocation = getFileLoader().getPathToAttach(file).getAbsolutePath();
                }
                TLRPC.Document preview = documents.get(pack.previewId);
                if (preview != null) {
                    pack.flags |= 2;
                    pack.previewDocument = preview;
                }
                pack.serializeToStream(serializedData);
            }
            preferences.edit().putString("emoji_packs_v3", Base64.encodeToString(serializedData.toByteArray(), Base64.NO_WRAP | Base64.NO_PADDING)).apply();
            serializedData.cleanup();

            AndroidUtilities.runOnUIThread(() -> {
                var iterator = emojiPacksInfo.listIterator();
                while (iterator.hasNext()) {
                    if (iterator.next() instanceof EmojiPackInfo) {
                        iterator.remove();
                    }
                }
                emojiPacksInfo.addAll(packs);
            });
        }
        delegate.onTLResponse(null, null);
    }

    @Override
    protected void onLoadSuccess(ArrayList<String> responses, Delegate delegate) {
        var string = responses.size() > 0 ? responses.get(0) : null;
        if (string == null) {
            return;
        }

        try {
            ArrayList<EmojiPackInfo> packs = new ArrayList<>();
            var emojis = GSON.fromJson(string, Emojis.class);
            var previews = new HashMap<EmojiPackInfo, Integer>();
            var files = new HashMap<EmojiPackInfo, Integer>();
            emojis.emojis.forEach(pack -> {
                if ("apple".equals(pack.packId)) {
                    pack.packId = "default";
                }
                packs.add(pack);

                previews.put(pack, pack.previewId);
                files.put(pack, pack.fileId);
            });

            var req = new TLRPC.TL_channels_getMessages();
            req.channel = getMessagesController().getInputChannel(-Extra.UPDATE_CHANNEL_ID);
            req.id.addAll(previews.values());
            req.id.addAll(files.values());
            getConnectionsManager().sendRequest(req, (response1, error1) -> {
                if (error1 == null) {
                    getNewVersionMessagesCallback(delegate, packs, response1);
                } else {
                    delegate.onTLResponse(null, error1.text);
                }
            });
        } catch (JsonSyntaxException e) {
            FileLog.e(e);
            delegate.onTLResponse(null, e.getLocalizedMessage());
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.fileLoaded || id == NotificationCenter.fileLoadProgressChanged || id == NotificationCenter.fileLoadFailed) {
            var path = (String) args[0];
            if (loadingEmojiPacks.containsKey(path)) {
                var pair = loadingEmojiPacks.get(path);
                if (pair == null) {
                    return;
                }
                var pack = pair.first;
                var params = pair.second;
                if (id == NotificationCenter.fileLoadProgressChanged) {
                    Long loadedSize = (Long) args[1];
                    Long totalSize = (Long) args[2];
                    float loadProgress = loadedSize / (float) totalSize;
                    callProgressChanged(pack, false, loadProgress, loadedSize);
                } else if (id == NotificationCenter.fileLoadFailed) {
                    if (params[1]) {
                        return;
                    }
                    int reason = (Integer) args[1];
                    if (reason == 0) {
                        EmojiHelper.getInstance().load((res, error) -> AndroidUtilities.runOnUIThread(() -> {
                            EmojiPackInfo newPack = EmojiHelper.getInstance().getEmojiPackInfo(pack.getPackId());
                            if (newPack == null) {
                                return;
                            }
                            if (newPack.getFileDocument() != null && newPack.getFileDocument().id == pack.getFileDocument().id) {
                                loadingEmojiPacks.remove(pack.getFileLocation());
                                downloadPack(newPack, params[0], true);
                            }
                        }));
                    }
                } else {
                    installDownloadedEmoji(pack, params[0]);
                }
            }
        }
    }

    private void callProgressChanged(EmojiPackInfo pack, boolean finished, float progress, long loadedBytes) {
        for (var listener : listeners) {
            listener.progressChanged(pack, finished, progress, loadedBytes);
        }
    }

    public void downloadPack(EmojiPackInfo pack, boolean update, boolean tried) {
        EmojiHelper.mkDirs();
        loadingEmojiPacks.put(FileLoader.getAttachFileName(pack.fileDocument), Pair.create(pack, new Boolean[]{update, tried}));
        checkAccount();
        FileLoader.getInstance(currentAccount).loadFile(pack.getFileDocument(), pack, FileLoader.PRIORITY_NORMAL, 0);
    }

    public void checkEmojiPacks() {
        loadEmojisInfo((error) -> {
            if (getSelectedEmojiPackId().equals("default")) return;
            if (emojiPacksInfo.isEmpty()) {
                if (!isInstalledOffline(emojiPack)) {
                    emojiPack = "default";
                }
                reloadEmoji();
                return;
            }
            for (EmojiPackBase emojiPackBase : emojiPacksInfo) {
                if (emojiPackBase instanceof EmojiPackInfo) {
                    EmojiPackInfo emojiPackInfo = (EmojiPackInfo) emojiPackBase;
                    boolean update = isInstalledOldVersion(emojiPackInfo.packId, emojiPackInfo.packVersion);
                    if (emojiPack.equals(emojiPackInfo.packId)) {
                        if (!isPackInstalled(emojiPackInfo)) {
                            downloadPack(emojiPackInfo, update, false);
                        } else {
                            reloadEmoji();
                        }
                        break;
                    }
                }
            }
        });
    }

    public static void reloadEmoji() {
        Emoji.reloadEmoji();
        AndroidUtilities.cancelRunOnUIThread(invalidateUiRunnable);
        AndroidUtilities.runOnUIThread(invalidateUiRunnable);
    }

    public interface EmojiPackLoadListener {
        void progressChanged(EmojiPackInfo pack, boolean finished, float progress, long bytesLoaded);
    }

    public interface EmojiPacksLoadedListener {
        void emojiPacksLoaded(String error);
    }

    public interface OnBulletinAction {
        void onPreStart();

        void onUndo();
    }

    public static class EmojiPackBase {
        @SerializedName("name")
        @Expose
        protected String packName;
        @SerializedName("id")
        @Expose
        protected String packId;
        protected String fileLocation;
        private String preview;
        protected long fileSize;

        public EmojiPackBase() {
            this(null, null, null, null, 0);
        }

        public EmojiPackBase(String packName, String packId, String fileLocation, String preview, long fileSize) {
            this.packName = packName;
            this.packId = packId;
            this.fileLocation = fileLocation;
            this.preview = preview;
            this.fileSize = fileSize;
        }

        public void loadFromFile(File file) {
            String fileName = file.getName();
            packName = fileName;
            int versionSep = packName.lastIndexOf("_v");
            packName = packName.substring(0, versionSep);
            packId = fileName.substring(versionSep);
            File fileFont = new File(file, packName + ".ttf");
            fileLocation = fileFont.getAbsolutePath();
            preview = file.getAbsolutePath() + "/preview.png";
            fileSize = fileFont.length();
        }

        public String getPackName() {
            return packName;
        }

        public String getPackId() {
            return packId;
        }

        public String getFileLocation() {
            return fileLocation;
        }

        public String getPreview() {
            return preview;
        }

        public Long getFileSize() {
            return fileSize;
        }
    }

    public static class EmojiPackInfo extends EmojiPackBase {
        private int flags;
        @SerializedName("preview")
        @Expose
        private int previewId;
        @SerializedName("file")
        @Expose
        private int fileId;
        @SerializedName("version")
        @Expose
        private int packVersion;

        private TLRPC.Document previewDocument;
        private TLRPC.Document fileDocument;

        public TLRPC.Document getPreviewDocument() {
            return previewDocument;
        }

        public TLRPC.Document getFileDocument() {
            return fileDocument;
        }

        public int getPackVersion() {
            return packVersion;
        }

        public static EmojiPackInfo deserialize(AbstractSerializedData stream) {
            EmojiPackInfo pack = new EmojiPackInfo();
            pack.flags = stream.readInt32(false);
            pack.packId = stream.readString(false);
            pack.packName = stream.readString(false);

            pack.fileId = stream.readInt32(false);
            pack.previewId = stream.readInt32(false);
            pack.packVersion = stream.readInt32(false);
            if ((pack.flags & 1) != 0) {
                pack.fileDocument = TLRPC.Document.TLdeserialize(stream, stream.readInt32(false), false);
                pack.fileSize = stream.readInt64(false);
                pack.fileLocation = stream.readString(false);
            }
            if ((pack.flags & 2) != 0) {
                pack.previewDocument = TLRPC.Document.TLdeserialize(stream, stream.readInt32(false), false);
            }
            return pack;
        }

        public void serializeToStream(AbstractSerializedData serializedData) {
            serializedData.writeInt32(flags);
            serializedData.writeString(packId);
            serializedData.writeString(packName);

            serializedData.writeInt32(fileId);
            serializedData.writeInt32(previewId);
            serializedData.writeInt32(packVersion);
            if ((flags & 1) != 0) {
                fileDocument.serializeToStream(serializedData);
                serializedData.writeInt64(fileSize);
                serializedData.writeString(fileLocation);
            }
            if ((flags & 2) != 0) {
                previewDocument.serializeToStream(serializedData);
            }
        }
    }

    public static class Emojis {
        @SerializedName("emojis")
        @Expose
        public List<EmojiPackInfo> emojis;
    }

    @SuppressLint("ViewConstructor")
    public static class EmojiSetBulletinLayout extends Bulletin.TwoLineLayout {
        public EmojiSetBulletinLayout(@NonNull Context context, String title, String description, EmojiHelper.EmojiPackBase data, Theme.ResourcesProvider resourcesProvider) {
            super(context, resourcesProvider);
            titleTextView.setText(title);
            subtitleTextView.setText(description);
            imageView.setImage(data.getPreview(), null, null);
        }
    }
}
