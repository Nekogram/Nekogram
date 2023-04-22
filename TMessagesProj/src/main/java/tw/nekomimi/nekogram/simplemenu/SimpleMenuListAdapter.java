package tw.nekomimi.nekogram.simplemenu;

import static tw.nekomimi.nekogram.simplemenu.SimpleMenuPopupWindow.DIALOG;
import static tw.nekomimi.nekogram.simplemenu.SimpleMenuPopupWindow.HORIZONTAL;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.RecyclerListView;

class SimpleMenuListAdapter extends RecyclerListView.SelectionAdapter {

    private final SimpleMenuPopupWindow mWindow;
    private final Theme.ResourcesProvider mResourcesProvider;

    public SimpleMenuListAdapter(SimpleMenuPopupWindow window, Theme.ResourcesProvider resourcesProvider) {
        super();

        mWindow = window;
        mResourcesProvider = resourcesProvider;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = new SimpleMenuItem(parent.getContext(), mResourcesProvider);
        view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SimpleMenuItem view = (SimpleMenuItem) holder.itemView;
        view.setTextAndCheck(mWindow.getEntries()[position], position == mWindow.getSelectedIndex(), mWindow.getMode() == DIALOG, mWindow.listPadding[mWindow.getMode()][HORIZONTAL]);
    }

    @Override
    public int getItemCount() {
        return mWindow.getEntries() == null ? 0 : mWindow.getEntries().length;
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        return true;
    }
}
