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

    private final static int LIB_VERSION = 42;
    private final static String LIB_NAME = "neko." + LIB_VERSION;

    public static synchronized void initNativeLibs(Context context) {
        ReLinker.loadLibrary(context, LIB_NAME);
    }

    private static native void init(String path, boolean enable);
    //public static native void crash();
}
