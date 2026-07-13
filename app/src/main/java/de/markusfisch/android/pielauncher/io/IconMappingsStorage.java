package de.markusfisch.android.pielauncher.io;

import android.content.ComponentName;
import android.content.Context;

import java.util.HashMap;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.graphics.IconPack;

public class IconMappingsStorage {
	public static void restore(
			Context context,
			String packageName,
			HashMap<ComponentName, IconPack.PackAndDrawable> mappings) {
		PieLauncherApp.getDatabase(context).restoreIconMappings(
				packageName, mappings);
	}

	public static void store(
			Context context,
			String packageName,
			HashMap<ComponentName, IconPack.PackAndDrawable> mappings) {
		PieLauncherApp.getDatabase(context).storeIconMappings(
				packageName, mappings);
	}
}
