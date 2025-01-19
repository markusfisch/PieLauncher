package de.markusfisch.android.pielauncher.io;

import android.content.ComponentName;
import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.graphics.IconPack;

public class IconMappings {
	private static final String SEPARATOR = ";";
	private static final String MAPPINGS_FILE = "mappings";

	public static void restore(Context context,
			String packageName,
			HashMap<ComponentName, IconPack.PackAndDrawable> mappings) {
		mappings.clear();
		try {
			boolean migrated = false;

			// Migrate previous mappings without package name.
			String mappingsFile = getMappingsFile(packageName);
			if (!fileExists(context, mappingsFile) &&
					fileExists(context, MAPPINGS_FILE)) {
				migrated = true;
				mappingsFile = MAPPINGS_FILE;
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					context.openFileInput(mappingsFile)));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(SEPARATOR);
				if (parts.length < 3) {
					continue;
				}
				String componentPart = parts[0];
				ComponentName componentName =
						ComponentName.unflattenFromString(componentPart);
				if (componentName == null) {
					migrated = true;
					componentName = AppMenu.getLaunchComponentForPackageName(
							context, componentPart);
				}
				if (componentName != null) {
					mappings.put(componentName, new IconPack.PackAndDrawable(
							parts[1], parts[2]));
				}
			}
			reader.close();

			if (migrated) {
				store(context, packageName, mappings);
			}
		} catch (FileNotFoundException e) {
			// Return an empty array.
		} catch (IOException e) {
			// Return an empty array.
		}
	}

	public static void store(Context context,
			String packageName,
			HashMap<ComponentName, IconPack.PackAndDrawable> mappings) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					context.openFileOutput(getMappingsFile(packageName),
							Context.MODE_PRIVATE)));
			for (Map.Entry<ComponentName, IconPack.PackAndDrawable> mapping :
					mappings.entrySet()) {
				writer.write(mapping.getKey().flattenToString());
				writer.write(SEPARATOR);
				IconPack.PackAndDrawable pad = mapping.getValue();
				writer.write(pad.packageName);
				writer.write(SEPARATOR);
				writer.write(pad.drawableName);
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// Ignore, can't do nothing about this.
		}
	}

	private static String getMappingsFile(String packageName) {
		StringBuilder sb = new StringBuilder();
		sb.append(MAPPINGS_FILE);
		if (packageName != null && !packageName.isEmpty()) {
			sb.append("-");
			sb.append(packageName);
		}
		return sb.toString();
	}

	private static boolean fileExists(Context context, String fileName) {
		File file = context.getFileStreamPath(fileName);
		return file != null && file.exists();
	}
}
