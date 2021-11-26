# 🐾 Nekogram
[![Crowdin](https://badges.crowdin.net/e/a094217ac83905ae1625526d59bba8dc/localized.svg)](https://neko.crowdin.com/nekogram)  
Nekogram is a third-party Telegram client with not many but useful modifications.

- Official site: https://nekogram.app
- Telegram channel: https://t.me/nekoupdates
- Telegram channel (Chinese): https://t.me/zuragram
- Downloads: https://nekogram.app/download
- Feedback: https://gitlab.com/Nekogram/Nekogram/-/issues

## API, Protocol documentation

Telegram API manuals: https://core.telegram.org/api

MTproto protocol manuals: https://core.telegram.org/mtproto

## Compilation Guide

1. Download the Nekogram source code from https://gitlab.com/Nekogram/Nekogram ( git clone https://gitlab.com/Nekogram/Nekogram.git )
2. Copy your release.keystore into TMessagesProj/config
3. Fill out RELEASE_KEY_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_STORE_PASSWORD in local.properties to access your  release.keystore
4. Go to https://console.firebase.google.com/, create two android apps with application IDs tw.nekomimi.nekogram and tw.nekomimi.nekogram.beta, turn on firebase messaging and download google-services.json, which should be copied to the same folder as TMessagesProj.
5. Open the project in the Studio (note that it should be opened, NOT imported).
6. Fill out values in TMessagesProj/src/main/java/tw/nekomimi/nekogram/Extra.java – there’s a link for each of the variables showing where and which data to obtain.
7. You are ready to compile Nekogram.

## Localization

Nekogram is forked from Telegram, thus most locales follows the translations of Telegram for Android, checkout https://translations.telegram.org/en/android/.

As for the Nekogram specialized strings, we use Crowdin to translate Nekogram. Join project at https://neko.crowdin.com/nekogram. Help us bring Nekogram to the world!
