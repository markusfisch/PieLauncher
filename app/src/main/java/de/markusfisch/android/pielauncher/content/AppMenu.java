package de.markusfisch.android.pielauncher.content;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.provider.Settings;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.graphics.Converter;

public class AppMenu extends CanvasPieMenu {
	public static class AppIcon extends CanvasPieMenu.CanvasIcon {
		public final Rect hitRect = new Rect();
		public final ComponentName componentName;
		public final String label;
		public final UserHandle userHandle;

		AppIcon(ComponentName componentName, String label, Drawable icon,
				UserHandle userHandle) {
			super(Converter.getBitmapFromDrawable(icon));
			this.componentName = componentName;
			this.label = label;
			this.userHandle = userHandle;
		}
	}

	public interface UpdateListener {
		void onUpdate();
	}

	public static final boolean HAS_LAUNCHER_APP =
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

	private static final String MENU = "menu";

	private final Handler handler = new Handler(Looper.getMainLooper());
	private final HashMap<LauncherItemKey, AppIcon> apps = new HashMap<>();
	private final Comparator<AppIcon> appLabelComparator = (left, right) -> {
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

	private UpdateListener updateListener;
	private LauncherApps launcherApps;
	private boolean indexing = false;

	public boolean launchSelectedApp(Context context) {
		int selectedIcon = getSelectedIcon();
		if (selectedIcon > -1 && selectedIcon < icons.size()) {
			launchApp(context, ((AppIcon) icons.get(selectedIcon)));
			return true;
		}
		return false;
	}

	public void launchApp(Context context, AppIcon icon) {
		if (HAS_LAUNCHER_APP) {
			try {
				LauncherApps lm = getLauncherApps(context);
				if (lm.isActivityEnabled(icon.componentName,
						icon.userHandle)) {
					lm.startMainActivity(
							icon.componentName,
							icon.userHandle,
							icon.rect,
							null);
				}
			} catch (SecurityException e) {
				// Ignore. According to vitals, `startMainActivity()`
				// throws this for unknown reasons.
			} catch (IllegalArgumentException e) {
				// Ignore. According to vitals, 'isActivityEnabled()`
				// throws this for unknown reasons.
			}
		} else {
			PackageManager pm = context.getPackageManager();
			Intent intent;
			if (pm != null && (intent = pm.getLaunchIntentForPackage(
					icon.componentName.getPackageName())) != null) {
				context.startActivity(intent);
			}
		}
	}

	public void launchAppInfo(Context context, AppIcon icon) {
		if (HAS_LAUNCHER_APP) {
			getLauncherApps(context).startAppDetailsActivity(
					icon.componentName,
					icon.userHandle,
					icon.rect,
					null);
		} else {
			Intent intent = new Intent(
					Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			intent.addCategory(Intent.CATEGORY_DEFAULT);
			intent.setData(Uri.parse("package:" +
					icon.componentName.getPackageName()));
			context.startActivity(intent);
		}
	}

	public void setUpdateListener(UpdateListener listener) {
		updateListener = listener;
	}

	public void store(Context context) {
		storeMenu(context, icons);
	}

	public List<AppIcon> filterAppsBy(String query) {
		if (indexing) {
			return null;
		}
		if (query == null) {
			query = "";
		}
		query = query.trim().toLowerCase(Locale.getDefault());
		ArrayList<AppIcon> list = new ArrayList<>();
		ArrayList<AppIcon> contain = new ArrayList<>();
		ArrayList<AppIcon> hamming = new ArrayList<>();
		if (query.length() < 1) {
			list.addAll(apps.values());
		} else {
			for (Map.Entry<LauncherItemKey, AppIcon> entry : apps.entrySet()) {
				AppIcon appIcon = entry.getValue();
				String label = appIcon.label.toLowerCase(Locale.getDefault());
				if (label.startsWith(query)) {
					list.add(appIcon);
				} else if (label.contains(query)) {
					contain.add(appIcon);
				} else if (hammingDistance(label, query) < 2) {
					hamming.add(appIcon);
				}
			}
		}
		Collections.sort(list, appLabelComparator);
		Collections.sort(contain, appLabelComparator);
		Collections.sort(hamming, appLabelComparator);
		list.addAll(contain);
		list.addAll(hamming);
		return list;
	}

	public void removePackageAsync(final String packageName,
			final UserHandle userHandle) {
		Executors.newSingleThreadExecutor().execute(() -> {
			removePackage(packageName, userHandle);
			handler.post(() -> {
				if (updateListener != null) {
					updateListener.onUpdate();
				}
			});
		});
	}

	public boolean isEmpty() {
		return apps.isEmpty();
	}

	public boolean isIndexing() {
		return indexing;
	}

	public void indexAppsAsync(Context context) {
		indexAppsAsync(context, null, null);
	}

	public void indexAppsAsync(Context context,
			String packageNameRestriction,
			UserHandle userHandleRestriction) {
		indexing = true;
		Executors.newSingleThreadExecutor().execute(() -> {
			Map<LauncherItemKey, AppIcon> newApps = new HashMap<>();
			if (packageNameRestriction != null) {
				// Copy apps since we're indexing just one app.
				newApps.putAll(apps);
				removePackageFromApps(newApps, packageNameRestriction,
						userHandleRestriction);
				// No need to call removePackageFromPieMenu() because the
				// menu will be re-created by createMenu() after indexing.
			}
			indexApps(context,
					packageNameRestriction,
					userHandleRestriction,
					newApps);
			List<Icon> newIcons = createMenu(context, newApps);
			handler.post(() -> {
				apps.clear();
				apps.putAll(newApps);
				icons.clear();
				icons.addAll(newIcons);
				indexing = false;
				if (updateListener != null) {
					updateListener.onUpdate();
				}
			});
		});
	}

	private void indexApps(
			Context context,
			String packageNameRestriction,
			UserHandle userHandleRestriction,
			Map<LauncherItemKey, AppIcon> allApps) {
		String skip = context.getPackageName();
		if (HAS_LAUNCHER_APP) {
			indexProfilesApps(
					(LauncherApps) context.getSystemService(
							Context.LAUNCHER_APPS_SERVICE),
					(UserManager) context.getSystemService(
							Context.USER_SERVICE),
					allApps, packageNameRestriction, userHandleRestriction,
					skip);
		} else {
			indexIntentsApps(context.getPackageManager(),
					allApps, packageNameRestriction,
					skip);
		}
	}

	private static void indexIntentsApps(
			PackageManager pm,
			Map<LauncherItemKey, AppIcon> allApps,
			String packageNameRestriction,
			String skipPackage) {
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		if (packageNameRestriction != null) {
			intent.setPackage(packageNameRestriction);
		}
		List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
		for (ResolveInfo info : activities) {
			String packageName = info.activityInfo.applicationInfo.packageName;
			if (skipPackage.equals(packageName)) {
				// Always skip this package.
				continue;
			}
			addApp(allApps,
					getComponentName(info.activityInfo),
					info.loadLabel(pm).toString(),
					info.loadIcon(pm),
					null);
		}
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static void indexProfilesApps(
			LauncherApps la,
			UserManager um,
			Map<LauncherItemKey, AppIcon> allApps,
			String packageNameRestriction,
			UserHandle userHandleRestriction,
			String skipPackage) {
		List<UserHandle> profiles =
				packageNameRestriction != null && userHandleRestriction != null
						? Collections.singletonList(userHandleRestriction)
						: um.getUserProfiles();
		// If packageNameRestriction == null and userHandleRestriction != null
		// apps was cleared and all profiles will be indexed.
		for (UserHandle profile : profiles) {
			for (LauncherActivityInfo info :
					la.getActivityList(packageNameRestriction, profile)) {
				String packageName = info.getApplicationInfo().packageName;
				if (skipPackage.equals(packageName)) {
					// Always skip this package.
					continue;
				}
				try {
					addApp(allApps,
							info.getComponentName(),
							info.getLabel().toString(),
							info.getBadgedIcon(0),
							profile);
				} catch (Exception e) {
					// According to Vitals, `getBadgedIcon()` may throw
					// a NPE on some devices. Ignore.
				}
			}
		}
	}

	private static void addApp(Map<LauncherItemKey, AppIcon> allApps,
			ComponentName componentName, String label,
			Drawable icon, UserHandle userHandle) {
		allApps.put(new LauncherItemKey(componentName, userHandle),
				new AppIcon(componentName, label, icon, userHandle));
	}

	private static List<Icon> createMenu(Context context,
			Map<LauncherItemKey, AppIcon> allApps) {
		List<Icon> menu = restoreMenu(context, allApps);
		if (menu.isEmpty()) {
			createInitialMenu(menu, allApps, context.getPackageManager());
		}
		return menu;
	}

	private static void createInitialMenu(List<Icon> menu,
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
		UserHandle userHandle = HAS_LAUNCHER_APP
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
		int max = Math.min(allApps.size(), 8);
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

	private static void addMenuIcon(List<Icon> menu, AppIcon appIcon) {
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
				getComponentName(resolveInfo.activityInfo),
				userHandle);
	}

	private static ComponentName getComponentName(ActivityInfo info) {
		return new ComponentName(info.packageName, info.name);
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

	private void removePackage(String packageName, UserHandle userHandle) {
		removePackageFromApps(apps, packageName, userHandle);
		removePackageFromPieMenu(packageName, userHandle);
	}

	private static void removePackageFromApps(
			Map<LauncherItemKey, AppIcon> allApps,
			String packageName,
			UserHandle userHandle) {
		Iterator<Map.Entry<LauncherItemKey, AppIcon>> it =
				allApps.entrySet().iterator();
		while (it.hasNext()) {
			AppIcon appIcon = it.next().getValue();
			if (packageName.equals(appIcon.componentName.getPackageName()) &&
					(userHandle == null || userHandle.equals(appIcon.userHandle))) {
				it.remove();
			}
		}
	}

	private void removePackageFromPieMenu(String packageName,
			UserHandle userHandle) {
		Iterator<Icon> it = icons.iterator();
		while (it.hasNext()) {
			AppIcon appIcon = ((AppIcon) it.next());
			if (packageName.equals(appIcon.componentName.getPackageName()) &&
					(userHandle == null || userHandle.equals(appIcon.userHandle))) {
				it.remove();
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private LauncherApps getLauncherApps(Context context) {
		if (launcherApps == null) {
			launcherApps = (LauncherApps) context.getSystemService(
					Context.LAUNCHER_APPS_SERVICE);
		}
		return launcherApps;
	}

	private static List<Icon> restoreMenu(Context context,
			Map<LauncherItemKey, AppIcon> apps) {
		ArrayList<Icon> icons = new ArrayList<>();
		try {
			for (String line : readLines(context.openFileInput(MENU))) {
				Icon icon = apps.get(
						LauncherItemKey.unflattenFromString(context, line));
				if (icon != null) {
					icons.add(icon);
				}
			}
		} catch (FileNotFoundException e) {
			// Return an empty array.
		}
		return icons;
	}

	private static List<String> readLines(InputStream is) {
		ArrayList<String> list = new ArrayList<>();
		BufferedReader reader = null;
		// It's not possible to use automatic resource management
		// for this statement because it requires minSDK 19.
		try {
			// StandardCharsets.UTF_8 cannot be used because it requires
			// minSDK 19.
			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			while (reader.ready()) {
				list.add(reader.readLine());
			}
		} catch (IOException e) {
			// Return what we got so far.
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				// Ignore, can't do anything about it.
			}
		}
		return list;
	}

	private static boolean storeMenu(Context context, List<Icon> icons) {
		ArrayList<String> items = new ArrayList<>();
		for (CanvasPieMenu.Icon icon : icons) {
			items.add(LauncherItemKey.flattenToString(
					context,
					((AppIcon) icon).componentName,
					((AppIcon) icon).userHandle));
		}
		try {
			return writeLines(context.openFileOutput(MENU,
					Context.MODE_PRIVATE), items);
		} catch (IOException e) {
			return false;
		}
	}

	private static boolean writeLines(OutputStream os, List<String> lines) {
		if (os == null) {
			return false;
		}
		try {
			// StandardCharsets.UTF_8 cannot be used because it requires
			// minSDK 19.
			byte[] lf = "\n".getBytes("UTF-8");
			for (String line : lines) {
				os.write(line.getBytes("UTF-8"));
				os.write(lf);
			}
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			try {
				os.close();
			} catch (IOException e) {
				// Ignore, can't do anything about it.
			}
		}
	}

	private static int hammingDistance(String a, String b) {
		int count = 0;
		for (int i = 0, l = Math.min(a.length(), b.length()); i < l; ++i) {
			if (a.charAt(i) != b.charAt(i)) {
				++count;
			}
		}
		return count;
	}
}
