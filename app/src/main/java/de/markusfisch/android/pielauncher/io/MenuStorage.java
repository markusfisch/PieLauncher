package de.markusfisch.android.pielauncher.io;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.content.LauncherItemKey;

public class MenuStorage {
	public static ArrayList<Apps.AppIcon> restore(Context context,
			String fileName,
			Map<LauncherItemKey, Apps.AppIcon> allApps) {
		return PieLauncherApp.getDatabase(context).restoreMenu(
				fileName, allApps);
	}

	public static void store(Context context,
			String fileName,
			List<Apps.AppIcon> icons) {
		PieLauncherApp.getDatabase(context).storeMenu(fileName, icons);
	}
}
