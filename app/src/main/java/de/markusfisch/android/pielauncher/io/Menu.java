package de.markusfisch.android.pielauncher.io;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.content.LauncherItemKey;

public class Menu {
	private static final String MENU_FILE = "menu";

	public static ArrayList<Apps.AppIcon> restore(Context context,
			Map<LauncherItemKey, Apps.AppIcon> allApps) {
		ArrayList<Apps.AppIcon> icons = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					context.openFileInput(MENU_FILE)));
			String line;
			while ((line = reader.readLine()) != null) {
				Apps.AppIcon icon = allApps.get(
						LauncherItemKey.unflattenFromString(context, line));
				if (icon != null) {
					icons.add(icon);
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// Return an empty array.
		} catch (IOException e) {
			// Return an empty array.
		}
		return icons;
	}

	public static void store(Context context, List<Apps.AppIcon> icons) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					context.openFileOutput(MENU_FILE, Context.MODE_PRIVATE)));
			for (Apps.AppIcon icon : icons) {
				writer.write(LauncherItemKey.flattenToString(
						context,
						icon.componentName,
						icon.userHandle));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// Ignore.
		}
	}
}
