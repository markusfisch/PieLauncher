package de.markusfisch.android.pielauncher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PackageEventReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
		} else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
		} else if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
		} else if (Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
		}
	}
}
