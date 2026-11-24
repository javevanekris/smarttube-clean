package com.liskovsoft.smartyoutubetv2.common.exoplayer.other;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.Allocation;
import com.google.android.exoplayer2.upstream.Allocator;

import org.junit.Test;

public class SegmentAwareLoadControlTest {
    private static final long MAX_BYTES = 100_000_000;
    private static final long SECOND_US = 1_000_000;

    @Test
    public void followsTheWrappedControlWhenNothingIsAsked() {
        FakeLoadControl wrapped = new FakeLoadControl();
        SegmentAwareLoadControl control = new SegmentAwareLoadControl(wrapped, MAX_BYTES);

        wrapped.keepLoading = false;
        assertFalse(control.shouldContinueLoading(30 * SECOND_US, 1f));

        wrapped.keepLoading = true;
        assertTrue(control.shouldContinueLoading(30 * SECOND_US, 1f));
    }

    @Test
    public void keepsLoadingUntilTheAskedDurationIsBuffered() {
        FakeLoadControl wrapped = new FakeLoadControl();
        SegmentAwareLoadControl control = new SegmentAwareLoadControl(wrapped, MAX_BYTES);
        wrapped.keepLoading = false;

        control.setRequiredBufferMs(90_000);

        assertTrue(control.shouldContinueLoading(30 * SECOND_US, 1f));
        assertTrue(control.shouldContinueLoading(89 * SECOND_US, 1f));
        assertFalse(control.shouldContinueLoading(90 * SECOND_US, 1f));
    }

    @Test
    public void stopsAskingForMoreWhenTheRequestIsCleared() {
        FakeLoadControl wrapped = new FakeLoadControl();
        SegmentAwareLoadControl control = new SegmentAwareLoadControl(wrapped, MAX_BYTES);
        wrapped.keepLoading = false;

        control.setRequiredBufferMs(90_000);
        control.setRequiredBufferMs(0);

        assertFalse(control.shouldContinueLoading(30 * SECOND_US, 1f));
    }

    @Test
    public void neverGoesOverTheMemoryLimit() {
        FakeLoadControl wrapped = new FakeLoadControl();
        SegmentAwareLoadControl control = new SegmentAwareLoadControl(wrapped, MAX_BYTES);
        wrapped.keepLoading = false;
        control.setRequiredBufferMs(90_000);

        wrapped.allocatedBytes = (int) MAX_BYTES;

        assertFalse(control.shouldContinueLoading(30 * SECOND_US, 1f));
    }

    @Test
    public void doesNothingExtraWithoutMemoryBudget() {
        FakeLoadControl wrapped = new FakeLoadControl();
        SegmentAwareLoadControl control = new SegmentAwareLoadControl(wrapped, 0);
        wrapped.keepLoading = false;
        control.setRequiredBufferMs(90_000);

        assertFalse(control.shouldContinueLoading(30 * SECOND_US, 1f));
    }

    private static class FakeLoadControl implements LoadControl, Allocator {
        boolean keepLoading;
        int allocatedBytes;

        @Override
        public void onPrepared() {
        }

        @Override
        public void onTracksSelected(Renderer[] renderers, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        }

        @Override
        public void onStopped() {
        }

        @Override
        public void onReleased() {
        }

        @Override
        public Allocator getAllocator() {
            return this;
        }

        @Override
        public long getBackBufferDurationUs() {
            return 0;
        }

        @Override
        public boolean retainBackBufferFromKeyframe() {
            return false;
        }

        @Override
        public boolean shouldContinueLoading(long bufferedDurationUs, float playbackSpeed) {
            return keepLoading;
        }

        @Override
        public boolean shouldStartPlayback(long bufferedDurationUs, float playbackSpeed, boolean rebuffering) {
            return true;
        }

        @Override
        public Allocation allocate() {
            return null;
        }

        @Override
        public void release(Allocation allocation) {
        }

        @Override
        public void release(Allocation[] allocations) {
        }

        @Override
        public void trim() {
        }

        @Override
        public int getTotalBytesAllocated() {
            return allocatedBytes;
        }

        @Override
        public int getIndividualAllocationLength() {
            return 0;
        }
    }
}
