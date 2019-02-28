package de.markusfisch.android.pielauncher.receiver;

import de.markusfisch.android.pielauncher.widget.AppPieView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PackageEventReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			return;
		}
		String action = intent.getAction();
		if (Intent.ACTION_PACKAGE_ADDED.equals(action) ||
				Intent.ACTION_PACKAGE_REMOVED.equals(action) ||
				Intent.ACTION_PACKAGE_CHANGED.equals(action) ||
				Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
			AppPieView.appMenu.indexAppsAsync(context);
		}
	}
}
