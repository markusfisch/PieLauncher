package de.markusfisch.android.pielauncher.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
	private static final String RADIUS = "radius";
	private static final String ORIENTATION = "orientation";

	private SharedPreferences preferences;

	public void init(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public int getRadius(int preset) {
		// Somehow preferences can be null here. Frankly, I have no idea how
		// that can possibly happen and I never ever saw it on one of my
		// devices but there is at least one NPE in Play's crash logging.
		return preferences != null
				? preferences.getInt(RADIUS, preset)
				: preset;
	}

	public int getOrientation(int preset) {
		return preferences != null
				? preferences.getInt(ORIENTATION, preset)
				: preset;
	}

	public void setOrientation(int orientation) {
		apply(ORIENTATION, orientation);
	}

	public void setRadius(int radius) {
		apply(RADIUS, radius);
	}

	private void apply(String key, int value) {
		if (preferences != null) {
			put(key, value).apply();
		}
	}

	private SharedPreferences.Editor put(String key, int value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(key, value);
		return editor;
	}
}
