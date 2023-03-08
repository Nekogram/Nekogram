/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.content.Context;

import com.getkeepsafe.relinker.ReLinker;

public class NativeLoader {

    private final static int LIB_VERSION = 43;
    private final static String LIB_NAME = "neko." + LIB_VERSION;

    private static volatile boolean nativeLoaded = false;

    public static synchronized void initNativeLibs(Context context) {
        try {
            ReLinker.loadLibrary(context, LIB_NAME);
            nativeLoaded = true;
        } catch (Error e) {
            FileLog.e(e);
        }
    }

    private static native void init(String path, boolean enable);

    public static boolean loaded() {
        return nativeLoaded;
    }
    //public static native void crash();
}
