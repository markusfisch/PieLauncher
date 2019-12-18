package de.markusfisch.android.pielauncher.app;

import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.preference.Preferences;

import android.app.Application;

public class PieLauncherApp extends Application {
	public static final Preferences prefs = new Preferences();
	public static final AppMenu appMenu = new AppMenu();

	@Override
	public void onCreate() {
		super.onCreate();
		prefs.init(this);
	}
}
