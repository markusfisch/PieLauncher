package de.markusfisch.android.pielauncher.view;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class SystemBars {
	public interface OnInsetListener {
		void onApplyInsets(int left, int top, int right, int bottom);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static void setTransparentSystemBars(Window window) {
		if (window == null ||
				Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return;
		}
		// This is important or subsequent (not the very first!) openings of
		// the soft keyboard will reposition the DecorView according to the
		// window insets.
		window.setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		window.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
						View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		window.setStatusBarColor(0);
		window.setNavigationBarColor(0);
	}

	public static void setNavigationBarColor(Window window, int color) {
		if (window == null ||
				Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return;
		}
		window.setNavigationBarColor(color);
	}

	public static void listenForWindowInsets(View view,
			OnInsetListener listener) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) {
			return;
		}
		view.setOnApplyWindowInsetsListener((v, insets) -> {
			if (insets.hasSystemWindowInsets()) {
				listener.onApplyInsets(
						insets.getSystemWindowInsetLeft(),
						insets.getSystemWindowInsetTop(),
						insets.getSystemWindowInsetRight(),
						insets.getSystemWindowInsetBottom());
			}
			return insets.consumeSystemWindowInsets();
		});
	}

	public static void addPaddingFromWindowInsets(
			View toolbar, View content) {
		Rect contentPadding = new Rect(
				content.getPaddingLeft(),
				content.getPaddingTop(),
				content.getPaddingRight(),
				content.getPaddingBottom());
		final int toolbarHeight;
		final Rect toolbarPadding;
		if (toolbar == null) {
			toolbarHeight = 0;
			toolbarPadding = null;
		} else {
			toolbar.measure(0, 0);
			toolbarHeight = toolbar.getMeasuredHeight();
			toolbarPadding = new Rect(
					toolbar.getPaddingLeft(),
					toolbar.getPaddingTop(),
					toolbar.getPaddingRight(),
					toolbar.getPaddingBottom());
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) {
			content.setPadding(
					contentPadding.left,
					contentPadding.top + toolbarHeight,
					contentPadding.right,
					contentPadding.bottom);
			return;
		}
		SystemBars.listenForWindowInsets(
				content,
				(left, top, right, bottom) -> {
					content.setPadding(
							contentPadding.left + left,
							contentPadding.top + top + toolbarHeight,
							contentPadding.right + right,
							contentPadding.bottom + bottom);
					if (toolbar != null) {
						toolbar.setPadding(
								toolbarPadding.left + left,
								toolbarPadding.top + top,
								toolbarPadding.right + right,
								// Skip bottom inset because the toolbar
								// doesn't touch the lower edge.
								toolbarPadding.bottom);
					}
				});
	}
}
