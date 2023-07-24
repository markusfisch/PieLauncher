package de.markusfisch.android.pielauncher.os;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public class DefaultLauncher {
	public static void setAsDefault(Activity activity) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			Intent intent = new Intent("android.intent.action.MAIN");
			intent.addCategory("android.intent.category.HOME");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
		} else {
			RoleManager roleManager = (RoleManager) activity.getSystemService(
					Context.ROLE_SERVICE);
			if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
					!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
				activity.startActivityForResult(
						roleManager.createRequestRoleIntent(
								RoleManager.ROLE_HOME),
						1);
			}
		}
	}

	public static boolean isDefault(Context context) {
		return isDefault(context.getPackageManager(), context.getPackageName());
	}

	public static boolean isDefault(PackageManager packageManager,
			String packageName) {
		IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
		filter.addCategory(Intent.CATEGORY_HOME);

		List<IntentFilter> filters = new ArrayList<>();
		filters.add(filter);

		List<ComponentName> activities = new ArrayList<>();
		packageManager.getPreferredActivities(filters, activities, null);

		for (ComponentName activity : activities) {
			if (packageName.equals(activity.getPackageName())) {
				return true;
			}
		}
		return false;
	}
}
