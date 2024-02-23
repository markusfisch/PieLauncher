package de.markusfisch.android.pielauncher.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

public class Preferences {
	public static final int DEAD_ZONE_NONE = 0;
	public static final int DEAD_ZONE_TOP = 1;
	public static final int DEAD_ZONE_BOTTOM = 2;
	public static final int DEAD_ZONE_BOTH = 3;
	public static final int SEARCH_STRICTNESS_HAMMING = 1;
	public static final int SEARCH_STRICTNESS_CONTAINS = 2;
	public static final int SEARCH_STRICTNESS_STARTS_WITH = 3;
	public static final int ICON_PRESS_DEFAULT = 0;
	public static final int ICON_PRESS_LONGER = 1;
	public static final int ICON_PRESS_MENU = 2;
	public static final int ICON_LOCK_MENU = 3;

	private static final String SKIP_SETUP = "skip_setup";
	private static final String RADIUS = "radius";
	private static final String ORIENTATION = "orientation";
	private static final String DARKEN_BACKGROUND = "darken_background";
	private static final String BLUR_BACKGROUND = "blur_background";
	private static final String DEAD_ZONE = "dead_zone";
	private static final String DISPLAY_KEYBOARD = "display_keyboard";
	private static final String DOUBE_SPACE_LAUNCH = "space_action_double_launch";
	private static final String AUTO_LAUNCH_MATCHING = "auto_launch_matching";
	private static final String SEARCH_STRICTNESS = "strictness";
	private static final String SHOW_APP_NAMES = "show_app_names";
	private static final String ICON_PRESS = "icon_press";
	private static final String ICON_PACK = "icon_pack";

	private final SharedPreferences preferences;
	private final SystemSettings systemSettings;

	private boolean skipSetup = false;
	private int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
	private boolean darkenBackground = false;
	private boolean blurBackground = false;
	private int deadZone = DEAD_ZONE_BOTH;
	private boolean displayKeyboard = true;
	private boolean doubleSpaceLaunch = false;
	private boolean autoLaunchMatching = false;
	private int searchStrictness = SEARCH_STRICTNESS_HAMMING;
	private boolean showAppNames = true;
	private int iconPress = ICON_PRESS_DEFAULT;
	private String iconPack;

	public Preferences(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		systemSettings = new SystemSettings(context.getContentResolver());

		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		int defaultOrientation = dm.heightPixels > dm.widthPixels
				? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
				: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

		skipSetup = preferences.getBoolean(SKIP_SETUP, skipSetup);
		orientation = preferences.getInt(ORIENTATION, defaultOrientation);
		darkenBackground = preferences.getBoolean(DARKEN_BACKGROUND,
				darkenBackground);
		blurBackground = preferences.getBoolean(BLUR_BACKGROUND,
				blurBackground);
		deadZone = preferences.getInt(DEAD_ZONE, deadZone);
		displayKeyboard = preferences.getBoolean(DISPLAY_KEYBOARD,
				displayKeyboard);
		doubleSpaceLaunch = preferences.getBoolean(DOUBE_SPACE_LAUNCH,
				doubleSpaceLaunch);
		autoLaunchMatching = preferences.getBoolean(AUTO_LAUNCH_MATCHING,
				autoLaunchMatching);
		searchStrictness = preferences.getInt(SEARCH_STRICTNESS,
				searchStrictness);
		showAppNames = preferences.getBoolean(SHOW_APP_NAMES, showAppNames);
		iconPress = preferences.getInt(ICON_PRESS, iconPress);
		iconPack = preferences.getString(ICON_PACK, iconPack);
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

	public int getOrientation() {
		return orientation;
	}

	public void setOrientation(int orientation) {
		this.orientation = orientation;
		put(ORIENTATION, orientation).commit();
	}

	public boolean darkenBackground() {
		return darkenBackground;
	}

	public void setDarkenBackground(boolean darkenBackground) {
		this.darkenBackground = darkenBackground;
		put(DARKEN_BACKGROUND, darkenBackground).apply();
	}

	public boolean blurBackground() {
		return blurBackground;
	}

	public void setBlurBackground(boolean blurBackground) {
		this.blurBackground = blurBackground;
		put(BLUR_BACKGROUND, blurBackground).apply();
	}

	public int getDeadZone() {
		return deadZone;
	}

	public void setDeadZone(int deadZone) {
		this.deadZone = deadZone;
		put(DEAD_ZONE, deadZone).commit();
	}

	public boolean displayKeyboard() {
		return displayKeyboard;
	}

	public void setDisplayKeyboard(boolean displayKeyboard) {
		this.displayKeyboard = displayKeyboard;
		put(DISPLAY_KEYBOARD, displayKeyboard).apply();
	}

	public boolean doubleSpaceLaunch() {
		return doubleSpaceLaunch;
	}

	public void setDoubleSpaceLaunch(boolean doubleSpaceLaunch) {
		this.doubleSpaceLaunch = doubleSpaceLaunch;
		put(DOUBE_SPACE_LAUNCH, doubleSpaceLaunch).apply();
	}

	public boolean autoLaunchMatching() {
		return autoLaunchMatching;
	}

	public void setAutoLaunchMatching(boolean autoLaunchMatching) {
		this.autoLaunchMatching = autoLaunchMatching;
		put(AUTO_LAUNCH_MATCHING, autoLaunchMatching).apply();
	}

	public int getSearchStrictness() {
		return searchStrictness;
	}

	public void setSearchStrictness(int searchStrictness) {
		this.searchStrictness = searchStrictness;
		put(SEARCH_STRICTNESS, searchStrictness).apply();
	}

	public boolean showAppNames() {
		return showAppNames;
	}

	public void setShowAppNames(boolean showAppNames) {
		this.showAppNames = showAppNames;
		put(SHOW_APP_NAMES, showAppNames).apply();
	}

	public int getIconPress() {
		return iconPress;
	}

	public void setIconPress(int iconPress) {
		this.iconPress = iconPress;
		put(ICON_PRESS, iconPress).apply();
	}

	public String getIconPack() {
		return iconPack;
	}

	public void setIconPack(String iconPack) {
		this.iconPack = iconPack;
		put(ICON_PACK, iconPack).apply();
	}

	public float getAnimationDuration() {
		return 200f * systemSettings.getAnimatorDurationScale();
	}

	private SharedPreferences.Editor put(String key, boolean value) {
		return put(editor -> editor.putBoolean(key, value));
	}

	private SharedPreferences.Editor put(String key, int value) {
		return put(editor -> editor.putInt(key, value));
	}

	private SharedPreferences.Editor put(String key, String value) {
		return put(editor -> editor.putString(key, value));
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
