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
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Base64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

		void onProfilesChanged();

		void onShowAllAppsOnResume();
	}

	public interface AppShortcutsListener {
		void onAppShortcuts(AppIcon app, List<AppIcon> shortcuts);
	}

	public static class AppIcon extends CanvasPieMenu.CanvasIcon {
		public final Rect hitRect = new Rect();
		public final String label;
		public final UserHandle userHandle;

		// Can't be final because apps can change their components
		// after indexing (e.g. when switching icons).
		public ComponentName componentName;

		// Non-null when this icon is a pinned shortcut (e.g. a PWA added to
		// the home screen from a browser) instead of an installed app. In
		// that case the item is launched with LauncherApps.startShortcut()
		// using these two values instead of starting a launcher activity.
		public String shortcutId;
		public String shortcutPackage;

		AppIcon(ComponentName componentName,
				String label,
				Drawable icon,
				UserHandle userHandle) {
			super(Converter.getBitmapFromDrawable(icon));
			this.componentName = componentName;
			this.label = label;
			this.userHandle = userHandle;
		}

		AppIcon(ComponentName componentName,
				String label,
				Bitmap icon,
				UserHandle userHandle) {
			super(icon);
			this.componentName = componentName;
			this.label = label;
			this.userHandle = userHandle;
		}

		byte[] iconBytes;
		double frecencyScore;
		long frecencyUpdatedAt;
		// Search terms this app was launched from. Null if there are none.
		HashMap<String, SearchPick> searchPicks;
	}

	public static final String MENU_PRIMARY = "menu";
	public static final String MENU_SECONDARY = "menu_alt";
	// Synthetic package used for the ComponentName of pinned shortcuts so
	// they flow through the same maps, menus and persistence as apps while
	// never colliding with a real package (which would let a browser update
	// wipe its shortcuts via the package-based add/remove paths).
	public static final String SHORTCUT_PACKAGE =
			"de.markusfisch.android.pielauncher.shortcut";
	public static final String ACTION_CONFIRM_PIN_SHORTCUT =
			"android.content.pm.action.CONFIRM_PIN_SHORTCUT";

	public final MenuItems menuPrimary = new MenuItems();
	public final MenuItems menuSecondary = new MenuItems();
	public final HiddenAppsStorage hiddenAppsStorage = new HiddenAppsStorage();
	public final AppSnapshot apps = new AppSnapshot();

	private static final int RECONCILED_MENU = 1;
	private static final int RETRY_PACKAGE = 2;

	private final Handler handler = new Handler(Looper.getMainLooper());
	private final HashSet<LauncherItemKey> pendingPackageRetries =
			new HashSet<>();

	private final Set<LauncherItemKey> removedPinnedShortcuts = new HashSet<>();

	private UpdateListener updateListener;
	private String drawerPackageName;
	private boolean indexing = false;
	private boolean editingMenu;
	private boolean indexAfterEditing;
	private boolean menusRestored;

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
		if (!menusRestored) {
			return;
		}
		MenuStorage.store(context, MENU_PRIMARY, menuPrimary);
		MenuStorage.store(context, MENU_SECONDARY, menuSecondary);
		hiddenAppsStorage.store(context);
	}

	public static boolean isShortcut(AppIcon icon) {
		return icon != null && icon.shortcutId != null;
	}

	public static boolean isPinItemRequest(Intent intent) {
		return intent != null &&
				ACTION_CONFIRM_PIN_SHORTCUT.equals(intent.getAction());
	}

	public static ComponentName shortcutComponentName(
			String shortcutPackage,
			String shortcutId) {
		// The class part only needs to be a stable, unique token per
		// (owner, id); it's never decoded because both values are stored
		// next to the icon. URL-safe Base64 avoids the '/' and '#' that
		// ComponentName and LauncherItemKey use as separators.
		String token = Base64.encodeToString(
				(shortcutPackage + "\n" + shortcutId).getBytes(),
				Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
		return new ComponentName(SHORTCUT_PACKAGE, token);
	}

	// Handles the CONFIRM_PIN_SHORTCUT intent the system sends to the default
	// launcher when an app (a browser adding a PWA, for instance) wants to pin
	// a shortcut. Accepts the request, persists it and adds it to the drawer.
	public void acceptPinRequest(Context context, Intent intent) {
		// Keep the PinItemRequest reference out of this method so it can be
		// safely invoked on pre-O devices without tripping class verification.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
				isPinItemRequest(intent)) {
			Context appContext = context.getApplicationContext();
			PieLauncherApp.getDatabase(appContext).execute(() ->
					acceptPinRequestApi26(appContext, intent));
		}
	}

	// Removes a pinned shortcut from the launcher completely.
	public void removePinnedShortcut(Context context, AppIcon icon) {
		if (!isShortcut(icon)) {
			return;
		}
		LauncherItemKey key = new LauncherItemKey(icon.componentName, icon.userHandle);
		removedPinnedShortcuts.add(key);
		PieLauncherApp.getDatabase(context).removePinnedShortcut(context, icon);
		apps.remove(key);
		menuPrimary.remove(icon);
		menuSecondary.remove(icon);
		store(context);
		propagateUpdate();
		AppLauncher.toast(context, context.getString(
				R.string.removed_from_home_screen, icon.label));
	}

	public void removePackageAsync(
			Context context,
			String packageName,
			UserHandle userHandle) {
		if (context == null || packageName == null) {
			return;
		}
		Context appContext = context.getApplicationContext();
		PieLauncherApp.getDatabase(context).execute(() -> {
			hiddenAppsStorage.removeAndStore(appContext, packageName);
			PieLauncherApp.getDatabase(appContext).removePackage(
					appContext, packageName, userHandle);
			handler.post(() -> {
				// Cancel a drag before removing icons so its backup cannot
				// put an uninstalled app back into the menu.
				if (editingMenu && updateListener != null) {
					updateListener.onProfilesChanged();
				}
				removePackageFromApps(apps, packageName, userHandle,
						true);
				removePackageFromPieMenu(menuPrimary, packageName,
						userHandle);
				removePackageFromPieMenu(menuSecondary, packageName,
						userHandle);
				propagateUpdate();
			});
		});
	}

	// Loads the app shortcuts (e.g. "New incognito tab") of the given app.
	// Only the default launcher may query them, so the list is empty
	// otherwise.
	public void loadAppShortcutsAsync(
			Context context,
			AppIcon app,
			AppShortcutsListener listener) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1 ||
				app == null || isShortcut(app)) {
			listener.onAppShortcuts(app, new ArrayList<>());
			return;
		}
		Context appContext = context.getApplicationContext();
		PieLauncherApp.getDatabase(context).execute(() -> {
			List<AppIcon> shortcuts = queryAppShortcuts(appContext, app);
			handler.post(() -> listener.onAppShortcuts(app, shortcuts));
		});
	}

	public void updateIconsAsync(Context context) {
		if (!indexAppsAsync(context)) {
			handler.postDelayed(() -> updateIconsAsync(context), 1000L);
		}
	}

	public void updatePackageAsync(
			Context context,
			String packageName,
			UserHandle userHandle) {
		if (!indexAppsAsync(context, packageName, userHandle)) {
			handler.postDelayed(() -> updatePackageAsync(
					context, packageName, userHandle), 1000L);
		}
	}

	public void refreshProfiles(Context context) {
		if (updateListener != null) {
			updateListener.onProfilesChanged();
		}
		removeLockedProfiles(context, apps, menuPrimary, menuSecondary);
		propagateUpdate();
		updateIconsAsync(context);
	}

	private static boolean removeLockedProfiles(
			Context context,
			Map<LauncherItemKey, AppIcon> allApps,
			MenuItems primary,
			MenuItems secondary) {
		Map<UserHandle, Boolean> lockedProfiles = new HashMap<>();
		Set<UserHandle> removed = new HashSet<>();
		Iterator<AppIcon> it = allApps.values().iterator();
		while (it.hasNext()) {
			AppIcon icon = it.next();
			Boolean locked = lockedProfiles.get(icon.userHandle);
			if (locked == null) {
				locked = AppLauncher.isPrivateProfileLocked(context, icon.userHandle);
				lockedProfiles.put(icon.userHandle, locked);
			}
			if (locked) {
				removed.add(icon.userHandle);
				it.remove();
			}
		}
		for (List<AppIcon> menu : Arrays.asList(primary, secondary)) {
			for (AppIcon icon : menu) {
				Boolean locked = lockedProfiles.get(icon.userHandle);
				if (locked == null) {
					locked = AppLauncher.isPrivateProfileLocked(context, icon.userHandle);
					lockedProfiles.put(icon.userHandle, locked);
				}
				if (locked) {
					removed.add(icon.userHandle);
				}
			}
		}
		for (UserHandle user : removed) {
			AppSnapshot.markUnavailable(allApps, user);
		}
		primary.hideProfiles(removed);
		secondary.hideProfiles(removed);
		return !removed.isEmpty();
	}

	public boolean isEmpty() {
		return apps.isEmpty();
	}

	public boolean isIndexing() {
		return indexing;
	}

	public boolean beginMenuEdit() {
		if (indexing) {
			return false;
		}
		editingMenu = true;
		return true;
	}

	public void endMenuEdit(Context context) {
		editingMenu = false;
		if (indexAfterEditing) {
			indexAfterEditing = false;
			Context appContext = context.getApplicationContext();
			handler.post(() -> updateIconsAsync(appContext));
		}
	}

	public boolean indexAppsAsync(Context context) {
		return indexAppsAsync(context, null, null);
	}

	public boolean indexAppsAsync(Context context,
			String packageNameRestriction,
			UserHandle userHandleRestriction) {
		if (editingMenu) {
			indexAfterEditing = true;
			return true;
		}
		if (indexing) {
			return false;
		}
		indexing = true;
		AppSnapshot newApps = new AppSnapshot();
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
		AppSnapshot oldApps = new AppSnapshot(apps);
		if (packageRestriction != null) {
			// Unchanged icons stay owned by the UI thread.
			newApps.putAll(oldApps);
			newApps.unavailableProfiles.addAll(oldApps.unavailableProfiles);
			removePackageFromApps(newApps, packageRestriction, userRestriction,
					false);
			// No need to call removePackageFromPieMenu() because the
			// menu will be re-created by getPrimaryMenu() after indexing.
		}
		PieLauncherApp.getDatabase(context).execute(() -> {
			hiddenAppsStorage.restore(context);
			HashSet<ComponentName> hideApps = hiddenAppsStorage.copyComponentNames();
			hideApps.add(new ComponentName(context, HomeActivity.class));
			Map<LauncherItemKey, AppIcon> oldPackageApps =
					getPackageApps(oldApps, packageRestriction, userRestriction);
			if (needsCacheRestore) {
				restoreAppsFromCache(context, hideApps);
			}
			indexApps(context,
					packageRestriction,
					userRestriction,
					hideApps,
					newApps);
			injectPinnedShortcuts(context, newApps, hideApps);
			int reconciliation = reconcileChangedPackage(
					newApps,
					oldPackageApps,
					packageRestriction,
					userRestriction);
			Database database = PieLauncherApp.getDatabase(context);
			Map<LauncherItemKey, AppIcon> newIcons = new HashMap<>();
			for (Map.Entry<LauncherItemKey, AppIcon> entry : newApps.entrySet()) {
				if (entry.getValue() != oldApps.get(entry.getKey())) {
					newIcons.put(entry.getKey(), entry.getValue());
				}
			}
			database.restoreFrecency(context, newIcons);
			database.restoreSearchPicks(context, newIcons);
			Menus menus = compileMenus(context, newApps);
			removeObsoletePackageKeys(newApps, oldPackageApps,
					packageRestriction);
			if ((reconciliation & RECONCILED_MENU) != 0) {
				MenuStorage.store(context, MENU_PRIMARY, menus.primary);
				MenuStorage.store(context, MENU_SECONDARY, menus.secondary);
			}
			if (packageRestriction == null) {
				database.replaceAllApps(context, newApps);
			} else {
				database.replacePackage(context,
						packageRestriction,
						userRestriction,
						newApps);
			}
			handler.post(() -> updateApps(context, newApps, menus, true));
			if ((reconciliation & RETRY_PACKAGE) != 0) {
				handler.postDelayed(() -> updatePackageAsync(context,
						packageRestriction, userRestriction), 1000L);
			}
		});
		return true;
	}

	private static Map<LauncherItemKey, AppIcon> getPackageApps(
			Map<LauncherItemKey, AppIcon> allApps,
			String packageName,
			UserHandle userHandle) {
		Map<LauncherItemKey, AppIcon> packageApps = new HashMap<>();
		if (packageName == null) {
			return packageApps;
		}
		for (Map.Entry<LauncherItemKey, AppIcon> entry : allApps.entrySet()) {
			AppIcon icon = entry.getValue();
			if (packageName.equals(icon.componentName.getPackageName()) &&
					(userHandle == null || userHandle.equals(icon.userHandle))) {
				packageApps.put(entry.getKey(), icon);
			}
		}
		return packageApps;
	}

	private int reconcileChangedPackage(
			Map<LauncherItemKey, AppIcon> allApps,
			Map<LauncherItemKey, AppIcon> oldPackageApps,
			String packageName, UserHandle userHandle) {
		if (packageName == null || oldPackageApps.isEmpty()) {
			return 0;
		}
		LauncherItemKey retryKey = new LauncherItemKey(
				new ComponentName(packageName, ""), userHandle);
		Map<LauncherItemKey, AppIcon> newPackageApps = getPackageApps(
				allApps, packageName, userHandle);
		if (newPackageApps.isEmpty()) {
			// Alias switches may briefly leave no launcher alias enabled.
			if (pendingPackageRetries.add(retryKey)) {
				allApps.putAll(oldPackageApps);
				return RETRY_PACKAGE;
			}
			pendingPackageRetries.remove(retryKey);
			return 0;
		}
		pendingPackageRetries.remove(retryKey);
		if (oldPackageApps.size() != 1 || newPackageApps.size() != 1) {
			return 0;
		}
		Map.Entry<LauncherItemKey, AppIcon> oldEntry =
				oldPackageApps.entrySet().iterator().next();
		Map.Entry<LauncherItemKey, AppIcon> newEntry =
				newPackageApps.entrySet().iterator().next();
		if (oldEntry.getKey().equals(newEntry.getKey())) {
			return 0;
		}
		// Let menu restoration resolve the persisted old alias to the new one.
		allApps.put(oldEntry.getKey(), newEntry.getValue());
		return RECONCILED_MENU;
	}

	@SuppressLint("UseRequiresApi")
	@TargetApi(Build.VERSION_CODES.O)
	private void acceptPinRequestApi26(Context context, Intent intent) {
		LauncherApps la = AppLauncher.getLauncherApps(context);
		if (la == null) {
			return;
		}
		LauncherApps.PinItemRequest request;
		try {
			request = la.getPinItemRequest(intent);
		} catch (Exception e) {
			return;
		}
		if (request == null ||
				request.getRequestType() !=
						LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT ||
				!request.isValid()) {
			return;
		}
		ShortcutInfo info = request.getShortcutInfo();
		if (info == null) {
			return;
		}
		AppIcon icon = buildShortcutIcon(context, la, info);
		// Only accept the pin once we can actually represent it, so a request
		// we can't render isn't silently accepted and then lost.
		if (icon == null || !request.accept()) {
			return;
		}
		// Persist first so the shortcut survives even if an index that is
		// already running clears the map before the next one re-injects it.
		PieLauncherApp.getDatabase(context).storePinnedShortcut(context, icon);
		handler.post(() -> {
			LauncherItemKey key = new LauncherItemKey(icon.componentName, icon.userHandle);
			removedPinnedShortcuts.remove(key);
			if (AppLauncher.isPrivateProfileLocked(context, icon.userHandle)) {
				return;
			}
			apps.put(key, icon);
			if (isIndexing()) {
				// An index already queued before this UI update may omit the
				// shortcut, so inject it again after that index finishes.
				updateIconsAsync(context);
			}
			propagateUpdate();
			AppLauncher.toast(context, context.getString(
					R.string.added_to_home_screen, icon.label));
		});
	}

	@SuppressLint("UseRequiresApi")
	@TargetApi(Build.VERSION_CODES.N_MR1)
	private static List<AppIcon> queryAppShortcuts(
			Context context,
			AppIcon app) {
		ArrayList<AppIcon> shortcuts = new ArrayList<>();
		LauncherApps la = AppLauncher.getLauncherApps(context);
		if (la == null || !la.hasShortcutHostPermission()) {
			return shortcuts;
		}
		LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
		query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC |
				LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST);
		query.setPackage(app.componentName.getPackageName());
		query.setActivity(app.componentName);
		List<ShortcutInfo> infos;
		try {
			infos = la.getShortcuts(query, app.userHandle);
		} catch (SecurityException | IllegalStateException e) {
			// The profile may be locked.
			return shortcuts;
		}
		if (infos == null) {
			return shortcuts;
		}
		// List manifest shortcuts first, like the system launcher does.
		infos = new ArrayList<>(infos);
		Collections.sort(infos, (left, right) -> {
			if (left.isDeclaredInManifest() != right.isDeclaredInManifest()) {
				return left.isDeclaredInManifest() ? -1 : 1;
			}
			return Integer.compare(left.getRank(), right.getRank());
		});
		// Some apps publish the same shortcut as manifest and as dynamic
		// shortcut, so skip labels that are listed already.
		HashSet<String> labels = new HashSet<>();
		for (ShortcutInfo info : infos) {
			if (!info.isEnabled()) {
				continue;
			}
			AppIcon icon = buildShortcutIcon(context, la, info);
			if (icon == null || !labels.add(icon.label)) {
				continue;
			}
			shortcuts.add(icon);
		}
		return shortcuts;
	}

	@SuppressLint("UseRequiresApi")
	@TargetApi(Build.VERSION_CODES.N_MR1)
	private static AppIcon buildShortcutIcon(
			Context context,
			LauncherApps la,
			ShortcutInfo info) {
		String shortcutPackage = info.getPackage();
		String shortcutId = info.getId();
		CharSequence label = info.getShortLabel();
		if (label == null || label.length() < 1) {
			label = info.getLongLabel();
		}
		if (label == null || label.length() < 1) {
			label = shortcutPackage;
		}
		Drawable drawable = null;
		try {
			drawable = la.getShortcutIconDrawable(info,
					context.getResources().getDisplayMetrics().densityDpi);
		} catch (Exception e) {
			// Fall through to the owning app's icon.
		}
		if (drawable == null) {
			try {
				drawable = context.getPackageManager()
						.getApplicationIcon(shortcutPackage);
			} catch (PackageManager.NameNotFoundException e) {
				return null;
			}
		}
		Bitmap bitmap = Converter.getBitmapFromDrawable(drawable);
		if (bitmap == null) {
			return null;
		}
		AppIcon icon = new AppIcon(
				shortcutComponentName(shortcutPackage, shortcutId),
				label.toString(),
				bitmap,
				info.getUserHandle());
		icon.shortcutPackage = shortcutPackage;
		icon.shortcutId = shortcutId;
		return icon;
	}

	private void injectPinnedShortcuts(
			Context context,
			Map<LauncherItemKey, AppIcon> allApps,
			Set<ComponentName> hideApps) {
		PieLauncherApp.getDatabase(context).restorePinnedShortcuts(
				context, allApps);
		if (hideApps != null) {
			removeApps(allApps, hideApps);
		}
	}

	private static void removeObsoletePackageKeys(
			Map<LauncherItemKey, AppIcon> allApps,
			Map<LauncherItemKey, AppIcon> oldPackageApps,
			String packageName) {
		if (packageName == null) {
			return;
		}
		for (LauncherItemKey key : oldPackageApps.keySet()) {
			AppIcon icon = allApps.get(key);
			if (icon != null && !key.componentName.equals(icon.componentName)) {
				allApps.remove(key);
			}
		}
	}

	private void restoreAppsFromCache(
			Context context,
			HashSet<ComponentName> hideApps) {
		AppSnapshot cachedApps = new AppSnapshot();
		PieLauncherApp.getDatabase(context).restoreApps(context, cachedApps);
		injectPinnedShortcuts(context, cachedApps, null);
		removeApps(cachedApps, hideApps);
		Menus menus = compileMenus(context, cachedApps);
		handler.post(() -> updateApps(context, cachedApps, menus, false));
	}

	private Menus compileMenus(
			Context context,
			Map<LauncherItemKey, AppIcon> newApps) {
		return new Menus(
				getPrimaryMenu(context, newApps,
						PieLauncherApp.getPrefs(context).openListWith() ==
								Preferences.OPEN_LIST_WITH_ICON),
				getSecondaryMenu(context, newApps));
	}

	private void updateApps(
			Context context,
			AppSnapshot newApps,
			Menus menus,
			boolean done) {
		// A profile may have been locked after the worker read its apps.
		boolean profilesChanged = removeLockedProfiles(context, newApps,
				menus.primary, menus.secondary);
		for (UserHandle user : newApps.unavailableProfiles) {
			if (!apps.unavailableProfiles.contains(user) &&
					AppLauncher.isPrivateProfileLocked(context, user)) {
				profilesChanged = true;
			}
		}
		if (profilesChanged && updateListener != null) {
			updateListener.onProfilesChanged();
		}
		// A queued database deletion may be newer than this index result.
		newApps.keySet().removeAll(removedPinnedShortcuts);
		menus.primary.removeKeys(removedPinnedShortcuts);
		menus.secondary.removeKeys(removedPinnedShortcuts);
		// Preserve launches made while indexing or while writes were queued.
		for (Map.Entry<LauncherItemKey, AppIcon> entry : newApps.entrySet()) {
			mergeUsage(entry.getValue(), apps.get(entry.getKey()));
		}
		apps.replaceWith(newApps);
		menuPrimary.replaceWith(menus.primary);
		menuSecondary.replaceWith(menus.secondary);
		menusRestored = true;
		if (done) {
			indexing = false;
			removedPinnedShortcuts.clear();
		}
		propagateUpdate();
	}

	private static void mergeUsage(AppIcon target, AppIcon source) {
		if (source == null || source == target) {
			return;
		}
		if (source.frecencyUpdatedAt > target.frecencyUpdatedAt ||
				(source.frecencyUpdatedAt == target.frecencyUpdatedAt &&
						source.frecencyScore > target.frecencyScore)) {
			target.frecencyScore = source.frecencyScore;
			target.frecencyUpdatedAt = source.frecencyUpdatedAt;
		}
		if (source.searchPicks == null) {
			return;
		}
		if (target.searchPicks == null) {
			target.searchPicks = new HashMap<>();
		}
		for (Map.Entry<String, SearchPick> entry : source.searchPicks.entrySet()) {
			SearchPick pick = entry.getValue();
			SearchPick restored = target.searchPicks.get(entry.getKey());
			if (restored == null || pick.updatedAt > restored.updatedAt ||
					(pick.updatedAt == restored.updatedAt && pick.score > restored.score)) {
				target.searchPicks.put(entry.getKey(),
						new SearchPick(pick.score, pick.updatedAt));
			}
		}
	}

	private static class Menus {
		final MenuItems primary;
		final MenuItems secondary;

		Menus(MenuItems primary, MenuItems secondary) {
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
					context,
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
			Context context,
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
			if (AppLauncher.isPrivateProfileLocked(context, profile)) {
				AppSnapshot.markUnavailable(allApps, profile);
				continue;
			}
			// A partial index cannot make a previously incomplete profile complete.
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

	private static AppIcon addApp(
			Map<LauncherItemKey, AppIcon> allApps,
			ComponentName componentName,
			String label,
			Drawable icon,
			UserHandle userHandle) {
		AppIcon appIcon = new AppIcon(componentName, label, icon, userHandle);
		allApps.put(new LauncherItemKey(componentName, userHandle), appIcon);
		return appIcon;
	}

	private MenuItems getPrimaryMenu(Context context,
			Map<LauncherItemKey, AppIcon> allApps,
			boolean useDrawerIcon) {
		AppIcon drawerIcon = useDrawerIcon
				? addDrawerIcon(context, allApps)
				: null;
		MenuItems menu = MenuStorage.restore(context,
				MENU_PRIMARY, allApps);
		if (drawerIcon != null) {
			if (!menu.contains(drawerIcon)) {
				menu.add(0, drawerIcon);
			}
			removePackageFromApps(allApps, drawerPackageName, null, false);
		} else {
			drawerPackageName = null;
		}
		if (menu.isEmpty() && !menu.hasUnavailableItems()) {
			MenuDefaults.createInitialMenu(menu, allApps,
					context.getPackageManager());
			MenuStorage.store(context, MENU_PRIMARY, menu);
		}
		return menu;
	}

	private MenuItems getSecondaryMenu(
			Context context,
			Map<LauncherItemKey, AppIcon> allApps) {
		MenuItems menu = MenuStorage.restore(context,
				MENU_SECONDARY, allApps);
		if (menu.isEmpty() && !menu.hasUnavailableItems()) {
			// Just add a few apps so users aren't confused by an empty menu.
			MenuDefaults.createMenuForPopularApps(menu, allApps, 4);
			MenuStorage.store(context, MENU_SECONDARY, menu);
		}
		return menu;
	}

	private AppIcon addDrawerIcon(
			Context context,
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
			UserHandle userHandle,
			boolean includeShortcuts) {
		Iterator<Map.Entry<LauncherItemKey, AppIcon>> it =
				allApps.entrySet().iterator();
		while (it.hasNext()) {
			AppIcon appIcon = it.next().getValue();
			if (isPackageMatch(appIcon, packageName, includeShortcuts) &&
					(userHandle == null || userHandle.equals(appIcon.userHandle))) {
				it.remove();
			}
		}
	}

	private void removePackageFromPieMenu(
			List<AppIcon> icons,
			String packageName,
			UserHandle userHandle) {
		Iterator<AppIcon> it = icons.iterator();
		while (it.hasNext()) {
			AppIcon appIcon = it.next();
			if (isPackageMatch(appIcon, packageName, true) &&
					(userHandle == null || userHandle.equals(appIcon.userHandle))) {
				it.remove();
			}
		}
	}

	private static boolean isPackageMatch(
			AppIcon appIcon,
			String packageName,
			boolean includeShortcuts) {
		return packageName.equals(appIcon.componentName.getPackageName()) ||
				(includeShortcuts && packageName.equals(appIcon.shortcutPackage));
	}
}
