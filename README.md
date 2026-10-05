# Immich Swipe

![GitHub release (latest by date)](https://img.shields.io/github/v/release/markvoronin354/immich-swipe-android)
![GitHub License](https://img.shields.io/github/license/markvoronin354/immich-swipe-android)
![Kotlin](https://img.shields.io/badge/language-Kotlin-purple)
![Android](https://img.shields.io/badge/platform-Android-green)

[Version française disponible ici](README.fr.md) | [Privacy Policy](PRIVACY_POLICY.md)

Immich Swipe is an open-source Android application designed to make sorting your photos and videos hosted on your [Immich](https://immich.app/) server easy and fun.

This project is a fork based on the original project at [Minos2020/immich-swipe-android](https://github.com/Minos2020/immich-swipe-android). I created this fork to act as a rough draft for new ideas and features that I would love to see added in the future.

> **Disclaimer**: This is an independent project and is not affiliated in any way with the official Immich project.

## 📸 Overview

|                                         Home Screen                                          |                                    Sorting Stack                                     |                                       Review Mode                                        |
|:--------------------------------------------------------------------------------------------:|:------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------:|
| <img src="metadata/en-US/images/phoneScreenshots/10_Light_HomeScreen_Listview.jpg" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/09_Dark_SwipeScreen.jpg" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/11_Dark_ReviewScreen.jpg" width="200"> |
|                                     *Browse your albums*                                     |                                  *Swipe to decide*                                   |                                 *Check before deleting*                                  |

<details>
<summary>🌙 <b>View Dark Mode Gallery</b></summary>

|                                        Home Screen                                         |                                     Sorting Stack                                      |                                       Review Mode                                        |                                       Settings                                         |
|:------------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------:|
| <img src="metadata/en-US/images/phoneScreenshots/05_Dark_HomeScreen_GridView.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/07_Dark_SwipeScreen.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/08_Dark_ReviewScreen.jpg" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/Dark_SettingsScreen.png" width="200"> |

</details>

## ✨ Features

- **🚀 Fast Sorting Stack**: Swipe right to keep, left to delete, or up for customizable actions (Archive, Favorite, Lock, Add to Album). Sorted assets disappear from the timeline in real-time.
- **👯 Duplicates Finder & Cleanup**: Discover and resolve duplicate photo clusters with side-by-side comparison, zoom preview, video playback, and format detail analysis.
- **📁 Collections (Virtual Albums)**: Access special groups such as **ALL**, **Orphans** (photos not in any album), and **Duplicates**.
- **📊 Usage Statistics**: Visualize your progress with statistics and decision breakdowns.
- **🛡️ Advanced Review Mode**: Inspect all decisions (Delete, Keep, Archive, Favorite, Lock) and see estimated reclaimed space before syncing with Immich.
- **⚙️ Customizable Actions & Interactions**: Configure action buttons, swipe gestures, vibration feedback, auto-advance, and video autoplay.
- **🔄 Multi-Account Support**: Switch between multiple Immich user accounts seamlessly with account-specific local progress.
- **🗃️ Database & Cache Management**: Export, import, or clear your local database (JSON format) and manage image/video caches.
- **🚦 Connection Diagnostics**: Real-time connection status indicator, HTTP/IP support, and in-app log viewer.
- **🎨 Modern Interface**: Built with Jetpack Compose and Material Design 3, with English, French, and Spanish language support.

## ⚙️ Configuration & API Key Permissions

1. Enter your Immich server URL or IP address with port (e.g., `https://immich.your-domain.com` or `http://10.0.0.10:2283`).
2. Enter your Immich API Key ([create one in Immich under User Settings > API Keys](https://my.immich.app/user-settings?isOpen=api-keys)).

### 🔐 API Key Permissions

To ensure proper functionality while adhering to the **Principle of Least Privilege**, configure your API key with the following permissions on your Immich server:

- `Asset.read` – Browse assets and asset metadata (`/api/search/metadata`, `/api/assets/{id}`).
- `Asset.statistics` – Fetch search statistics and count assets (`/api/search/statistics`).
- `Asset.download` – View high-resolution photos and stream videos (`/api/assets/{id}/original`, `/video/playback`).
- `Asset.update` – Archive, favorite, lock, or update assets (`/api/assets`, `/api/assets/{id}`).
- `Asset.edit.create` – Apply edits or rotations to assets (`/api/assets/{id}/edits`).
- `Asset.delete` – Move photos/videos to Immich trash upon syncing (`/api/assets`).
- `Asset.view` – View asset thumbnails (`/api/assets/{id}/thumbnail`).
- `Album.read` – Fetch user's albums list (`/api/albums`).
- `Album.update` – Add assets directly to an existing album (`/api/albums/{id}/assets`).
- `Duplicate.read` – Scan and resolve duplicate asset clusters (`/api/duplicates`).
- `User.read` – Fetch profile details for current user authentication (`/api/users/me`).
- `userProfileImage.read` – Display user avatar image in the account switcher popup (`/api/users/{id}/profile-image`).

3. Select an album or virtual collection and start sorting!

## 📦 Installation

|                                                                                                                       **Orion Store**                                                                                                                        |   **Direct Download**   |  **IzzyOnDroid / F-Droid**    |
|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|:-------------------------:|:----------------------------:|
| [<img src="https://github.com/RookieEnough/Orion-Store/blob/main/assets/orion-badge.png?raw=true"  alt="Get it on Orion Store" height="50">](https://rookieenough.github.io/Orion-Data/redirect.html?id=immich-swipe-android) | Get the latest APK from the [Releases](https://github.com/markvoronin354/immich-swipe-android/releases) section   |  Coming soon, hopefully |

## 🛠️ Build

If you want to compile the application yourself:

- **JDK 17** or higher required.
- **Android Studio** (Ladybug version or newer recommended).
- Clone the repository and import the project into Android Studio.
- Sync Gradle.
- Run `./gradlew assembleDebug` to generate a debug APK.

## 📄 License

This project is licensed under the GNU GPL v3. See the [LICENSE](LICENSE) file for more details.

## ⚖️ Disclaimer

While this project is developed with care and tested regularly, I cannot guarantee absolute data safety. By using Immich Swipe, you acknowledge that the author shall not be held liable for any data loss or accidental deletion of media.

Please keep in mind:
- **Trash Safety**: Immich Swipe never permanently deletes files or empties the trash on your Immich server. Deleted items are sent to Immich Trash and remain recoverable through the official Immich web/app interface during your server's trash retention period.
- **Principle of Least Privilege**: To minimize risks, configure your API key with only the strictly required permissions listed in [Configuration & API Key Permissions](#-configuration--api-key-permissions).
