package de.markusfisch.android.pielauncher.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

public class Preferences {
	private static final String RADIUS = "radius";
	private static final String ORIENTATION = "orientation";

	private SharedPreferences preferences;
	private int defaultOrientation;

	public void init(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		defaultOrientation = dm.heightPixels > dm.widthPixels
				? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
				: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
	}

	public int getOrientation() {
		return preferences.getInt(ORIENTATION, defaultOrientation);
	}

	public void setOrientation(int orientation) {
		put(ORIENTATION, orientation).commit();
	}

	public int getRadius(int preset) {
		return preferences.getInt(RADIUS, preset);
	}

	public void setRadius(int radius) {
		put(RADIUS, radius).apply();
	}

	private SharedPreferences.Editor put(String key, int value) {
		return put(editor -> editor.putInt(key, value));
	}

	private SharedPreferences.Editor put(PutListener listener) {
		SharedPreferences.Editor editor = preferences.edit();
		listener.put(editor);
		return editor;
	}

	private interface PutListener {
		void put(SharedPreferences.Editor editor);
	}
}
