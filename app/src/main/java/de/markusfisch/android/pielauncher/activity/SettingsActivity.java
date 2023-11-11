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

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.os.BatteryOptimization;
import de.markusfisch.android.pielauncher.os.DefaultLauncher;
import de.markusfisch.android.pielauncher.view.SystemBars;

public class SettingsActivity extends Activity {
	private static final String WELCOME = "welcome";

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
		return PieLauncherApp.getPrefs().isSkippingSetup() ||
				(BatteryOptimization.isIgnoringBatteryOptimizations(context) &&
						DefaultLauncher.isDefault(context));
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_settings);

		Intent intent = getIntent();
		isWelcomeMode = intent != null &&
				intent.getBooleanExtra(WELCOME, false);

		initHeadline();
		initDisplayKeyboard();
		initOrientation();
		initDoneButton();

		disableBatteryOptimizations = findViewById(
				R.id.disable_battery_optimization);
		defaultLauncherView = findViewById(R.id.make_default_launcher);

		SystemBars.addPaddingFromWindowInsets(findViewById(R.id.content));
		SystemBars.setTransparentSystemBars(getWindow());
	}

	@Override
	protected void onResume() {
		super.onResume();
		setRequestedOrientation(PieLauncherApp.getPrefs().getOrientation());

		// These may change while this activity is shown.
		if (updateDisableBatteryOptimizations() &&
				updateDefaultLauncher() &&
				// Auto close in welcome mode only.
				isWelcomeMode) {
			finish();
		}
	}

	private void initHeadline() {
		TextView headline = findViewById(R.id.headline);
		if (isWelcomeMode) {
			headline.setText(R.string.welcome);
			headline.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		} else {
			headline.setOnClickListener(v -> finish());
			findViewById(R.id.welcome).setVisibility(View.GONE);
		}
	}

	private void initDoneButton() {
		View doneButton = findViewById(R.id.done);
		if (isWelcomeMode) {
			doneButton.setOnClickListener(v -> {
				PieLauncherApp.getPrefs().setSkipSetup();
				finish();
			});
		} else {
			doneButton.setVisibility(View.GONE);
		}
	}

	private void initDisplayKeyboard() {
		TextView displayKeyboardView = findViewById(R.id.display_keyboard);
		if (isWelcomeMode) {
			displayKeyboardView.setVisibility(View.GONE);
			return;
		}
		displayKeyboardView.setOnClickListener(v -> {
			showOptionsDialog(
					R.string.display_keyboard,
					R.array.display_keyboard_names,
					(view, which) -> {
						boolean show;
						switch (which) {
							default:
							case 0:
								show = true;
								break;
							case 1:
								show = false;
								break;
						}
						PieLauncherApp.getPrefs().setDisplayKeyboard(show);
						updateDisplayKeyboardText(displayKeyboardView);
					});
		});
		updateDisplayKeyboardText(displayKeyboardView);
	}

	private static void updateDisplayKeyboardText(TextView tv) {
		tv.setText(getLabelAndValue(
				tv.getContext(),
				R.string.display_keyboard,
				PieLauncherApp.getPrefs().displayKeyboard()
						? R.string.display_keyboard_yes
						: R.string.display_keyboard_no));
	}

	private void initOrientation() {
		TextView orientationView = findViewById(R.id.orientation);
		if (isWelcomeMode) {
			orientationView.setVisibility(View.GONE);
			return;
		}
		orientationView.setOnClickListener(v -> {
			showOptionsDialog(
					R.string.orientation,
					R.array.orientation_names,
					(view, which) -> {
						int orientation;
						switch (which) {
							default:
							case 0:
								orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
								break;
							case 1:
								orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
								break;
						}
						PieLauncherApp.getPrefs().setOrientation(orientation);
						setRequestedOrientation(orientation);
						updateOrientationText(orientationView);
					});
		});
		updateOrientationText(orientationView);
	}

	public static void updateOrientationText(TextView tv) {
		tv.setText(getLabelAndValue(
				tv.getContext(),
				R.string.orientation,
				getOrientationResId(PieLauncherApp.getPrefs().getOrientation())));
	}

	public static int getOrientationResId(int orientation) {
		switch (orientation) {
			default:
			case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
				return R.string.orientation_portrait;
			case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
				return R.string.orientation_landscape;
		}
	}

	private void showOptionsDialog(int titleId, int itemsId,
			DialogInterface.OnClickListener onClickListener) {
		new AlertDialog.Builder(this)
				.setTitle(titleId)
				.setItems(itemsId, onClickListener)
				.show();
	}

	private static Spanned getLabelAndValue(Context context,
			int labelId, int valueId) {
		StringBuilder sb = new StringBuilder();
		sb.append("<big>");
		sb.append(context.getString(labelId));
		sb.append("</big><br/>");
		sb.append(context.getString(valueId));
		String html = sb.toString();
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
					BatteryOptimization.requestDisable(SettingsActivity.this));
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
		Intent intent = new Intent(context, SettingsActivity.class);
		if (welcome) {
			intent.putExtra(WELCOME, true);
		}
		context.startActivity(intent);
	}
}
