package de.markusfisch.android.pielauncher.content;

import android.os.UserHandle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// Visible icons and the unavailable slots that must survive a visible edit.
public class MenuItems extends ArrayList<Apps.AppIcon> {
	private List<LauncherItemKey> storedOrder = new ArrayList<>();
	private final Set<LauncherItemKey> unavailable = new HashSet<>();
	private final Map<LauncherItemKey, Apps.AppIcon> visibleSlots = new HashMap<>();

	void remember(LauncherItemKey key, boolean hidden, Apps.AppIcon icon) {
		storedOrder.add(key);
		if (hidden) {
			unavailable.add(key);
		} else {
			visibleSlots.put(key, icon);
		}
	}

	public boolean hasUnavailableItems() {
		return !unavailable.isEmpty();
	}

	List<LauncherItemKey> snapshotKeys() {
		List<LauncherItemKey> visible = new ArrayList<>(size());
		for (Apps.AppIcon icon : this) {
			visible.add(new LauncherItemKey(icon.componentName, icon.userHandle));
		}
		Set<LauncherItemKey> visibleKeys = new HashSet<>(visible);
		Iterator<LauncherItemKey> edited = visible.iterator();
		List<LauncherItemKey> merged = new ArrayList<>();
		for (LauncherItemKey storedKey : storedOrder) {
			Apps.AppIcon original = visibleSlots.get(storedKey);
			LauncherItemKey key = original == null ? storedKey :
					new LauncherItemKey(original.componentName, original.userHandle);
			if (unavailable.contains(key) && !visibleKeys.contains(key)) {
				merged.add(key);
			} else if (visibleKeys.contains(key) && edited.hasNext()) {
				merged.add(edited.next());
			}
		}
		while (edited.hasNext()) {
			merged.add(edited.next());
		}
		return merged;
	}

	void hideProfiles(Set<UserHandle> users) {
		if (users.isEmpty()) {
			return;
		}
		storedOrder = snapshotKeys();
		visibleSlots.clear();
		for (Apps.AppIcon icon : this) {
			visibleSlots.put(new LauncherItemKey(icon.componentName, icon.userHandle), icon);
		}
		Iterator<Apps.AppIcon> it = iterator();
		while (it.hasNext()) {
			Apps.AppIcon icon = it.next();
			if (users.contains(icon.userHandle)) {
				LauncherItemKey key = new LauncherItemKey(icon.componentName, icon.userHandle);
				unavailable.add(key);
				visibleSlots.remove(key);
				it.remove();
			}
		}
	}

	void replaceWith(MenuItems menu) {
		if (menu == this) {
			return;
		}
		clear();
		addAll(menu);
		storedOrder = new ArrayList<>(menu.storedOrder);
		unavailable.clear();
		unavailable.addAll(menu.unavailable);
		visibleSlots.clear();
		visibleSlots.putAll(menu.visibleSlots);
	}
}
