package tw.nekomimi.nekogram.folder;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ExtendedGridLayoutManager;
import org.telegram.ui.Components.RecyclerListView;

public class IconSelectorAlert {

    public static void show(BaseFragment fragment, OnIconSelectedListener onIconSelectedListener) {
        Context context = fragment.getParentActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        GridAdapter gridAdapter = new GridAdapter();
        RecyclerListView recyclerListView = new RecyclerListView(context);
        recyclerListView.setClipToPadding(false);
        recyclerListView.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
        recyclerListView.setLayoutManager(new ExtendedGridLayoutManager(recyclerListView.getContext(), 6));
        recyclerListView.setAdapter(gridAdapter);
        recyclerListView.setSelectorDrawableColor(0);
        recyclerListView.setOnItemClickListener((view, position) -> {
            onIconSelectedListener.onIconSelected((String) view.getTag());
            builder.getDismissRunnable().run();
        });

        builder.setView(recyclerListView);
        fragment.showDialog(builder.create());
    }

    private static class GridAdapter extends RecyclerListView.SelectionAdapter {
        private final String[] icons = FolderIconHelper.folderIcons.keySet().toArray(new String[0]);

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            var view = new ImageView(parent.getContext()) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    int iconSize = MeasureSpec.makeMeasureSpec(parent.getMeasuredWidth() / 6, MeasureSpec.EXACTLY);
                    super.onMeasure(iconSize, iconSize);
                }
            };
            view.setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_listSelector), AndroidUtilities.dp(2), AndroidUtilities.dp(2)));
            view.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.MULTIPLY));
            view.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            var imageView = (ImageView) holder.itemView;
            imageView.setTag(icons[position]);
            imageView.setImageResource(FolderIconHelper.getTabIcon(icons[position]));
        }

        @Override
        public int getItemCount() {
            return icons.length;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }
    }

    public interface OnIconSelectedListener {
        void onIconSelected(String emoticon);
    }
}
