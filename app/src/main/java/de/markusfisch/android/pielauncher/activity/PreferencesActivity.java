package de.markusfisch.android.pielauncher.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.os.BatteryOptimization;
import de.markusfisch.android.pielauncher.os.DefaultLauncher;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.view.SystemBars;

public class PreferencesActivity extends Activity {
	private static final String WELCOME = "welcome";

	private Preferences prefs;
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

		TextView headline = findViewById(R.id.headline);
		View doneButton = findViewById(R.id.done);

		disableBatteryOptimizations = findViewById(
				R.id.disable_battery_optimization);
		defaultLauncherView = findViewById(R.id.make_default_launcher);

		Intent intent = getIntent();
		isWelcomeMode = intent != null &&
				intent.getBooleanExtra(WELCOME, false);

		if (isWelcomeMode) {
			headline.setText(R.string.welcome);
			headline.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

			doneButton.setOnClickListener(view -> {
				prefs.setSkipSetup();
				finish();
			});

			findViewById(R.id.hide_in_welcome_mode).setVisibility(View.GONE);
		} else {
			headline.setOnClickListener(v -> finish());
			findViewById(R.id.welcome).setVisibility(View.GONE);

			doneButton.setVisibility(View.GONE);

			initPreferences();
		}

		SystemBars.addPaddingFromWindowInsets(findViewById(R.id.content));
		SystemBars.setTransparentSystemBars(getWindow());
	}

	@Override
	protected void onResume() {
		super.onResume();
		setRequestedOrientation(prefs.getOrientation());

		// These may change while this activity is shown.
		if (updateDisableBatteryOptimizations() &&
				updateDefaultLauncher() &&
				// Auto close in welcome mode only.
				isWelcomeMode) {
			finish();
		}
	}

	private void initPreferences() {
		initPreference(R.id.orientation,
				R.string.orientation,
				R.array.orientation_names,
				getOrientationOptions(),
				(value) -> prefs.setOrientation(value),
				() -> prefs.getOrientation());
		initPreference(R.id.darken_background,
				R.string.darken_background,
				R.array.darken_background_names,
				getDarkenBackgroundOptions(),
				(value) -> prefs.setDarkenBackground(value),
				() -> prefs.darkenBackground());
		initPreference(R.id.dead_zone,
				R.string.dead_zone,
				R.array.dead_zone_names,
				getDeadZoneOptions(),
				(value) -> prefs.setDeadZone(value),
				() -> prefs.getDeadZone());
		initPreference(R.id.display_keyboard,
				R.string.display_keyboard,
				R.array.display_keyboard_names,
				getDisplayKeyboardOptions(),
				(value) -> prefs.setDisplayKeyboard(value),
				() -> prefs.displayKeyboard());
		initPreference(R.id.space_action,
				R.string.space_action,
				R.array.space_action_names,
				getSpaceActionOptions(),
				(value) -> prefs.setDoubleSpaceLaunch(value),
				() -> prefs.doubleSpaceLaunch());
		initPreference(R.id.auto_launch_matching,
				R.string.auto_launch_matching,
				R.array.auto_launch_matching_names,
				getAutoLaunchMatchingOptions(),
				(value) -> prefs.setAutoLaunchMatching(value),
				() -> prefs.autoLaunchMatching());
		initPreference(R.id.search_strictness,
				R.string.search_strictness,
				R.array.search_strictness_names,
				getSearchStrictnessOptions(),
				(value) -> prefs.setSearchStrictness(value),
				() -> prefs.getSearchStrictness());
	}

	private <T> void initPreference(
			int viewId,
			int titleId,
			int itemsId,
			Map<T, Integer> options,
			SetListener<T> setter,
			GetListener<T> getter) {
		TextView tv = findViewById(viewId);
		tv.setOnClickListener(v -> {
			showOptionsDialog(titleId, itemsId, (view, which) -> {
				Set<T> keys = options.keySet();
				int i = 0;
				for (T key : keys) {
					if (i++ == which) {
						setter.onSet(key);
						updatePreference(tv, titleId, options.get(getter.onGet()));
						break;
					}
				}
			});
		});
		updatePreference(tv, titleId, options.get(getter.onGet()));
	}

	private void showOptionsDialog(int titleId, int itemsId,
			DialogInterface.OnClickListener onClickListener) {
		new AlertDialog.Builder(this)
				.setTitle(titleId)
				.setItems(itemsId, onClickListener)
				.show();
	}

	private static void updatePreference(TextView tv, int labelId, Integer valueId) {
		if (valueId != null) {
			tv.setText(getLabelAndValue(tv.getContext(), labelId, valueId));
		}
	}

	@SuppressWarnings("deprecation")
	private static Spanned getLabelAndValue(Context context,
			int labelId, int valueId) {
		String html = "<big><font color=\"#ffffff\">" +
				context.getString(labelId) +
				"</font></big><br/>" +
				context.getString(valueId);
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
					BatteryOptimization.requestDisable(PreferencesActivity.this));
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

	private static Map<Integer, Integer> getDeadZoneOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.DEAD_ZONE_NONE, R.string.dead_zone_none);
		map.put(Preferences.DEAD_ZONE_TOP, R.string.dead_zone_top);
		map.put(Preferences.DEAD_ZONE_BOTTOM, R.string.dead_zone_bottom);
		map.put(Preferences.DEAD_ZONE_BOTH, R.string.dead_zone_both);
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

	private interface GetListener<T> {
		T onGet();
	}

	private interface SetListener<T> {
		void onSet(T value);
	}
}
