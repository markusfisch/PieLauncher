package de.markusfisch.android.pielauncher.os;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.widget.TextView;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;

public class Orientation {
	public static void setOrientation(Activity activity,
			TextView orientationView) {
		int newOrientation = PieLauncherApp.prefs.getOrientation() ==
				ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
				? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
				: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		PieLauncherApp.prefs.setOrientation(newOrientation);
		setOrientationText(orientationView, newOrientation);
		activity.setRequestedOrientation(newOrientation);
	}

	public static void setOrientationText(TextView tv, int orientation) {
		tv.setText(getOrientationResId(orientation));
	}

	public static int getOrientationResId(int orientation) {
		switch (orientation) {
			default:
				return R.string.orientation_default;
			case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
				return R.string.orientation_landscape;
			case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
				return R.string.orientation_portrait;
		}
	}
}
