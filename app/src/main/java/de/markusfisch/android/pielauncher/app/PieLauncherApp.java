package de.markusfisch.android.pielauncher.app;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.UserHandle;

import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.graphics.IconPack;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.receiver.ConfigurationChangedReceiver;
import de.markusfisch.android.pielauncher.receiver.ManagedProfileEventReceiver;
import de.markusfisch.android.pielauncher.receiver.PackageEventReceiver;

public class PieLauncherApp extends Application {
	public static final AppMenu appMenu = new AppMenu();
	public static final IconPack iconPack = new IconPack();

	private static final ConfigurationChangedReceiver configurationChangedReceiver =
			new ConfigurationChangedReceiver();
	private static final ManagedProfileEventReceiver managedProfileEventReceiver =
			new ManagedProfileEventReceiver();
	private static final PackageEventReceiver packageEventReceiver =
			new PackageEventReceiver();

	private static Preferences prefs;

	// Necessary because PreferenceManager.getDefaultSharedPreferences()
	// requires a context after encrypted storage has been unlocked.
	// The application context may be initialized before that when this
	// app is started at boot, but before the user has (initially) unlocked
	// the device (and encrypted storage with it).
	public static Preferences getPrefs(Context context) {
		if (prefs == null) {
			prefs = new Preferences(context);
		}
		return prefs;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		registerConfigurationChangedReceiver();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			registerPackageEventReceiver();
		} else {
			registerLauncherAppsCallback();
			registerManagedEventReceiver();
		}
		// Note it's not required to unregister receivers because they
		// need to be there as long as this application is running.
	}

	private void registerConfigurationChangedReceiver() {
		configurationChangedReceiver.initialize(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
		// Add ACTION_LOCALE_CHANGED even if ACTION_CONFIGURATION_CHANGED
		// includes locale changes to avoid having to filter configuration
		// changes. ACTION_CONFIGURATION_CHANGED is sent for many events
		// and indexing apps should be kept to a minimum.
		filter.addAction(Intent.ACTION_LOCALE_CHANGED);
		registerReceiver(configurationChangedReceiver, filter);
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
				appMenu.postIndexApps(PieLauncherApp.this,
						packageName, user);
			}

			@Override
			public void onPackageChanged(String packageName,
					UserHandle user) {
				appMenu.postIndexApps(PieLauncherApp.this,
						packageName, user);
			}

			@Override
			public void onPackageRemoved(String packageName,
					UserHandle user) {
				appMenu.removePackage(PieLauncherApp.this, packageName, user);
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
