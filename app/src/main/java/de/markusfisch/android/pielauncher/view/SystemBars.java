package de.markusfisch.android.pielauncher.view;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

public class SystemBars {
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

	@TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
	public static void listenForWindowInsets(View view) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) {
			return;
		}
		view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
			@Override
			public WindowInsets onApplyWindowInsets(View v,
					WindowInsets insets) {
				if (insets.hasSystemWindowInsets()) {
					v.setPadding(
							insets.getSystemWindowInsetLeft(),
							// Never set a top padding because the list should
							// appear under the status bar.
							0,
							insets.getSystemWindowInsetRight(),
							insets.getSystemWindowInsetBottom());
				}
				return insets.consumeSystemWindowInsets();
			}
		});
	}
}
