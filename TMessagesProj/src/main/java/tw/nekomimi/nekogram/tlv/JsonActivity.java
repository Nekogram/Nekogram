package tw.nekomimi.nekogram.tlv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.List;

import tw.nekomimi.nekogram.helpers.EntitiesHelper;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;

public class JsonActivity extends BaseNekoSettingsActivity {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final JsonElement jsonElement;

    private final List<Node> allNodes = new ArrayList<>();
    private final List<Integer> visibleNodeIndexes = new ArrayList<>();

    public JsonActivity(JsonElement element) {
        jsonElement = element;
        visit(element, null, 0, false);
    }

    private void visit(JsonElement element, String key, int depth, boolean trailingComma) {
        if (element == null || element.isJsonNull()) {
            allNodes.add(new Node(depth, key, "null", Node.Type.VALUE_NULL, trailingComma));
            return;
        }

        if (element.isJsonPrimitive()) {
            var p = element.getAsJsonPrimitive();
            var type = p.isString() ? Node.Type.VALUE_STRING : p.isBoolean() ? Node.Type.VALUE_BOOLEAN : Node.Type.VALUE_NUMBER;
            allNodes.add(new Node(depth, key, p.getAsString(), type, trailingComma));
            return;
        }

        if (element.isJsonObject()) {
            var obj = element.getAsJsonObject();

            var start = new Node(
                    depth,
                    key,
                    null,
                    Node.Type.OBJECT_START,
                    trailingComma
            );
            start.element = element;
            start.childCount = obj.size();

            allNodes.add(start);

            var iterator = obj.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                visit(entry.getValue(), entry.getKey(), depth + 1, iterator.hasNext());
            }

            var end = new Node(
                    depth,
                    null,
                    null,
                    Node.Type.OBJECT_END,
                    trailingComma
            );
            end.startNode = start;

            allNodes.add(end);

            return;
        }

        if (element.isJsonArray()) {
            var array = element.getAsJsonArray();

            var start = new Node(
                    depth,
                    key,
                    null,
                    Node.Type.ARRAY_START,
                    trailingComma
            );
            start.element = element;
            start.childCount = array.size();

            allNodes.add(start);

            var size = array.size();
            for (var i = 0; i < size; i++) {
                visit(array.get(i), null, depth + 1, i < size - 1);
            }

            var end = new Node(
                    depth,
                    null,
                    null,
                    Node.Type.ARRAY_END,
                    trailingComma
            );
            end.startNode = start;

            allNodes.add(end);
        }
    }

    private void rebuildVisible() {
        visibleNodeIndexes.clear();
        var collapsedLevel = 0;

        for (var i = 0; i < allNodes.size(); i++) {
            var node = allNodes.get(i);

            if (collapsedLevel == 0 && (node.startNode == null || node.startNode.expanded)) {
                visibleNodeIndexes.add(i);
            }

            if (!node.expanded) {
                collapsedLevel++;
            }

            if (node.startNode != null && !node.startNode.expanded) {
                collapsedLevel = Math.max(0, collapsedLevel - 1);
            }
        }
    }

    @Override
    public View createView(Context context) {
        var fragmentView = super.createView(context);

        var menu = actionBar.createMenu();
        var copyItem = menu.addItem(1, R.drawable.msg_copy);
        copyItem.setContentDescription(LocaleController.getString(R.string.ExportAsJson));
        copyItem.setOnClickListener(v -> {
            AndroidUtilities.addToClipboard(EntitiesHelper.warpInLanguageSpan(GSON.toJson(jsonElement), "json"));
            BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
        });

        return fragmentView;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        rebuildVisible();
        for (var index : visibleNodeIndexes) {
            items.add(JsonNodeViewFactory.of(index, allNodes.get(index)));
        }
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        var node = (Node) item.object;
        if (node.type == Node.Type.ARRAY_START || node.type == Node.Type.OBJECT_START) {
            node.expanded = !node.expanded;
            listView.adapter.update(true);
        }
        if (node.startNode != null) {
            node.startNode.expanded = !node.startNode.expanded;
            listView.adapter.update(true);
        }
    }

    @Override
    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        var node = (Node) item.object;
        if (node.type == Node.Type.ARRAY_END || node.type == Node.Type.OBJECT_END) {
            return false;
        }
        ItemOptions.makeOptions(this, view)
                .setScrimViewBackground(listView.getClipBackground(view))
                .addIf(node.key != null, R.drawable.msg_copy, LocaleController.getString(R.string.JsonCopyKey), () -> {
                    AndroidUtilities.addToClipboard(node.key);
                    BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
                })
                .add(R.drawable.msg_copy, LocaleController.getString(R.string.JsonCopyValue), () -> {
                    AndroidUtilities.addToClipboard(node.element == null ? node.value :
                            EntitiesHelper.warpInLanguageSpan(GSON.toJson(node.element), "json"));
                    BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
                })
                .show();
        return true;
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.ViewAsJson);
    }

    public static class Node {
        public enum Type {
            OBJECT_START,
            OBJECT_END,
            ARRAY_START,
            ARRAY_END,
            VALUE_NULL,
            VALUE_NUMBER,
            VALUE_BOOLEAN,
            VALUE_STRING,
        }

        final int depth;
        final String key;
        final String value;
        final Type type;
        boolean trailingComma;

        JsonElement element;

        Node startNode;
        boolean expanded = true;

        int childCount = -1;

        Node(int depth, String key, String value, Type type, boolean trailingComma) {
            this.depth = depth;
            this.key = key;
            this.value = value;
            this.type = type;
            this.trailingComma = trailingComma;
        }

        public int getColorKey() {
            return switch (this.type) {
                case VALUE_BOOLEAN -> Theme.key_code_constant;
                case VALUE_STRING -> Theme.key_code_string;
                case VALUE_NUMBER -> Theme.key_code_number;
                case VALUE_NULL -> Theme.key_code_keyword;
                default -> -1;
            };
        }
    }

    private static class JsonNodeViewFactory extends UItem.UItemFactory<JsonNodeView> {
        static {
            setup(new JsonNodeViewFactory());
        }

        @Override
        public JsonNodeView createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            var view = new JsonNodeView(context, currentAccount, resourcesProvider);
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return view;
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            var cell = (JsonNodeView) view;
            cell.setNode((Node) item.object);
        }

        public static UItem of(int id, Node node) {
            var item = UItem.ofFactory(JsonNodeViewFactory.class);
            item.id = id;
            item.object = node;
            return item;
        }
    }

    private static class JsonNodeView extends LinearLayout {

        private static final String DOCUMENTATION_URL_PREFIX = "https://core.telegram.org/constructor/";
        private static final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        static {
            guidePaint.setStyle(Paint.Style.STROKE);
            guidePaint.setStrokeWidth(AndroidUtilities.dp(1));
            guidePaint.setPathEffect(
                    new DashPathEffect(
                            new float[]{
                                    AndroidUtilities.dp(2),
                                    AndroidUtilities.dp(2)
                            },
                            0
                    )
            );
        }

        private Node node;
        private boolean expanded;

        private final TextView keyTextView;
        private final LinkSpanDrawable.LinksTextView valueTextView;
        private final Theme.ResourcesProvider resourcesProvider;

        public JsonNodeView(@NonNull Context context, int currentAccount, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            setOrientation(HORIZONTAL);
            setClipToPadding(false);
            setClipChildren(false);

            this.resourcesProvider = resourcesProvider;

            guidePaint.setColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));

            keyTextView = new TextView(context);
            keyTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            keyTextView.setTypeface(Typeface.MONOSPACE);
            keyTextView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            keyTextView.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
            addView(keyTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT));

            valueTextView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider) {
                @Override
                public int overrideColor() {
                    return ColorUtils.setAlphaComponent(getThemedColor(Theme.key_code_string), 51);
                }

                @Override
                protected int processColor(int color) {
                    return ColorUtils.setAlphaComponent(getThemedColor(Theme.key_code_string), 51);
                }
            };
            valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            valueTextView.setTypeface(Typeface.MONOSPACE);
            valueTextView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            valueTextView.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
            valueTextView.setOnLinkPressListener(span -> {
                valueTextView.setLoading(span);

                var url = DOCUMENTATION_URL_PREFIX + node.value;
                var req = new TLRPC.TL_messages_getWebPage();
                req.url = url;
                req.hash = 0;
                ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, error) -> AndroidUtilities.runOnUIThread(() -> {
                    valueTextView.setLoading(null);

                    if (res instanceof TLRPC.TL_messages_webPage webPage) {
                        MessagesController.getInstance(currentAccount).putUsers(webPage.users, false);
                        MessagesController.getInstance(currentAccount).putChats(webPage.chats, false);
                        if (webPage.webpage instanceof TLRPC.TL_webPage page && page.cached_page != null) {
                            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.openArticle, page, url);
                            return;
                        }
                    }

                    Browser.openUrl(context, url);
                }));
            });
            addView(valueTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT));

            NotificationCenter.listenEmojiLoading(valueTextView);
        }

        public void setNode(Node node) {
            if (node != this.node) {
                this.node = node;

                var indent = AndroidUtilities.dp(node.depth * 16);
                setPadding(indent + AndroidUtilities.dp(8), AndroidUtilities.dp(2), AndroidUtilities.dp(8), AndroidUtilities.dp(2));

                var keyText = node.key == null ? null : "\"" + node.key + "\": ";
                if (keyText == null) {
                    keyTextView.setVisibility(GONE);
                } else {
                    keyTextView.setVisibility(VISIBLE);
                    keyTextView.setText(keyText);
                }
            } else if (expanded == node.expanded) {
                return;
            }
            this.expanded = node.expanded;

            var valueText = new SpannableStringBuilder();
            switch (node.type) {
                case OBJECT_START:
                    valueText.append('{');
                    if (!node.expanded) {
                        valueText.append("...}");
                    }
                    break;
                case OBJECT_END:
                    valueText.append('}');
                    break;
                case ARRAY_START:
                    valueText.append('[');
                    if (!node.expanded) {
                        valueText.append("...]");
                    }
                    break;
                case ARRAY_END:
                    valueText.append(']');
                    break;
                default:
                    if (node.type == Node.Type.VALUE_STRING) {
                        valueText.append('"');
                        valueText.append(Emoji.replaceEmoji(node.value, valueTextView.getPaint().getFontMetricsInt(), false));
                        valueText.append('"');
                    } else {
                        valueText.append(node.value);
                    }
            }
            if ("_".equals(node.key)) {
                valueText.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                    }
                }, 1, valueText.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                valueTextView.setClickable(true);
            } else {
                valueTextView.setClickable(false);
            }
            var colorKey = node.getColorKey();
            if (colorKey > 0) {
                valueText.setSpan(new ForegroundColorSpan(getThemedColor(colorKey)), 0, valueText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            var showTrailingComma = node.trailingComma &&
                    (node.type != Node.Type.OBJECT_START && node.type != Node.Type.ARRAY_START || !node.expanded);
            if (showTrailingComma) {
                valueText.append(",");
            }
            if (!node.expanded) {
                var comment = " // " + LocaleController.formatPluralStringComma("items", node.childCount);
                valueText.append(comment);
                valueText.setSpan(new ForegroundColorSpan(getThemedColor(Theme.key_code_comment)), valueText.length() - comment.length(), valueText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            valueTextView.setText(valueText);
        }

        @Override
        protected void dispatchDraw(@NonNull Canvas canvas) {
            if (node != null && node.depth > 0) {
                var bottom = getMeasuredHeight();
                for (var level = 0; level < node.depth; level++) {
                    var x = AndroidUtilities.dp(16 * level + 11);
                    canvas.drawLine(x, 0, x, bottom, guidePaint);
                }
            }
            super.dispatchDraw(canvas);
        }

        private int getThemedColor(int key) {
            return Theme.getColor(key, resourcesProvider);
        }

    }
}
