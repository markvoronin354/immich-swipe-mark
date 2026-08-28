# Implementation Plan - Accurate Swipe Progress and Statistics

The goal of this plan is to fix the inaccuracy in the progress percentage and the "remaining" asset count displayed in the `SwipeScreen`. Currently, the percentage is based on the server's total count, while the "remaining" count and stats are based on the (potentially incomplete) local work pile, leading to inconsistent UI states.

## User Review Required

> [!IMPORTANT]
> The "Remaining" size and count will now be estimated based on the server's total count when the album is not fully loaded locally. This provides a more realistic view of the remaining work but may fluctuate slightly during initial loading.

## Proposed Changes

### [Feature] Swipe UI Logic

#### [MODIFY] [SwipeUiState.kt](file:///Users/markvoronin/StudioProjects/immich-swipe-mark4/app/src/main/java/com/markvoronin/immichswipe/feature/swipe/SwipeUiState.kt)

- Update `totalCount` to return `remoteTotalCount` (the server's total) instead of `assets.size`.
- Update `remainingCount` to be `(totalCount - processedCount).coerceAtLeast(0)`.
- Improve `remainingSize` calculation to estimate the size of non-loaded assets using the `averageKnownSize` and the difference between `remoteTotalCount` and `assets.size`.
- Add `isRemainingEstimated` logic to account for non-loaded assets.

## Verification Plan

### Manual Verification
- Open a large album (e.g., 1000+ items).
- Verify that the percentage in the header and the "Remaining" count in the status badges are consistent (e.g., if it's 10% done, Remaining should be ~90% of the total).
- Verify that as more assets load in the background, the counts remain relatively stable or update accurately towards the final server total.
- Check the "Summary" dialog to ensure the "Remaining" count matches the header progress.
