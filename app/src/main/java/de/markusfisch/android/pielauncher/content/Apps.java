package de.markusfisch.android.pielauncher.content;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.activity.HomeActivity;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.graphics.Converter;
import de.markusfisch.android.pielauncher.io.HiddenAppsStorage;
import de.markusfisch.android.pielauncher.io.MenuStorage;
import de.markusfisch.android.pielauncher.preference.Preferences;

public class Apps {
	public interface UpdateListener {
		void onUpdate();

		void onShowAllAppsOnResume();
	}

	public static class AppIcon extends CanvasPieMenu.CanvasIcon {
		public final Rect hitRect = new Rect();
		public final String label;
		public final UserHandle userHandle;

		// Can't be final because apps can change their components
		// after indexing (e.g. when switching icons).
		public ComponentName componentName;

		AppIcon(ComponentName componentName, String label, Drawable icon,
				UserHandle userHandle) {
			super(Converter.getBitmapFromDrawable(icon));
			this.componentName = componentName;
			this.label = label;
			this.userHandle = userHandle;
		}

		AppIcon(ComponentName componentName, String label, Bitmap icon,
				UserHandle userHandle) {
			super(icon);
			this.componentName = componentName;
			this.label = label;
			this.userHandle = userHandle;
		}

		byte[] iconBytes;
		double frecencyScore;
		long frecencyUpdatedAt;
	}

	public static final String MENU_PRIMARY = "menu";
	public static final String MENU_SECONDARY = "menu_alt";

	public final ArrayList<AppIcon> menuPrimary = new ArrayList<>();
	public final ArrayList<AppIcon> menuSecondary = new ArrayList<>();
	public final HiddenAppsStorage hiddenAppsStorage = new HiddenAppsStorage();
	public final HashMap<LauncherItemKey, AppIcon> apps = new HashMap<>();

	private final Handler handler = new Handler(Looper.getMainLooper());
	private final ExecutorService executor =
			Executors.newSingleThreadExecutor();

	private UpdateListener updateListener;
	private String drawerPackageName;
	private boolean indexing = false;

	public boolean isDrawerIcon(AppIcon icon) {
		return icon != null &&
				isDrawerPackageName(icon.componentName.getPackageName());
	}

	public boolean isDrawerPackageName(String packageName) {
		return drawerPackageName != null &&
				drawerPackageName.equals(packageName);
	}

	public UpdateListener getUpdateListener() {
		return updateListener;
	}

	public void setUpdateListener(UpdateListener listener) {
		updateListener = listener;
	}

	public void propagateUpdate() {
		if (updateListener != null) {
			updateListener.onUpdate();
		}
	}

	public void store(Context context) {
		MenuStorage.store(context, MENU_PRIMARY, menuPrimary);
		MenuStorage.store(context, MENU_SECONDARY, menuSecondary);
		hiddenAppsStorage.store(context);
	}

	public void removePackageAsync(Context context, String packageName,
			UserHandle userHandle) {
		if (context == null || packageName == null) {
			return;
		}
		Context appContext = context.getApplicationContext();
		executor.execute(() -> {
			hiddenAppsStorage.removeAndStore(appContext, packageName);
			PieLauncherApp.getDatabase(appContext).removePackage(
					appContext, packageName, userHandle);
			handler.post(() -> {
				removePackageFromApps(apps, packageName, userHandle);
				removePackageFromPieMenu(menuPrimary, packageName,
						userHandle);
				removePackageFromPieMenu(menuSecondary, packageName,
						userHandle);
				propagateUpdate();
			});
		});
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
		return indexing;
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
		hiddenAppsStorage.restore(context);
		HashSet<ComponentName> hideApps = hiddenAppsStorage.copyComponentNames();
		hideApps.add(new ComponentName(context, HomeActivity.class));
		Map<LauncherItemKey, AppIcon> newApps = new HashMap<>();
		final boolean needsCacheRestore =
				apps.isEmpty() && packageNameRestriction == null;
		// If apps haven't been indexed yet, a partial index would restore
		// the menu against a near-empty map, causing all stored entries to
		// be silently dropped. Fall back to a full index in that case.
		final String packageRestriction =
				packageNameRestriction != null && !apps.isEmpty()
						? packageNameRestriction
						: null;
		final UserHandle userRestriction = packageRestriction != null
				? userHandleRestriction
				: null;
		if (packageRestriction != null) {
			// Copy apps since we're indexing just one app.
			newApps.putAll(apps);
			removePackageFromApps(newApps, packageRestriction, userRestriction);
			// No need to call removePackageFromPieMenu() because the
			// menu will be re-created by getPrimaryMenu() after indexing.
		}
		executor.execute(() -> {
			if (needsCacheRestore) {
				restoreAppsFromCache(context, hideApps);
			}
			indexApps(context,
					packageRestriction,
					userRestriction,
					hideApps,
					newApps);
			Database database = PieLauncherApp.getDatabase(context);
			database.restoreFrecency(context, newApps);
			Menus menus = compileMenus(context, newApps);
			if (packageRestriction == null) {
				database.replaceAllApps(context, newApps);
			} else {
				database.replacePackage(context,
						packageRestriction,
						userRestriction,
						newApps);
			}
			handler.post(() -> updateApps(newApps, menus, true));
		});
		return true;
	}

	private void restoreAppsFromCache(Context context,
			HashSet<ComponentName> hideApps) {
		Map<LauncherItemKey, AppIcon> cachedApps = new HashMap<>();
		PieLauncherApp.getDatabase(context).restoreApps(context, cachedApps);
		if (cachedApps.isEmpty()) {
			return;
		}
		removeApps(cachedApps, hideApps);
		if (cachedApps.isEmpty()) {
			return;
		}
		Menus menus = compileMenus(context, cachedApps);
		handler.post(() -> updateApps(cachedApps, menus, false));
	}

	private Menus compileMenus(Context context,
			Map<LauncherItemKey, AppIcon> newApps) {
		return new Menus(
				getPrimaryMenu(context, newApps,
						PieLauncherApp.getPrefs(context).openListWith() ==
								Preferences.OPEN_LIST_WITH_ICON),
				getSecondaryMenu(context, newApps));
	}

	private void updateApps(
			Map<LauncherItemKey, AppIcon> newApps,
			Menus menus,
			boolean done) {
		apps.clear();
		apps.putAll(newApps);
		menuPrimary.clear();
		menuPrimary.addAll(menus.primary);
		menuSecondary.clear();
		menuSecondary.addAll(menus.secondary);
		if (done) {
			indexing = false;
		}
		propagateUpdate();
	}

	private static class Menus {
		final List<AppIcon> primary;
		final List<AppIcon> secondary;

		Menus(List<AppIcon> primary, List<AppIcon> secondary) {
			this.primary = primary;
			this.secondary = secondary;
		}
	}

	private static void removeApps(
			Map<LauncherItemKey, AppIcon> allApps,
			Set<ComponentName> componentNames) {
		Iterator<Map.Entry<LauncherItemKey, AppIcon>> it =
				allApps.entrySet().iterator();
		while (it.hasNext()) {
			if (componentNames.contains(it.next().getValue().componentName)) {
				it.remove();
			}
		}
	}

	private void indexApps(
			Context context,
			String packageNameRestriction,
			UserHandle userHandleRestriction,
			HashSet<ComponentName> hideApps,
			Map<LauncherItemKey, AppIcon> allApps) {
		PackageManager pm = context.getPackageManager();
		PieLauncherApp.iconPack.selectPack(pm,
				PieLauncherApp.getPrefs(context).getIconPack());
		PieLauncherApp.iconPack.restoreMappings(context);
		if (AppLauncher.HAS_LAUNCHER_APP) {
			indexProfilesApps(
					AppLauncher.getLauncherApps(context),
					AppLauncher.getUserManager(context),
					allApps,
					packageNameRestriction,
					userHandleRestriction,
					hideApps);
			if (!allApps.isEmpty()) {
				return;
			}
			// Fall through if no apps were loaded (can happen on some
			// Android distributions where AppLauncher.getActivityList() fails).
		}
		indexIntentsApps(pm, allApps, packageNameRestriction, hideApps);
	}

	private static void indexIntentsApps(
			PackageManager pm,
			Map<LauncherItemKey, AppIcon> allApps,
			String packageNameRestriction,
			Set<ComponentName> hideApps) {
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		if (packageNameRestriction != null) {
			intent.setPackage(packageNameRestriction);
		}
		List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
		for (ResolveInfo info : activities) {
			ComponentName componentName = getComponentName(info.activityInfo);
			if (hideApps.contains(componentName)) {
				continue;
			}
			Drawable icon = PieLauncherApp.iconPack.getIcon(componentName);
			if (icon == null) {
				icon = info.loadIcon(pm);
			}
			addApp(allApps,
					componentName,
					info.loadLabel(pm).toString(),
					icon,
					null);
		}
	}

	@SuppressLint("UseRequiresApi")
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static void indexProfilesApps(
			LauncherApps la,
			UserManager um,
			Map<LauncherItemKey, AppIcon> allApps,
			String packageNameRestriction,
			UserHandle userHandleRestriction,
			Set<ComponentName> hideApps) {
		List<UserHandle> profiles;
		if (packageNameRestriction != null && userHandleRestriction != null) {
			profiles = Collections.singletonList(userHandleRestriction);
		} else {
			profiles = AppLauncher.getProfiles(um);
		}
		if (la == null || profiles.isEmpty()) {
			return;
		}
		// If packageNameRestriction == null and userHandleRestriction != null
		// apps was cleared and all profiles will be indexed.
		for (UserHandle profile : profiles) {
			for (LauncherActivityInfo info :
					AppLauncher.getActivityList(la, packageNameRestriction, profile)) {
				ComponentName componentName = info.getComponentName();
				if (hideApps.contains(componentName)) {
					continue;
				}
				Drawable icon = PieLauncherApp.iconPack.getIcon(componentName);
				if (icon == null &&
						(icon = getBadgedIcon(info)) == null) {
					continue;
				}
				addApp(allApps,
						componentName,
						info.getLabel().toString(),
						icon,
						profile);
			}
		}
	}

	@SuppressLint("UseRequiresApi")
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

	private List<AppIcon> getPrimaryMenu(Context context,
			Map<LauncherItemKey, AppIcon> allApps,
			boolean useDrawerIcon) {
		AppIcon drawerIcon = useDrawerIcon
				? addDrawerIcon(context, allApps)
				: null;
		ArrayList<AppIcon> menu = MenuStorage.restore(context,
				MENU_PRIMARY, allApps);
		if (drawerIcon != null) {
			if (!menu.contains(drawerIcon)) {
				menu.add(0, drawerIcon);
			}
			removePackageFromApps(allApps, drawerPackageName, null);
		} else {
			drawerPackageName = null;
		}
		if (menu.isEmpty()) {
			MenuDefaults.createInitialMenu(menu, allApps,
					context.getPackageManager());
			MenuStorage.store(context, MENU_PRIMARY, menu);
		}
		return menu;
	}

	private List<AppIcon> getSecondaryMenu(Context context,
			Map<LauncherItemKey, AppIcon> allApps) {
		ArrayList<AppIcon> menu = MenuStorage.restore(context,
				MENU_SECONDARY, allApps);
		if (menu.isEmpty()) {
			// Just add a few apps so users aren't confused by an empty menu.
			MenuDefaults.createMenuForPopularApps(menu, allApps, 4);
			MenuStorage.store(context, MENU_SECONDARY, menu);
		}
		return menu;
	}

	private AppIcon addDrawerIcon(Context context,
			Map<LauncherItemKey, AppIcon> allApps) {
		String appPackageName = context.getPackageName();
		drawerPackageName = appPackageName + ".drawer";
		ComponentName componentName = new ComponentName(
				drawerPackageName, "Drawer");
		Drawable icon = PieLauncherApp.iconPack.getIcon(componentName);
		if (icon == null) {
			icon = Converter.getDrawable(
					context.getResources(), R.drawable.ic_drawer);
		}
		return addApp(allApps,
				componentName,
				"Drawer",
				icon,
				null);
	}

	public static ComponentName getComponentName(ActivityInfo info) {
		return new ComponentName(info.packageName, info.name);
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

	private void removePackageFromPieMenu(List<AppIcon> icons,
			String packageName, UserHandle userHandle) {
		Iterator<AppIcon> it = icons.iterator();
		while (it.hasNext()) {
			AppIcon appIcon = it.next();
			if (packageName.equals(appIcon.componentName.getPackageName()) &&
					(userHandle == null || userHandle.equals(appIcon.userHandle))) {
				it.remove();
			}
		}
	}
}
