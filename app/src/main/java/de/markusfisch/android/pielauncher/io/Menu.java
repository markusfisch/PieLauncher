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

import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.content.LauncherItemKey;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.graphics.PieMenu;

public class Menu {
	private static final String MENU_FILE = "menu";

	public static ArrayList<PieMenu.Icon> restore(Context context,
			Map<LauncherItemKey, AppMenu.AppIcon> allApps) {
		ArrayList<PieMenu.Icon> icons = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					context.openFileInput(MENU_FILE)));
			String line;
			while ((line = reader.readLine()) != null) {
				PieMenu.Icon icon = allApps.get(
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

	public static void store(Context context, List<PieMenu.Icon> icons) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					context.openFileOutput(MENU_FILE, Context.MODE_PRIVATE)));
			for (CanvasPieMenu.Icon icon : icons) {
				writer.write(LauncherItemKey.flattenToString(
						context,
						((AppMenu.AppIcon) icon).componentName,
						((AppMenu.AppIcon) icon).userHandle));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// Ignore.
		}
	}
}
