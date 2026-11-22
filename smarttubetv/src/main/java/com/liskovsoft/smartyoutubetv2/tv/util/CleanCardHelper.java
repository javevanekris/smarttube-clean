package com.liskovsoft.smartyoutubetv2.tv.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * CLEAN UI: video cards without a box behind the text.<br/>
 * The thumbnail gets its own rounded corners and a white outline when the card is focused.<br/>
 * NOTE: uses API 21+ classes. Call only after checking the SDK version.
 */
@RequiresApi(21)
public final class CleanCardHelper {
    private CleanCardHelper() {
    }

    /**
     * Rounds all four corners of the thumbnail. Follows the "rounded corners" setting.
     */
    public static void roundThumbnail(View thumbnail) {
        if (thumbnail == null) {
            return;
        }

        final float radius = getCornerRadius(thumbnail.getContext());

        thumbnail.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        thumbnail.setClipToOutline(true);
    }

    /**
     * The outline that marks the focused card. Create one per card.
     */
    public static Drawable createFocusOutline(Context context) {
        GradientDrawable outline = new GradientDrawable();
        outline.setShape(GradientDrawable.RECTANGLE);
        outline.setColor(Color.TRANSPARENT);
        outline.setCornerRadius(getCornerRadius(context));
        outline.setStroke(
                context.getResources().getDimensionPixelSize(R.dimen.card_focus_outline_width),
                ContextCompat.getColor(context, R.color.card_focus_outline));
        return outline;
    }

    private static float getCornerRadius(Context context) {
        return MainUIData.instance(context).isUiTweakEnabled(MainUIData.UI_TWEAK_ROUNDED_CORNERS) ?
                context.getResources().getDimension(R.dimen.lb_rounded_rect_corner_radius) : 0;
    }
}
