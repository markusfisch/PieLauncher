package de.markusfisch.android.pielauncher.activity;

import android.app.Activity;
import android.os.Bundle;
import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.os.BatteryOptimization;
import de.markusfisch.android.pielauncher.view.SystemBars;

public class BatteryOptimizationActivity extends Activity {
	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_battery_optimization);
               
		findViewById(R.id.disable_battery_optimization).setOnClickListener(v -> {
			BatteryOptimization.requestDisable(
					BatteryOptimizationActivity.this);
			finish();
		});

		SystemBars.setTransparentSystemBars(getWindow());
	}
}
