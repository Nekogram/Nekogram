#include <string_view>
#include <dirent.h>
#include <unistd.h>
#include <zlib.h>

#include "colorado.h"
#include "logging.h"
#include "utils.h"

char *get_package_name() {
    static unsigned int m = 0;
    if (m == 0) {
        m = 20;
    } else if (m == 23) {
        m = 29;
    }
    char name[] = PACKAGE_NAME;
    unsigned int length = sizeof(name) - 1;
    for (unsigned int i = 0; i < length; ++i) {
        name[i] ^= ((i + length) % m);
    }
    name[length] = '\0';
    return strdup(name);
}

bool check_signature() {
    std::hash<std::string> hasher;
    char *package_name = get_package_name();
    DIR *dir = opendir("/proc/self/fd");
    int dir_fd = dirfd(dir);
    struct dirent *ent;
    char buf[PATH_MAX];
    bool checked = false;
    while ((ent = readdir(dir)) != NULL) {
        if (ent->d_name[0] == '.') continue;

        ssize_t len = readlinkat(dir_fd, ent->d_name, buf, PATH_MAX);
        if (len <= 0) {
            continue;
        }

        std::string_view real_path(buf, len);
        if (!starts_with(real_path, "/data/app/") ||
            !ends_with(real_path, ".apk") ||
            !contains(real_path, package_name)) {
            continue;
        }

        std::string cert = read_certificate(atoi(ent->d_name));
        size_t size = cert.size();
        uLong crc = crc32(0, (unsigned const char *) cert.data(), cert.length());
        if (size == CERT_SIZE && crc == CERT_HASH) {
            checked = true;
        } else {
#ifndef NDEBUG
            LOGE("colorado: mismatch, expected %zx and %zx got %zx and %lx",
                 CERT_SIZE, CERT_HASH,
                 size, crc);
#endif
            checked = false;
            break;
        }
    }
    closedir(dir);
    free(package_name);
    if (!checked) {
        kill(getpid(), SIGKILL);
    }
    return checked;
}