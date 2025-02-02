package de.markusfisch.android.pielauncher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;

public class PackageEventReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			return;
		}
		String action = intent.getAction();
		Uri data = intent.getData();
		if (data == null) {
			return;
		}
		String packageName = data.getSchemeSpecificPart();
		if (Intent.ACTION_PACKAGE_ADDED.equals(action) ||
				// Sent when a component of a package changed.
				Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
			PieLauncherApp.appMenu.postIndexApps(context, packageName,
					null);
		} else if (Intent.ACTION_PACKAGE_REMOVED.equals(action) &&
				// Skip ACTION_PACKAGE_REMOVED when replacing because it
				// will be immediately followed by ACTION_PACKAGE_ADDED.
				!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
			PieLauncherApp.appMenu.removePackage(context, packageName, null);
		}
	}
}
