package de.markusfisch.android.pielauncher.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

public class Preferences {
	private static final String INTRODUCED = "introduced";
	private static final String RADIUS = "radius";
	private static final String ORIENTATION = "orientation";
	private static final String DISPLAY_KEYBOARD = "display_keyboard";

	private SharedPreferences preferences;
	private boolean introduced = false;
	private int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
	private boolean displayKeyboard = true;

	public void init(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);

		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		int defaultOrientation = dm.heightPixels > dm.widthPixels
				? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
				: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

		introduced = preferences.getBoolean(INTRODUCED, introduced);
		orientation = preferences.getInt(ORIENTATION, defaultOrientation);
		displayKeyboard = preferences.getBoolean(INTRODUCED, displayKeyboard);
	}

	public boolean isIntroduced() {
		return introduced;
	}

	public void setIntroduced() {
		introduced = true;
		put(INTRODUCED, true).apply();
	}

	public int getRadius(int preset) {
		return preferences.getInt(RADIUS, preset);
	}

	public void setRadius(int radius) {
		put(RADIUS, radius).apply();
	}

	public int getOrientation() {
		return orientation;
	}

	public void setOrientation(int orientation) {
		this.orientation = orientation;
		put(ORIENTATION, orientation).apply();
	}

	public boolean displayKeyboard() {
		return displayKeyboard;
	}

	public void setDisplayKeyboard(boolean displayKeyboard) {
		this.displayKeyboard = displayKeyboard;
		put(DISPLAY_KEYBOARD, displayKeyboard).apply();
	}

	private SharedPreferences.Editor put(String key, boolean value) {
		return put(editor -> editor.putBoolean(key, value));
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
