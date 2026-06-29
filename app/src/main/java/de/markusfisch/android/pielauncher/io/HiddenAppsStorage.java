package de.markusfisch.android.pielauncher.io;

import android.content.ComponentName;
import android.content.Context;

import java.util.HashSet;
import java.util.Iterator;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;

public class HiddenAppsStorage {
	public final HashSet<ComponentName> componentNames = new HashSet<>();

	private boolean restored = false;

	public synchronized void addAndStore(Context context,
			ComponentName componentName) {
		componentNames.add(componentName);
		store(context);
	}

	public synchronized void removeAndStore(Context context,
			String packageName) {
		for (Iterator<ComponentName> it = componentNames.iterator();
				it.hasNext(); ) {
			ComponentName cn = it.next();
			if (cn.getPackageName().equals(packageName)) {
				it.remove();
			}
		}
		store(context);
	}

	public synchronized void restore(Context context) {
		if (restored) {
			return;
		}
		componentNames.clear();
		PieLauncherApp.getDatabase(context).restoreHiddenApps(componentNames);
		restored = true;
	}

	public synchronized void store(Context context) {
		PieLauncherApp.getDatabase(context).storeHiddenApps(componentNames);
	}

	public synchronized HashSet<ComponentName> copyComponentNames() {
		return new HashSet<>(componentNames);
	}
}
