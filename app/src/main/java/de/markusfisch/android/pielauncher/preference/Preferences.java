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
	public static final int DEAD_ZONE_TOP_BOTTOM = 3;
	public static final int DEAD_ZONE_ALL = 4;
	public static final int OPEN_LIST_WITH_TAP = 0;
	public static final int OPEN_LIST_WITH_ANY_TOUCH = 1;
	public static final int OPEN_LIST_WITH_ICON = 2;
	public static final int OPEN_LIST_WITH_LONG_PRESS = 3;
	public static final int OPEN_LIST_WITH_DOUBLE_TAP = 4;
	public static final int SEARCH_STRICTNESS_HAMMING = 1;
	public static final int SEARCH_STRICTNESS_CONTAINS = 2;
	public static final int SEARCH_STRICTNESS_STARTS_WITH = 3;
	public static final int SEARCH_PARAMETER_APP_LABEL = 0;
	public static final int SEARCH_PARAMETER_PACKAGE_NAME = 1;
	public static final int SHOW_APP_NAMES_ALWAYS = 0;
	public static final int SHOW_APP_NAMES_SEARCH = 1;
	public static final int SHOW_APP_NAMES_NEVER = 2;
	public static final int ICON_PRESS_DEFAULT = 0;
	public static final int ICON_PRESS_LONGER = 1;
	public static final int ICON_PRESS_MENU = 2;
	public static final int ICON_LOCK_MENU = 3;
	public static final int IMMERSIVE_MODE_DISABLED = 0;
	public static final int IMMERSIVE_MODE_STATUS_BAR = 1;
	public static final int IMMERSIVE_MODE_NAVIGATION_BAR = 2;
	public static final int IMMERSIVE_MODE_FULL = 3;
	public static final int HAPTIC_FEEDBACK_FOLLOW_SYSTEM = 0;
	public static final int HAPTIC_FEEDBACK_DISABLE_LAUNCH = 1;
	public static final int HAPTIC_FEEDBACK_DISABLE_ALL = 2;

	private static final String SKIP_SETUP = "skip_setup";
	private static final String RADIUS = "radius";
	private static final String TWIST = "twist";
	private static final String ICON_SCALE = "icon_scale";
	private static final String ORIENTATION = "orientation";
	private static final String DARKEN_BACKGROUND = "darken_background";
	private static final String BLUR_BACKGROUND = "blur_background";
	private static final String DEAD_ZONE = "dead_zone";
	private static final String IMMERSIVE_MODE = "immersive_mode_option";
	private static final String ANIMATE_IN_OUT = "animate_in_out";
	private static final String OPEN_LIST_WITH = "open_list_with";
	private static final String DISPLAY_KEYBOARD = "display_keyboard";
	private static final String DOUBLE_SPACE_LAUNCH = "space_action_double_launch";
	private static final String AUTO_LAUNCH_MATCHING = "auto_launch_matching";
	private static final String SEARCH_STRICTNESS = "strictness";
	private static final String SEARCH_PARAMETER = "search_parameter";
	private static final String SHOW_APP_NAMES = "show_app_names";
	private static final String ICON_PRESS = "icon_press";
	private static final String EXCLUDE_PIE = "exclude_pie";
	private static final String ICON_PACK = "icon_pack";
	private static final String HAPTIC_FEEDBACK = "haptic_feedback";
	private static final String USE_LIGHT_DIALOGS = "use_light_dialogs";
	private static final String FORCE_RELAUNCH = "force_relaunch";

	private final SharedPreferences preferences;
	private final SystemSettings systemSettings;

	private boolean skipSetup = false;
	private float twist = 0f;
	private float iconScale = 1f;
	private int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
	private boolean darkenBackground = false;
	private boolean blurBackground = false;
	private int deadZone = DEAD_ZONE_TOP_BOTTOM;
	private int immersiveMode = IMMERSIVE_MODE_DISABLED;
	private boolean animateInOut = true;
	private int hapticFeedback = HAPTIC_FEEDBACK_FOLLOW_SYSTEM;
	private int openListWith = OPEN_LIST_WITH_TAP;
	private boolean displayKeyboard = true;
	private boolean doubleSpaceLaunch = false;
	private boolean autoLaunchMatching = false;
	private int searchStrictness = SEARCH_STRICTNESS_HAMMING;
	private int searchParameter = SEARCH_PARAMETER_APP_LABEL;
	private int showAppNames = SHOW_APP_NAMES_SEARCH;
	private boolean excludePie = false;
	private int iconPress = ICON_PRESS_DEFAULT;
	private String iconPack;
	private boolean useLightDialogs = false;
	private boolean forceRelaunch = false;

	public Preferences(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		systemSettings = new SystemSettings(context.getContentResolver());

		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		int defaultOrientation = dm.heightPixels > dm.widthPixels
				? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
				: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

		// Migrate old immersive mode setting.
		String oldImmersiveModeName = "immersive_mode";
		if (preferences.getBoolean(oldImmersiveModeName, false)) {
			put(oldImmersiveModeName, false).apply();
			immersiveMode = IMMERSIVE_MODE_FULL;
			setImmersiveMode(immersiveMode);
		}

		skipSetup = preferences.getBoolean(SKIP_SETUP, skipSetup);
		twist = preferences.getFloat(TWIST, twist);
		iconScale = preferences.getFloat(ICON_SCALE, iconScale);
		orientation = preferences.getInt(ORIENTATION, defaultOrientation);
		darkenBackground = preferences.getBoolean(DARKEN_BACKGROUND,
				darkenBackground);
		blurBackground = preferences.getBoolean(BLUR_BACKGROUND,
				blurBackground);
		deadZone = preferences.getInt(DEAD_ZONE, deadZone);
		immersiveMode = preferences.getInt(IMMERSIVE_MODE, immersiveMode);
		animateInOut = preferences.getBoolean(ANIMATE_IN_OUT, animateInOut);
		openListWith = preferences.getInt(OPEN_LIST_WITH, getOpenListWith());
		displayKeyboard = preferences.getBoolean(DISPLAY_KEYBOARD,
				displayKeyboard);
		doubleSpaceLaunch = preferences.getBoolean(DOUBLE_SPACE_LAUNCH,
				doubleSpaceLaunch);
		autoLaunchMatching = preferences.getBoolean(AUTO_LAUNCH_MATCHING,
				autoLaunchMatching);
		searchStrictness = preferences.getInt(SEARCH_STRICTNESS,
				searchStrictness);
		searchParameter = preferences.getInt(SEARCH_PARAMETER,
				searchParameter);
		showAppNames = preferences.getInt(SHOW_APP_NAMES, showAppNames);
		excludePie = preferences.getBoolean(EXCLUDE_PIE, excludePie);
		iconPress = preferences.getInt(ICON_PRESS, iconPress);
		iconPack = preferences.getString(ICON_PACK, iconPack);
		hapticFeedback = preferences.getInt(HAPTIC_FEEDBACK, hapticFeedback);
		useLightDialogs = preferences.getBoolean(USE_LIGHT_DIALOGS,
				isEReader(context));
		forceRelaunch = preferences.getBoolean(FORCE_RELAUNCH, forceRelaunch);
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

	public float getTwist() {
		return twist;
	}

	public void setTwist(float twist) {
		this.twist = twist;
		put(TWIST, twist).apply();
	}

	public float getIconScale() {
		return iconScale;
	}

	public void setIconScale(float iconScale) {
		this.iconScale = iconScale;
		put(ICON_SCALE, iconScale).apply();
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

	public int getImmersiveMode() {
		return immersiveMode;
	}

	public void setImmersiveMode(int immersiveMode) {
		this.immersiveMode = immersiveMode;
		put(IMMERSIVE_MODE, immersiveMode).commit();
	}

	public boolean animateInOut() {
		return animateInOut;
	}

	public void setAnimateInOut(boolean animateInOut) {
		this.animateInOut = animateInOut;
		put(ANIMATE_IN_OUT, animateInOut).apply();
	}

	public int openListWith() {
		return openListWith;
	}

	public void setOpenListWith(int openListWith) {
		this.openListWith = openListWith;
		put(OPEN_LIST_WITH, openListWith).apply();
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
		put(DOUBLE_SPACE_LAUNCH, doubleSpaceLaunch).apply();
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

	public int getSearchParameter() {
		return searchParameter;
	}

	public void setSearchParameter(int searchParameter) {
		this.searchParameter = searchParameter;
		put(SEARCH_PARAMETER, searchParameter).apply();
	}

	public int showAppNames() {
		return showAppNames;
	}

	public void setShowAppNames(int showAppNames) {
		this.showAppNames = showAppNames;
		put(SHOW_APP_NAMES, showAppNames).apply();
	}

	public boolean excludePie() {
		return excludePie;
	}

	public void setExcludePie(boolean excludePie) {
		this.excludePie = excludePie;
		put(EXCLUDE_PIE, excludePie).apply();
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

	public int hapticFeedback() {
		return hapticFeedback;
	}

	public void setHapticFeedback(int hapticFeedback) {
		this.hapticFeedback = hapticFeedback;
		put(HAPTIC_FEEDBACK, hapticFeedback).commit();
	}

	public boolean useLightDialogs() {
		return useLightDialogs;
	}

	public void setUseLightDialogs(boolean useLightDialogs) {
		this.useLightDialogs = useLightDialogs;
		put(USE_LIGHT_DIALOGS, useLightDialogs).apply();
	}

	public boolean forceRelaunch() {
		return forceRelaunch;
	}

	public void setForceRelaunch(boolean forceRelaunch) {
		this.forceRelaunch = forceRelaunch;
		put(FORCE_RELAUNCH, forceRelaunch).apply();
	}

	public float getAnimationDuration() {
		return 200f * systemSettings.getAnimatorDurationScale();
	}

	private int getOpenListWith() {
		// Initialize from previous setting that existed before
		// versionCode 45. Subject to be removed after a couple
		// of versions.
		String useDrawerIcon = "use_drawer_icon";
		if (preferences.getBoolean(useDrawerIcon, false)) {
			put(useDrawerIcon, false);
			return OPEN_LIST_WITH_ICON;
		}
		return openListWith;
	}

	private boolean isEReader(Context context) {
		return context.getPackageManager().hasSystemFeature(
				"android.hardware.ereader.display");
	}

	private SharedPreferences.Editor put(String key, boolean value) {
		return put(editor -> editor.putBoolean(key, value));
	}

	private SharedPreferences.Editor put(String key, int value) {
		return put(editor -> editor.putInt(key, value));
	}

	private SharedPreferences.Editor put(String key, float value) {
		return put(editor -> editor.putFloat(key, value));
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
