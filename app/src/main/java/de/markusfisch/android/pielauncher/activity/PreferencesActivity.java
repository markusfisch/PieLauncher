package de.markusfisch.android.pielauncher.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.graphics.BackgroundBlur;
import de.markusfisch.android.pielauncher.graphics.ToolbarBackground;
import de.markusfisch.android.pielauncher.os.BatteryOptimization;
import de.markusfisch.android.pielauncher.os.DefaultLauncher;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.view.SystemBars;
import de.markusfisch.android.pielauncher.widget.OptionsDialog;
import de.markusfisch.android.pielauncher.widget.ScrollWithListenerView;

public class PreferencesActivity extends Activity {
	private static final String WELCOME = "welcome";

	private final Handler handler = new Handler(Looper.getMainLooper());

	private Preferences prefs;
	private ToolbarBackground toolbarBackground;
	private View disableBatteryOptimizations;
	private View defaultLauncherView;
	private boolean isWelcomeMode = false;

	public static void startWelcome(Context context) {
		start(context, true);
	}

	public static void start(Context context) {
		start(context, false);
	}

	public static boolean isReady(Context context) {
		return PieLauncherApp.getPrefs(context).skipSetup() ||
				(BatteryOptimization.isIgnoringBatteryOptimizations(context) &&
						DefaultLauncher.isDefault(context));
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_preferences);

		prefs = PieLauncherApp.getPrefs(this);
		BackgroundBlur.blurIfTrue(getWindow(), prefs.blurBackground());
		toolbarBackground = new ToolbarBackground(getResources());

		TextView toolbar = findViewById(R.id.toolbar);
		View skipButton = findViewById(R.id.skip);

		disableBatteryOptimizations = findViewById(
				R.id.disable_battery_optimization);
		defaultLauncherView = findViewById(R.id.make_default_launcher);

		Intent intent = getIntent();
		isWelcomeMode = intent != null &&
				intent.getBooleanExtra(WELCOME, false);

		if (isWelcomeMode) {
			toolbar.setText(R.string.welcome);
			toolbar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

			skipButton.setOnClickListener(view -> {
				prefs.setSkipSetup();
				finish();
			});

			findViewById(R.id.hide_in_welcome_mode).setVisibility(View.GONE);
		} else {
			toolbar.setOnClickListener(v -> finish());
			findViewById(R.id.welcome).setVisibility(View.GONE);

			skipButton.setVisibility(View.GONE);

			initPreferences();
			findViewById(R.id.hidden_apps).setOnClickListener((view) -> {
				HiddenAppsActivity.start(this);
			});
		}

		Window window = getWindow();
		ScrollWithListenerView scrollView = findViewById(R.id.content);
		scrollView.setOnScrollPositionListener((int y, boolean scrollable) -> {
			toolbar.setBackgroundColor(toolbarBackground.getColorForY(y));
			SystemBars.setNavigationBarColor(window,
					scrollable ? toolbarBackground.backgroundColor : 0);
		});
		SystemBars.addPaddingFromWindowInsets(toolbar, scrollView);
		SystemBars.setTransparentSystemBars(window);
	}

	@Override
	protected void onResume() {
		super.onResume();
		setRequestedOrientation(prefs.getOrientation());

		// These may change while this activity is shown.
		boolean disableBatteryVisible = updateDisableBatteryOptimizations();
		boolean updateDefaultVisible = updateDefaultLauncher();
		if (disableBatteryVisible && updateDefaultVisible &&
				// Auto close in welcome mode only.
				isWelcomeMode) {
			finish();
		}

		// Ugly hack to hide the separator between these views when
		// defaultLauncherView is invisible.
		if (disableBatteryOptimizations.getVisibility() == View.VISIBLE &&
				defaultLauncherView.getVisibility() == View.GONE) {
			disableBatteryOptimizations.setBackgroundResource(0);
		}
	}

	private void initPreferences() {
		initPreference(R.id.orientation,
				R.string.orientation,
				PreferencesActivity::getOrientationOptions,
				() -> prefs.getOrientation(),
				(value) -> prefs.setOrientation(value));
		initPreference(R.id.darken_background,
				R.string.darken_background,
				PreferencesActivity::getDarkenBackgroundOptions,
				() -> prefs.darkenBackground(),
				(value) -> prefs.setDarkenBackground(value));
		if (BackgroundBlur.canBlur(getWindow())) {
			initPreference(R.id.blur_background,
					R.string.blur_background,
					PreferencesActivity::getBlurBackgroundOptions,
					() -> prefs.blurBackground(),
					(value) -> {
						prefs.setBlurBackground(value);
						BackgroundBlur.setBlur(getWindow(), value);
					});
		} else {
			findViewById(R.id.blur_background).setVisibility(View.GONE);
		}
		initPreference(R.id.dead_zone,
				R.string.dead_zone,
				PreferencesActivity::getDeadZoneOptions,
				() -> prefs.getDeadZone(),
				(value) -> prefs.setDeadZone(value));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			initPreference(R.id.immersive_mode,
					R.string.immersive_mode,
					PreferencesActivity::getImmersiveModeOptions,
					() -> prefs.getImmersiveMode(),
					(value) -> prefs.setImmersiveMode(value));
		} else {
			findViewById(R.id.immersive_mode).setVisibility(View.GONE);
		}
		initPreference(R.id.animate_in_out,
				R.string.animate_in_out,
				PreferencesActivity::getAnimateInOutOptions,
				() -> prefs.animateInOut(),
				(value) -> prefs.setAnimateInOut(value));
		initPreference(R.id.open_list_with,
				R.string.open_list_with,
				PreferencesActivity::getOpenListWithOptions,
				() -> prefs.openListWith(),
				(value) -> {
					prefs.setOpenListWith(value);
					PieLauncherApp.appMenu.postIndexApps(this);
				});
		initPreference(R.id.display_keyboard,
				R.string.display_keyboard,
				PreferencesActivity::getDisplayKeyboardOptions,
				() -> prefs.displayKeyboard(),
				(value) -> prefs.setDisplayKeyboard(value));
		initPreference(R.id.space_action,
				R.string.space_action,
				PreferencesActivity::getSpaceActionOptions,
				() -> prefs.doubleSpaceLaunch(),
				(value) -> prefs.setDoubleSpaceLaunch(value));
		initPreference(R.id.auto_launch_matching,
				R.string.auto_launch_matching,
				PreferencesActivity::getAutoLaunchMatchingOptions,
				() -> prefs.autoLaunchMatching(),
				(value) -> prefs.setAutoLaunchMatching(value));
		initPreference(R.id.search_strictness,
				R.string.search_strictness,
				PreferencesActivity::getSearchStrictnessOptions,
				() -> prefs.getSearchStrictness(),
				(value) -> prefs.setSearchStrictness(value));
		initPreference(R.id.search_parameter,
				R.string.search_parameter,
				PreferencesActivity::getSearchParameterOptions,
				() -> prefs.getSearchParameter(),
				(value) -> prefs.setSearchParameter(value));
		initPreference(R.id.show_app_names,
				R.string.show_app_names,
				PreferencesActivity::getShowAppNamesOptions,
				() -> prefs.showAppNames(),
				(value) -> prefs.setShowAppNames(value));
		initPreference(R.id.exclude_pie,
				R.string.exclude_pie,
				PreferencesActivity::getExcludePieOptions,
				() -> prefs.excludePie(),
				(value) -> {
					prefs.setExcludePie(value);
					PieLauncherApp.appMenu.postIndexApps(this);
				});
		initPreference(R.id.icon_press,
				R.string.icon_press,
				PreferencesActivity::getIconPressOptions,
				() -> prefs.getIconPress(),
				(value) -> prefs.setIconPress(value));
		initPreference(R.id.icon_pack,
				R.string.icon_pack,
				this::getIconPackOptions,
				() -> prefs.getIconPack(),
				(value) -> {
					prefs.setIconPack(value);
					PieLauncherApp.appMenu.postIndexApps(this);
				},
				() -> PieLauncherApp.iconPack.updatePacks(
						getPackageManager()));
		initPreference(R.id.haptic_feedback,
				R.string.haptic_feedback,
				PreferencesActivity::getHapticFeedbackOptions,
				() -> prefs.hapticFeedback(),
				(value) -> prefs.setHapticFeedback(value));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			initPreference(R.id.use_light_dialogs,
					R.string.use_light_dialogs,
					PreferencesActivity::getUseLightDialogOptions,
					() -> prefs.useLightDialogs(),
					(value) -> prefs.setUseLightDialogs(value));
		} else {
			findViewById(R.id.use_light_dialogs).setVisibility(View.GONE);
		}
		initPreference(R.id.force_relaunch,
				R.string.force_relaunch,
				PreferencesActivity::getForceRelaunchOptions,
				() -> prefs.forceRelaunch(),
				(value) -> prefs.setForceRelaunch(value));
	}

	private <T, G> void initPreference(
			int viewId,
			int titleId,
			GetOptionsListener<T, G> options,
			GetListener<T> getter,
			SetListener<T> setter) {
		initPreference(viewId, titleId, options, getter, setter, null);
	}

	private <T, G> void initPreference(
			int viewId,
			int titleId,
			GetOptionsListener<T, G> options,
			GetListener<T> getter,
			SetListener<T> setter,
			Initializer initializer) {
		TextView tv = findViewById(viewId);
		initPreference(tv, titleId, options, getter, setter, initializer);
	}

	private <T, G> void initPreference(
			TextView tv,
			int titleId,
			GetOptionsListener<T, G> options,
			GetListener<T> getter,
			SetListener<T> setter,
			Initializer initializer) {
		if (initializer != null) {
			Executors.newSingleThreadExecutor().execute(() -> {
				initializer.onInit();
				handler.post(() -> initPreference(
						tv, titleId, options, getter, setter, null));
			});
			updatePreference(tv, titleId, R.string.tip_loading);
			return;
		}
		Map<T, G> optionsMap = options.onGetOptions();
		tv.setOnClickListener(v -> {
			CharSequence[] items = getItemsFromOptions(optionsMap);
			OptionsDialog.show(this, titleId, items, (view, which) -> {
				Set<T> keys = optionsMap.keySet();
				int i = 0;
				for (T key : keys) {
					if (i++ == which) {
						setter.onSet(key);
						updatePreference(tv, titleId,
								optionsMap.get(getter.onGet()));
						break;
					}
				}
			});
		});
		updatePreference(tv, titleId, optionsMap.get(getter.onGet()));
	}

	private <T, G> CharSequence[] getItemsFromOptions(Map<T, G> options) {
		CharSequence[] items = new CharSequence[options.size()];
		int i = 0;
		for (G value : options.values()) {
			items[i++] = getName(value);
		}
		return items;
	}

	private <T> String getName(T value) {
		if (value instanceof Integer) {
			return getString((int) value);
		} else if (value instanceof String) {
			return (String) value;
		} else {
			return null;
		}
	}

	private static <T> void updatePreference(TextView tv, int labelId, T value) {
		if (value != null) {
			tv.setText(getLabelAndValue(tv.getContext(), labelId, value));
		}
	}

	@SuppressWarnings("deprecation")
	private static <T> Spanned getLabelAndValue(Context context,
			int labelId, T value) {
		String valueString;
		if (value instanceof Integer) {
			valueString = context.getString((Integer) value);
		} else if (value instanceof String) {
			valueString = (String) value;
		} else {
			return null;
		}
		String html = "<big><font color=\"#ffffff\">" +
				context.getString(labelId) +
				"</font></big><br/>" +
				valueString;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			return Html.fromHtml(html);
		} else {
			return Html.fromHtml(html, 0);
		}
	}

	private boolean updateDisableBatteryOptimizations() {
		if (BatteryOptimization.isIgnoringBatteryOptimizations(this)) {
			disableBatteryOptimizations.setVisibility(View.GONE);
			return true;
		} else {
			disableBatteryOptimizations.setOnClickListener(v ->
					BatteryOptimization.requestDisable(
							PreferencesActivity.this));
			return false;
		}
	}

	private boolean updateDefaultLauncher() {
		if (DefaultLauncher.isDefault(this)) {
			defaultLauncherView.setVisibility(View.GONE);
			return true;
		} else {
			defaultLauncherView.setOnClickListener(v ->
					DefaultLauncher.setAsDefault(this));
			return false;
		}
	}

	private static void start(Context context, boolean welcome) {
		Intent intent = new Intent(context, PreferencesActivity.class);
		if (welcome) {
			intent.putExtra(WELCOME, true);
		}
		context.startActivity(intent);
	}

	private static Map<Integer, Integer> getOrientationOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
				R.string.orientation_portrait);
		map.put(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
				R.string.orientation_landscape);
		map.put(ActivityInfo.SCREEN_ORIENTATION_USER,
				R.string.orientation_user);
		return map;
	}

	private static Map<Boolean, Integer> getDarkenBackgroundOptions() {
		Map<Boolean, Integer> map = new LinkedHashMap<>();
		map.put(Boolean.TRUE, R.string.darken_background_yes);
		map.put(Boolean.FALSE, R.string.darken_background_no);
		return map;
	}

	private static Map<Boolean, Integer> getBlurBackgroundOptions() {
		Map<Boolean, Integer> map = new LinkedHashMap<>();
		map.put(Boolean.TRUE, R.string.blur_background_yes);
		map.put(Boolean.FALSE, R.string.blur_background_no);
		return map;
	}

	private static Map<Integer, Integer> getDeadZoneOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.DEAD_ZONE_NONE, R.string.dead_zone_none);
		map.put(Preferences.DEAD_ZONE_TOP, R.string.dead_zone_top);
		map.put(Preferences.DEAD_ZONE_BOTTOM, R.string.dead_zone_bottom);
		map.put(Preferences.DEAD_ZONE_TOP_BOTTOM,
				R.string.dead_zone_top_bottom);
		map.put(Preferences.DEAD_ZONE_ALL, R.string.dead_zone_all);
		return map;
	}

	private static Map<Integer, Integer> getImmersiveModeOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.IMMERSIVE_MODE_DISABLED,
				R.string.immersive_mode_disabled);
		map.put(Preferences.IMMERSIVE_MODE_STATUS_BAR,
				R.string.immersive_mode_status_bar);
		map.put(Preferences.IMMERSIVE_MODE_NAVIGATION_BAR,
				R.string.immersive_mode_navigation_bar);
		map.put(Preferences.IMMERSIVE_MODE_FULL,
				R.string.immersive_mode_full);
		return map;
	}

	private static Map<Boolean, Integer> getAnimateInOutOptions() {
		Map<Boolean, Integer> map = new LinkedHashMap<>();
		map.put(Boolean.TRUE, R.string.animate_in_out_yes);
		map.put(Boolean.FALSE, R.string.animate_in_out_no);
		return map;
	}

	private static Map<Integer, Integer> getOpenListWithOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.OPEN_LIST_WITH_TAP,
				R.string.open_list_with_tap);
		map.put(Preferences.OPEN_LIST_WITH_ANY_TOUCH,
				R.string.open_list_with_any_touch);
		map.put(Preferences.OPEN_LIST_WITH_LONG_PRESS,
				R.string.open_list_with_long_press);
		map.put(Preferences.OPEN_LIST_WITH_DOUBLE_TAP,
				R.string.open_list_with_double_tap);
		map.put(Preferences.OPEN_LIST_WITH_ICON,
				R.string.open_list_with_icon);
		return map;
	}

	private static Map<Boolean, Integer> getDisplayKeyboardOptions() {
		Map<Boolean, Integer> map = new LinkedHashMap<>();
		map.put(Boolean.TRUE, R.string.display_keyboard_yes);
		map.put(Boolean.FALSE, R.string.display_keyboard_no);
		return map;
	}

	private static Map<Boolean, Integer> getSpaceActionOptions() {
		Map<Boolean, Integer> map = new LinkedHashMap<>();
		map.put(Boolean.TRUE, R.string.space_action_double_launch);
		map.put(Boolean.FALSE, R.string.space_action_move_selection);
		return map;
	}

	private static Map<Boolean, Integer> getAutoLaunchMatchingOptions() {
		Map<Boolean, Integer> map = new LinkedHashMap<>();
		map.put(Boolean.TRUE, R.string.auto_launch_matching_yes);
		map.put(Boolean.FALSE, R.string.auto_launch_matching_no);
		return map;
	}

	private static Map<Integer, Integer> getSearchStrictnessOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.SEARCH_STRICTNESS_HAMMING,
				R.string.search_strictness_hamming);
		map.put(Preferences.SEARCH_STRICTNESS_CONTAINS,
				R.string.search_strictness_contains);
		map.put(Preferences.SEARCH_STRICTNESS_STARTS_WITH,
				R.string.search_strictness_starts_with);
		return map;
	}

	private static Map<Integer, Integer> getSearchParameterOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.SEARCH_PARAMETER_APP_LABEL,
				R.string.search_parameter_app_label);
		map.put(Preferences.SEARCH_PARAMETER_PACKAGE_NAME,
				R.string.search_parameter_package_name);
		return map;
	}

	private static Map<Integer, Integer> getShowAppNamesOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.SHOW_APP_NAMES_ALWAYS,
				R.string.show_app_names_always);
		map.put(Preferences.SHOW_APP_NAMES_SEARCH,
				R.string.show_app_names_search);
		map.put(Preferences.SHOW_APP_NAMES_NEVER,
				R.string.show_app_names_never);
		return map;
	}

	private static Map<Boolean, Integer> getExcludePieOptions() {
		Map<Boolean, Integer> map = new LinkedHashMap<>();
		map.put(Boolean.TRUE, R.string.exclude_pie_yes);
		map.put(Boolean.FALSE, R.string.exclude_pie_no);
		return map;
	}

	private static Map<Integer, Integer> getIconPressOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.ICON_PRESS_DEFAULT, R.string.icon_press_default);
		map.put(Preferences.ICON_PRESS_LONGER, R.string.icon_press_longer);
		map.put(Preferences.ICON_PRESS_MENU, R.string.icon_press_menu);
		map.put(Preferences.ICON_LOCK_MENU, R.string.icon_lock_menu);
		return map;
	}

	private Map<String, String> getIconPackOptions() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put(null, getString(R.string.icon_pack_default));
		map.putAll(PieLauncherApp.iconPack.getIconPacks());
		return map;
	}

	private static Map<Integer, Integer> getHapticFeedbackOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.HAPTIC_FEEDBACK_FOLLOW_SYSTEM,
				R.string.haptic_feedback_follow_system);
		map.put(Preferences.HAPTIC_FEEDBACK_DISABLE_LAUNCH,
				R.string.haptic_feedback_disable_launch);
		map.put(Preferences.HAPTIC_FEEDBACK_DISABLE_ALL,
				R.string.haptic_feedback_disable_all);
		return map;
	}

	private static Map<Boolean, Integer> getUseLightDialogOptions() {
		Map<Boolean, Integer> map = new LinkedHashMap<>();
		map.put(Boolean.TRUE, R.string.use_light_dialogs_yes);
		map.put(Boolean.FALSE, R.string.use_light_dialogs_no);
		return map;
	}

	private static Map<Boolean, Integer> getForceRelaunchOptions() {
		Map<Boolean, Integer> map = new LinkedHashMap<>();
		map.put(Boolean.TRUE, R.string.force_relaunch_yes);
		map.put(Boolean.FALSE, R.string.force_relaunch_no);
		return map;
	}

	private interface GetOptionsListener<T, G> {
		Map<T, G> onGetOptions();
	}

	private interface GetListener<T> {
		T onGet();
	}

	private interface SetListener<T> {
		void onSet(T value);
	}

	private interface Initializer {
		void onInit();
	}
}
