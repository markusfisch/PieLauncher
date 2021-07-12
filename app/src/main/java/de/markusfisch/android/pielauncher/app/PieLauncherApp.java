package de.markusfisch.android.pielauncher.app;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.UserHandle;

import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.receiver.LocaleEventReceiver;
import de.markusfisch.android.pielauncher.receiver.ManagedProfileEventReceiver;
import de.markusfisch.android.pielauncher.receiver.PackageEventReceiver;

public class PieLauncherApp extends Application {
	private static final LocaleEventReceiver localeEventReceiver =
			new LocaleEventReceiver();
	private static final ManagedProfileEventReceiver managedProfileEventReceiver =
			new ManagedProfileEventReceiver();
	private static final PackageEventReceiver packageEventReceiver =
			new PackageEventReceiver();

	public static final Preferences prefs = new Preferences();
	public static final AppMenu appMenu = new AppMenu();

	@Override
	public void onCreate() {
		super.onCreate();
		prefs.init(this);

		registerLocaleEventReceiver();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			registerPackageEventReceiver();
		} else {
			registerLauncherAppsCallback();
			registerManagedEventReceiver();
		}
		// Note it's not required to unregister receivers because they
		// need to be there as long as this application is running.
	}

	private void registerLocaleEventReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_LOCALE_CHANGED);
		registerReceiver(localeEventReceiver, filter);
	}

	private void registerPackageEventReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
		filter.addDataScheme("package");
		filter.addDataScheme("file");
		registerReceiver(packageEventReceiver, filter);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void registerManagedEventReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
		filter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
		registerReceiver(managedProfileEventReceiver, filter);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void registerLauncherAppsCallback() {
		LauncherApps launcherApps = (LauncherApps) getSystemService(
				LAUNCHER_APPS_SERVICE);
		launcherApps.registerCallback(new LauncherApps.Callback() {
			@Override
			public void onPackageAdded(String packageName,
					UserHandle user) {
				appMenu.indexAppsAsync(PieLauncherApp.this, packageName, user);
			}

			@Override
			public void onPackageChanged(String packageName,
					UserHandle user) {
				appMenu.indexAppsAsync(PieLauncherApp.this, packageName, user);
			}

			@Override
			public void onPackageRemoved(String packageName,
					UserHandle user) {
				appMenu.removePackageAsync(packageName, user);
			}

			@Override
			public void onPackagesAvailable(String[] packageNames,
					UserHandle user, boolean replacing) {
			}

			@Override
			public void onPackagesUnavailable(String[] packageNames,
					UserHandle user, boolean replacing) {
			}
		});
	}
}
