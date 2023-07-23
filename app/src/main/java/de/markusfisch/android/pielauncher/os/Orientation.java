package de.markusfisch.android.pielauncher.os;

import android.app.Activity;
import android.content.pm.ActivityInfo;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;

public class Orientation {
	public static void setOrientation(Activity activity) {
		int newOrientation = PieLauncherApp.prefs.getOrientation() ==
				ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
				? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
				: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		PieLauncherApp.prefs.setOrientation(newOrientation);
		activity.setRequestedOrientation(newOrientation);
	}
}
