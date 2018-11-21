package de.markusfisch.android.pielauncher.content;

import de.markusfisch.android.pielauncher.graphics.PieMenu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import java.util.HashMap;

public class AppMenu extends PieMenu {
	private final HashMap<String, App> apps = new HashMap<>();

	public AppMenu(Context context) {
		indexAppsAsync(context);
	}

	public void draw(Canvas canvas) {
		for (int n = numberOfIcons; n-- > 0; ) {
			((AppIcon) icons.get(n)).draw(canvas);
		}
	}

	public void launch(Context context) {
		if (selectedIcon > -1) {
			((AppIcon) icons.get(selectedIcon)).launch(context);
		}
	}

	// this AsyncTask is running for a short and finite time only
	// and it's perfectly okay to delay garbage collection of the
	// parent instance until this task has been terminated
	@SuppressLint("StaticFieldLeak")
	private void indexAppsAsync(final Context context) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... nothing) {
				indexApps(context);
				restore();
				return null;
			}
		}.execute();
	}

	private void restore() {
		numberOfIcons = 0;
		icons.clear();

		// if this is the first run, just take a few apps
		for (App app : apps.values()) {
			icons.add(new AppIcon(app));

			// DEBUG
			if (icons.size() > 8) {
				break;
			}
		}

		numberOfIcons = icons.size();
	}

	private void indexApps(Context context) {
		apps.clear();

		PackageManager pm = context.getPackageManager();

		for (PackageInfo pkg : pm.getInstalledPackages(0)) {
			apps.put(pkg.packageName, new App(
					pkg.packageName,
					pkg.applicationInfo.loadLabel(pm).toString(),
					pkg.applicationInfo.loadIcon(pm)));
		}
	}

	private static class App {
		final String packageName;
		final String appName;
		final Drawable icon;

		App(String packageName, String appName, Drawable icon) {
			this.packageName = packageName;
			this.appName = appName;
			this.icon = icon;
		}
	}

	private static class AppIcon extends PieMenu.Icon {
		final App app;

		AppIcon(App app) {
			this.app = app;
		}

		void launch(Context context) {
			PackageManager pm = context.getPackageManager();
			Intent intent;

			if (pm == null || (intent = pm.getLaunchIntentForPackage(
					app.packageName)) == null) {
				return;
			}

			context.startActivity(intent);
		}

		void draw(Canvas canvas) {
			int s = (int) size >> 1;

			if (s < 1) {
				return;
			}

			int left = x - s;
			int top = y - s;
			s <<= 1;

			app.icon.setBounds(left, top, left + s, top + s);
			app.icon.draw(canvas);
		}
	}
}
