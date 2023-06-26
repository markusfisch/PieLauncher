package de.markusfisch.android.pielauncher.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;

public class PreferencesDialog {
	public static void create(Context context) {
		Activity activity = (Activity) context;
		LayoutInflater inflater = activity.getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_preferences, null);

		TextView orientationView = view.findViewById(R.id.orientation);
		orientationView.setOnClickListener(
				v -> setOrientation(activity, orientationView));
		setOrientationText(orientationView,
				PieLauncherApp.prefs.getOrientation());

		TextView defaultLauncherView = view.findViewById(
				R.id.make_default_launcher);

		AlertDialog dialog = new AlertDialog.Builder(context)
				.setTitle(R.string.preferences)
				.setView(view)
				.setPositiveButton(android.R.string.ok, null)
				.show();

		if (isDefault(
				activity.getPackageManager(),
				activity.getPackageName())) {
			defaultLauncherView.setVisibility(View.GONE);
		} else {
			defaultLauncherView.setOnClickListener(v -> {
				setAsDefault(activity);
				dialog.dismiss();
			});
		}
	}

	private static void setOrientation(Activity activity,
			TextView orientationView) {
		int newOrientation = PieLauncherApp.prefs.getOrientation() ==
				ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
				? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
				: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		PieLauncherApp.prefs.setOrientation(newOrientation);
		setOrientationText(orientationView, newOrientation);
		activity.setRequestedOrientation(newOrientation);
	}

	private static void setOrientationText(TextView tv, int orientation) {
		tv.setText(getOrientationResId(orientation));
	}

	private static int getOrientationResId(int orientation) {
		switch (orientation) {
			default:
				return R.string.orientation_default;
			case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
				return R.string.orientation_landscape;
			case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
				return R.string.orientation_portrait;
		}
	}

	private static void setAsDefault(Activity activity) {
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

	private static boolean isDefault(PackageManager packageManager,
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
