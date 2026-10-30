package de.markusfisch.android.pielauncher.io;

import android.content.ComponentName;
import android.content.Context;

import java.util.HashSet;
import java.util.Iterator;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;

public class HiddenAppsStorage {
	public final HashSet<ComponentName> componentNames = new HashSet<>();

	private boolean restored = false;

	public void addAndStore(Context context, ComponentName componentName) {
		synchronized (this) {
			componentNames.add(componentName);
		}
		store(context);
	}

	public void removeAndStore(Context context, String packageName) {
		synchronized (this) {
			for (Iterator<ComponentName> it = componentNames.iterator();
					it.hasNext(); ) {
				if (it.next().getPackageName().equals(packageName)) {
					it.remove();
				}
			}
		}
		store(context);
	}

	public void restore(Context context) {
		synchronized (this) {
			if (restored) {
				return;
			}
		}
		HashSet<ComponentName> snapshot = new HashSet<>();
		PieLauncherApp.getDatabase(context).restoreHiddenApps(snapshot);
		synchronized (this) {
			if (!restored) {
				componentNames.addAll(snapshot);
				restored = true;
			}
		}
	}

	public void store(Context context) {
		PieLauncherApp.getDatabase(context).storeHiddenApps(copyComponentNames());
	}

	public synchronized HashSet<ComponentName> copyComponentNames() {
		return new HashSet<>(componentNames);
	}
}
