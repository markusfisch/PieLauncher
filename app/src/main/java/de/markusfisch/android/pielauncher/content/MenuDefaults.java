package de.markusfisch.android.pielauncher.content;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.provider.CalendarContract;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.markusfisch.android.pielauncher.content.Apps.AppIcon;

public class MenuDefaults {
	public static void createInitialMenu(
			List<AppIcon> menu,
			Map<LauncherItemKey, AppIcon> allApps,
			PackageManager pm) {
		Intent[] intents = new Intent[]{
				new Intent(Intent.ACTION_VIEW, Uri.parse("http://")),
				new Intent(Intent.ACTION_DIAL),
				new Intent(Intent.ACTION_SENDTO, Uri.parse("sms:")),
				new Intent(Intent.ACTION_PICK,
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
				new Intent(Intent.ACTION_VIEW, Uri.parse("geo:47.6,-122.3")),
				new Intent(Intent.ACTION_VIEW, Uri.parse("google.streetview:cbll=46.414382,10.013988"))
						.setPackage("com.google.android.apps.maps"),
				getCalendarIntent(),
				new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")),
				new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
		};
		UserHandle userHandle = AppLauncher.HAS_LAUNCHER_APP
				? Process.myUserHandle()
				: null;
		ArrayList<LauncherItemKey> defaults = new ArrayList<>();
		for (Intent intent : intents) {
			LauncherItemKey defaultItemKey = resolveDefaultAppForIntent(
					pm, intent, userHandle);
			if (defaultItemKey == null || defaults.contains(defaultItemKey)) {
				continue;
			}
			// Get launch intent because the class name from above
			// doesn't necessarily match the launch intent and so
			// doesn't match with the keys in apps.
			Intent launchIntent = pm.getLaunchIntentForPackage(
					defaultItemKey.componentName.getPackageName());
			if (launchIntent == null) {
				continue;
			}
			LauncherItemKey launcherItemKey = new LauncherItemKey(
					launchIntent.getComponent(), userHandle);
			AppIcon appIcon = allApps.get(launcherItemKey);
			if (appIcon != null) {
				defaults.add(launcherItemKey);
				addMenuIcon(menu, appIcon);
			}
		}

		fillMenu(menu, allApps, defaults, 8);
	}

	public static void createMenuForPopularApps(
			List<AppIcon> menu,
			Map<LauncherItemKey, AppIcon> allApps,
			int numberOfIcons) {
		String[] popularApps = new String[]{
				"com.whatsapp",
				"com.facebook.katana",
				"com.facebook.orca",
				"com.instagram.android",
				"com.google.android.youtube",
				"com.snapchat.android",
				"com.twitter.android",
				"com.netflix.mediaclient",
				"com.spotify.music"};
		ArrayList<LauncherItemKey> defaults = new ArrayList<>();
		int max = Math.min(allApps.size(), numberOfIcons);
		int i = menu.size();
		for (String packageName : popularApps) {
			if (i >= max) {
				break;
			}
			Map.Entry<LauncherItemKey, AppIcon> entry = findByPackageName(
					allApps, packageName);
			if (entry != null) {
				defaults.add(entry.getKey());
				addMenuIcon(menu, entry.getValue());
				++i;
			}
		}

		fillMenu(menu, allApps, defaults, numberOfIcons);
	}

	private static void fillMenu(
			List<AppIcon> menu,
			Map<LauncherItemKey, AppIcon> allApps,
			ArrayList<LauncherItemKey> defaults,
			int numberOfIcons) {
		int max = Math.min(allApps.size(), numberOfIcons);
		int i = menu.size();
		for (Map.Entry<LauncherItemKey, AppIcon> entry : allApps.entrySet()) {
			if (i >= max) {
				break;
			}
			if (!defaults.contains(entry.getKey())) {
				addMenuIcon(menu, entry.getValue());
				++i;
			}
		}
	}

	private static void addMenuIcon(List<AppIcon> menu, AppIcon appIcon) {
		if (appIcon != null) {
			menu.add(appIcon);
		}
	}

	private static LauncherItemKey resolveDefaultAppForIntent(
			PackageManager pm,
			Intent intent,
			UserHandle userHandle) {
		ResolveInfo resolveInfo = pm.resolveActivity(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		if (resolveInfo == null) {
			return null;
		}
		return new LauncherItemKey(
				Apps.getComponentName(resolveInfo.activityInfo),
				userHandle);
	}

	private static Intent getCalendarIntent() {
		Intent intent = new Intent(Intent.ACTION_EDIT)
				.setType("vnd.android.cursor.item/event");
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return intent
					.putExtra("title", "dummy")
					.putExtra("beginTime", 0)
					.putExtra("endTime", 0);
		} else {
			return intent
					.putExtra(CalendarContract.Events.TITLE, "dummy")
					.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0)
					.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0);
		}
	}

	private static Map.Entry<LauncherItemKey, AppIcon> findByPackageName(
			Map<LauncherItemKey, AppIcon> allApps,
			String packageName) {
		for (Map.Entry<LauncherItemKey, AppIcon> entry : allApps.entrySet()) {
			if (entry.getKey().componentName.getPackageName().equals(
					packageName)) {
				return entry;
			}
		}
		return null;
	}

}
