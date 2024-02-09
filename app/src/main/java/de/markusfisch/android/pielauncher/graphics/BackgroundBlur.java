package de.markusfisch.android.pielauncher.graphics;

import android.os.Build;
import android.view.Window;

public class BackgroundBlur {
	public static final int BLUR = 20;

	public static boolean canBlur(Window window) {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
				window.getWindowManager().isCrossWindowBlurEnabled();
	}

	public static void blurIfTrue(Window window, boolean blur) {
		if (blur) {
			setBlur(window, true);
		}
	}

	public static void setBlur(Window window, boolean blur) {
		setBlurRadius(window, blur ? BLUR : 0);
	}

	public static void setBlurRadius(Window window, int radius) {
		if (window != null && canBlur(window)) {
			window.setBackgroundBlurRadius(radius);
		}
	}
}
