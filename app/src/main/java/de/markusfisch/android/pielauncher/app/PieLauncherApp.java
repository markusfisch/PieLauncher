package de.markusfisch.android.pielauncher.app;

import android.app.Application;

import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.preference.Preferences;

public class PieLauncherApp extends Application {
	public static final Preferences prefs = new Preferences();
	public static final AppMenu appMenu = new AppMenu();

	@Override
	public void onCreate() {
		super.onCreate();
		prefs.init(this);
	}
}
