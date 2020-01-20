package de.markusfisch.android.pielauncher.receiver;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class PackageEventReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			return;
		}
		String action = intent.getAction();
		if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
			PieLauncherApp.appMenu.indexAppsAsync(context);
			return;
		}
		Uri data = intent.getData();
		if (data == null) {
			return;
		}
		String packageName = data.getSchemeSpecificPart();
		if (Intent.ACTION_PACKAGE_ADDED.equals(action) ||
				// sent when a component of a package changed
				Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
			PieLauncherApp.appMenu.indexAppsAsync(context, packageName);
		} else if (Intent.ACTION_PACKAGE_REMOVED.equals(action) &&
				!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
			PieLauncherApp.appMenu.removePackageAsync(packageName);
		}
	}
}
