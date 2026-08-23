# Asset Loading Optimization Plan

This plan aims to improve the perceived performance of asset loading in the Swipe screen, specifically targeting the "next 5 assets" window. The focus is on making video playback start instantly and ensuring metadata/images are ready before the user swipes.

## User Review Required

> [!IMPORTANT]
> The proposed video pre-caching will use additional network data to download the first segments of upcoming videos. We should ensure this doesn't negatively impact users on metered connections (though the current implementation doesn't seem to have a "data saver" mode yet).

## Proposed Changes

### 1. Metadata Pre-fetching (ViewModel)
Currently, only the *next* asset's details are loaded. We will expand this to the next 5-10 assets to ensure file sizes and EXIF data are available for all upcoming cards.

#### [MODIFY] [SwipeViewModel.kt](file:///Users/markvoronin/StudioProjects/immich-swipe-mark4/app/src/main/java/com/markvoronin/immichswipe/feature/swipe/SwipeViewModel.kt)
- Update `onSwipe` and `loadAssetsAndDecisions` to trigger pre-loading for a range of assets instead of just one.
- Implement a `preloadUpcomingAssets(startIndex: Int)` method.

### 2. Video Segment Pre-caching (Core/Cache)
We will implement a mechanism to download the first few megabytes of upcoming videos into the `SimpleCache` using Media3's `CacheWriter`.

#### [NEW] [VideoPreloader.kt](file:///Users/markvoronin/StudioProjects/immich-swipe-mark4/app/src/main/java/com/markvoronin/immichswipe/core/cache/VideoPreloader.kt)
- Create a utility to manage background caching of video segments.
- It will use `VideoCache.getCache(context)` to populate the existing cache.

### 3. Image Pre-loading (ViewModel/UI)
We will use Coil's `ImageLoader` to pre-fetch preview images for the next few assets.

#### [MODIFY] [SwipeViewModel.kt](file:///Users/markvoronin/StudioProjects/immich-swipe-mark4/app/src/main/java/com/markvoronin/immichswipe/feature/swipe/SwipeViewModel.kt)
- Add image pre-loading logic to `preloadUpcomingAssets`.

---

## Verification Plan

### Automated Tests
- No automated tests planned as this is mostly a performance optimization.

### Manual Verification
- Deploy to a device with a moderate network connection.
- Swipe through assets and observe:
    - How quickly videos start playing compared to the current version.
    - If "Loading..." indicators are less frequent.
    - If file sizes in the summary are immediately available for upcoming assets.
- Monitor logcat for "ExoPlayer" and "VideoPreloader" logs to ensure pre-caching is working as expected.
