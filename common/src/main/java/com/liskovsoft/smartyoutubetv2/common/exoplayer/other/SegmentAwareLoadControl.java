package com.liskovsoft.smartyoutubetv2.common.exoplayer.other;

import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.Allocator;

/**
 * CLEAN UI: keeps loading past a segment that is about to be skipped.<br/>
 * The jump then lands in media that is already in the buffer, so nothing has to be downloaded at that moment.<br/>
 * Everything else is left to the wrapped load control.
 */
public class SegmentAwareLoadControl implements LoadControl {
    private final LoadControl mWrapped;
    private final long mMaxBytes;
    private volatile long mRequiredBufferUs;

    /**
     * @param maxBytes never ask for more once this much is allocated. Zero switches the extra loading off.
     */
    public SegmentAwareLoadControl(LoadControl wrapped, long maxBytes) {
        mWrapped = wrapped;
        mMaxBytes = maxBytes;
    }

    /**
     * @param durationMs how much media has to be buffered ahead of the playback position. Zero for nothing special.
     */
    public void setRequiredBufferMs(long durationMs) {
        mRequiredBufferUs = durationMs > 0 ? durationMs * 1_000 : 0;
    }

    @Override
    public void onPrepared() {
        mWrapped.onPrepared();
    }

    @Override
    public void onTracksSelected(Renderer[] renderers, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        mWrapped.onTracksSelected(renderers, trackGroups, trackSelections);
    }

    @Override
    public void onStopped() {
        mWrapped.onStopped();
    }

    @Override
    public void onReleased() {
        mWrapped.onReleased();
    }

    @Override
    public Allocator getAllocator() {
        return mWrapped.getAllocator();
    }

    @Override
    public long getBackBufferDurationUs() {
        return mWrapped.getBackBufferDurationUs();
    }

    @Override
    public boolean retainBackBufferFromKeyframe() {
        return mWrapped.retainBackBufferFromKeyframe();
    }

    @Override
    public boolean shouldContinueLoading(long bufferedDurationUs, float playbackSpeed) {
        // Always ask the wrapped control, it keeps its own state up to date in this call
        if (mWrapped.shouldContinueLoading(bufferedDurationUs, playbackSpeed)) {
            return true;
        }

        // Called on the playback thread, the request comes from the main thread
        long requiredUs = mRequiredBufferUs;

        return requiredUs > 0 && bufferedDurationUs < requiredUs && getAllocator().getTotalBytesAllocated() < mMaxBytes;
    }

    @Override
    public boolean shouldStartPlayback(long bufferedDurationUs, float playbackSpeed, boolean rebuffering) {
        return mWrapped.shouldStartPlayback(bufferedDurationUs, playbackSpeed, rebuffering);
    }
}
