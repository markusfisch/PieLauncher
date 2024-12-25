package de.markusfisch.android.pielauncher.content;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.Arrays;

public class LauncherItemKey {
	public final ComponentName componentName;
	public final UserHandle userHandle;

	private final int cachedHashCode;

	public static String flattenToString(
			Context context,
			ComponentName componentName,
			UserHandle userHandle) {
		return userHandle == null
				? componentName.flattenToString()
				: flattenToStringWithUserHandle(context, componentName,
				userHandle);
	}

	public static LauncherItemKey unflattenFromString(
			Context context,
			String s) {
		return AppMenu.HAS_LAUNCHER_APP
				? unflattenFromStringWithUserHandle(context, s)
				: new LauncherItemKey(
				ComponentName.unflattenFromString(s), null);
	}

	public LauncherItemKey(
			ComponentName componentName,
			UserHandle userHandle) {
		this.componentName = componentName;
		this.userHandle = AppMenu.HAS_LAUNCHER_APP
				? getUserHandle(userHandle)
				: null;
		this.cachedHashCode = Arrays.hashCode(
				new Object[]{componentName, this.userHandle});
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof LauncherItemKey)) {
			return false;
		}
		LauncherItemKey key = (LauncherItemKey) o;
		return equals(componentName, key.componentName) &&
				equals(userHandle, key.userHandle);
	}

	@Override
	public int hashCode() {
		return cachedHashCode;
	}

	private static boolean equals(Object a, Object b) {
		// Objects.equals() would require minSDK 19.
		//noinspection EqualsReplaceableByObjectsCall
		return a == b || (a != null && a.equals(b));
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static UserHandle getUserHandle(UserHandle userHandle) {
		return userHandle == null ? Process.myUserHandle() : userHandle;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static LauncherItemKey unflattenFromStringWithUserHandle(
			Context context,
			String s) {
		int sep = s.indexOf('#');
		if (sep < 0) {
			return new LauncherItemKey(
					ComponentName.unflattenFromString(s),
					null);
		}
		if (sep + 1 >= s.length()) {
			return new LauncherItemKey(
					ComponentName.unflattenFromString(s.substring(0, sep)),
					null);
		}
		ComponentName componentName = ComponentName.unflattenFromString(
				s.substring(0, sep));
		UserHandle userHandle = null;
		try {
			long id = Long.parseLong(s.substring(sep + 1));
			userHandle = getUserManager(context).getUserForSerialNumber(id);
		} catch (NumberFormatException nfe) {
			// Fall through, userHandle is null.
		}
		return new LauncherItemKey(componentName, userHandle);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static String flattenToStringWithUserHandle(
			Context context,
			ComponentName componentName,
			UserHandle userHandle) {
		StringBuilder sb = new StringBuilder();
		sb.append(componentName.flattenToString());
		if (!Process.myUserHandle().equals(userHandle)) {
			sb.append("#");
			sb.append(getUserManager(context).getSerialNumberForUser(
					userHandle));
		}
		return sb.toString();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static UserManager getUserManager(Context context) {
		return (UserManager) context.getSystemService(Context.USER_SERVICE);
	}
}
