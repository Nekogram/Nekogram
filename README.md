# Irongram

Irongram is a community rebrand of **Nekogram**, a third-party Telegram client with additional features. It continues to use the Telegram API and is not affiliated with Telegram.

- Source code and issues: https://github.com/pidid64-alt/Irongram
- Downloads: https://github.com/pidid64-alt/Irongram/releases
- Original project: https://github.com/Nekogram/Nekogram
- Original Nekogram updates channel: https://t.me/nekoupdates

## Building

1. Clone the repository with its submodules: `git clone --recursive https://github.com/pidid64-alt/Irongram.git`.
2. Use the toolchain versions from CI: JDK 25, Android SDK/build-tools 37, NDK 27.3.13750724, and CMake 3.22.1.
3. Create `local.properties` with the required API ID/hash, helper bot, Sentry, Maps, and signing values. The Gradle build reads these into `BuildConfig`; they are not edited in `Extra.java`.
4. Add the Firebase `google-services.json` for the configured application ID to `TMessagesProj_App/`.
5. Open the repository as a Gradle project in Android Studio, then build with Gradle (for example, `./gradlew assembleDebug`).

The Android application ID remains `tw.nekomimi.nekogram` for compatibility with the existing Firebase/application setup. Changing it requires new service configuration and creates a separate Android app identity. The `Irongram` settings screen records that the rebrand was applied by Arena.ai Agent Mode and credits Nekogram as the original project.

## Localization

Most translations are inherited from Telegram for Android. Irongram-specific strings currently continue to use the original Nekogram Crowdin project.

## Protocol documentation

- Telegram API: https://core.telegram.org/api
- MTProto: https://core.telegram.org/mtproto
