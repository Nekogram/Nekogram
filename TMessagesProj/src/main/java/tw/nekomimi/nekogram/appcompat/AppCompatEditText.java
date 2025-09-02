/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tw.nekomimi.nekogram.appcompat;

import static tw.nekomimi.nekogram.appcompat.AppCompatReceiveContentHelper.maybeHandleDragEventViaPerformReceiveContent;
import static tw.nekomimi.nekogram.appcompat.AppCompatReceiveContentHelper.maybeHandleMenuActionViaPerformReceiveContent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.view.DragEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.OnReceiveContentViewBehavior;
import androidx.core.view.ViewCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.widget.TextViewOnReceiveContentListener;

import org.telegram.messenger.XiaomiUtilities;

@SuppressLint("RestrictedApi")
public class AppCompatEditText extends EditText implements OnReceiveContentViewBehavior {

    private final TextViewOnReceiveContentListener mDefaultOnReceiveContentListener;

    public AppCompatEditText(@NonNull Context context) {
        super(context);
        mDefaultOnReceiveContentListener = new TextViewOnReceiveContentListener();
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.VANILLA_ICE_CREAM && XiaomiUtilities.isOS2()) {
            setLocalePreferredLineHeightForMinimumUsed(false);
        }
    }

    /**
     * Return the text that the view is displaying. If an editable text has not been set yet, this
     * will return null.
     */
    @Override
    @Nullable
    public Editable getText() {
        if (Build.VERSION.SDK_INT >= 28) {
            return super.getText();
        }
        // A bug pre-P makes getText() crash if called before the first setText due to a cast, so
        // retrieve the editable text.
        return super.getEditableText();
    }

    /**
     * If a {@link ViewCompat#setOnReceiveContentListener listener is set}, the returned
     * {@link InputConnection} will use it to handle calls to {@link InputConnection#commitContent}.
     * <p>
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection ic = super.onCreateInputConnection(outAttrs);

        // On SDK 30 and below, we manually configure the InputConnection here to use
        // ViewCompat.performReceiveContent. On S and above, the platform's BaseInputConnection
        // implementation calls View.performReceiveContent by default.
        if (ic != null && Build.VERSION.SDK_INT <= 30) {
            String[] mimeTypes = ViewCompat.getOnReceiveContentMimeTypes(this);
            if (mimeTypes != null) {
                EditorInfoCompat.setContentMimeTypes(outAttrs, mimeTypes);
                ic = InputConnectionCompat.createWrapper(this, ic, outAttrs);
            }
        }
        return ic;
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        if (maybeHandleDragEventViaPerformReceiveContent(this, event)) {
            return true;
        }
        return super.onDragEvent(event);
    }

    /**
     * If a {@link ViewCompat#setOnReceiveContentListener listener is set}, uses it to execute the
     * "Paste" and "Paste as plain text" menu actions.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean onTextContextMenuItem(int id) {
        if (maybeHandleMenuActionViaPerformReceiveContent(this, id)) {
            return true;
        }
        return super.onTextContextMenuItem(id);
    }

    /**
     * Implements the default behavior for receiving content, which coerces all content to text
     * and inserts into the view.
     *
     * <p>IMPORTANT: This method is provided to enable custom widgets that extend this class
     * to customize the default behavior for receiving content. Apps wishing to provide custom
     * behavior for receiving content should not override this method, but rather should set
     * a listener via {@link ViewCompat#setOnReceiveContentListener}. App code wishing to inject
     * content into this view should not call this method directly, but rather should invoke
     * {@link ViewCompat#performReceiveContent}.
     *
     * @param payload The content to insert and related metadata.
     * @return The portion of the passed-in content that was not handled (may be all, some, or none
     * of the passed-in content).
     */
    @Nullable
    @Override
    public ContentInfoCompat onReceiveContent(@NonNull ContentInfoCompat payload) {
        return mDefaultOnReceiveContentListener.onReceiveContent(this, payload);
    }
}