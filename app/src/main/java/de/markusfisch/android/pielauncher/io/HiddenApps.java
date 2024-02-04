package de.markusfisch.android.pielauncher.io;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;

public class HiddenApps {
	private static final String HIDDEN_APPS_FILE = "hidden";

	private static boolean restored = false;

	public static void restore(Context context, HashSet<String> hiddenApps) {
		if (restored) {
			return;
		}
		hiddenApps.clear();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					context.openFileInput(HIDDEN_APPS_FILE)));
			String line;
			while ((line = reader.readLine()) != null) {
				hiddenApps.add(line);
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// Return an empty array.
		} catch (IOException e) {
			// Return an empty array.
		}
		restored = true;
	}

	public static void store(Context context, Set<String> hiddenApps) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					context.openFileOutput(HIDDEN_APPS_FILE, Context.MODE_PRIVATE)));
			for (String packageName : hiddenApps) {
				writer.write(packageName);
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// Ignore.
		}
	}
}
