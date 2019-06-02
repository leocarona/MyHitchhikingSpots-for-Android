package com.myhitchhikingspots;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Created by leoboaventura on 23/04/2017.
 */

public class SnackbarWrapper {
    private final CharSequence text;
    private final int duration;
    private final WindowManager windowManager;
    private final Context appplicationContext;
    CoordinatorLayout snackbarContainer;
    FrameLayout frameLayout;
    Snackbar snackbar;

    @Nullable
    private Snackbar.Callback externalCallback;
    @Nullable
    private Action action;

    @NonNull
    public static SnackbarWrapper make(@NonNull Context applicationContext, @NonNull CharSequence text, @Snackbar.Duration int duration) {
        return new SnackbarWrapper(applicationContext, text, duration);
    }

    private SnackbarWrapper(@NonNull final Context appplicationContext, @NonNull CharSequence text, @Snackbar.Duration int duration) {
        this.appplicationContext = appplicationContext;
        this.windowManager = (WindowManager) appplicationContext.getSystemService(Context.WINDOW_SERVICE);
        this.text = text;
        this.duration = duration;
    }

    public void show() {
        WindowManager.LayoutParams layoutParams = createDefaultLayoutParams(WindowManager.LayoutParams.TYPE_TOAST, null);
        if (snackbarContainer != null) {
            windowManager.removeView(snackbarContainer);
            snackbarContainer = null;
        }
        if (frameLayout != null) {
            windowManager.removeView(frameLayout);
        }
        if(snackbar!=null && snackbar.isShown())
            snackbar.dismiss();

        windowManager.addView(new FrameLayout(appplicationContext) {
            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                frameLayout = this;
                onRootViewAvailable(this);
            }

        }, layoutParams);
    }

    public void dismiss(){
        if(snackbar!=null && snackbar.isShown())
            snackbar.dismiss();
    }

    private void onRootViewAvailable(final FrameLayout rootView) {
        CoordinatorLayout snackbarContainer2 = new CoordinatorLayout(new ContextThemeWrapper(appplicationContext, R.style.FOL_Theme_SnackbarWrapper)) {
            @Override
            public void onAttachedToWindow() {
                super.onAttachedToWindow();
                snackbarContainer = this;
                onSnackbarContainerAttached(rootView, this);
            }
        };
        windowManager.addView(snackbarContainer2, createDefaultLayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL, rootView.getWindowToken()));
    }

    private void onSnackbarContainerAttached(final View rootView, final CoordinatorLayout snackbarContainer) {
        if(snackbar==null){ snackbar = Snackbar.make(snackbarContainer, text, duration);
        snackbar.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                // Clean up (NOTE! This callback can be called multiple times)
                if (snackbarContainer.getParent() != null && rootView.getParent() != null) {
                    windowManager.removeView(snackbarContainer);
                    windowManager.removeView(rootView);
                }
                if (externalCallback != null) {
                    externalCallback.onDismissed(snackbar, event);
                }
            }

            @Override
            public void onShown(Snackbar snackbar) {
                super.onShown(snackbar);
                if (externalCallback != null) {
                    externalCallback.onShown(snackbar);
                }
            }
        });
        if (action != null) {
            snackbar.setAction(action.text, action.listener);
        }}
        snackbar.show();
    }

    private WindowManager.LayoutParams createDefaultLayoutParams(int type, @Nullable IBinder windowToken) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.gravity = GravityCompat.getAbsoluteGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, ViewCompat.LAYOUT_DIRECTION_LTR);
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        layoutParams.type = type;
        layoutParams.token = windowToken;
        return layoutParams;
    }

    @NonNull
    public SnackbarWrapper setCallback(@Nullable Snackbar.Callback callback) {
        this.externalCallback = callback;
        return this;
    }

    @NonNull
    public SnackbarWrapper setAction(CharSequence text, final View.OnClickListener listener) {
        action = new Action(text, listener);
        return this;
    }

    private static class Action {
        private final CharSequence text;
        private final View.OnClickListener listener;

        public Action(CharSequence text, View.OnClickListener listener) {
            this.text = text;
            this.listener = listener;
        }
    }
}
