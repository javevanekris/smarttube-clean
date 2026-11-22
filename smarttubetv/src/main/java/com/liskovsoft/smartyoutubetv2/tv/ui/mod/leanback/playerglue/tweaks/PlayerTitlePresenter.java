package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tweaks;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.framedrops.PlaybackBaseControlGlue;

/**
 * Compact title block for the player overlay: the video title and one info line below it.<br/>
 * Replaces the large leanback details description.
 */
public class PlayerTitlePresenter extends Presenter {
    public static class ViewHolder extends Presenter.ViewHolder {
        private final TextView mTitle;
        private final TextView mSubtitle;
        private final TextView mChapter;

        public ViewHolder(View view) {
            super(view);
            mTitle = (TextView) view.findViewById(R.id.player_title);
            mSubtitle = (TextView) view.findViewById(R.id.player_subtitle);
            mChapter = (TextView) view.findViewById(R.id.player_chapter);
        }

        /**
         * The title of the current chapter. Hidden when the video has no chapters.
         */
        public void setChapter(CharSequence chapter) {
            bindText(mChapter, chapter);
        }
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.player_title, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ViewHolder vh = (ViewHolder) viewHolder;
        PlaybackBaseControlGlue<?> glue = (PlaybackBaseControlGlue<?>) item;

        bindText(vh.mTitle, glue.getTitle());
        bindText(vh.mSubtitle, glue.getSubtitle());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        // NOP
    }

    private static void bindText(TextView view, CharSequence text) {
        view.setText(text);
        view.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }
}
