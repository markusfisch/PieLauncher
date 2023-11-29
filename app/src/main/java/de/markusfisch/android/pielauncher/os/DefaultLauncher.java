package de.markusfisch.android.pielauncher.os;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

public class DefaultLauncher {
	public static void setAsDefault(Activity activity) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
			return;
		}
		RoleManager roleManager = (RoleManager) activity.getSystemService(
				Context.ROLE_SERVICE);
		if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME) ||
				roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
			return;
		}
		// Must be startActivityForResult() or the chooser won't come up.
		activity.startActivityForResult(
				roleManager.createRequestRoleIntent(
						RoleManager.ROLE_HOME),
				1);
	}

	public static boolean isDefault(Context context) {
		return isDefault(context.getPackageManager(), context.getPackageName());
	}

	public static boolean isDefault(PackageManager packageManager,
			String packageName) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		ResolveInfo res = packageManager.resolveActivity(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return res != null && res.activityInfo != null &&
				packageName.equals(res.activityInfo.packageName);
	}
}
