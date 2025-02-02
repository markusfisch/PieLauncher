package de.markusfisch.android.pielauncher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.TypedValue;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;

public class ConfigurationChangedReceiver extends BroadcastReceiver {
	private int lastPrimaryColor = -1;

	public void initialize(Context context) {
		// Avoids triggering indexing on the first
		// ACTION_CONFIGURATION_CHANGED.
		lastPrimaryColor = getPrimaryColor(context);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			return;
		}
		String action = intent.getAction();
		if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
			PieLauncherApp.appMenu.postIndexApps(context);
		} else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
			// Only index on color changes. `ACTION_CONFIGURATION_CHANGED`
			// is sent for all kinds of changes and indexing should be
			// kept to a minimum.
			int newPrimaryColor = getPrimaryColor(context);
			if (newPrimaryColor != lastPrimaryColor) {
				lastPrimaryColor = newPrimaryColor;
				PieLauncherApp.appMenu.postIndexApps(context);
			}
		}
	}

	private int getPrimaryColor(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			TypedValue typedValue = new TypedValue();
			context.getTheme().resolveAttribute(
					android.R.attr.colorPrimary, typedValue, true);
			return typedValue.data;
		} else {
			return 0;
		}
	}
}
