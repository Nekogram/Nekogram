package tw.nekomimi.nekogram

import android.os.SystemClock
import android.util.TypedValue
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.messenger.browser.Browser
import org.telegram.tgnet.ConnectionsManager
import org.telegram.ui.ActionBar.ActionBarMenuSubItem
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.ItemOptions
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.LinkSpanDrawable.LinksTextView
import org.telegram.utils.proxy.ProxySettings
import tw.nekomimi.nekogram.helpers.UserHelper
import java.lang.ref.WeakReference

object DatacenterPopupWrapper {
    private val datacenterInfos: List<DatacenterInfo> = List(5) { DatacenterInfo(it + 1) }

    @JvmStatic
    fun fillDcItems(options: ItemOptions, closeSwipeBack: Runnable?) {
        options.apply {
            closeSwipeBack?.let {
                add(
                    R.drawable.msg_arrow_back,
                    LocaleController.getString(R.string.Back),
                    closeSwipeBack
                )
                addGap()
            }

            datacenterInfos.forEach { info ->
                add(info.formatDCString(), null)
                val item = getLast()
                info.attachToItem(item)
                item.setOnClickListener { info.check(true) }
                info.check(false)
            }

            addGap()

            val textView = LinksTextView(context).apply {
                setTag(R.id.fit_width_tag, 1)
                setPadding(dp(13f), 0, dp(13f), dp(8f))
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
                setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem))
                setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText))
                text = AndroidUtilities.replaceSingleTag(
                    LocaleController.getString(R.string.DatacenterStatusAbout)
                ) {
                    Browser.openUrl(context, "https://core.telegram.org/api/datacenter")
                }
            }
            addView(
                textView,
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0,
                    0, 8, 0, 0
                )
            )
        }
    }

    private data class DatacenterInfo(private val id: Int) {
        private var ping: Long = 0
        private var checking: Boolean = false
        private var available: Boolean = false
        private var availableCheckTime: Long = 0

        private var itemRef: WeakReference<ActionBarMenuSubItem>? = null

        fun attachToItem(item: ActionBarMenuSubItem) {
            itemRef = WeakReference(item)
            updateItem(item, false)
        }

        fun formatDCString(): String {
            return UserHelper.formatDCString(id)
        }

        fun check(force: Boolean) {
            if (checking) {
                return
            }
            if (!force && SystemClock.elapsedRealtime() - availableCheckTime < 2 * 60 * 1000L) {
                return
            }
            checking = true
            notifyUpdate()
            val proxySettings = ProxySettings.builder().setAddress("ping.neko").setPort(id).setType(
                ProxySettings.Type.SOCKS5
            ).build()
            ConnectionsManager.getInstance(UserConfig.selectedAccount)
                .checkProxy(proxySettings) { time: Long ->
                    AndroidUtilities.runOnUIThread {
                        availableCheckTime = SystemClock.elapsedRealtime()
                        checking = false
                        available = time != -1L
                        ping = time
                        notifyUpdate()
                    }
                }
        }

        private fun notifyUpdate() {
            itemRef?.get()?.let {
                updateItem(it, true)
            }
        }

        private fun updateItem(item: ActionBarMenuSubItem, animated: Boolean) {
            val colorKey: Int
            when {
                checking -> {
                    item.setAnimatedSubtext(LocaleController.getString(R.string.Checking), animated)
                    colorKey = Theme.key_windowBackgroundWhiteGrayText2
                }

                available -> {
                    when {
                        ping >= 1000 -> {
                            item.setAnimatedSubtext(
                                LocaleController.formatString(R.string.Ping, ping),
                                animated
                            )
                            colorKey = Theme.key_text_RedRegular
                        }

                        ping > 0L -> {
                            item.setAnimatedSubtext(
                                LocaleController.formatString(R.string.Ping, ping),
                                animated
                            )
                            colorKey = Theme.key_windowBackgroundWhiteGreenText
                        }

                        else -> {
                            item.setAnimatedSubtext(
                                LocaleController.getString(R.string.Available),
                                animated
                            )
                            colorKey = Theme.key_windowBackgroundWhiteGreenText
                        }
                    }
                }

                else -> {
                    item.setAnimatedSubtext(
                        LocaleController.getString(R.string.Unavailable),
                        animated
                    )
                    colorKey = Theme.key_text_RedRegular
                }
            }
            item.setAnimatedSubtextColorKey(colorKey)
        }
    }
}
