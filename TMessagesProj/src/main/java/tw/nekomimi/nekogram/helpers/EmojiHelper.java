package tw.nekomimi.nekogram.helpers;

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

import androidx.annotation.NonNull;

import com.jaredrummler.truetypeparser.TTFFile;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Bulletin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import tw.nekomimi.nekogram.NekoConfig;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class EmojiHelper {
    private static final String EMOJI_PACKS_FILE_DIR;
    public static EmojiPack DEFAULT_PACK = new EmojiPack("Apple", "default", "", "", 0);
    private static final Runnable invalidateUiRunnable = () -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.emojiLoaded);
    private static final String[] previewEmojis = {
            "\uD83D\uDE00",
            "\uD83D\uDE09",
            "\uD83D\uDE14",
            "\uD83D\uDE28"
    };
    private static TextPaint textPaint;

    private final HashMap<String, Typeface> typefaceCache = new HashMap<>();
    private final ArrayList<EmojiPack> emojiPacksInfo = new ArrayList<>();
    private final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekoemojis", Context.MODE_PRIVATE);

    private String emojiPack;
    private Bitmap systemEmojiPreview;
    private String pendingDeleteEmojiPackId;
    private Bulletin emojiPackBulletin;

    static {
        var files = ApplicationLoader.applicationContext.getExternalFilesDir(null);
        if (files != null) {
            EMOJI_PACKS_FILE_DIR = files.getAbsolutePath() + "/emojis/";
        } else {
            EMOJI_PACKS_FILE_DIR = ApplicationLoader.applicationContext.getFilesDir().getAbsolutePath() + "/emojis/";
        }
    }

    private EmojiHelper() {
        emojiPack = preferences.getString("emoji_pack", "");
        loadEmojiPacks();
    }

    private static final class InstanceHolder {
        private static final EmojiHelper instance = new EmojiHelper();
    }

    public static EmojiHelper getInstance() {
        return InstanceHolder.instance;
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

    public static boolean isValidPack(File file) {
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
        if (textPaint == null) {
            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextLocale(Locale.forLanguageTag("und-Zsye"));
        }
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(fontSize);
        var fm = textPaint.getFontMetricsInt();
        var textHeight = fm.descent - fm.ascent;
        var baseline = y + (emojiSize - textHeight) / 2f - fm.ascent;
        var centerX = x + emojiSize / 2f;
        canvas.drawText(emoji, centerX, baseline, textPaint);
    }

    public Long getEmojiSize() {
        return getAllEmojis().stream()
                .filter(file -> !file.getName().startsWith(emojiPack))
                .filter(file -> !isValidPack(file))
                .map(EmojiHelper::calculateFolderSize)
                .reduce(0L, Long::sum);
    }

    public void deleteAll() {
        getAllEmojis().stream()
                .filter(file -> !file.getName().startsWith(emojiPack))
                .filter(file -> !isValidPack(file))
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

    public Typeface getCurrentTypeface() {
        if (NekoConfig.useSystemEmoji) {
            return getSystemEmojiTypeface();
        } else {
            return getSelectedTypeface();
        }
    }

    public Typeface getSystemEmojiTypeface() {
        return TypefaceHelper.getSystemEmojiTypeface();
    }

    private Typeface getSelectedTypeface() {
        EmojiPack pack = getEmojiPacksInfo()
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

    public ArrayList<EmojiPack> getEmojiPacks() {
        return emojiPacksInfo;
    }

    public ArrayList<EmojiPack> getEmojiPacksInfo() {
        return emojiPacksInfo.stream()
                .filter(e -> !e.getPackId().equals(pendingDeleteEmojiPackId))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public EmojiPack getCurrentEmojiPackInfo() {
        var selected = getSelectedEmojiPackId();
        if ("default".equals(selected)) {
            return DEFAULT_PACK;
        }
        return emojiPacksInfo.stream()
                .filter(emojiPackInfo -> emojiPackInfo != null && emojiPackInfo.packId.equals(selected))
                .findFirst()
                .orElse(null);
    }

    public EmojiPack installEmoji(File emojiFile) throws Exception {
        return installEmoji(emojiFile, true);
    }

    public EmojiPack installEmoji(File emojiFile, boolean checkInstallation) throws IOException, NoSuchAlgorithmException {
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
                .filter(EmojiHelper::isValidPack)
                .anyMatch(file -> file.getName().endsWith(sb.toString()));
        if (isAlreadyInstalled) {
            if (checkInstallation) {
                return null;
            } else {
                EmojiPack emojiPack = new EmojiPack();
                emojiPack.loadFromFile(emojiDir);
                return emojiPack;
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
        EmojiPack emojiPack = new EmojiPack();
        emojiPack.loadFromFile(emojiDir);
        emojiPacksInfo.add(emojiPack);
        return emojiPack;
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

    private void loadEmojiPacks() {
        getAllEmojis().stream()
                .filter(EmojiHelper::isValidPack)
                .sorted(Comparator.comparingLong(File::lastModified))
                .map(file -> {
                    EmojiPack emojiPack = new EmojiPack();
                    emojiPack.loadFromFile(file);
                    return emojiPack;
                })
                .forEach(emojiPacksInfo::add);
    }

    public boolean isSelectedEmojiPack() {
        return getAllEmojis().stream()
                .filter(EmojiHelper::isValidPack)
                .anyMatch(file -> file.getName().endsWith(emojiPack));
    }

    public void cancelableDelete(BaseFragment fragment, EmojiPack emojiPack, OnBulletinAction onUndoBulletinAction) {
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
                        cancelableDelete(fragment, emojiPack, onUndoBulletinAction);
                    });
                }
            }.start();
            progressDialog.setCanCancel(false);
            progressDialog.showDelayed(150);
            return;
        }
        pendingDeleteEmojiPackId = emojiPack.getPackId();
        onUndoBulletinAction.onPreStart();
        boolean wasSelected = emojiPack.getPackId().equals(this.emojiPack);
        if (wasSelected) {
            EmojiHelper.getInstance().setEmojiPack("default", false);
        }
        EmojiSetBulletinLayout bulletinLayout = new EmojiSetBulletinLayout(
                fragment.getParentActivity(),
                LocaleController.getString(R.string.EmojiSetRemoved),
                LocaleController.formatString(R.string.EmojiSetRemovedInfo, emojiPack.getPackName()),
                emojiPack,
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
                deleteEmojiPack(emojiPack);
                reloadEmoji();
                pendingDeleteEmojiPackId = null;
            }
        }.start());
        bulletinLayout.setButton(undoButton);
        emojiPackBulletin = Bulletin.make(fragment, bulletinLayout, Bulletin.DURATION_LONG).show();
    }

    public void deleteEmojiPack(EmojiPack emojiPack) {
        File emojiDir = new File(emojiPack.getFileLocation()).getParentFile();
        if (emojiDir != null && emojiDir.exists()) {
            File[] files = emojiDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            emojiDir.delete();
        }
        emojiPacksInfo.remove(emojiPack);
        if (emojiPack.getPackId().equals(this.emojiPack)) {
            EmojiHelper.getInstance().setEmojiPack("default", false);
        }
    }

    public static void reloadEmoji() {
        Emoji.reloadEmoji();
        AndroidUtilities.cancelRunOnUIThread(invalidateUiRunnable);
        AndroidUtilities.runOnUIThread(invalidateUiRunnable);
    }

    public interface OnBulletinAction {
        void onPreStart();

        void onUndo();
    }

    public static class EmojiPack {
        protected String packName;
        protected String packId;
        protected String fileLocation;
        private String preview;
        protected long fileSize;

        public EmojiPack() {
            this(null, null, null, null, 0);
        }

        public EmojiPack(String packName, String packId, String fileLocation, String preview, long fileSize) {
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

    @SuppressLint("ViewConstructor")
    public static class EmojiSetBulletinLayout extends Bulletin.TwoLineLayout {
        public EmojiSetBulletinLayout(@NonNull Context context, String title, String description, EmojiPack data, Theme.ResourcesProvider resourcesProvider) {
            super(context, resourcesProvider);
            titleTextView.setText(title);
            subtitleTextView.setText(description);
            imageView.setImage(data.getPreview(), null, null);
        }
    }
}
