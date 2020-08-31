package tw.nekomimi.nekogram.simplemenu;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

import static tw.nekomimi.nekogram.simplemenu.SimpleMenuPopupWindow.DIALOG;
import static tw.nekomimi.nekogram.simplemenu.SimpleMenuPopupWindow.HORIZONTAL;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class SimpleMenuListAdapter extends RecyclerView.Adapter<SimpleMenuListAdapter.ViewHolder> {

    private SimpleMenuPopupWindow mWindow;

    public SimpleMenuListAdapter(SimpleMenuPopupWindow window) {
        super();

        mWindow = window;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_menu_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.bind(mWindow, position);
    }

    @Override
    public int getItemCount() {
        return mWindow.getEntries() == null ? 0 : mWindow.getEntries().length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ForegroundCheckTextView mCheckedTextView;

        private SimpleMenuPopupWindow mWindow;

        public ViewHolder(View itemView) {
            super(itemView);

            mCheckedTextView = itemView.findViewById(android.R.id.text1);
            itemView.setOnClickListener(this);
        }

        public void bind(SimpleMenuPopupWindow window, int position) {
            mWindow = window;
            mCheckedTextView.setText(mWindow.getEntries()[position]);
            mCheckedTextView.setChecked(position == mWindow.getSelectedIndex());
            mCheckedTextView.setMaxLines(mWindow.getMode() == DIALOG ? Integer.MAX_VALUE : 1);
            mCheckedTextView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
            mCheckedTextView.setForeground(Theme.getSelectorDrawable(false));

            int padding = mWindow.listPadding[mWindow.getMode()][HORIZONTAL];
            int paddingVertical = mCheckedTextView.getPaddingTop();
            mCheckedTextView.setPadding(padding, paddingVertical, padding, paddingVertical);
        }

        @Override
        public void onClick(View view) {
            if (mWindow.getOnItemClickListener() != null) {
                mWindow.getOnItemClickListener().onClick(getAdapterPosition());
            }

            if (mWindow.isShowing()) {
                mWindow.dismiss();
            }
        }
    }
}
