package de.markusfisch.android.pielauncher.io;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;

public class HiddenApps {
	private static final String HIDDEN_APPS_FILE = "hidden";

	public final HashSet<String> packageNames = new HashSet<>();

	private boolean restored = false;

	public void addAndStore(Context context, String packageName) {
		packageNames.add(packageName);
		store(context);
	}

	public void removeAndStore(Context context, String packageName) {
		packageNames.remove(packageName);
		store(context);
	}

	public void restore(Context context) {
		if (restored) {
			return;
		}
		packageNames.clear();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					context.openFileInput(HIDDEN_APPS_FILE)));
			String line;
			while ((line = reader.readLine()) != null) {
				packageNames.add(line);
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// Return an empty array.
		} catch (IOException e) {
			// Return an empty array.
		}
		restored = true;
	}

	public void store(Context context) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					context.openFileOutput(HIDDEN_APPS_FILE,
							Context.MODE_PRIVATE)));
			for (String packageName : packageNames) {
				writer.write(packageName);
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// Ignore.
		}
	}
}
