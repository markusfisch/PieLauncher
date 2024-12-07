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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.graphics.Converter;
import de.markusfisch.android.pielauncher.io.HiddenApps;
import de.markusfisch.android.pielauncher.io.Menu;
import de.markusfisch.android.pielauncher.preference.Preferences;

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

	public final HiddenApps hiddenApps = new HiddenApps();

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
	private String drawerPackageName;
	private boolean indexing = false;
	private boolean isAlphabetFiltering = false;

	public AppIcon getSelectedApp() {
		int index = getSelectedIcon();
		return index > -1 && index < icons.size()
				? (AppIcon) icons.get(index)
				: null;
	}

	public boolean isDrawerIcon(AppIcon icon) {
		return icon != null &&
				isDrawerPackageName(icon.componentName.getPackageName());
	}

	public boolean isDrawerPackageName(String packageName) {
		return drawerPackageName != null &&
				drawerPackageName.equals(packageName);
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
			} catch (Exception e) {
				// According to vitals, `startMainActivity()`
				// and `isActivityEnabled()` can throw all kinds
				// of exceptions this app can do nothing about.
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
		Menu.store(context, icons);
		hiddenApps.store(context);
	}

	public List<AppIcon> filterAppsBy(Context context, String query, boolean isAlphabetFilter, Set<String> aliases) {
		if (indexing) {
			return null;
		}
		Locale defaultLocale = Locale.getDefault();
		query = query == null
				? ""
				: query.trim().toLowerCase(defaultLocale);

		ArrayList<AppIcon> list = new ArrayList<>();
		if (query.isEmpty()) {
			list.addAll(apps.values());
		} else {
			int item = PieLauncherApp.getPrefs(context).getSearchParameter();
			for (Map.Entry<LauncherItemKey, AppIcon> entry : apps.entrySet()) {
				AppIcon appIcon = entry.getValue();
				String subject = getSubject(item, appIcon, defaultLocale);

				if (isAlphabetFilter) {
					// For alphabet bar: match only the first letter (and aliases)
					if (query.equals("#")) {
						// For "#", match anything that doesn't start with a letter
						if (subject.length() > 0 && !Character.isLetter(subject.charAt(0))) {
							list.add(appIcon);
						}
					} else if (query.equals("⚑")) {
						// For "🌐", match anything that isn't a Latin char or number
						// This includes characters with diacritics like umlauts and accents
						if (subject.length() > 0 && !subject.matches("[\\p{IsLatin}\\p{Nd}].*")) {
							list.add(appIcon);
						}
					} else if (subject.length() > 0 && (aliases.stream().anyMatch(alias -> subject.toUpperCase().startsWith(alias.toUpperCase())) || subject.startsWith(query))) {
						list.add(appIcon);
					}
				} else {
					// For normal search: use the existing search strategy
					Preferences prefs = PieLauncherApp.getPrefs(context);
					int strategy = prefs.getSearchStrictness();
					boolean add = false;
					switch (strategy) {
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
					} else if (strategy == Preferences.SEARCH_STRICTNESS_HAMMING &&
							hammingDistance(subject, query) < 2) {
						list.add(appIcon);
					}
				}
			}
		}

		Collections.sort(list, appLabelComparator);
		return list;
	}

	public void removePackage(Context context, String packageName,
			UserHandle userHandle) {
		removePackageFromApps(apps, packageName, userHandle);
		removePackageFromPieMenu(packageName, userHandle);
		hiddenApps.removeAndStore(context, packageName);
		if (updateListener != null) {
			updateListener.onUpdate();
		}
	}

	public void updateIconsAsync(Context context) {
		if (!indexAppsAsync(context)) {
			handler.postDelayed(() -> updateIconsAsync(context), 1000L);
		}
	}

	public boolean isEmpty() {
		return apps.isEmpty();
	}

	public boolean isIndexing() {
		return apps.isEmpty() && indexing;
	}

	public boolean indexAppsAsync(Context context) {
		return indexAppsAsync(context, null, null);
	}

	public boolean indexAppsAsync(Context context,
			String packageNameRestriction,
			UserHandle userHandleRestriction) {
		if (indexing) {
			return false;
		}
		indexing = true;
		hiddenApps.restore(context);
		HashSet<String> hideApps = new HashSet<>(hiddenApps.packageNames);
		Map<LauncherItemKey, AppIcon> newApps = new HashMap<>();
		if (packageNameRestriction != null) {
			// Copy apps since we're indexing just one app.
			newApps.putAll(apps);
			removePackageFromApps(newApps, packageNameRestriction,
					userHandleRestriction);
			// No need to call removePackageFromPieMenu() because the
			// menu will be re-created by createMenu() after indexing.
		}
		Executors.newSingleThreadExecutor().execute(() -> {
			indexApps(context,
					packageNameRestriction,
					userHandleRestriction,
					hideApps,
					newApps);
			List<Icon> newIcons = createMenu(context, newApps,
					PieLauncherApp.getPrefs(context).openListWith() ==
							Preferences.OPEN_LIST_WITH_ICON);
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
		return true;
	}

	private static void indexApps(
			Context context,
			String packageNameRestriction,
			UserHandle userHandleRestriction,
			HashSet<String> hideApps,
			Map<LauncherItemKey, AppIcon> allApps) {
		PackageManager pm = context.getPackageManager();
		PieLauncherApp.iconPack.restoreMappingsIfEmpty(context);
		PieLauncherApp.iconPack.selectPack(pm,
				PieLauncherApp.getPrefs(context).getIconPack());
		hideApps.add(context.getPackageName());
		if (HAS_LAUNCHER_APP) {
			indexProfilesApps(
					(LauncherApps) context.getSystemService(
							Context.LAUNCHER_APPS_SERVICE),
					(UserManager) context.getSystemService(
							Context.USER_SERVICE),
					allApps, packageNameRestriction, userHandleRestriction,
					hideApps);
		} else {
			indexIntentsApps(pm, allApps, packageNameRestriction, hideApps);
		}
	}

	private static void indexIntentsApps(
			PackageManager pm,
			Map<LauncherItemKey, AppIcon> allApps,
			String packageNameRestriction,
			Set<String> hideApps) {
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		if (packageNameRestriction != null) {
			intent.setPackage(packageNameRestriction);
		}
		List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
		for (ResolveInfo info : activities) {
			String packageName = info.activityInfo.applicationInfo.packageName;
			if (hideApps.contains(packageName)) {
				continue;
			}
			Drawable icon = PieLauncherApp.iconPack.getIcon(packageName);
			if (icon == null) {
				icon = info.loadIcon(pm);
			}
			addApp(allApps,
					getComponentName(info.activityInfo),
					info.loadLabel(pm).toString(),
					icon,
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
			Set<String> hideApps) {
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
				if (hideApps.contains(packageName)) {
					// Always skip this package.
					continue;
				}
				Drawable icon = PieLauncherApp.iconPack.getIcon(packageName);
				if (icon == null &&
						(icon = getBadgedIcon(info)) == null) {
					continue;
				}
				addApp(allApps,
						info.getComponentName(),
						info.getLabel().toString(),
						icon,
						profile);
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static Drawable getBadgedIcon(LauncherActivityInfo info) {
		// According to Vitals, `getBadgedIcon()` can throw a NPE
		// for some unknown reason. Let's try `getIcon()` then.
		try {
			return info.getBadgedIcon(0);
		} catch (Exception e) {
			try {
				return info.getIcon(0);
			} catch (Exception e2) {
				return null;
			}
		}
	}

	private static AppIcon addApp(Map<LauncherItemKey, AppIcon> allApps,
			ComponentName componentName, String label,
			Drawable icon, UserHandle userHandle) {
		AppIcon appIcon = new AppIcon(componentName, label, icon, userHandle);
		allApps.put(new LauncherItemKey(componentName, userHandle), appIcon);
		return appIcon;
	}

	private List<Icon> createMenu(Context context,
			Map<LauncherItemKey, AppIcon> allApps,
			boolean useDrawerIcon) {
		AppMenu.Icon drawerIcon = useDrawerIcon
				? addDrawerIcon(context, allApps)
				: null;
		ArrayList<Icon> menu = Menu.restore(context, allApps);
		if (menu.isEmpty()) {
			createInitialMenu(menu, allApps, context.getPackageManager());
		}
		if (drawerIcon != null) {
			if (!menu.contains(drawerIcon)) {
				menu.add(0, drawerIcon);
			}
			removePackageFromApps(allApps, drawerPackageName, null);
		} else {
			drawerPackageName = null;
		}
		return menu;
	}

	private AppMenu.Icon addDrawerIcon(Context context,
			Map<LauncherItemKey, AppIcon> allApps) {
		String appPackageName = context.getPackageName();
		drawerPackageName = appPackageName + ".drawer";
		Drawable icon = PieLauncherApp.iconPack.getIcon(drawerPackageName);
		if (icon == null) {
			icon = Converter.getDrawable(
					context.getResources(), R.drawable.ic_drawer);
		}
		return addApp(allApps,
				new ComponentName(drawerPackageName, "Drawer"),
				"Drawer",
				icon,
				null);
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

	private static String getSubject(int item, AppIcon appIcon,
			Locale defaultLocale) {
		if (item == Preferences.SEARCH_PARAMETER_PACKAGE_NAME) {
			return appIcon.componentName.getPackageName()
					.toLowerCase(defaultLocale);
		}
		return appIcon.label.toLowerCase(defaultLocale);
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

	public void setAlphabetFiltering(boolean filtering) {
		isAlphabetFiltering = filtering;
	}

	public boolean isAlphabetFiltering() {
		return isAlphabetFiltering;
	}
}
