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
	public static void start(Context context) {
		context.startActivity(new Intent(context, SettingsActivity.class));
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_settings);

		TextView headline = findViewById(R.id.headline);
		headline.setOnClickListener(v -> finish());
		if (PieLauncherApp.prefs.isIntroduced()) {
			findViewById(R.id.welcome).setVisibility(View.GONE);
		} else {
			PieLauncherApp.prefs.setIntroduced();
			headline.setText(R.string.welcome);
		}

		TextView disableBatteryOptimizations = findViewById(
				R.id.disable_battery_optimization);
		if (!BatteryOptimization.isIgnoringBatteryOptimizations(this)) {
			disableBatteryOptimizations.setOnClickListener(v -> {
				BatteryOptimization.requestDisable(
						SettingsActivity.this);
				finish();
			});
		} else {
			disableBatteryOptimizations.setVisibility(View.GONE);
		}

		TextView defaultLauncherView = findViewById(
				R.id.make_default_launcher);
		if (DefaultLauncher.isDefault(
				getPackageManager(),
				getPackageName())) {
			defaultLauncherView.setVisibility(View.GONE);
		} else {
			defaultLauncherView.setOnClickListener(v -> {
				DefaultLauncher.setAsDefault(this);
				finish();
			});
		}

		TextView orientationView = findViewById(R.id.orientation);
		orientationView.setOnClickListener(
				v -> Orientation.setOrientation(this, orientationView));
		Orientation.setOrientationText(orientationView,
				PieLauncherApp.prefs.getOrientation());

		SystemBars.addPaddingFromWindowInsets(findViewById(R.id.content));
		SystemBars.setTransparentSystemBars(getWindow());
	}

	@Override
	protected void onStart() {
		super.onStart();
		setRequestedOrientation(PieLauncherApp.prefs.getOrientation());
	}
}
