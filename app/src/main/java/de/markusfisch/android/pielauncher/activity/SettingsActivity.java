package de.markusfisch.android.pielauncher.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.os.BatteryOptimization;
import de.markusfisch.android.pielauncher.os.DefaultLauncher;
import de.markusfisch.android.pielauncher.os.Orientation;
import de.markusfisch.android.pielauncher.view.SystemBars;

public class SettingsActivity extends Activity {
	private View disableBatteryOptimizations;
	private View defaultLauncherView;

	public static void start(Context context) {
		context.startActivity(new Intent(context, SettingsActivity.class));
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_settings);

		initHeadline();
		initOrientation();
		initDisplayKeyboard();
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
		setRequestedOrientation(PieLauncherApp.prefs.getOrientation());

		// These may change once set.
		updateDisableBatteryOptimizations();
		updateDefaultLauncher();
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Stop showing intro as soon as these settings are set.
		if (!PieLauncherApp.prefs.isIntroduced() &&
				BatteryOptimization.isIgnoringBatteryOptimizations(this) &&
				isDefaultLauncher()) {
			PieLauncherApp.prefs.setIntroduced();
		}
	}

	private void initHeadline() {
		TextView headline = findViewById(R.id.headline);
		if (PieLauncherApp.prefs.isIntroduced()) {
			headline.setOnClickListener(v -> finish());
			findViewById(R.id.welcome).setVisibility(View.GONE);
		} else {
			headline.setText(R.string.welcome);
			headline.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		}
	}

	private void initDoneButton() {
		View doneButton = findViewById(R.id.done);
		if (PieLauncherApp.prefs.isIntroduced()) {
			doneButton.setVisibility(View.GONE);
		} else {
			doneButton.setOnClickListener(v -> {
				PieLauncherApp.prefs.setIntroduced();
				finish();
			});
		}
	}

	private void initOrientation() {
		TextView orientationView = findViewById(R.id.orientation);
		orientationView.setOnClickListener(v ->
				Orientation.setOrientation(this, orientationView));
		Orientation.setOrientationText(orientationView,
				PieLauncherApp.prefs.getOrientation());
	}

	private void initDisplayKeyboard() {
		TextView displayKeyboardView = findViewById(R.id.display_keyboard);
		displayKeyboardView.setOnClickListener(v -> {
			PieLauncherApp.prefs.setDisplayKeyboard(
					!PieLauncherApp.prefs.displayKeyboard());
			updateDisplayKeyboardText(displayKeyboardView);
		});
		updateDisplayKeyboardText(displayKeyboardView);
	}

	private static void updateDisplayKeyboardText(TextView view) {
		view.setText(PieLauncherApp.prefs.displayKeyboard()
				? R.string.display_keyboard_yes
				: R.string.display_keyboard_no);
	}

	private void updateDisableBatteryOptimizations() {
		if (BatteryOptimization.isIgnoringBatteryOptimizations(this)) {
			disableBatteryOptimizations.setVisibility(View.GONE);
		} else {
			disableBatteryOptimizations.setOnClickListener(v ->
					BatteryOptimization.requestDisable(SettingsActivity.this));
		}
	}

	private void updateDefaultLauncher() {
		if (isDefaultLauncher()) {
			defaultLauncherView.setVisibility(View.GONE);
		} else {
			defaultLauncherView.setOnClickListener(v ->
					DefaultLauncher.setAsDefault(this));
		}
	}

	private boolean isDefaultLauncher() {
		return DefaultLauncher.isDefault(
				getPackageManager(), getPackageName());
	}
}
