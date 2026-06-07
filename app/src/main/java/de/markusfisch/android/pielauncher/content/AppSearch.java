package de.markusfisch.android.pielauncher.content;

import java.util.Comparator;
import java.util.Locale;
import java.util.List;

import android.content.Context;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;


import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.content.Apps.AppIcon;

public class AppSearch {

	public static final Comparator<AppIcon> appLabelComparator = (left, right) -> {
		// Fast enough to do it for every comparison.
		// Otherwise, if defaultLocale was a permanent field outside
		// this scope, we'd need to listen for configuration changes
		// because the locale may change.
		Locale defaultLocale = Locale.getDefault();
		// compareToIgnoreCase() does not take locale into account.
		int result = left.label.toLowerCase(defaultLocale).compareTo(
				right.label.toLowerCase(defaultLocale));
		return result == 0 && left.userHandle != null && right.userHandle != null
				? left.userHandle.hashCode() - right.userHandle.hashCode()
				: result;
	};

	public static final Comparator<HammingHit> hammingComparator = (left, right) -> {
		int d = left.distance - right.distance;
		if (d != 0) {
			return d;
		}
		return appLabelComparator.compare(left.appIcon, right.appIcon);
	};


	public static String getSubject(
			int item,
			AppIcon appIcon,
			Locale defaultLocale) {
		if (item == Preferences.SEARCH_PARAMETER_PACKAGE_NAME) {
			return appIcon.componentName.getPackageName()
					.toLowerCase(defaultLocale);
		}
		return appIcon.label.toLowerCase(defaultLocale);
	}

	public static int hammingDistance(String a, String b, int l) {
		int count = 0;
		for (int i = 0; i < l; ++i) {
			if (a.charAt(i) != b.charAt(i)) {
				++count;
			}
		}
		return count;
	}

	public static class HammingHit {
		public final int distance;
		public final AppIcon appIcon;

		HammingHit(int distance, AppIcon appIcon) {
			this.distance = distance;
			this.appIcon = appIcon;
		}
	}


	public static List<AppIcon> filterAppsBy(Apps repo, Context context, String query) {
		if (repo.isIndexing()) {
			return null;
		}

		UserHandle privateUser = AppLauncher.findPrivateProfileUser(context);
		boolean privateOnly = false;

		query = query == null ? "" : query.trim();
		if (privateUser != null && query.startsWith(".")) {
			query = query.substring(1);
			if (AppLauncher.isPrivateProfileLocked(context, privateUser)) {
				if (AppLauncher.unlockPrivateProfile(context, () -> {
					if (repo.updateListener != null) repo.updateListener.onShowAllAppsOnResume();
				})) {
					return null;
				}
			} else {
				privateOnly = true;
			}
		}

		Locale defaultLocale = Locale.getDefault();
		query = query.toLowerCase(defaultLocale);

		Preferences prefs = PieLauncherApp.getPrefs(context);
		int strategy = prefs.getSearchStrictness();
		ArrayList<AppIcon> list = new ArrayList<>();
		ArrayList<AppSearch.HammingHit> hamming = new ArrayList<>();

		if (query.isEmpty()) {
			for (AppIcon appIcon : repo.apps.values()) {
				if (inProfile(appIcon.userHandle, privateUser, privateOnly)) {
					list.add(appIcon);
				}
			}
			switch (prefs.excludePie()) {
				case Preferences.EXCLUDE_PIE_ALL:
					list.removeAll(new HashSet<>(repo.menuSecondary));
					// Fall through.
				case Preferences.EXCLUDE_PIE_PRIMARY:
					list.removeAll(new HashSet<>(repo.menuPrimary));
					break;
			}
		} else {
			int item = prefs.getSearchParameter();
			for (Map.Entry<LauncherItemKey, AppIcon> entry : repo.apps.entrySet()) {
				AppIcon appIcon = entry.getValue();
				if (!inProfile(appIcon.userHandle, privateUser, privateOnly)) {
					continue;
				}
				String subject = AppSearch.getSubject(item, appIcon, defaultLocale);
				boolean add = false;
				switch (strategy) {
					// HAMMING includes CONTAINS for historical reasons.
					case Preferences.SEARCH_STRICTNESS_HAMMING:
					case Preferences.SEARCH_STRICTNESS_CONTAINS:
						add = subject.contains(query);
						break;
					case Preferences.SEARCH_STRICTNESS_STARTS_WITH:
						add = subject.startsWith(query);
						break;
				}
				if (add) {
					list.add(appIcon);
				} else {
					int min = Math.min(subject.length(), query.length());
					int distance = AppSearch.hammingDistance(subject, query, min);
					if (distance <= min >> 1) {
						hamming.add(new AppSearch.HammingHit(distance, appIcon));
					}
				}
			}
		}

		Collections.sort(list, AppSearch.appLabelComparator);
		if (!hamming.isEmpty() && (list.isEmpty() ||
				strategy == Preferences.SEARCH_STRICTNESS_HAMMING)) {
			// Only append hamming matches as they're less likely
			// as good as exact matches.
			Collections.sort(hamming, AppSearch.hammingComparator);
			for (AppSearch.HammingHit hit : hamming) {
				list.add(hit.appIcon);
			}
		}
		return list;
	}

	private static boolean inProfile(
			UserHandle appProfile,
			UserHandle privateUser,
			boolean privateOnly) {
		return privateUser == null ||
				privateUser.equals(appProfile) == privateOnly;
	}
}