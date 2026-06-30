package de.markusfisch.android.pielauncher.content;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherUserInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.content.Apps.AppIcon;

public class AppLauncher {
	public static final boolean HAS_LAUNCHER_APP =
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

	private static LauncherApps launcherApps;
	private static UserManager userManager;

	public static ComponentName getLaunchComponentForPackageName(
			Context context, String packageName) {
		Intent intent = getLaunchIntent(context, packageName);
		return intent != null ? intent.getComponent() : null;
	}

	public static boolean launchPackage(Context context, String packageName) {
		Intent intent = getLaunchIntent(context, packageName);
		if (intent != null) {
			context.startActivity(intent);
			return true;
		}
		return false;
	}

	public static Intent getLaunchIntent(Context context, String packageName) {
		PackageManager pm = context.getPackageManager();
		return pm != null ? pm.getLaunchIntentForPackage(packageName) : null;
	}

	public static boolean launchApp(Context context, AppIcon icon) {
		if (HAS_LAUNCHER_APP) {
			return launchAppWithLauncherApp(context, icon);
		}
		return launchPackage(context, icon.componentName.getPackageName());
	}

	public static void launchAppInfo(Context context, AppIcon icon) {
		if (HAS_LAUNCHER_APP) {
			getLauncherApps(context).startAppDetailsActivity(
					icon.componentName,
					icon.userHandle,
					icon.rect,
					null);
		} else {
			launchAppInfo(context, icon.componentName.getPackageName());
		}
	}

	public static void launchAppInfo(Context context, String packageName) {
		Intent intent = new Intent(
				Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.setData(Uri.parse("package:" + packageName));
		context.startActivity(intent);
	}

	@SuppressLint("UseRequiresApi")
	@TargetApi(Build.VERSION_CODES.N)
	public static boolean launchAppWithLauncherApp(
			Context context, AppIcon icon) {
		LauncherApps la = getLauncherApps(context);
		UserManager um = getUserManager(context);
		if (icon.componentName == null || icon.userHandle == null) {
			return false;
		}
		try {
			if (!um.isUserUnlocked(icon.userHandle) &&
					isPrivateProfile(la, icon.userHandle)) {
				AppLauncher.toast(context, R.string.user_profile_locked);
				return false;
			}
			la.startMainActivity(
					icon.componentName,
					icon.userHandle,
					icon.rect,
					null);
			return true;
		} catch (Exception e) {
			// The stored component may have changed (e.g. app rotated its
			// activity alias). Try the first launcher activity we can find.
			ComponentName componentName = findEnabledActivity(
					la,
					icon.componentName.getPackageName(),
					icon.userHandle);
			if (componentName == null ||
					componentName.equals(icon.componentName)) {
				AppLauncher.toast(context, R.string.activity_not_enabled);
				return false;
			}
			icon.componentName = componentName;
			try {
				la.startMainActivity(
						icon.componentName,
						icon.userHandle,
						icon.rect,
						null);
				return true;
			} catch (Exception e2) {
				AppLauncher.toast(context, R.string.activity_not_enabled);
				return false;
			}
		}
	}

	@SuppressLint("UseRequiresApi")
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static LauncherApps getLauncherApps(Context context) {
		if (launcherApps == null) {
			launcherApps = (LauncherApps) context.getApplicationContext().getSystemService(
					Context.LAUNCHER_APPS_SERVICE);
		}
		return launcherApps;
	}

	@SuppressLint("UseRequiresApi")
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static UserManager getUserManager(Context context) {
		if (userManager == null) {
			userManager = (UserManager) context.getApplicationContext().getSystemService(
					Context.USER_SERVICE);
		}
		return userManager;
	}

	@SuppressLint("UseRequiresApi")
	@TargetApi(Build.VERSION_CODES.N)
	public static ComponentName findEnabledActivity(
			LauncherApps la,
			String packageName,
			UserHandle userHandle) {
		// getActivityList() already returns only enabled launcher activities,
		// so an additional isActivityEnabled() check is redundant — and on
		// some ROMs it returns false negatives that skip every result.
		List<LauncherActivityInfo> list =
				getActivityList(la, packageName, userHandle);
		return list.isEmpty() ? null : list.get(0).getComponentName();
	}

	@SuppressLint("UseRequiresApi")
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static List<LauncherActivityInfo> getActivityList(
			LauncherApps la, String packageName, UserHandle userHandle) {
		try {
			return la.getActivityList(packageName, userHandle);
		} catch (Exception e) {
			// Throws on some Android versions for private profiles.
			return new ArrayList<>();
		}
	}

	public static boolean unlockPrivateProfile(Context context, Runnable onResume) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
			return false;
		}
		UserHandle privateUser = findPrivateProfileUser(context);
		if (privateUser == null) {
			AppLauncher.toast(context, R.string.private_space_not_available);
			return false;
		}
		if (!isPrivateProfileLocked(context, privateUser)) {
			return false;
		}
		UserManager um = getUserManager(context);
		if (um == null) {
			AppLauncher.toast(context, R.string.private_space_unlock_failed);
			return false;
		}
		AppLauncher.toast(context, R.string.private_space_unlocking);
		try {
			um.requestQuietModeEnabled(false, privateUser);
			if (onResume != null) {
				onResume.run();
			}
			return true;
		} catch (SecurityException e) {
			AppLauncher.toast(context, R.string.private_space_unlock_failed);
		}
		return false;
	}

	public static boolean isPrivateProfileLocked(
			Context context,
			UserHandle userHandle) {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
				userHandle != null &&
				getUserManager(context).isQuietModeEnabled(userHandle);
	}

	public static boolean isPrivateProfile(
			LauncherApps la,
			UserHandle userHandle) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM ||
				la == null || userHandle == null) {
			return false;
		}
		try {
			LauncherUserInfo info = la.getLauncherUserInfo(userHandle);
			return info != null &&
					"android.os.usertype.profile.PRIVATE".equals(info.getUserType());
		} catch (Exception e) {
			return false;
		}
	}

	public static UserHandle findPrivateProfileUser(Context context) {
		if (!HAS_LAUNCHER_APP) {
			return null;
		}
		LauncherApps la = getLauncherApps(context);
		UserManager um = getUserManager(context);
		for (UserHandle profile : getProfiles(um)) {
			if (isPrivateProfile(la, profile)) {
				return profile;
			}
		}
		return null;
	}

	public static List<UserHandle> getProfiles(UserManager um) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
				um != null) {
			List<UserHandle> profiles = um.getUserProfiles();
			if (profiles != null) {
				return profiles;
			}
		}
		return new ArrayList<>();
	}

	public static <T> void toast(Context context, T message) {
		String m;
		if (message instanceof Integer) {
			m = context.getString((Integer) message);
		} else if (message instanceof String) {
			m = (String) message;
		} else {
			return;
		}
		Toast.makeText(context, m, Toast.LENGTH_SHORT).show();
	}
}
