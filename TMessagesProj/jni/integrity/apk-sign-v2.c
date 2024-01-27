//
// Created by Thom on 2019/3/8.
//

#include <fcntl.h>
#include <unistd.h>
#include <stdbool.h>

#ifdef MAIN
#include <stdio.h>
#ifdef __APPLE__
#include <string.h>
#include <CommonCrypto/CommonDigest.h>
#endif
#else

#include "common.h"
#include "openat.h"

#endif

#include "apk-sign-v2.h"

static bool isApkSigBlock42(const char *buffer) {
    // APK Sig Block 42
    return *buffer == 'A'
           && *++buffer == 'P'
           && *++buffer == 'K'
           && *++buffer == ' '
           && *++buffer == 'S'
           && *++buffer == 'i'
           && *++buffer == 'g'
           && *++buffer == ' '
           && *++buffer == 'B'
           && *++buffer == 'l'
           && *++buffer == 'o'
           && *++buffer == 'c'
           && *++buffer == 'k'
           && *++buffer == ' '
           && *++buffer == '4'
           && *++buffer == '2';
}

#if defined(MAIN) && defined(__APPLE__)
static unsigned char digest[CC_SHA1_DIGEST_LENGTH];
#endif

static unsigned calcHash(int fd, unsigned size) {
    signed char c;
    unsigned i = 0;
    int hash = 1;

#if defined(MAIN) && defined(__APPLE__)
    CC_SHA1_CTX context;
    CC_SHA1_Init(&context);
    memset(digest, 0, CC_SHA1_DIGEST_LENGTH);
#endif

    while (i < size) {
        read(fd, &c, 0x1);
#if defined(MAIN) && defined(__APPLE__)
        CC_SHA1_Update(&context, &c, 1);
#endif
        hash = 31 * hash + c;
        ++i;
    }
#if defined(MAIN) && defined(__APPLE__)
    CC_SHA1_Final(digest, &context);
#endif
    return (unsigned) hash;
}

#ifdef MAIN
static void showCert(unsigned size, unsigned hash, int sign, const char *prefix) {
    printf("%ssize: 0x%04x, hash: 0x%08x", prefix, size, hash ^ 0x14131211u);
#ifdef __APPLE__
    printf(", sha1: ");
    for (int i = 0; i < CC_SHA1_DIGEST_LENGTH; ++i) {
        printf("%02x", digest[i]);
    }
#endif
    printf(", sign v%d\n", sign);
}
#endif

int checkSignature(const char *path) {
    unsigned char buffer[0x11] = {0};
    uint32_t size4;
    uint64_t size8, size_of_block;

#ifdef DEBUG
    LOGI("check signature for %s", path);
#endif

    int sign = -1;
#ifdef MAIN
#define openAt openat
#endif
    int fd = (int) openAt(AT_FDCWD, path, O_RDONLY);
#ifdef DEBUG_OPENAT
    LOGI("openat %s returns %d", path, fd);
#endif
    if (fd < 0) {
        return sign;
    }

    bool verified = false;
    sign = 1;
    // https://en.wikipedia.org/wiki/Zip_(file_format)#End_of_central_directory_record_(EOCD)
    for (int i = 0;; ++i) {
        unsigned short n;
        lseek(fd, -i - 2, SEEK_END);
        read(fd, &n, 2);
        if (n == i) {
            lseek(fd, -22, SEEK_CUR);
            read(fd, &size4, 4);
            if ((size4 ^ 0xcafebabeu) == 0xccfbf1eeu) {
#ifdef MAIN
                if (i > 0) {
                    printf("warning: comment length is %d\n", i);
                }
#endif
                break;
            }
        }
        if (i == 0xffff) {
#ifdef MAIN
            printf("error: cannot find eocd\n");
#endif
            goto clean;
        }
    }

    lseek(fd, 12, SEEK_CUR);
    // offset
    read(fd, &size4, 0x4);
    lseek(fd, (off_t) (size4 - 0x18), SEEK_SET);

    read(fd, &size8, 0x8);
    read(fd, buffer, 0x10);
    if (!isApkSigBlock42((char *) buffer)) {
        goto clean;
    }

    lseek(fd, (off_t) (size4 - (size8 + 0x8)), SEEK_SET);
    read(fd, &size_of_block, 0x8);
    if (size_of_block != size8) {
        goto clean;
    }

    for (;;) {
        uint32_t id;
        uint32_t offset;
        read(fd, &size8, 0x8); // sequence length
        if (size8 == size_of_block) {
            break;
        }
        read(fd, &id, 0x4); // id
        offset = 4;
#ifdef MAIN
        printf("id: 0x%08x\n", id);
#endif
        if ((id ^ 0xdeadbeefu) == 0xafa439f5u) {
            sign = 2;
        } else if ((id ^ 0xdeadbeefu) == 0x2efed62f) {
            sign = 3;
        } else if ((id ^ 0xdeadbeefu) == 0xc53e138eu) {
            sign = 31;
        } else {
            sign = 1;
        }
        if (sign > 1) {
            uint32_t size, hash;
            verified = false;
            read(fd, &size4, 0x4); // signer-sequence length
            read(fd, &size4, 0x4); // signer length
            read(fd, &size4, 0x4); // signed data length
            offset += 0x4 * 3;

            read(fd, &size4, 0x4); // digests-sequence length
            lseek(fd, (off_t) (size4), SEEK_CUR);// skip digests
            offset += 0x4 + size4;

            read(fd, &size4, 0x4); // certificates length
            read(fd, &size, 0x4); // certificate length
            offset += 0x4 * 2;
            hash = calcHash(fd, size);
            offset += size;
#ifdef MAIN
            showCert(size, hash, sign, "    signer, ");
#endif
#if defined(GENUINE_SIZE) && defined(GENUINE_HASH)
            if (size == GENUINE_SIZE && (hash ^ 0x14131211u) == GENUINE_HASH) {
                verified = true;
            }
#endif
            if (!verified && size4 - size == 4 && (sign == 3 || sign == 31)) {
                int64_t attributes_size, attribute_size;
                uint32_t minSdk, maxSdk;
                read(fd, &minSdk, 0x4); // minSdk
                read(fd, &maxSdk, 0x4); // maxSdk
                read(fd, &size4, 0x4); // length of additional attributes
                attributes_size = size4;
                offset += 0x4 * 3;
#ifdef MAIN
                printf("    minSdk: %d, maxSdk: %d\n", minSdk, maxSdk);
#endif
                while (attributes_size > 0) {
                    read(fd, &size4, 0x4); // attribute length
                    attribute_size = size4;
                    attributes_size -= 0x4;
                    attributes_size -= attribute_size;
                    read(fd, &size4, 0x4); // attribute ID
                    attribute_size -= 0x4;
                    offset += 0x4 * 2;
#ifdef MAIN
                    printf("    attribute id: 0x%08x, size: %lld\n", size4, attribute_size);
#endif
                    if ((size4 ^ 0xdeadbeefu) == 0xe50dd163u) {
                        read(fd, &size4, 0x4); // version
#ifdef MAIN
                        printf("        version: %d\n", size4);
#endif
                        offset += 0x4;
                        attribute_size -= 0x4;
                        while (attribute_size > 0) {
                            unsigned node;
                            read(fd, &node, 0x4); // length of node
                            offset += 0x4 + node;
                            attribute_size -= 0x4 + node;
                            read(fd, &size4, 0x4); // length of signed data
                            read(fd, &size, 0x4); // length of certificate
                            node -= 0x4 * 2 + size;
                            hash = calcHash(fd, size);
#ifdef MAIN
                            showCert(size, hash, sign, "        rotate, ");
#endif
#if defined(GENUINE_SIZE) && defined(GENUINE_HASH)
                            if (size == GENUINE_SIZE && (hash ^ 0x14131211u) == GENUINE_HASH) {
                                verified = true;
                            }
#endif
                            lseek(fd, (off_t) node, SEEK_CUR);
                        }
#ifdef MAIN
                        } else if (size4 == 0x559f8b02) {
                            read(fd, &size4, 0x4);
                            offset += 0x4;
                            attribute_size -= 0x4;
                            printf("        rotation minSdk: %d\n", size4);
                        } else if (size4 == 0xc2a6b3ba) {
                            printf("        rotation minSdk: development\n");
#endif
                    }
                    if (attribute_size > 0) {
                        lseek(fd, (off_t) attribute_size, SEEK_CUR);
                        offset += attribute_size;
                    }
                }
            }
#if defined(GENUINE_SIZE) && defined(GENUINE_HASH)
            if (!verified) {
                break;
            }
#endif
        }
        lseek(fd, (off_t) (size8 - offset), SEEK_CUR);
    }

    clean:
    close(fd);

    if (verified) {
        return 0;
    }
    return sign;
}

#ifdef MAIN
int main(int argc, char **argv) {
    if (argc > 1) {
        checkSignature(argv[1]);
    }
}
#endif
