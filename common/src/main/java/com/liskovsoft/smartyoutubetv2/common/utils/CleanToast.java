package com.liskovsoft.smartyoutubetv2.common.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.R;

import java.lang.ref.WeakReference;

/**
 * CLEAN UI: a compact glass notification in the top right corner.<br/>
 * Used instead of the system toast for events during playback (e.g. a skipped segment).
 */
public final class CleanToast {
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static WeakReference<Toast> sLastToast;

    private CleanToast() {
    }

    public static void show(Context ctx, CharSequence text) {
        show(ctx, text, false);
    }

    public static void show(Context ctx, CharSequence text, boolean longDuration) {
        if (ctx == null || TextUtils.isEmpty(text)) {
            return;
        }

        final Context context = ctx.getApplicationContext(); // memory leak fix

        Runnable show = () -> {
            try {
                View view = LayoutInflater.from(context).inflate(R.layout.clean_toast, null);
                ((TextView) view.findViewById(R.id.clean_toast_text)).setText(text);

                int margin = context.getResources().getDimensionPixelSize(R.dimen.clean_toast_margin);

                Toast toast = new Toast(context);
                toast.setView(view);
                toast.setDuration(longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP | Gravity.END, margin, margin);

                Toast last = sLastToast != null ? sLastToast.get() : null;
                if (last != null) {
                    last.cancel();
                }
                sLastToast = new WeakReference<>(toast);

                toast.show();
            } catch (Exception e) {
                // Custom toast views may be unavailable on some firmwares. Use the regular one.
                MessageHelpers.showMessage(context, text.toString());
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            show.run();
        } else {
            sHandler.post(show);
        }
    }
}
