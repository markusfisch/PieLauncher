package de.markusfisch.android.pielauncher.view;

import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

import de.markusfisch.android.pielauncher.preference.Preferences;

public class SystemBars {
	public interface OnInsetListener {
		void onApplyInsets(int left, int top, int right, int bottom);
	}

	public static void setTransparentSystemBars(Window window) {
		setTransparentSystemBars(window, Preferences.IMMERSIVE_MODE_DISABLED);
	}

	public static void setTransparentSystemBars(Window window, int immersive) {
		if (window == null ||
				Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return;
		}
		// This is important or subsequent (not the very first!) openings of
		// the soft keyboard will reposition the DecorView according to the
		// window insets.
		window.setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		setSystemUIVisibility(window, immersive);
		// System bars can no longer be colored from VANILLA_ICE_CREAM on.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
			window.setStatusBarColor(0);
			window.setNavigationBarColor(0);
		}
	}

	public static void setSystemUIVisibility(Window window, int immersive) {
		if (window == null ||
				Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return;
		}

		int uiFlags = 0;
		boolean hideStatusBar =
				(immersive & Preferences.IMMERSIVE_MODE_STATUS_BAR) > 0;
		boolean hideNavigationBar =
				(immersive & Preferences.IMMERSIVE_MODE_NAVIGATION_BAR) > 0;
		boolean hideAnyBar = hideStatusBar || hideNavigationBar;
		if (hideStatusBar) {
			uiFlags |= View.SYSTEM_UI_FLAG_FULLSCREEN;
		}
		if (hideNavigationBar) {
			uiFlags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
		}
		if (hideAnyBar) {
			uiFlags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		}
		window.getDecorView().setSystemUiVisibility(uiFlags |
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

		if (hideAnyBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			WindowManager.LayoutParams params = window.getAttributes();
			params.layoutInDisplayCutoutMode =
					WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
			window.setAttributes(params);
		}
	}

	public static void setNavigationBarColor(Window window, int color) {
		if (window == null ||
				// setNavigationBarColor() was added in LOLLIPOP.
				Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ||
				// setNavigationBarColor() has no effect over UPSIDE_DOWN_CAKE.
				Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				Insets systemBarsInsets = insets.getInsets(
						WindowInsets.Type.systemBars());
				listener.onApplyInsets(
						systemBarsInsets.left,
						systemBarsInsets.top,
						systemBarsInsets.right,
						systemBarsInsets.bottom);
				return insets;
			} else {
				//noinspection deprecation
				if (insets.hasSystemWindowInsets()) {
					//noinspection deprecation
					listener.onApplyInsets(
							insets.getSystemWindowInsetLeft(),
							insets.getSystemWindowInsetTop(),
							insets.getSystemWindowInsetRight(),
							insets.getSystemWindowInsetBottom());
				}
				//noinspection deprecation
				return insets.consumeSystemWindowInsets();
			}
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
