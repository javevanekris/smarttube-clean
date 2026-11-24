package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import com.liskovsoft.mediaserviceinterfaces.data.SponsorSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CLEAN UI: the timing behind a clean segment skip.<br/>
 * Plain maths without player or Android classes, so that it can be unit tested.
 */
final class SegmentSkipPlanner {
    /** Start loading past a segment once playback is this close to its start. */
    static final long PRELOAD_HORIZON_MS = 60_000;
    /** How much has to be buffered behind the landing point, so that playback continues right away. */
    static final long PRELOAD_MARGIN_MS = 10_000;
    /** Longer segments don't fit into the buffer anyway. */
    static final long PRELOAD_MAX_SEGMENT_MS = 180_000;
    /** Segments that start this soon after the previous one ends are skipped together. */
    static final long CHAIN_GAP_MS = 3_000;
    /** The watcher skips a segment only while playback is this close to its start (at normal speed). */
    static final long SKIP_WINDOW_MS = 2_000;
    /** Skip this much before the segment starts. Losing two frames of the video beats showing two frames of the segment. */
    static final long EARLY_TOLERANCE_MS = 60;
    /** Wake up this much before the segment starts. */
    static final long TIMER_LEAD_MS = 20;
    /** Also set the timer when the start is slightly further away than the next poll, polls aren't punctual. */
    static final long TIMER_SLACK_MS = 100;

    private SegmentSkipPlanner() {
    }

    /**
     * How much media has to be buffered ahead of the playback position, so that the jump over the segments that are
     * about to be skipped lands inside the buffer.
     * @param segments the segments that get skipped without asking
     * @param ignoredDurationMs segments shorter than this aren't skipped at all
     * @return zero when no skip is coming up
     */
    static long requiredBufferMs(long positionMs, float speed, List<? extends SponsorSegment> segments, long ignoredDurationMs) {
        if (segments == null || segments.isEmpty()) {
            return 0;
        }

        List<SponsorSegment> sorted = new ArrayList<>(segments);
        Collections.sort(sorted, (first, second) -> Long.compare(first.getStartMs(), second.getStartMs()));

        long skipWindowMs = (long) (SKIP_WINDOW_MS * speed);
        long requiredMs = 0;

        for (int i = 0; i < sorted.size(); i++) {
            SponsorSegment head = sorted.get(i);
            long startMs = head.getStartMs();
            long endMs = head.getEndMs();

            if (endMs - startMs < ignoredDurationMs) {
                continue; // too short, stays in the video
            }

            if (positionMs > Math.min(startMs + skipWindowMs, endMs)) {
                continue; // behind us, or the viewer is watching it
            }

            if (startMs - positionMs > PRELOAD_HORIZON_MS) {
                continue; // too far away to bother yet
            }

            // Segments glued to this one are skipped in the same go
            for (int j = i + 1; j < sorted.size(); j++) {
                SponsorSegment next = sorted.get(j);

                if (next.getStartMs() > endMs + CHAIN_GAP_MS) {
                    break;
                }

                endMs = Math.max(endMs, next.getEndMs());
            }

            if (endMs - startMs > PRELOAD_MAX_SEGMENT_MS) {
                continue; // won't fit into the buffer
            }

            requiredMs = Math.max(requiredMs, endMs - positionMs + PRELOAD_MARGIN_MS);
        }

        return requiredMs;
    }

    /**
     * Real time until playback reaches the next segment, so that the skip can happen at that very moment
     * instead of at the next poll.
     * @param segments the segments that get skipped without asking
     * @return -1 when the next poll comes first or no timer is needed
     */
    static long delayToNextStartMs(long positionMs, float speed, List<? extends SponsorSegment> segments, long pollIntervalMs) {
        if (segments == null || speed <= 0) {
            return -1;
        }

        long toleranceMs = (long) (EARLY_TOLERANCE_MS * speed);
        long nearestDelayMs = -1;

        for (SponsorSegment segment : segments) {
            long distanceMs = segment.getStartMs() - positionMs;

            if (distanceMs <= toleranceMs) {
                continue; // started already or matched by the regular check
            }

            long delayMs = Math.max((long) (distanceMs / speed) - TIMER_LEAD_MS, 0);

            if (delayMs > pollIntervalMs + TIMER_SLACK_MS) {
                continue;
            }

            if (nearestDelayMs == -1 || delayMs < nearestDelayMs) {
                nearestDelayMs = delayMs;
            }
        }

        return nearestDelayMs;
    }
}
