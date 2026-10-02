# Privacy Policy for Immich Swipe

**Last Updated:** October 1, 2026

## 1. Overview

**Immich Swipe** ("the Application") is an open-source Android application designed as a client to help users sort, manage, and clean up photos and videos hosted on their self-hosted [Immich](https://immich.app/) instances.

Your privacy is extremely important to us. **Immich Swipe does not collect, store, transmit, sell, or share any personal data, usage analytics, or telemetry to any third-party servers or developers.**

---

## 2. Information Handled by the Application

### A. Server Connection Credentials
* **What is stored:** Your self-hosted Immich server URL, port, and API Key / Access Token.
* **How it is used:** Used strictly to authenticate requests between your mobile device and your configured Immich server instance.
* **Storage location:** Stored locally and securely on your device using Android's DataStore / Encrypted Preferences. This data is never sent to any server other than the Immich server URL you explicitly provide.

### B. Photos, Videos, and Media Metadata
* **What is processed:** Thumbnails, photo/video files, EXIF metadata, asset IDs, album information, and duplicate detection metrics.
* **How it is used:** Used locally within the application to display media cards, handle swipe decisions (keep, delete, favorite, archive, add to album), and show duplicate clusters.
* **Network Transmission:** Media files and metadata are fetched directly from your Immich server and sent back to your Immich server when performing sync actions (such as deleting, favoriting, or archiving media).

### C. Local Storage & Cache
* **Database:** Temporary swipe decisions, album cache, and duplicate cluster data are stored locally in a SQLite database on your device via Android Room.
* **Media Cache:** Images and video clips are cached locally on your device to ensure smooth playback and performance.
* **Control:** You can clear the cache or export/import/reset the local database at any time from within the **Settings** menu of the application.

---

## 3. Device Permissions

To function properly, Immich Swipe requests the following Android permissions:

* **Internet Access (`INTERNET`)**: Required to communicate with your self-hosted Immich server instance over HTTP or HTTPS.
* **Read External Storage (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_EXTERNAL_STORAGE`)**: Optional/Requested if you enable local sync deletion. This allows the application to synchronize photo/video deletions made in the app with your device's local gallery storage.
* **Access Media Location (`ACCESS_MEDIA_LOCATION`)**: Used solely to read location metadata from local media if local synchronization is enabled.

---

## 4. Third-Party Services & Analytics

* **No Analytics or Telemetry**: Immich Swipe contains no third-party analytics (such as Google Analytics, Firebase Analytics, or Mixpanel).
* **No Crash Reporting Services**: No automated crash reporting frameworks (such as Sentry, Crashlytics) are integrated.
* **No Advertising**: Immich Swipe is completely ad-free and contains no advertising SDKs.
* **No External Data Transfers**: Your data is never transferred to or processed by any third-party company or individual.

---

## 5. Data Retention & Deletion

* All stored settings, account details, and decision logs remain exclusively on your device.
* Logging out of an account within the app removes that account's session details from local storage.
* Uninstalling the Application permanently removes all locally stored application data, caches, and database files from your device.

---

## 6. Children's Privacy

Immich Swipe does not knowingly collect or solicit personal information from children. Because no personal data is collected or transmitted to external servers, the app complies with standard privacy frameworks including COPPA and GDPR.

---

## 7. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. Any changes will be published in the application repository. You are advised to review this page periodically for any updates.

---

## 8. Contact Us

If you have any questions or suggestions regarding this Privacy Policy, please open an issue or contact the maintainers via the project repository:

* **GitHub Repository:** [https://github.com/markvoronin354/immich-swipe-android](https://github.com/markvoronin354/immich-swipe-android)
