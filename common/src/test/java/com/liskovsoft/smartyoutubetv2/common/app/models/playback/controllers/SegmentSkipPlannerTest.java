package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import static org.junit.Assert.assertEquals;

import com.liskovsoft.mediaserviceinterfaces.data.SponsorSegment;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SegmentSkipPlannerTest {
    private static final long NOTHING_IGNORED = 0;
    private static final long POLL_MS = 250;

    @Test
    public void noSegmentsNeedNoExtraBuffer() {
        assertEquals(0, SegmentSkipPlanner.requiredBufferMs(10_000, 1f, Collections.emptyList(), NOTHING_IGNORED));
        assertEquals(0, SegmentSkipPlanner.requiredBufferMs(10_000, 1f, null, NOTHING_IGNORED));
    }

    @Test
    public void upcomingSegmentIsBufferedToItsEndPlusMargin() {
        List<SponsorSegment> segments = Collections.singletonList(segment(100_000, 160_000));

        // 30 seconds before the segment: 30s to its start + 60s of segment + margin
        assertEquals(90_000 + SegmentSkipPlanner.PRELOAD_MARGIN_MS,
                SegmentSkipPlanner.requiredBufferMs(70_000, 1f, segments, NOTHING_IGNORED));
    }

    @Test
    public void segmentBeyondTheHorizonIsLeftAlone() {
        List<SponsorSegment> segments = Collections.singletonList(segment(100_000, 160_000));
        long tooEarlyMs = 100_000 - SegmentSkipPlanner.PRELOAD_HORIZON_MS - 1;

        assertEquals(0, SegmentSkipPlanner.requiredBufferMs(tooEarlyMs, 1f, segments, NOTHING_IGNORED));
    }

    @Test
    public void segmentThatWontBeSkippedAnymoreIsLeftAlone() {
        List<SponsorSegment> segments = Collections.singletonList(segment(100_000, 160_000));

        // The watcher only skips within the first two seconds of a segment (at normal speed)
        assertEquals(0, SegmentSkipPlanner.requiredBufferMs(102_001, 1f, segments, NOTHING_IGNORED));
        // Already behind us
        assertEquals(0, SegmentSkipPlanner.requiredBufferMs(170_000, 1f, segments, NOTHING_IGNORED));
    }

    @Test
    public void segmentsTheWatcherIgnoresAreLeftAlone() {
        List<SponsorSegment> shortSegment = Collections.singletonList(segment(100_000, 103_000));
        assertEquals(0, SegmentSkipPlanner.requiredBufferMs(90_000, 1f, shortSegment, 5_000));

        List<SponsorSegment> hugeSegment =
                Collections.singletonList(segment(100_000, 100_000 + SegmentSkipPlanner.PRELOAD_MAX_SEGMENT_MS + 1));
        assertEquals(0, SegmentSkipPlanner.requiredBufferMs(90_000, 1f, hugeSegment, NOTHING_IGNORED));
    }

    @Test
    public void segmentsThatFollowEachOtherAreBufferedAsOne() {
        // The second one starts two seconds after the first one ends: both get skipped in one go
        List<SponsorSegment> segments = Arrays.asList(segment(100_000, 130_000), segment(132_000, 150_000));

        assertEquals(60_000 + SegmentSkipPlanner.PRELOAD_MARGIN_MS,
                SegmentSkipPlanner.requiredBufferMs(90_000, 1f, segments, NOTHING_IGNORED));
    }

    @Test
    public void laterSegmentIsNotPartOfTheChain() {
        // The second one is neither glued to the first one nor close enough to matter yet
        List<SponsorSegment> segments = Arrays.asList(segment(100_000, 130_000), segment(200_000, 210_000));

        assertEquals(40_000 + SegmentSkipPlanner.PRELOAD_MARGIN_MS,
                SegmentSkipPlanner.requiredBufferMs(90_000, 1f, segments, NOTHING_IGNORED));
    }

    @Test
    public void unsortedSegmentsGiveTheSameAnswer() {
        List<SponsorSegment> segments = Arrays.asList(segment(132_000, 150_000), segment(100_000, 130_000));

        assertEquals(60_000 + SegmentSkipPlanner.PRELOAD_MARGIN_MS,
                SegmentSkipPlanner.requiredBufferMs(90_000, 1f, segments, NOTHING_IGNORED));
    }

    @Test
    public void timerFiresJustBeforeTheSegmentStarts() {
        List<SponsorSegment> segments = Collections.singletonList(segment(100_000, 160_000));

        assertEquals(200 - SegmentSkipPlanner.TIMER_LEAD_MS,
                SegmentSkipPlanner.delayToNextStartMs(99_800, 1f, segments, POLL_MS));
    }

    @Test
    public void timerAccountsForPlaybackSpeed() {
        List<SponsorSegment> segments = Collections.singletonList(segment(100_000, 160_000));

        // 400 ms of video takes 200 ms at double speed
        assertEquals(200 - SegmentSkipPlanner.TIMER_LEAD_MS,
                SegmentSkipPlanner.delayToNextStartMs(99_600, 2f, segments, POLL_MS));
    }

    @Test
    public void noTimerWhenTheNextPollComesFirst() {
        List<SponsorSegment> segments = Collections.singletonList(segment(100_000, 160_000));

        assertEquals(-1, SegmentSkipPlanner.delayToNextStartMs(95_000, 1f, segments, POLL_MS));
    }

    @Test
    public void noTimerWhenTheSegmentAlreadyMatches() {
        List<SponsorSegment> segments = Collections.singletonList(segment(100_000, 160_000));

        // Inside the tolerance the regular check skips right away, a timer would only repeat it
        assertEquals(-1, SegmentSkipPlanner.delayToNextStartMs(100_000 - SegmentSkipPlanner.EARLY_TOLERANCE_MS, 1f, segments, POLL_MS));
        assertEquals(-1, SegmentSkipPlanner.delayToNextStartMs(120_000, 1f, segments, POLL_MS));
    }

    @Test
    public void timerPicksTheNearestSegment() {
        List<SponsorSegment> segments = Arrays.asList(segment(100_200, 160_000), segment(100_100, 110_000));

        assertEquals(100 - SegmentSkipPlanner.TIMER_LEAD_MS,
                SegmentSkipPlanner.delayToNextStartMs(100_000, 1f, segments, POLL_MS));
    }

    @Test
    public void noTimerWithoutSegmentsOrSpeed() {
        assertEquals(-1, SegmentSkipPlanner.delayToNextStartMs(100_000, 1f, null, POLL_MS));
        assertEquals(-1, SegmentSkipPlanner.delayToNextStartMs(99_900, 0f, Collections.singletonList(segment(100_000, 160_000)), POLL_MS));
    }

    private static SponsorSegment segment(long startMs, long endMs) {
        return new SponsorSegment() {
            @Override
            public long getStartMs() {
                return startMs;
            }

            @Override
            public long getEndMs() {
                return endMs;
            }

            @Override
            public String getCategory() {
                return CATEGORY_SPONSOR;
            }

            @Override
            public String getAction() {
                return ACTION_SKIP;
            }
        };
    }
}
