package com.liskovsoft.smartyoutubetv2.tv.ui.playback.other;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.media.PlaybackGlueHost;
import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.media.PlayerAdapter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.PlaybackControlsRow.MultiAction;
import androidx.leanback.widget.PlaybackRowPresenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerTweaksData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tweaks.MaxControlsVideoPlayerGlue;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tweaks.PlaybackTransportRowPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.widget.OnActionLongClickedListener;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.AFRAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.ActionHelpers;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.ChannelAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.ChatAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.ClosedCaptioningAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.ContentBlockAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.FlipAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.HighQualityAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.MoreAction;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.RotateAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.ScreenDimmingAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.SeekIntervalAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.ShareAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.SoundOffAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.VideoInfoAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.PipAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.PlaybackQueueAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.PlaylistAddAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.PlaybackModeAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.SearchAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.SubscribeAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.TwoStateAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.ThumbsDownAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.ThumbsUpAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.VideoSpeedAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.VideoStatsAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.VideoZoomAction;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages customizing the actions in the {@link PlaybackControlsRow}. Adds and manages the
 * following actions to the primary and secondary controls:
 *
 * <ul>
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.RepeatAction}
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.ThumbsDownAction}
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.ThumbsUpAction}
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.SkipPreviousAction}
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.SkipNextAction}
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.FastForwardAction}
 *   <li>{@link androidx.leanback.widget.PlaybackControlsRow.RewindAction}
 * </ul>
 *
 * Note that the superclass, {@link PlaybackTransportControlGlue}, manages the playback controls
 * row.
 */
public class VideoPlayerGlue extends MaxControlsVideoPlayerGlue<PlayerAdapter> implements OnActionLongClickedListener {
    private static final long TEN_SECONDS = TimeUnit.SECONDS.toMillis(10);
    private static final String TAG = VideoPlayerGlue.class.getSimpleName();
    private final PlaybackControlsRow.SkipPreviousAction mSkipPreviousAction;
    private final PlaybackControlsRow.SkipNextAction mSkipNextAction;
    private final PlaybackControlsRow.FastForwardAction mFastForwardAction;
    private final PlaybackControlsRow.RewindAction mRewindAction;
    private final Map<Integer, Action> mActions = new HashMap<>();
    // CLEAN UI: buttons that are switched off in the settings stay reachable through the "More" button.
    // Pairs of {button flag, action id}, in the order of the list.
    private static final int[][] MORE_ACTIONS = {
            {PlayerTweaksData.PLAYER_BUTTON_PLAYBACK_QUEUE, R.id.action_playback_queue},
            {PlayerTweaksData.PLAYER_BUTTON_ADD_TO_PLAYLIST, R.id.action_playlist_add},
            {PlayerTweaksData.PLAYER_BUTTON_VIDEO_INFO, R.id.action_info},
            {PlayerTweaksData.PLAYER_BUTTON_OPEN_CHANNEL, R.id.action_channel},
            {PlayerTweaksData.PLAYER_BUTTON_CHAT, R.id.action_chat},
            {PlayerTweaksData.PLAYER_BUTTON_REPEAT_MODE, R.id.action_repeat},
            {PlayerTweaksData.PLAYER_BUTTON_VIDEO_SPEED, R.id.action_video_speed},
            {PlayerTweaksData.PLAYER_BUTTON_HIGH_QUALITY, R.id.lb_control_high_quality},
            {PlayerTweaksData.PLAYER_BUTTON_SUBTITLES, R.id.lb_control_closed_captioning},
            {PlayerTweaksData.PLAYER_BUTTON_LIKE, R.id.action_thumbs_up},
            {PlayerTweaksData.PLAYER_BUTTON_DISLIKE, R.id.action_thumbs_down},
            {PlayerTweaksData.PLAYER_BUTTON_SUBSCRIBE, R.id.action_subscribe},
            {PlayerTweaksData.PLAYER_BUTTON_PIP, R.id.action_pip},
            {PlayerTweaksData.PLAYER_BUTTON_SEARCH, R.id.action_search},
            {PlayerTweaksData.PLAYER_BUTTON_SHARE, R.id.action_share},
            {PlayerTweaksData.PLAYER_BUTTON_CONTENT_BLOCK, R.id.action_content_block},
            {PlayerTweaksData.PLAYER_BUTTON_VIDEO_STATS, R.id.action_video_stats},
            {PlayerTweaksData.PLAYER_BUTTON_SEEK_INTERVAL, R.id.action_seek_interval},
            {PlayerTweaksData.PLAYER_BUTTON_VIDEO_ZOOM, R.id.action_video_zoom},
            {PlayerTweaksData.PLAYER_BUTTON_VIDEO_ROTATE, R.id.action_rotate},
            {PlayerTweaksData.PLAYER_BUTTON_VIDEO_FLIP, R.id.action_flip},
            {PlayerTweaksData.PLAYER_BUTTON_SOUND_OFF, R.id.action_sound_off},
            {PlayerTweaksData.PLAYER_BUTTON_SCREEN_DIMMING, R.id.action_screen_dimming},
            {PlayerTweaksData.PLAYER_BUTTON_AFR, R.id.action_afr}
    };
    private final Set<Integer> mDisabledActionIds = new HashSet<>();
    private final Action mMoreAction;
    private final OnActionClickedListener mActionListener;
    private final PlayerTweaksData mPlayerTweaksData;
    private final GeneralData mGeneralData;
    private int mPreviousAction = KeyEvent.ACTION_UP;

    public VideoPlayerGlue(
            Context context,
            PlayerAdapter playerAdapter,
            OnActionClickedListener actionListener) {
        super(context, playerAdapter);

        mPlayerTweaksData = PlayerTweaksData.instance(getContext());
        mGeneralData = GeneralData.instance(getContext());

        mActionListener = actionListener;

        mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(context);
        mSkipNextAction = new PlaybackControlsRow.SkipNextAction(context);
        mFastForwardAction = new PlaybackControlsRow.FastForwardAction(context);
        mRewindAction = new PlaybackControlsRow.RewindAction(context);

        ThumbsUpAction thumbsUpAction = new ThumbsUpAction(context);
        ThumbsDownAction thumbsDownAction = new ThumbsDownAction(context);
        thumbsUpAction.setBoundAction(thumbsDownAction);
        thumbsDownAction.setBoundAction(thumbsUpAction);
        putAction(thumbsUpAction);
        putAction(thumbsDownAction);

        putAction(new HighQualityAction(context));
        putAction(new PlaybackModeAction(context));
        putAction(new ChannelAction(context));
        putAction(new ClosedCaptioningAction(context));
        putAction(new PlaylistAddAction(context));
        putAction(new SubscribeAction(context));
        putAction(new VideoInfoAction(context));
        putAction(new VideoSpeedAction(context));
        putAction(new VideoStatsAction(context));
        putAction(new SearchAction(context));
        putAction(new PipAction(context));
        putAction(new AFRAction(context));
        putAction(new ScreenDimmingAction(context));
        putAction(new ChatAction(context));
        putAction(new PlaybackQueueAction(context));
        putAction(new ContentBlockAction(context));
        putAction(new ShareAction(context));
        putAction(new SeekIntervalAction(context));
        putAction(new VideoZoomAction(context));
        putAction(new RotateAction(context));
        putAction(new FlipAction(context));
        putAction(new SoundOffAction(context));

        mMoreAction = new MoreAction(context);
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter adapter) {
        // Order matters, super.onCreatePrimaryActions() will create the play / pause action.
        // Will display as follows:
        // play/pause, previous, rewind, fast forward, next
        //   > /||      |<        <<        >>         >|
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_PLAY_PAUSE)) {
            super.onCreatePrimaryActions(adapter);
        }

        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_PREVIOUS)) {
            adapter.add(mSkipPreviousAction);
        }
        //adapter.add(mRewindAction);
        //adapter.add(mFastForwardAction);
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_NEXT)) {
            adapter.add(mSkipNextAction);
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_REPEAT_MODE)) {
            adapter.add(mActions.get(R.id.action_repeat));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_VIDEO_SPEED)) {
            adapter.add(mActions.get(R.id.action_video_speed));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_PIP)) {
            adapter.add(mActions.get(R.id.action_pip));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_SCREEN_DIMMING)) {
            adapter.add(mActions.get(R.id.action_screen_dimming));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_CHAT)) {
            adapter.add(mActions.get(R.id.action_chat));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_SEARCH)) {
            adapter.add(mActions.get(R.id.action_search));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_SHARE)) {
            adapter.add(mActions.get(R.id.action_share));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_SEEK_INTERVAL)) {
            adapter.add(mActions.get(R.id.action_seek_interval));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_VIDEO_ZOOM)) {
            adapter.add(mActions.get(R.id.action_video_zoom));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_VIDEO_ROTATE)) {
            adapter.add(mActions.get(R.id.action_rotate));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_VIDEO_FLIP)) {
            adapter.add(mActions.get(R.id.action_flip));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_SOUND_OFF)) {
            adapter.add(mActions.get(R.id.action_sound_off));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_AFR)) {
            adapter.add(mActions.get(R.id.action_afr));
        }
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter adapter) {
        // Does nothing
        super.onCreateSecondaryActions(adapter);

        // MAX: 7 items. But with custom modification it supports more.
        // Origin: {@link androidx.leanback.widget.ControlBarPresenter#MAX_CONTROLS}
        // Custom mod: {@link com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.ControlBarPresenter#MAX_CONTROLS}

        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_HIGH_QUALITY)) {
            adapter.add(mActions.get(R.id.lb_control_high_quality));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_OPEN_CHANNEL)) {
            adapter.add(mActions.get(R.id.action_channel));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_LIKE)) {
            adapter.add(mActions.get(R.id.action_thumbs_up));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_DISLIKE)) {
            adapter.add(mActions.get(R.id.action_thumbs_down));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_SUBTITLES)) {
            adapter.add(mActions.get(R.id.lb_control_closed_captioning));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_ADD_TO_PLAYLIST)) {
            adapter.add(mActions.get(R.id.action_playlist_add));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_SUBSCRIBE)) {
            adapter.add(mActions.get(R.id.action_subscribe));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_VIDEO_INFO)) {
            adapter.add(mActions.get(R.id.action_info));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_PLAYBACK_QUEUE)) {
            adapter.add(mActions.get(R.id.action_playback_queue));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_CONTENT_BLOCK)) {
            adapter.add(mActions.get(R.id.action_content_block));
        }
        if (mPlayerTweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_VIDEO_STATS)) {
            adapter.add(mActions.get(R.id.action_video_stats));
        }
        // CLEAN UI: everything that has no button on the bar
        if (!getMoreActions().isEmpty()) {
            adapter.add(mMoreAction);
        }
    }

    @Override
    protected PlaybackRowPresenter onCreateRowPresenter() {
        PlaybackRowPresenter rowPresenter = super.onCreateRowPresenter();

        ((PlaybackTransportRowPresenter) rowPresenter).setOnActionLongClickedListener(this);

        return rowPresenter;
    }

    @Override
    public void onActionClicked(Action action) {
        if (!dispatchAction(action)) {
            // Super class handles play/pause and delegates to abstract methods next()/previous().
            super.onActionClicked(action);
        }
    }

    @Override
    public boolean onActionLongClicked(Action action) {
        return dispatchLongClickAction(action);
    }

    @Override
    public void play() {
        super.play();
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void next() {
        mActionListener.onNext();
    }

    @Override
    public void previous() {
        mActionListener.onPrevious();
    }

    public void togglePlayback() {
        if (isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    /** Skips backwards 10 seconds. */
    public void rewind() {
        if (isSeekBlocked()) {
            return;
        }

        long newPosition = getCurrentPosition() - TEN_SECONDS;
        newPosition = (newPosition < 0) ? 0 : newPosition;
        getPlayerAdapter().seekTo(newPosition);
    }

    /** Skips forward 10 seconds. */
    public void fastForward() {
        if (!isSeekBlocked() && getDuration() > -1) {
            long newPosition = getCurrentPosition() + TEN_SECONDS;
            newPosition = Math.min(newPosition, getDuration());
            getPlayerAdapter().seekTo(newPosition);
        }
    }

    public int getButtonState(int buttonId) {
        return getActionIndex(mActions.get(buttonId));
    }

    public void setButtonState(int buttonId, int buttonState) {
        setActionIndex(mActions.get(buttonId), buttonState);
    }

    public void setChannelIcon(String iconUrl) {
        ChannelAction channelAction = (ChannelAction) mActions.get(R.id.action_channel);

        if (channelAction == null) {
            return;
        }

        if (iconUrl == null) {
            channelAction.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.action_channel));
            invalidateUi(channelAction);
            return;
        }

        Drawable originIcon = channelAction.getIcon();
        Glide.with(getContext())
                .load(iconUrl)
                .apply(ViewUtil.glideOptions())
                .circleCrop() // resize image
                .into(new SimpleTarget<Drawable>(originIcon.getIntrinsicWidth(), originIcon.getIntrinsicHeight()) {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        channelAction.setIcon(resource);
                        channelAction.setPadding(3);
                        invalidateUi(channelAction);
                    }
                });
    }

    public void setNextTitle(CharSequence title) {
        mSkipNextAction.setLabel1(title != null ? title : getContext().getString(R.string.lb_playback_controls_skip_next));
        invalidateUi(mSkipNextAction);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (!isSingleKeyDown(event.getAction())) {
            return false;
        }

        boolean handled = mActionListener.onKeyDown(keyCode);

        if (!handled) {
            Action action = findAction(keyCode);

            handled = dispatchAction(action);
        }

        // Ignore result to give a chance to handle this event in
        // com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.maxcontrols.PlaybackTransportRowPresenter.ViewHolder
        return handled || super.onKey(v, keyCode, event);
    }

    /**
     * Fixing sticky key press? <br/>
     * Notify key down only when there are paired action available.
     */
    private boolean isSingleKeyDown(int action) {
        boolean result = action == KeyEvent.ACTION_DOWN && mPreviousAction == KeyEvent.ACTION_UP;
        mPreviousAction = action;
        return result;
    }

    private boolean dispatchAction(Action action) {
        if (action == null) {
            return false;
        }

        if (checkShortActionDisabled(action)) {
            return true;
        }

        boolean handled = false;

        // Primary actions are handled manually.
        if (action == mRewindAction) {
            rewind();
            handled = true;
        } else if (action == mFastForwardAction) {
            fastForward();
            handled = true;
        } else if (action == mMoreAction) {
            showMoreDialog();
            handled = true;
        } else if (mActions.containsKey((int) action.getId())) {
            mActionListener.onAction((int) action.getId(), getActionIndex(action));
            handled = true;
        }

        if (handled) {
            invalidateUi(action);

            if (action instanceof TwoStateAction) {
                invalidateUi(((TwoStateAction) action).getBoundAction());
            }
        }

        return handled;
    }

    private boolean dispatchLongClickAction(Action action) {
        if (action == null) {
            return false;
        }

        if (checkLongActionDisabled(action)) {
            return false;
        }

        boolean handled = false;

        if (mActions.containsKey((int) action.getId())) {
            mActionListener.onLongAction((int) action.getId(), getActionIndex(action));
            handled = true;
        }

        return handled;
    }

    private void incrementActionIndex(Action action) {
        if (action instanceof PlaybackControlsRow.MultiAction) {
            PlaybackControlsRow.MultiAction multiAction = (PlaybackControlsRow.MultiAction) action;
            multiAction.nextIndex();
        }
    }

    /**
     * Properly handle ui changes of multi-action buttons
     */
    private void invalidateUi(Action action) {
        if (action != null) {
            // Notify adapter of action changes to handle primary actions, such as, play/pause.
            notifyActionChanged(
                    action,
                    (ArrayObjectAdapter) getControlsRow().getPrimaryActionsAdapter());

            // Notify adapter of action changes to handle secondary actions, such as, thumbs up/down and repeat.
            notifyActionChanged(
                    action,
                    (ArrayObjectAdapter) getControlsRow().getSecondaryActionsAdapter());
        }
    }

    private void notifyActionChanged(
            Action action, ArrayObjectAdapter adapter) {
        if (adapter != null) {
            int index = adapter.indexOf(action);
            if (index >= 0) {
                adapter.notifyArrayItemRangeChanged(index, 1);
            }
        }
    }

    private void removePrimaryAction(Action action) {
        ArrayObjectAdapter adapter = (ArrayObjectAdapter) getControlsRow().getPrimaryActionsAdapter();
        if (adapter != null) {
            adapter.remove(action);
        }
    }

    private void removeSecondaryAction(Action action) {
        ArrayObjectAdapter adapter = (ArrayObjectAdapter) getControlsRow().getSecondaryActionsAdapter();
        if (adapter != null) {
            adapter.remove(action);
        }
    }

    private void addPrimaryAction(Action action, int position) {
        ArrayObjectAdapter adapter = (ArrayObjectAdapter) getControlsRow().getPrimaryActionsAdapter();
        addAction(action, position, adapter);
    }

    private void addSecondaryAction(Action action, int position) {
        ArrayObjectAdapter adapter = (ArrayObjectAdapter) getControlsRow().getSecondaryActionsAdapter();
        addAction(action, position, adapter);
    }

    private void addAction(Action action, int position, ArrayObjectAdapter adapter) {
        if (adapter != null) {
            int index = adapter.indexOf(action);
            if (index == -1) {
                int size = adapter.size();
                adapter.add(Math.min(position, size), action);
            }
        }
    }

    private Action findAction(int keyCode) {
        Action action = null;
        PlaybackControlsRow controlsRow = getControlsRow();

        if (controlsRow != null) {
            final ObjectAdapter primaryActionsAdapter = controlsRow.getPrimaryActionsAdapter();
            action = controlsRow.getActionForKeyCode(primaryActionsAdapter, keyCode);

            if (action == null) {
                action = controlsRow.getActionForKeyCode(controlsRow.getSecondaryActionsAdapter(),
                        keyCode);
            }
        }

        return action;
    }

    /**
     * CLEAN UI: actions that have no button on the bar and can be used for the current video.
     */
    private List<Action> getMoreActions() {
        List<Action> result = new ArrayList<>();

        for (int[] item : MORE_ACTIONS) {
            Action action = mActions.get(item[1]);

            if (action != null && action.getLabel1() != null && !mPlayerTweaksData.isPlayerButtonEnabled(item[0])
                    && !mDisabledActionIds.contains(item[1])) {
                result.add(action);
            }
        }

        return result;
    }

    private void showMoreDialog() {
        AppDialogPresenter dialog = AppDialogPresenter.instance(getContext());
        List<OptionItem> options = new ArrayList<>();

        for (Action action : getMoreActions()) {
            options.add(UiOptionItem.from(action.getLabel1(), optionItem -> {
                int shownBefore = dialog.getShowCount();

                dispatchAction(action);

                // A switch has nothing more to show. A button with its own menu has replaced this list by now.
                if (dialog.getShowCount() == shownBefore) {
                    dialog.closeDialog();
                }
            }));
        }

        if (options.isEmpty()) {
            return;
        }

        CharSequence title = getContext().getString(R.string.action_more);
        dialog.appendStringsCategory(title, options);
        dialog.showDialog(title);
    }

    private void putAction(Action action) {
        mActions.put((int) action.getId(), action);
    }

    private int getActionIndex(Action action) {
        if (action instanceof PlaybackControlsRow.MultiAction) {
            PlaybackControlsRow.MultiAction multiAction = (PlaybackControlsRow.MultiAction) action;
            return multiAction.getIndex();
        }

        return 0;
    }

    private void setActionIndex(Action action, int actionIndex) {
        if (action != null) {
            if (actionIndex == -1) {
                mDisabledActionIds.add((int) action.getId());
            } else {
                mDisabledActionIds.remove((int) action.getId());
            }
        }

        if (actionIndex == -1) { // button disabled
            disableAction(action);
        } else if (action instanceof MultiAction) {
            ((MultiAction) action).setIndex(actionIndex);
            invalidateUi(action);
        }
    }

    private void disableAction(Action action) {
        Drawable icon = action.getIcon();
        action.setIcon(ActionHelpers.createDrawable(getContext(), (BitmapDrawable) icon, ActionHelpers.getIconGrayedOutColor(getContext())));
        invalidateUi(action);
    }

    /**
     * Long press actions usually more important than short ones. So, try to use it first in case long click is disabled.
     */
    private boolean checkShortActionDisabled(Action action) {
        if (!mGeneralData.isOkButtonLongPressDisabled() && mPlayerTweaksData.isButtonLongClickEnabled()) {
            return false;
        }

        return (action == mActions.get(R.id.lb_control_closed_captioning) || action == mActions.get(R.id.action_video_speed)) &&
                dispatchLongClickAction(action); // replace short with long
    }

    private boolean checkLongActionDisabled(Action action) {
        if (!mGeneralData.isOkButtonLongPressDisabled() && mPlayerTweaksData.isButtonLongClickEnabled()) {
            return false;
        }

        return action.getId() == R.id.action_chat;
    }

    @Override
    protected void onAttachedToHost(PlaybackGlueHost host) {
        super.onAttachedToHost(host);

        Log.d(TAG, "On attached to host");
    }

    @Override
    public void onTopEdgeFocused() {
        mActionListener.onTopEdgeFocused();
    }

    /** Listens for when skip to next and previous actions have been dispatched. */
    public interface OnActionClickedListener {
        /** Skip to the previous item in the queue. */
        void onPrevious();

        /** Skip to the next item in the queue. */
        void onNext();

        void onPlay();

        void onPause();

        void onAction(int actionId, int actionIndex);

        void onLongAction(int actionId, int actionIndex);

        void onTopEdgeFocused();

        boolean onKeyDown(int keyCode);
    }
}
