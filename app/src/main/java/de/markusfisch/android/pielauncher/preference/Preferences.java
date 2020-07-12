package de.markusfisch.android.pielauncher.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
	private static final String RADIUS = "radius";

	private SharedPreferences preferences;

	public void init(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public int getRadius(int preset) {
		return preferences.getInt(RADIUS, preset);
	}

	public void setRadius(int radius) {
		apply(RADIUS, radius);
	}

	private void apply(String key, int value) {
		put(key, value).apply();
	}

	private SharedPreferences.Editor put(String key, int value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(key, value);
		return editor;
	}
}
