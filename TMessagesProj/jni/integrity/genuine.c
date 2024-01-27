#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <fcntl.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>
#include <dlfcn.h>
#include <sys/system_properties.h>
#include <inttypes.h>
#include <android/log.h>
#include <errno.h>
#include <pthread.h>

#include "apk-sign-v2.h"
#include "pm.h"
#include "openat.h"

#ifdef CHECK_MOUNT
#include "mount.h"
#endif

static int genuine = CHECK_TRUE;

static int sdk;

static int uid;

static inline bool isSame(const char *path1, const char *path2) {
    if (path1[0] == '/') {
        return strcmp(path1, path2) == 0;
    } else {
        return strcmp(path1, strrchr(path2, '/') + 1) == 0;
    }
}

static inline void fill_d_s(char v[]) {
    // %d: %s
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = '$';
    v[0x1] = 'f';
    v[0x2] = '9';
    v[0x3] = '$';
    v[0x4] = '%';
    v[0x5] = 'r';
    for (unsigned int i = 0; i < 0x6; ++i) {
        v[i] ^= ((i + 0x6) % m);
    }
    v[0x6] = '\0';
}

static inline void fill_cannot_find_s(char v[]) {
    // cannot find %s
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'b';
    v[0x1] = 'c';
    v[0x2] = 'm';
    v[0x3] = 'j';
    v[0x4] = 'j';
    v[0x5] = 'r';
    v[0x6] = '\'';
    v[0x7] = 'n';
    v[0x8] = '`';
    v[0x9] = 'd';
    v[0xa] = 'o';
    v[0xb] = ',';
    v[0xc] = '%';
    v[0xd] = 'r';
    for (unsigned int i = 0; i < 0xe; ++i) {
        v[i] ^= ((i + 0xe) % m);
    }
    v[0xe] = '\0';
}

static inline void fill_sdk_d_genuine_d(char v[]) {
    // sdk: %d, genuine: %d
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 'r';
    v[0x1] = 'f';
    v[0x2] = 'h';
    v[0x3] = '>';
    v[0x4] = '%';
    v[0x5] = '#';
    v[0x6] = 'c';
    v[0x7] = '$';
    v[0x8] = ')';
    v[0x9] = 'm';
    v[0xa] = 'n';
    v[0xb] = 'b';
    v[0xc] = 'x';
    v[0xd] = 'g';
    v[0xe] = 'a';
    v[0xf] = 'u';
    v[0x10] = '+';
    v[0x11] = '2';
    v[0x12] = '%';
    v[0x13] = 'e';
    for (unsigned int i = 0; i < 0x14; ++i) {
        v[i] ^= ((i + 0x14) % m);
    }
    v[0x14] = '\0';
}

static inline void fill_add_sigcont(char v[]) {
    // add sigcont handler
    static unsigned int m = 0;

    if (m == 0) {
        m = 17;
    } else if (m == 19) {
        m = 23;
    }

    v[0x0] = 'c';
    v[0x1] = 'g';
    v[0x2] = '`';
    v[0x3] = '%';
    v[0x4] = 'u';
    v[0x5] = 'n';
    v[0x6] = 'o';
    v[0x7] = 'j';
    v[0x8] = 'e';
    v[0x9] = 'e';
    v[0xa] = 'x';
    v[0xb] = '-';
    v[0xc] = 'f';
    v[0xd] = 'n';
    v[0xe] = '~';
    v[0xf] = 'd';
    v[0x10] = 'm';
    v[0x11] = 'g';
    v[0x12] = 'q';
    for (unsigned int i = 0; i < 0x13; ++i) {
        v[i] ^= ((i + 0x13) % m);
    }
    v[0x13] = '\0';
}

static inline void fill_received_sigcont(char v[]) {
    // received sigcont
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'q';
    v[0x1] = 'a';
    v[0x2] = 'f';
    v[0x3] = 'c';
    v[0x4] = 'n';
    v[0x5] = '~';
    v[0x6] = 'l';
    v[0x7] = 'n';
    v[0x8] = '+';
    v[0x9] = '\x7f';
    v[0xa] = 'i';
    v[0xb] = 'f';
    v[0xc] = 'a';
    v[0xd] = 'l';
    v[0xe] = 'j';
    v[0xf] = 'q';
    for (unsigned int i = 0; i < 0x10; ++i) {
        v[i] ^= ((i + 0x10) % m);
    }
    v[0x10] = '\0';
}

static void handler(int sig __unused) {
    char v[0x11];
    fill_received_sigcont(v);
    LOGI(v);
}

static inline void fill_invalid_signature_path_s(char v[]) {
    // invalid signature, path: %s
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = 'm';
    v[0x1] = 'k';
    v[0x2] = 'p';
    v[0x3] = 'f';
    v[0x4] = 'd';
    v[0x5] = '`';
    v[0x6] = 'n';
    v[0x7] = '+';
    v[0x8] = '\x7f';
    v[0x9] = 'd';
    v[0xa] = 'i';
    v[0xb] = 'a';
    v[0xc] = 'q';
    v[0xd] = 'e';
    v[0xe] = 'g';
    v[0xf] = 'a';
    v[0x10] = 'q';
    v[0x11] = '9';
    v[0x12] = '6';
    v[0x13] = 'p';
    v[0x14] = '`';
    v[0x15] = 'v';
    v[0x16] = 'k';
    v[0x17] = '>';
    v[0x18] = '%';
    v[0x19] = '#';
    v[0x1a] = 't';
    for (unsigned int i = 0; i < 0x1b; ++i) {
        v[i] ^= ((i + 0x1b) % m);
    }
    v[0x1b] = '\0';
}

bool checkGenuine(JNIEnv *env) {
    char v1[0x20];

    signal(SIGCONT, handler);
    fill_add_sigcont(v1);
    LOGI(v1); // 0x14

    sdk = getSdk();

    if (sdk < 21) {
        genuine = CHECK_FATAL;
        goto done;
    }

    uid = getuid();

#ifdef DEBUG
    LOGI("JNI_OnLoad start, sdk: %d, uid: %d", sdk, uid);
#endif

    char *packageName = getGenuinePackageName();
    if (uid < 10000) {
        goto clean;
    }

    char *packagePath = NULL;
    fill_d_s(v1);
    for (int i = 0; i < 0x3; ++i) {
        packagePath = getPath(env, uid, packageName);
        LOGI(v1, i, packagePath);
        if (packagePath != NULL) {
            break;
        }
    }
    if (packagePath == NULL) {
        fill_cannot_find_s(v1);
        LOGE(v1, packageName);
        genuine = CHECK_FAKE;
        goto clean;
    }
    int sign = checkSignature(packagePath);
    if (sign) {
        fill_invalid_signature_path_s(v1);
        LOGE(v1, packagePath);
#ifndef DEBUG_FAKE
        genuine = sign < 0 ? CHECK_NOAPK : CHECK_FAKE;
        goto cleanPackagePath;
#endif
    }

    cleanPackagePath:
    free(packagePath);

    clean:
#ifdef GENUINE_NAME
    free(packageName);
#endif

    done:
    fill_sdk_d_genuine_d(v1); // 0x15
    LOGI(v1, sdk, genuine);

    return setGenuine(env, genuine);
}
