package de.markusfisch.android.pielauncher.io;

import android.content.ComponentName;
import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Iterator;

import de.markusfisch.android.pielauncher.content.AppMenu;

public class HiddenApps {
	private static final String HIDDEN_APPS_FILE = "hidden";

	public final HashSet<ComponentName> componentNames = new HashSet<>();

	private boolean restored = false;

	public void addAndStore(Context context, ComponentName componentName) {
		componentNames.add(componentName);
		store(context);
	}

	public void removeAndStore(Context context, String packageName) {
		for (Iterator<ComponentName> it = componentNames.iterator();
				it.hasNext(); ) {
			ComponentName cn = it.next();
			if (cn.getPackageName().equals(packageName)) {
				it.remove();
			}
		}
		store(context);
	}

	public void restore(Context context) {
		if (restored) {
			return;
		}
		componentNames.clear();
		try {
			boolean migrated = false;
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					context.openFileInput(HIDDEN_APPS_FILE)));
			String line;
			while ((line = reader.readLine()) != null) {
				ComponentName componentName =
						ComponentName.unflattenFromString(line);
				if (componentName == null) {
					migrated = true;
					componentName = AppMenu.getLaunchComponentForPackageName(
							context, line);
				}
				if (componentName != null) {
					componentNames.add(componentName);
				}
			}
			reader.close();
			if (migrated) {
				store(context);
			}
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
			for (ComponentName componentName : componentNames) {
				writer.write(componentName.flattenToString());
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// Ignore.
		}
	}
}
