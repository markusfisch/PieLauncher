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

	private static final boolean HAS_LAUNCHER_APP =
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

	public LauncherItemKey(ComponentName componentName, UserHandle userHandle) {
		this.componentName = componentName;
		this.userHandle = (HAS_LAUNCHER_APP)
			? getUserHandle(userHandle)
			: null;
		this.cachedHashCode =
			Arrays.hashCode(new Object[] {componentName, userHandle});
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static UserHandle getUserHandle(UserHandle userHandle) {
		return (userHandle == null)
			? Process.myUserHandle()
			: userHandle;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if ((null == o) || !(o instanceof LauncherItemKey)) {
			return false;
		}
		LauncherItemKey c = (LauncherItemKey) o;
		return (this.componentName == c.componentName
				|| (this.componentName != null
					&& this.componentName.equals(c.componentName)))
			&& (this.userHandle == c.userHandle
					|| (this.userHandle != null
						&& this.userHandle.equals(c.userHandle)));
	}

	@Override
	public int hashCode() {
		return this.cachedHashCode;
	}

	public String flattenToString(Context context) {
		return flattenToString(context, this.componentName, this.userHandle);
	}

	public static String flattenToString(Context context,
			ComponentName componentName, UserHandle userHandle) {
		if (userHandle == null) {
			return componentName.flattenToString();
		} else {
			return flattenToStringWithUserHandle(
					context,
					componentName,
					userHandle);
		}
	}

	public static LauncherItemKey unflattenFromString(Context context, String s) {
		if (HAS_LAUNCHER_APP) {
			return unflattenFromStringWithUserHandle(context, s);
		} else {
			return new LauncherItemKey(
					ComponentName.unflattenFromString(s),
					null);
		}
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static LauncherItemKey
	unflattenFromStringWithUserHandle(Context context, String s) {
		int sep = s.indexOf('#');
		if (sep < 0) {
			return new LauncherItemKey(
					ComponentName.unflattenFromString(s),
					null);
		}
		if (sep+1 >= s.length()) {
			return new LauncherItemKey(
					ComponentName.unflattenFromString(s.substring(0, sep)),
					null);
		}
		ComponentName componentName =
			ComponentName.unflattenFromString(s.substring(0, sep));
		UserHandle userHandle = null;
		try {
			long id = Long.parseLong(s.substring(sep+1));
			userHandle = ((UserManager)
					context.getSystemService(Context.USER_SERVICE))
				.getUserForSerialNumber(id);
		} catch (NumberFormatException nfe) {
		}
		return new LauncherItemKey(componentName, userHandle);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static String flattenToStringWithUserHandle(
			Context context,
			ComponentName componentName,
			UserHandle userHandle) {
		return Process.myUserHandle().equals(userHandle)
			? componentName.flattenToString()
			: componentName.flattenToString()
			+ "#"
			+ Long.toString(
					((UserManager) context.getSystemService(Context.USER_SERVICE))
					.getSerialNumberForUser(userHandle));
	}
}
