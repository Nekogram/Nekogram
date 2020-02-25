# Nekogram
![Logo](https://raw.githubusercontent.com/Nekogram/Nekogram/master/TMessagesProj/src/main/res/mipmap-xxxhdpi/ic_launcher.png)  
Nekogram is an UNOFFICIAL app that uses Telegram's API.

- Google play store: https://play.google.com/store/apps/details?id=tw.nekomimi.nekogram
- Update news (English): https://t.me/nekoupdates
- Update news (Chinese): https://t.me/zuragram
- APKs: https://github.com/Nekogram/Nekogram/releases
- Feedback: https://t.me/nekosupportbot
- Feedback: https://github.com/Nekogram/Nekogram/issues

## API, Protocol documentation

Telegram API manuals: https://core.telegram.org/api

MTproto protocol manuals: https://core.telegram.org/mtproto

## Compilation Guide

1. Download the Nekogram source code from https://github.com/Nekogram/Nekogram ( git clone https://github.com/Nekogram/Nekogram.git )
2. Copy your release.keystore into TMessagesProj/config
3. Fill out RELEASE_KEY_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_STORE_PASSWORD in local.properties to access your  release.keystore
4. Open the project in the Studio (note that it should be opened, NOT imported).
5. If you're compiling DEBUG version, make sure your build variants is set to afatDebugMultidex.
6. You are ready to compile Nekogram.

## Localization

Nekogram is forked from Telegram, thus most locales follows the translations of Telegram for Android, checkout https://translations.telegram.org/en/android/.

As for the Nekogram specialized strings, we use Crowdin to translate Nekogram. Join project at https://neko.crowdin.com/nekogram. Help us bring Nekogram to the world!

## Contributors

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
| [<img src="https://avatars2.githubusercontent.com/u/42698724?s=460&v=4" width="80px;"/><br /><sub>猫耳逆变器</sub>](https://github.com/NekoInverter)<br />[💻](https://github.com/Nekogram/Nekogram/commits?author=NekoInverter "Code") | [<img src="https://avatars1.githubusercontent.com/u/18373361?s=460&v=4" width="80px;"/><br /><sub>梨子</sub>](https://github.com/rikakomoe)<br />[💻](https://github.com/Nekogram/Nekogram/commits?author=rikakomoe "Code") | [<img src="https://i.loli.net/2020/01/17/e9Z5zkG7lNwUBPE.jpg" width="80px;"/><br /><sub>呆瓜</sub>](https://t.me/Duang)<br /> [🎨](#design-duang "Design") |
| :---: | :---: | :---: |
<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/kentcdodds/all-contributors) specification. Contributions of any kind welcome!
