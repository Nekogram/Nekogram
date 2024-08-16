#include <string_view>
#include <dirent.h>
#include <unistd.h>
#include <zlib.h>

#include "colorado.h"
#include "logging.h"
#include "obfs-string.h"
#include "utils.h"

void kill_self() {
    kill(getpid(), SIGKILL);
}

bool check_signature() {
    std::hash<std::string> hasher;
    DIR *dir = opendir("/proc/self/fd"_iobfs.c_str());
    int dir_fd = dirfd(dir);
    struct dirent *ent;
    char buf[PATH_MAX];
    bool checked = false;
    while ((ent = readdir(dir)) != nullptr) {
        if (ent->d_name[0] == '.') continue;

        ssize_t len = readlinkat(dir_fd, ent->d_name, buf, PATH_MAX);
        if (len <= 0) {
            continue;
        }

        std::string_view real_path(buf, len);
        if (!starts_with(real_path, "/data/app/"_iobfs.c_str()) ||
            !ends_with(real_path, ".apk"_iobfs.c_str()) ||
            !contains(real_path, PACKAGE_NAME)) {
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
    if (!checked) {
        kill_self();
    }
    return checked;
}