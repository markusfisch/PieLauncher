package de.markusfisch.android.pielauncher.app;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;

import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.receiver.PackageEventReceiver;

public class PieLauncherApp extends Application {
	private static final PackageEventReceiver packageEventReceiver =
			new PackageEventReceiver();

	public static final Preferences prefs = new Preferences();
	public static final AppMenu appMenu = new AppMenu();

	@Override
	public void onCreate() {
		super.onCreate();
		prefs.init(this);
		registerPackageEventReceiver();
	}

	public void registerPackageEventReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_LOCALE_CHANGED);
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
		filter.addDataScheme("package");
		filter.addDataScheme("file");
		registerReceiver(packageEventReceiver, filter);
		// Note it's not required to unregister the receiver because it
		// needs to be there as long as this application is running.
	}
}
