package com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions;

import android.content.Context;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Action;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * CLEAN UI: one button for everything that has no place on the controls bar.
 */
public class MoreAction extends Action {
    public MoreAction(Context context) {
        super(R.id.action_more);

        setIcon(ContextCompat.getDrawable(context, R.drawable.lb_ic_more));
        setLabel1(context.getString(R.string.action_more));
    }
}
