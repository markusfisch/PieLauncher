package de.markusfisch.android.pielauncher.content;

import android.os.UserHandle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Profile availability belongs to the captured apps, not the later restore time.
public class AppSnapshot extends HashMap<LauncherItemKey, Apps.AppIcon> {
	final Set<UserHandle> unavailableProfiles = new HashSet<>();
	boolean provisional;

	AppSnapshot() {
	}

	AppSnapshot(AppSnapshot source) {
		putAll(source);
		provisional = source.provisional;
		unavailableProfiles.addAll(source.unavailableProfiles);
	}

	void replaceWith(AppSnapshot source) {
		if (source == this) {
			return;
		}
		clear();
		putAll(source);
		provisional = source.provisional;
		unavailableProfiles.clear();
		unavailableProfiles.addAll(source.unavailableProfiles);
	}

	static void markUnavailable(Map<LauncherItemKey, Apps.AppIcon> apps, UserHandle user) {
		if (apps instanceof AppSnapshot) {
			((AppSnapshot) apps).unavailableProfiles.add(user);
		}
	}
}
