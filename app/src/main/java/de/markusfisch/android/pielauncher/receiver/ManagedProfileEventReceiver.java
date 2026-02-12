package de.markusfisch.android.pielauncher.receiver;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;

@SuppressLint("UseRequiresApi")
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
			PieLauncherApp.apps.indexAppsAsync(context);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
				Intent.ACTION_PROFILE_AVAILABLE.equals(action)) {
			PieLauncherApp.apps.propagateUpdate();
		}
	}
}
