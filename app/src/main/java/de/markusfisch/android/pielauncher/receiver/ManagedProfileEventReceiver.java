package de.markusfisch.android.pielauncher.receiver;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ManagedProfileEventReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			return;
		}
		String action = intent.getAction();
		if (Intent.ACTION_MANAGED_PROFILE_ADDED.equals(action) ||
				Intent.ACTION_MANAGED_PROFILE_REMOVED.equals(action)) {
			PieLauncherApp.appMenu.postIndexApps(context);
		}
		// Ignore:
		// - ACTION_MANAGED_PROFILE_AVAILABLE
		// - ACTION_MANAGED_PROFILE_UNAVAILABLE
		// - ACTION_MANAGED_PROFILE_UNLOCKED
		// added in API level 24.
		// When the managed profile is unavailable, trying to launch an app
		// should propose to activate the profile and then launch the app (can
		// take time). App icons can then be kept in the app list and pie menu
		// all the time.
	}
}
