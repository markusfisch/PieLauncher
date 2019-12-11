package de.markusfisch.android.pielauncher.content;

import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.graphics.Converter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppMenu extends CanvasPieMenu {
	public static class AppIcon extends CanvasPieMenu.CanvasIcon {
		public final Drawable icon;
		public final String packageName;
		public final String appName;

		AppIcon(String packageName, String appName, Drawable icon) {
			super(Converter.getBitmapFromDrawable(icon));
			this.icon = icon;
			this.packageName = packageName;
			this.appName = appName;
		}

		public void launch(Context context) {
			PackageManager pm = context.getPackageManager();
			Intent intent;
			if (pm == null || (intent = pm.getLaunchIntentForPackage(
					packageName)) == null) {
				return;
			}
			context.startActivity(intent);
		}
	}

	public interface UpdateListener {
		void onUpdate();
	}

	private final HashMap<String, AppIcon> apps = new HashMap<>();

	private static final String MENU = "menu";
	private static final Comparator<AppIcon> appNameComparator = new Comparator<AppIcon>() {
		public int compare(AppIcon left, AppIcon right) {
			return left.appName.compareTo(right.appName);
		}
	};

	private UpdateListener updateListener;

	public void launch(Context context) {
		int selectedIcon = getSelectedIcon();
		if (selectedIcon > -1) {
			((AppIcon) icons.get(selectedIcon)).launch(context);
		}
	}

	public void setUpdateListener(UpdateListener listener) {
		updateListener = listener;
	}

	public void store(Context context) {
		if (context != null) {
			writeMenu(context, icons);
		}
	}

	public List<AppIcon> filterAppsBy(String query) {
		if (query == null) {
			query = "";
		}
		query = query.trim().toLowerCase(Locale.US);
		ArrayList<AppIcon> list = new ArrayList<>();
		if (query.length() < 1) {
			list.addAll(apps.values());
		} else {
			for (Map.Entry entry : apps.entrySet()) {
				AppIcon appIcon = (AppIcon) entry.getValue();
				if (appIcon.appName.toLowerCase(Locale.US).contains(query)) {
					list.add(appIcon);
				}
			}
		}
		Collections.sort(list, appNameComparator);
		return list;
	}

	// this AsyncTask is running for a short and finite time only
	// and it's perfectly okay to delay garbage collection of the
	// parent instance until this task has been terminated
	@SuppressLint("StaticFieldLeak")
	public void indexAppsAsync(Context context) {
		// get application context to not block garbage collection
		// on other Context objects
		final Context appContext = context.getApplicationContext();
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... nothing) {
				indexApps(appContext);
				return null;
			}

			@Override
			protected void onPostExecute(Void nothing) {
				if (updateListener != null) {
					updateListener.onUpdate();
				}
			}
		}.execute();
	}

	private void indexApps(Context context) {
		apps.clear();
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
		if (activities == null) {
			return;
		}
		String skip = context.getPackageName();
		for (ResolveInfo info : activities) {
			String pn = info.activityInfo.packageName;
			if (skip.equals(pn)) {
				continue;
			}
			apps.put(pn, new AppIcon(
					pn,
					info.loadLabel(pm).toString(),
					info.loadIcon(pm)));
		}
		createIcons(context);
	}

	private void createIcons(Context context) {
		icons.clear();
		List<String> menu = readMenu(context);
		if (menu.isEmpty()) {
			int max = Math.min(apps.size(), 8);
			int i = 0;
			for (Map.Entry entry : apps.entrySet()) {
				addAppIcon((AppIcon) entry.getValue());
				if (++i >= max) {
					break;
				}
			}
		} else {
			for (String name : readMenu(context)) {
				addAppIcon(apps.get(name));
			}
		}
	}

	private void addAppIcon(AppIcon appIcon) {
		if (appIcon != null) {
			icons.add(appIcon);
		}
	}

	private static List<String> readMenu(Context context) {
		try {
			return readLines(context.openFileInput(MENU));
		} catch (FileNotFoundException e) {
			return new ArrayList<>();
		}
	}

	private static List<String> readLines(InputStream is) {
		ArrayList<String> list = new ArrayList<>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			while (reader.ready()) {
				list.add(reader.readLine());
			}
		} catch (IOException e) {
			// return what we got so far
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				// ignore, can't do anything about it
			}
		}
		return list;
	}

	private static boolean writeMenu(Context context, List<Icon> icons) {
		ArrayList<String> items = new ArrayList<>();
		for (CanvasPieMenu.Icon icon : icons) {
			items.add(((AppIcon) icon).packageName);
		}
		try {
			return writeLines(context.openFileOutput(MENU,
					Context.MODE_PRIVATE), items);
		} catch (IOException e) {
			return false;
		}
	}

	private static boolean writeLines(OutputStream os, List<String> lines) {
		if (os == null) {
			return false;
		}
		try {
			byte[] lf = "\n".getBytes("UTF-8");
			for (String line : lines) {
				os.write(line.getBytes("UTF-8"));
				os.write(lf);
			}
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			try {
				os.close();
			} catch (IOException e) {
				// ignore, can't do anything about it
			}
		}
	}
}
