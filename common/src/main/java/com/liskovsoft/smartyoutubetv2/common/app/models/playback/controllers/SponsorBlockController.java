package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.liskovsoft.mediaserviceinterfaces.MediaItemService;
import com.liskovsoft.mediaserviceinterfaces.ServiceManager;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemMetadata;
import com.liskovsoft.mediaserviceinterfaces.data.SponsorSegment;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.BasePlayerController;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerUI;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.SponsorBlockSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.SponsorBlockData;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.utils.CleanToast;
import android.view.KeyEvent;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SponsorBlockController extends BasePlayerController {
    private static final String TAG = SponsorBlockController.class.getSimpleName();
    // CLEAN UI: check four times a second (upstream: 1_000). With one check a second up to a second of the segment was played before the skip.
    // The skip itself doesn't wait for a check: a timer fires at the moment the segment starts, see planAhead.
    private static final long POLL_INTERVAL_MS = 250;
    private static final int CONTENT_BLOCK_ID = 144;
    private MediaItemService mMediaItemService;
    private List<SponsorSegment> mOriginalSegments;
    private List<SponsorSegment> mActiveSegments;
    private long mLastSkipPosMs;
    private boolean mSkipExclude;
    private Disposable mSegmentsAction;
    private Observable<List<SponsorSegment>> mCachedSegmentsAction;
    private String mVideoId;
    // CLEAN UI: OK right after a skip jumps back into the skipped segment
    private static final long UNDO_WINDOW_MS = 4_000;
    private SponsorSegment mUndoSegment;
    private long mUndoDeadlineMs;
    // CLEAN UI: runs the check at the moment a segment starts
    private final Runnable mPreciseCheck = () -> {
        if (getPlayer() != null) {
            skipSegment(0);
        }
    };

    public static class SegmentAction {
        public String segmentCategory;
        public int actionType;

        public static SegmentAction from(String spec) {
            if (spec == null) {
                return null;
            }

            String[] split = spec.split(",");

            if (split.length != 2) {
                return null;
            }

            String name = Helpers.parseStr(split[0]);
            int action = Helpers.parseInt(split[1]);

            return from(name, action);
        }

        public static SegmentAction from(String name, int action) {
            SegmentAction blockedSegment = new SegmentAction();
            blockedSegment.segmentCategory = name;
            blockedSegment.actionType = action;

            return blockedSegment;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("%s,%s", segmentCategory, actionType);
        }
    }

    @Override
    public void onInit() {
        ServiceManager service = YouTubeServiceManager.instance();
        mMediaItemService = service.getMediaItemService();
    }

    @Override
    public void onNewVideo(Video item) {
        mUndoSegment = null;
        mSkipExclude = false;
        cancelPlan();
        if (getPlayer() != null) {
            getPlayer().setSeekBarSegments(null); // reset colors
        }
    }

    @Override
    public void onVideoLoaded(Video item) {
        disposeActions();

        if (getPlayer() == null) {
            return;
        }

        boolean enabled = getSponsorBlockData().isSponsorBlockEnabled() && checkVideo(item) && !isChannelExcluded(item.channelId);
        getPlayer().setButtonState(R.id.action_content_block, enabled ? PlayerUI.BUTTON_ON : PlayerUI.BUTTON_OFF);

        if (enabled) {
            updateSponsorSegmentsAndWatch(item);
        }
    }

    @Override
    public void onMetadata(MediaItemMetadata metadata) {
        // Disable sponsor for the live streams.
        // Fix when using remote control.
        if (!getSponsorBlockData().isSponsorBlockEnabled() || !checkVideo(getVideo())) {
            disposeActions();
        } else if (isChannelExcluded(metadata.getChannelId())) { // got channel id. check the exclusions
            if (getPlayer() != null) {
                getPlayer().setButtonState(R.id.action_content_block, PlayerUI.BUTTON_OFF);
            }
            disposeActions();
        }
    }

    @Override
    public void onEngineReleased() {
        disposeActions();
    }

    @Override
    public void onButtonClicked(int buttonId, int buttonState) {
        if (getPlayer() == null) {
            return;
        }

        if (buttonId == R.id.action_content_block) {
            List<SponsorSegment> foundSegments = findMatchedSegments(getPlayer().getPositionMs(), mOriginalSegments, true);

            if (foundSegments != null) {
                SponsorSegment lastSegment = foundSegments.get(foundSegments.size() - 1);
                setPositionMs(lastSegment.getEndMs());
            } else {
                Video video = getVideo();
                String channelId = video != null ? video.channelId : null;

                getSponsorBlockData().setSponsorBlockEnabled(buttonState != PlayerUI.BUTTON_ON);
                getSponsorBlockData().stopExcludingChannel(channelId);
                onVideoLoaded(video);
            }
        }
    }

    @Override
    public void onButtonLongClicked(int buttonId, int buttonState) {
        if (buttonId == R.id.action_content_block) {
            SponsorBlockSettingsPresenter.instance(getContext()).show(() -> {
                if (getPlayer() != null) {
                    onVideoLoaded(getVideo());
                }
            });
        }
    }

    private boolean checkVideo(Video video) {
        //return video != null && !video.isLive && !video.isUpcoming;
        return video != null;
    }

    private void updateSponsorSegmentsAndWatch(Video item) {
        if (item == null || item.videoId == null || item.isLive || getSponsorBlockData().getEnabledCategories().isEmpty()) {
            mActiveSegments = mOriginalSegments = null;
            mCachedSegmentsAction = null;
            return;
        }

        if (!Helpers.equals(mVideoId, item.videoId) || mCachedSegmentsAction == null) {
            // NOTE: SponsorBlock (when happened java.net.SocketTimeoutException) could block whole application with Schedulers.io()
            // Because Schedulers.io() reuses blocked threads in RxJava 2: https://github.com/ReactiveX/RxJava/issues/6542
            mCachedSegmentsAction = mMediaItemService.getSponsorSegmentsObserve(item.videoId, getSponsorBlockData().getEnabledCategories())
                    .cache();
            mVideoId = item.videoId;
        }

        mSegmentsAction = mCachedSegmentsAction
                .flatMap(this::startSponsorWatcher)
                .subscribe(
                        this::skipSegment,
                        error -> Log.d(TAG, "It's ok. Nothing to block in this video. Error msg: %s", error.getMessage())
                );
    }

    private Observable<Long> startSponsorWatcher(List<SponsorSegment> segments) {
        if (getPlayer() == null || segments == null || segments.isEmpty()) {
            mActiveSegments = mOriginalSegments = null;
            return Observable.empty();
        }

        mOriginalSegments = segments;

        mActiveSegments = new ArrayList<>(segments);

        if (getSponsorBlockData().isColorMarkersEnabled()) {
            getPlayer().setSeekBarSegments(toSeekBarSegments(segments));
        }
        if (getSponsorBlockData().isActionsEnabled()) {
            // Warn. Try to not access player object here.
            // Or you'll get "Player is accessed on the wrong thread" error.
            return RxHelper.interval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
        } else {
            return Observable.empty();
        }
    }

    private void disposeActions() {
        RxHelper.disposeActions(mSegmentsAction);
        cancelPlan();

        // Note, removes all segments at once
        //getPlayer().setSeekBarSegments(null); // reset colors

        // Reset previously found segment (fix no dialog popup)
        mLastSkipPosMs = 0;
    }

    private void skipSegment(long interval) {
        if (mActiveSegments == null || mActiveSegments.isEmpty() || getVideo() == null || !Helpers.equals(mVideoId, getVideo().videoId)) {
            disposeActions();
            return;
        }

        long positionMs = getPlayer().getPositionMs();

        // Fix looping messages at the end of the video (playback mode: pause at the end of the video)
        if (!getPlayer().isPlaying()) {
            // CLEAN UI: keep the buffer in step with the position, the viewer may seek while paused
            planBuffer(positionMs);
            return;
        }

        List<SponsorSegment> foundSegments = findMatchedSegments(positionMs, mActiveSegments, false);

        applyActions(foundSegments);

        // Skip each segment only once
        if (foundSegments != null && getSponsorBlockData().isDontSkipSegmentAgainEnabled()) {
            mActiveSegments.removeAll(foundSegments);
        }

        // CLEAN UI: after a skip the position is stale, the next check plans from the new one
        if (foundSegments == null) {
            planAhead(positionMs);
        }
    }

    /**
     * CLEAN UI: prepare the next skip, so that it happens on time and without a download.
     */
    private void planAhead(long positionMs) {
        List<SponsorSegment> skippedSegments = planBuffer(positionMs);

        // The poll is too coarse to hit the start of a segment. Wake up at that very moment instead.
        long delayMs = SegmentSkipPlanner.delayToNextStartMs(positionMs, getPlayer().getSpeed(), skippedSegments, POLL_INTERVAL_MS);

        if (delayMs >= 0) {
            Utils.postDelayed(mPreciseCheck, delayMs);
        }
    }

    /**
     * CLEAN UI: have the player load past the segments that are about to be skipped. The jump then lands in the buffer.
     * @return the segments that get skipped without asking
     */
    private List<SponsorSegment> planBuffer(long positionMs) {
        List<SponsorSegment> skippedSegments = getSkippedSegments();

        // Same rule as in applyActions: shorter segments stay in the video
        long ignoredDurationMs = Math.max(getSponsorBlockData().getIgnoredDurationMs(), getPlayerTweaksData().isTextureViewEnabled() ? 10_000 : 0);

        getPlayer().setBufferAheadMs(SegmentSkipPlanner.requiredBufferMs(positionMs, getPlayer().getSpeed(), skippedSegments, ignoredDurationMs));

        return skippedSegments;
    }

    private List<SponsorSegment> getSkippedSegments() {
        List<SponsorSegment> result = new ArrayList<>();

        for (SponsorSegment segment : mActiveSegments) {
            int action = getSponsorBlockData().getAction(segment.getCategory());

            if (action == SponsorBlockData.ACTION_SKIP_ONLY || action == SponsorBlockData.ACTION_SKIP_WITH_TOAST) {
                result.add(segment);
            }
        }

        return result;
    }

    private void cancelPlan() {
        Utils.removeCallbacks(mPreciseCheck);

        if (getPlayer() != null) {
            getPlayer().setBufferAheadMs(0);
        }
    }

    private boolean isPositionInsideSegment(long positionMs, SponsorSegment segment, boolean fullMatch) {
        // NOTE: in case of using Player.setSeekParameters (inaccurate seeking) increase sponsor segment window
        // int seekShift = 1_000;
        // return positionMs >= (segment.getStartMs() - seekShift) && positionMs <= (segment.getEndMs() + seekShift);

        if (fullMatch) {
            return positionMs >= segment.getStartMs() && positionMs <= segment.getEndMs();
        } else {
            long windowSizeMs = (long) (SegmentSkipPlanner.SKIP_WINDOW_MS * getPlayer().getSpeed());
            // CLEAN UI: jump just before the segment starts, so that nothing of it is shown.
            // A timer brings us here at the right moment (see planAhead), so two frames of margin are enough.
            long lookAheadMs = (long) (SegmentSkipPlanner.EARLY_TOLERANCE_MS * getPlayer().getSpeed());
            return positionMs >= (segment.getStartMs() - lookAheadMs) && positionMs <= Math.min(segment.getStartMs() + windowSizeMs, segment.getEndMs());
        }
    }

    private void simpleSkip(long skipPosMs) {
        if (mLastSkipPosMs == skipPosMs) {
            return;
        }

        setPositionMs(skipPosMs);
        closeTransparentDialog();
    }

    private void messageSkip(long skipPosMs, String category, SponsorSegment segment) {
        if (mLastSkipPosMs == skipPosMs) {
            return;
        }

        // CLEAN UI: compact glass notification instead of the system toast
        mUndoSegment = segment;
        mUndoDeadlineMs = System.currentTimeMillis() + UNDO_WINDOW_MS;
        CleanToast.show(getContext(), getContext().getString(R.string.msg_segment_skipped_undo, category), true);
        setPositionMs(skipPosMs);
        closeTransparentDialog();
    }

    /**
     * CLEAN UI: undo the last skip when OK is pressed shortly after it.
     * @return true when the key was used for the undo
     */
    public boolean undoLastSkip(int keyCode) {
        boolean isConfirm = keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER;
        SponsorSegment segment = mUndoSegment;

        if (!isConfirm || segment == null || getPlayer() == null) {
            return false;
        }

        mUndoSegment = null;

        if (System.currentTimeMillis() > mUndoDeadlineMs || getPlayer().isOverlayShown()) {
            return false;
        }

        if (mActiveSegments != null) {
            mActiveSegments.remove(segment); // watch it this time
        }

        mLastSkipPosMs = 0;
        getPlayer().setPositionMs(Math.max(0, segment.getStartMs()));
        CleanToast.show(getContext(), getContext().getString(R.string.msg_segment_skip_undone));

        return true;
    }

    private void confirmSkip(long skipPosMs, String category) {
        if (mLastSkipPosMs == skipPosMs) {
            return;
        }

        AppDialogPresenter dialogPresenter = AppDialogPresenter.instance(getContext());

        if (dialogPresenter.isDialogShown() || getPlayer().isSuggestionsShown()) {
            // Another dialog is opened. Don't distract a user.
            return;
        }

        getPlayer().showControls(false);

        OptionItem acceptOption = UiOptionItem.from(
                getContext().getString(R.string.confirm_segment_skip, category),
                option -> {
                    // return to previous dialog or close if no other dialogs in stack
                    dialogPresenter.goBack();
                    setPositionMs(skipPosMs);
                }
        );

        dialogPresenter.appendSingleButton(acceptOption);
        dialogPresenter.setCloseTimeoutMs((long) ((skipPosMs - getPlayer().getPositionMs()) * getPlayer().getSpeed()));

        dialogPresenter.enableTransparent(true);
        dialogPresenter.enableOverlay(true);
        dialogPresenter.enableExpandable(false);
        dialogPresenter.setId(CONTENT_BLOCK_ID);
        dialogPresenter.showDialog(getContext().getString(R.string.content_block_provider));
    }

    private List<SeekBarSegment> toSeekBarSegments(List<SponsorSegment> segments) {
        if (segments == null) {
            return null;
        }

        List<SeekBarSegment> result = new ArrayList<>();

        for (SponsorSegment sponsorSegment : segments) {
            if (!getSponsorBlockData().isColorMarkerEnabled(sponsorSegment.getCategory())) {
                continue;
            }

            SeekBarSegment seekBarSegment = new SeekBarSegment();
            float startRatio = (float) sponsorSegment.getStartMs() / getPlayer().getDurationMs(); // Range: [0, 1]
            float endRatio = (float) sponsorSegment.getEndMs() / getPlayer().getDurationMs(); // Range: [0, 1]
            seekBarSegment.startProgress = startRatio;
            seekBarSegment.endProgress = endRatio;
            seekBarSegment.color = ContextCompat.getColor(getContext(), getSponsorBlockData().getColorRes(sponsorSegment.getCategory()));
            result.add(seekBarSegment);
        }

        return result;
    }

    /**
     * Sponsor block fix. Position may exceed real media length.
     */
    private void setPositionMs(long positionMs) {
        long durationMs = getPlayer().getDurationMs();

        // Sponsor block fix. Position may exceed real media length.
        getPlayer().setPositionMs(Math.min(positionMs, durationMs));
    }

    /**
     * @param fullMatch Match only the beginning or the full segment length
     */
    private List<SponsorSegment> findMatchedSegments(long positionMs, List<SponsorSegment> segments, boolean fullMatch) {
        if (segments == null) {
            return null;
        }

        List<SponsorSegment> foundSegment = null;

        for (SponsorSegment segment : segments) {
            int action = getSponsorBlockData().getAction(segment.getCategory());
            boolean isSkipAction = action == SponsorBlockData.ACTION_SKIP_ONLY ||
                    action == SponsorBlockData.ACTION_SKIP_WITH_TOAST;
            if (foundSegment == null) {
                if (isPositionInsideSegment(positionMs, segment, fullMatch)) {
                    foundSegment = new ArrayList<>();
                    foundSegment.add(segment);

                    // Action grouping aren't supported for dialogs
                    if (!isSkipAction) {
                        break;
                    }
                }
            } else {
                SponsorSegment lastSegment = foundSegment.get(foundSegment.size() - 1);
                if (isSkipAction && isPositionInsideSegment(lastSegment.getEndMs() + 3_000, segment, fullMatch)) {
                    foundSegment.add(segment);
                }
            }
        }

        return foundSegment;
    }

    private void applyActions(List<SponsorSegment> foundSegments) {
        if (getPlayer() == null || foundSegments == null) {
            mLastSkipPosMs = 0;
            return;
        }

        SponsorSegment lastSegment = foundSegments.get(foundSegments.size() - 1);

        Integer resId = getSponsorBlockData().getLocalizedRes(lastSegment.getCategory());
        String skipMessage = resId != null ? getContext().getString(resId) : lastSegment.getCategory();

        int type = getSponsorBlockData().getAction(lastSegment.getCategory());

        long skipPosMs = lastSegment.getEndMs();
        // Fix infinite skip loop by ignoring short segments. TextureView has a seek bug.
        long skipDurationMs = Math.min(skipPosMs, getPlayer().getDurationMs()) - getPlayer().getPositionMs();
        boolean isDurationIgnored = (skipDurationMs < 10_000 && getPlayerTweaksData().isTextureViewEnabled())
                || (skipDurationMs < getSponsorBlockData().getIgnoredDurationMs());

        if (!isDurationIgnored) {
            if (type == SponsorBlockData.ACTION_SKIP_ONLY || getPlayer().isInPIPMode() || Utils.isScreenOff(getContext()) || isEmbedPlayer()) {
                simpleSkip(skipPosMs);
            } else if (type == SponsorBlockData.ACTION_SKIP_WITH_TOAST) {
                messageSkip(skipPosMs, skipMessage, lastSegment);
            } else if (type == SponsorBlockData.ACTION_SHOW_DIALOG) {
                confirmSkip(skipPosMs, skipMessage);
            }
        }

        mLastSkipPosMs = skipPosMs;
    }

    private void closeTransparentDialog() {
        AppDialogPresenter dialogPresenter = AppDialogPresenter.instance(getContext());

        if (dialogPresenter.isDialogShown() && dialogPresenter.getId() == CONTENT_BLOCK_ID) {
            dialogPresenter.closeDialog();
        }
    }

    private boolean isChannelExcluded(String channelId) {
        return !mSkipExclude && getSponsorBlockData().isChannelExcluded(channelId);
    }
}
