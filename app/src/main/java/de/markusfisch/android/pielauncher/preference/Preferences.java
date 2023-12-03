package de.markusfisch.android.pielauncher.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import de.markusfisch.android.pielauncher.R;

public class Preferences {
	public enum SearchStrictness {
		HAMMING,
		CONTAINS,
		STARTS_WITH
	}

	private static final String SKIP_SETUP = "skip_setup";
	private static final String RADIUS = "radius";
	private static final String DISPLAY_KEYBOARD = "display_keyboard";
	private static final String SEARCH_STRICTNESS = "search_strictness";
	private static final String AUTO_LAUNCH_MATCHING = "auto_launch_matching";
	private static final String ORIENTATION = "orientation";

	private final SharedPreferences preferences;

	private boolean skipSetup = false;
	private boolean displayKeyboard = true;
	private SearchStrictness searchStrictness = SearchStrictness.HAMMING;
	private boolean autoLaunchMatching = false;
	private int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

	public Preferences(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);

		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		int defaultOrientation = dm.heightPixels > dm.widthPixels
				? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
				: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

		skipSetup = preferences.getBoolean(SKIP_SETUP, skipSetup);
		displayKeyboard = preferences.getBoolean(SKIP_SETUP, displayKeyboard);
		searchStrictness = SearchStrictness.valueOf(
				preferences.getString(SEARCH_STRICTNESS,
						searchStrictness.toString()));
		autoLaunchMatching = preferences.getBoolean(AUTO_LAUNCH_MATCHING,
				autoLaunchMatching);
		orientation = preferences.getInt(ORIENTATION, defaultOrientation);
	}

	public boolean skipSetup() {
		return skipSetup;
	}

	public void setSkipSetup() {
		skipSetup = true;
		put(SKIP_SETUP, true).apply();
	}

	public int getRadius(int preset) {
		return preferences.getInt(RADIUS, preset);
	}

	public void setRadius(int radius) {
		put(RADIUS, radius).apply();
	}

	public boolean displayKeyboard() {
		return displayKeyboard;
	}

	public void setDisplayKeyboard(boolean displayKeyboard) {
		this.displayKeyboard = displayKeyboard;
		put(DISPLAY_KEYBOARD, displayKeyboard).apply();
	}

	public SearchStrictness searchStrictness() {
		return searchStrictness;
	}

	public void setSearchStrictness(SearchStrictness searchStrictness) {
		this.searchStrictness = searchStrictness;
		put(SEARCH_STRICTNESS, searchStrictness.toString()).apply();
	}

	public boolean autoLaunchMatching() {
		return autoLaunchMatching;
	}

	public void setAutoLaunchMatching(boolean autoLaunchMatching) {
		this.autoLaunchMatching = autoLaunchMatching;
		put(AUTO_LAUNCH_MATCHING, autoLaunchMatching).apply();
	}

	public int getOrientation() {
		return orientation;
	}

	public void setOrientation(int orientation) {
		this.orientation = orientation;
		put(ORIENTATION, orientation).commit();
	}

	private SharedPreferences.Editor put(String key, String value) {
		return put(editor -> editor.putString(key, value));
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
