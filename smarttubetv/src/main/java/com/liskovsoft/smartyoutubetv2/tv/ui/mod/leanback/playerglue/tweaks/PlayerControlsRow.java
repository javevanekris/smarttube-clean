package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tweaks;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Holds the two player button bars under the seek bar.<br/>
 * Default: both bars on one line (playback buttons left, action buttons right).<br/>
 * When the enabled buttons don't fit on one line the bars are stacked, like the original two rows.
 */
public class PlayerControlsRow extends LinearLayout {
    private boolean mStacked;
    private View mLastFocusedChild;

    public PlayerControlsRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerControlsRow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            int available = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
            int unspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            int needed = 0;

            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (isBar(child)) {
                    child.measure(unspecified, unspecified);
                    MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                    needed += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                }
            }

            setStacked(needed > available);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void setStacked(boolean stacked) {
        if (mStacked == stacked) {
            return;
        }

        mStacked = stacked;
        setOrientation(stacked ? VERTICAL : HORIZONTAL);

        // The spacers only make sense when the bars are side by side
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (!isBar(child)) {
                child.setVisibility(stacked ? GONE : VISIBLE);
            }
        }
    }

    private static boolean isBar(View child) {
        return child instanceof ViewGroup;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        mLastFocusedChild = child;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (mStacked) {
            // Same as the original rows: enter the nearest bar
            return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
        }

        // Side by side: return to the bar that was used last, otherwise start at the left
        if (mLastFocusedChild != null && mLastFocusedChild.getParent() == this
                && mLastFocusedChild.requestFocus(direction, previouslyFocusedRect)) {
            return true;
        }

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE && child.requestFocus(direction, previouslyFocusedRect)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public View focusSearch(View focused, int direction) {
        if (mStacked && focused != null && (direction == FOCUS_UP || direction == FOCUS_DOWN)) {
            int step = direction == FOCUS_DOWN ? 1 : -1;

            for (int i = indexOfChild(getFocusedChild()) + step; i >= 0 && i < getChildCount(); i += step) {
                View child = getChildAt(i);
                if (child.getVisibility() == VISIBLE && child.hasFocusable()) {
                    return child;
                }
            }
        }

        return super.focusSearch(focused, direction);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
