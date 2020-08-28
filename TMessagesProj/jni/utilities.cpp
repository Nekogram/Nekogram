#include <jni.h>
#include <sys/stat.h>
#include <climits>
#include <unistd.h>
#include <string>
#include <openssl/sha.h>
#include <openssl/mem.h>

thread_local static char buf[PATH_MAX + 1];

extern "C" JNIEXPORT jstring Java_org_telegram_messenger_Utilities_readlink(JNIEnv *env, jclass clazz, jstring path) {
    const char *fileName = env->GetStringUTFChars(path, NULL);
    ssize_t result = readlink(fileName, buf, PATH_MAX);
    jstring value = 0;
    if (result != -1) {
        buf[result] = '\0';
        value = env->NewStringUTF(buf);
    }
    env->ReleaseStringUTFChars(path, fileName);
    return value;
}

extern "C" JNIEXPORT jstring Java_tw_nekomimi_nekogram_translator_MicrosoftTranslator_Hash(JNIEnv *env, jobject thiz, jstring to, jstring query, jstring t) {
    const char *toChar = env->GetStringUTFChars(to, nullptr);
    const char *queryChar = env->GetStringUTFChars(query, nullptr);
    const char *tChar = env->GetStringUTFChars(t, nullptr);

    std::string text = std::string(tChar) + "114-" + std::string(queryChar) + "-514-" + std::string(toChar);

    const char *textChar = text.c_str();
    unsigned char digest[SHA512_DIGEST_LENGTH];

    SHA256_CTX ctx;
    SHA256_Init(&ctx);
    SHA256_Update(&ctx, textChar, strlen(textChar));
    SHA256_Final(digest, &ctx);

    OPENSSL_cleanse(&ctx, sizeof(ctx));

    char hex[SHA256_DIGEST_LENGTH * 2 + 1];
    for (int i = 0; i < SHA256_DIGEST_LENGTH; i++) {
        sprintf(&hex[i * 2], "%02x", (unsigned int) digest[i]);
    }
    env->ReleaseStringUTFChars(to, toChar);
    env->ReleaseStringUTFChars(query, queryChar);
    env->ReleaseStringUTFChars(t, tChar);

    return env->NewStringUTF(hex);
}