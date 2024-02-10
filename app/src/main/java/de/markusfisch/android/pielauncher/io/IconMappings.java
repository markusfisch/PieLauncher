package de.markusfisch.android.pielauncher.io;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import de.markusfisch.android.pielauncher.graphics.IconPack;

public class IconMappings {
	private static final String SEPARATOR = ";";
	private static final String MAPPINGS_FILE = "mappings";

	public static void restore(Context context,
			HashMap<String, IconPack.PackAndDrawable> mappings) {
		mappings.clear();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					context.openFileInput(MAPPINGS_FILE)));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(SEPARATOR);
				if (parts.length < 3) {
					continue;
				}
				mappings.put(parts[0], new IconPack.PackAndDrawable(
						parts[1], parts[2]));
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// Return an empty array.
		} catch (IOException e) {
			// Return an empty array.
		}
	}

	public static void store(Context context,
			HashMap<String, IconPack.PackAndDrawable> mappings) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					context.openFileOutput(MAPPINGS_FILE,
							Context.MODE_PRIVATE)));
			for (Map.Entry<String, IconPack.PackAndDrawable> mapping :
					mappings.entrySet()) {
				writer.write(mapping.getKey());
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
}
