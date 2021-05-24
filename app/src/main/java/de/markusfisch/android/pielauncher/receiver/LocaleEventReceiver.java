package de.markusfisch.android.pielauncher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;

public class LocaleEventReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			return;
		}
		String action = intent.getAction();
		if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
			PieLauncherApp.appMenu.indexAppsAsync(context);
		}
	}
}
