package de.markusfisch.android.pielauncher.os;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

public class BatteryOptimization {
	// Suppress the BatteryLife warning because the whole point of a home
	// screen launcher is to be available all the time. This app does not
	// consume battery when not in use. And it should always and immediately
	// be available (and thus in memory). Killing this process in Doze mode
	// and reloading it when the user unlocks their device actually consumes
	// more battery.
	@SuppressLint("BatteryLife")
	@TargetApi(Build.VERSION_CODES.M)
	public static void requestDisable(Context context) {
		Intent intent = new Intent();
		intent.setAction(
				Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
		intent.setData(Uri.parse("package:" + context.getPackageName()));
		context.startActivity(intent);
	}

	public static boolean isIgnoringBatteryOptimizations(Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return true;
		}
		PowerManager pm = (PowerManager) context.getSystemService(
				Context.POWER_SERVICE);
		return pm.isIgnoringBatteryOptimizations(context.getPackageName());
	}
}
