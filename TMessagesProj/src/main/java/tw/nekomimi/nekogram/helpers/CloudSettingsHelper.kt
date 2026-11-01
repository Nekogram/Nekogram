package tw.nekomimi.nekogram.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.view.Gravity
import android.view.View
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.AndroidUtilities.dpf2
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.ui.ActionBar.ActionBarMenuSubItem
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.Bulletin
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.Components.ItemOptions
import tw.nekomimi.nekogram.NekoConfig
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.time.Duration.Companion.milliseconds

class CloudSettingsHelper {

    private val preferences: SharedPreferences =
        ApplicationLoader.applicationContext.getSharedPreferences("nekocloud", Context.MODE_PRIVATE)

    private val cloudSyncedDate = LongArray(UserConfig.MAX_ACCOUNT_COUNT)
    private var localSyncedDate = preferences.getLong("updated_at", -1L)
    private var autoSync = preferences.getBoolean("auto_sync", false)

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var syncJob: Job? = null

    fun showPopup(parentFragment: BaseFragment, scrimView: View) {
        val resourcesProvider = parentFragment.resourceProvider
        val context = parentFragment.parentActivity ?: return
        val currentAccount = parentFragment.currentAccount
        val bulletinFactory =
            BulletinFactory.of(Bulletin.BulletinWindow.make(context), resourcesProvider)

        ItemOptions.makeOptions(parentFragment, scrimView).apply {
            setDimAlpha(0x08)

            add(R.drawable.cloud_upload, LocaleController.getString(R.string.CloudConfigSync), null)
            val syncOption = last

            add(
                R.drawable.cloud_download,
                LocaleController.getString(R.string.CloudConfigRestore),
                null
            )
            val restoreOption = last

            fun updateText(animated: Boolean) {
                syncOption.setAnimatedSubtext(formatLastSyncDate(), animated)
                restoreOption.setAnimatedSubtext(formatCloudDate(currentAccount), animated)
            }
            updateText(animated = false)

            syncOption.setOnClickListener {
                syncOption.setAnimatedSubtext(
                    LocaleController.formatString(R.string.CloudConfigSyncing),
                    true
                )
                syncToCloud(currentAccount) { success, error ->
                    updateText(animated = true)
                    if (!success) {
                        showBulletin(bulletinFactory, R.string.CloudConfigSyncFailed, error)
                    }
                }
            }

            restoreOption.setOnClickListener {
                restoreOption.setAnimatedSubtext(
                    LocaleController.formatString(R.string.CloudConfigSyncing),
                    true
                )
                restoreFromCloud(currentAccount) { success, error ->
                    updateText(animated = true)
                    if (!success) {
                        showBulletin(bulletinFactory, R.string.CloudConfigRestoreFailed, error)
                    }
                }
            }

            getCloudStorageHelper(currentAccount).getItem("neko_settings_updated_at") { res, error ->
                cloudSyncedDate[currentAccount] = if (error == null && !res.isNullOrBlank()) {
                    res.toLongOrNull() ?: -1L
                } else {
                    -1L
                }
                restoreOption.setAnimatedSubtext(formatCloudDate(currentAccount), true)
            }

            addGap()

            val autoSyncOption = AutoSyncItem(context, resourcesProvider).apply {
                setTextAndIcon(
                    LocaleController.getString(R.string.CloudConfigAutoSync),
                    R.drawable.sync
                )
                setChecked(autoSync)
                setOnClickListener {
                    autoSync = !autoSync
                    preferences.edit { putBoolean("auto_sync", autoSync) }
                    setChecked(autoSync)
                }
            }
            addView(autoSyncOption)

            addGap()
            addText(LocaleController.getString(R.string.CloudConfigDesc), 13)

            setBlur(false, true)
            setGravity(if (LocaleController.isRTL) Gravity.LEFT else Gravity.RIGHT)
            show()
            setTranslationY(-dpf2(64f))
        }
    }

    fun doAutoSync() {
        if (!autoSync) return

        syncJob?.cancel()
        syncJob = scope.launch {
            delay(1200.milliseconds)
            syncToCloud(UserConfig.selectedAccount) { success, error ->
                if (!success) {
                    showBulletin(BulletinFactory.global(), R.string.CloudConfigSyncFailed, error)
                }
            }
        }
    }

    private fun syncToCloud(
        accountIndex: Int,
        callback: (success: Boolean, error: String?) -> Unit
    ) {
        val rawConfig = NekoConfig.exportConfigs()
        val compressed = encodeConfig(rawConfig)
        val payload = if (compressed.length < rawConfig.length) compressed else rawConfig
        val storageHelper = getCloudStorageHelper(accountIndex)

        storageHelper.setItem("neko_settings", payload) { _, error ->
            if (error == null) {
                val now = System.currentTimeMillis()
                cloudSyncedDate[accountIndex] = now
                localSyncedDate = now
                storageHelper.setItem("neko_settings_updated_at", now.toString()) { _, _ -> }
                preferences.edit { putLong("updated_at", now) }
                callback(true, null)
            } else {
                callback(false, error)
            }
        }
    }

    private fun restoreFromCloud(
        accountIndex: Int,
        callback: (success: Boolean, error: String?) -> Unit
    ) {
        getCloudStorageHelper(accountIndex).getItem("neko_settings") { res, error ->
            if (error != null) {
                callback(false, error)
                return@getItem
            }

            if (res.isNullOrBlank()) {
                callback(false, "EMPTY_CONFIG")
                return@getItem
            }

            val config = decodeConfig(res)
            if (config == null) {
                callback(false, "DECODE_FAILED")
                return@getItem
            }

            try {
                NekoConfig.importConfigs(config)
                val now = System.currentTimeMillis()
                localSyncedDate = now
                preferences.edit { putLong("updated_at", now) }
                callback(true, null)
            } catch (e: Exception) {
                FileLog.e(e)
                callback(false, e.localizedMessage)
            }
        }
    }

    fun encodeConfig(string: String): String {
        return try {
            val bos = ByteArrayOutputStream(string.length)
            GZIPOutputStream(bos).use { it.write(string.toByteArray(StandardCharsets.UTF_8)) }
            CONFIG_VERSION.toString() + Base64.encodeToString(
                bos.toByteArray(),
                Base64.NO_PADDING or Base64.NO_WRAP
            )
        } catch (e: Exception) {
            FileLog.e(e)
            string
        }
    }

    private fun decodeConfig(string: String): String? {
        return when {
            string.startsWith("{") -> string
            string.startsWith(CONFIG_VERSION.toString()) -> {
                try {
                    val decoded = Base64.decode(string.substring(1), Base64.DEFAULT)
                    GZIPInputStream(ByteArrayInputStream(decoded)).use { gis ->
                        String(gis.readBytes(), StandardCharsets.UTF_8)
                    }
                } catch (e: Exception) {
                    FileLog.e(e)
                    null
                }
            }

            else -> null
        }
    }

    private fun getCloudStorageHelper(accountIndex: Int): CloudStorageHelper {
        return CloudStorageHelper.getInstance(accountIndex)
    }

    private fun formatLastSyncDate(): String {
        val dateString = if (localSyncedDate > 0) {
            formatRelativeDate(localSyncedDate)
        } else {
            LocaleController.getString(R.string.CloudConfigSyncDateNever)
        }
        return LocaleController.formatString(R.string.CloudConfigLastSyncDate, dateString)
    }

    private fun formatCloudDate(accountIndex: Int = UserConfig.selectedAccount): String {
        val date = cloudSyncedDate.getOrElse(accountIndex) { -1L }
        val dateString = if (date > 0) {
            formatRelativeDate(date)
        } else {
            LocaleController.getString(R.string.CloudConfigSyncDateNever)
        }
        return LocaleController.formatString(R.string.CloudConfigCloudDate, dateString)
    }

    private fun formatRelativeDate(date: Long): String? {
        return LocaleController.formatRelativeDate((System.currentTimeMillis() - date) / 1000)
    }

    private fun showBulletin(factory: BulletinFactory, titleRes: Int, error: String?) {
        val title = LocaleController.getString(titleRes)
        val bulletin = if (error == null) {
            factory.createSimpleBulletin(R.raw.chats_infotip, title)
        } else {
            factory.createSimpleBulletin(R.raw.chats_infotip, title, error)
        }
        bulletin.show()
    }

    @SuppressLint("ViewConstructor")
    class AutoSyncItem(context: Context?, resourcesProvider: Theme.ResourcesProvider?) :
        ActionBarMenuSubItem(context, false, false, resourcesProvider) {
        init {
            setPadding(dp(18f), 0, dp(18f), 0)
            setColors(
                Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, resourcesProvider),
                Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon, resourcesProvider)
            )
            setSelectorColor(
                Theme.multAlpha(
                    Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, resourcesProvider),
                    0.12f
                )
            )
            makeCheckView(3)
        }
    }

    companion object {
        private const val CONFIG_VERSION = 0

        @JvmStatic
        val instance: CloudSettingsHelper by lazy { CloudSettingsHelper() }
    }
}