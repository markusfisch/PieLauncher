package de.markusfisch.android.pielauncher.graphics;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

public class Icons {
	public final HashMap<String, String> packs = new HashMap<>();

	private final HashMap<String, String> componentToDrawableNames = new HashMap<>();

	private PackageManager packageManager;
	private Resources iconPackRes;
	private String selectedPackage;

	public void updatePacks(PackageManager pm) {
		packs.clear();
		for (String theme : new String[]{
				"org.adw.launcher.THEMES",
				"com.gau.go.launcherex.theme"
		}) {
			for (ResolveInfo info : queryIntentActivities(
					pm, new Intent(theme))) {
				String packageName = info.activityInfo.packageName;
				try {
					packs.put(packageName, pm.getApplicationLabel(
							getApplicationInfo(pm, packageName)).toString());
				} catch (PackageManager.NameNotFoundException e) {
					// Ignore.
				}
			}
		}
	}

	public void selectPack(PackageManager pm, String packageName) {
		selectedPackage = null;
		if (pm == null || packageName == null || packageName.isEmpty()) {
			return;
		}
		try {
			iconPackRes = pm.getResourcesForApplication(packageName);
		} catch (PackageManager.NameNotFoundException e) {
			return;
		}
		// Always update because packs may have been added/removed.
		updatePacks(pm);
		// Always reload packages and drawables as the pack may have
		// been updated.
		try {
			addComponentAndDrawableNames(
					componentToDrawableNames,
					iconPackRes.getAssets().open("appfilter.xml"));
			packageManager = pm;
			selectedPackage = packageName;
		} catch (XmlPullParserException | IOException e) {
			// Ignore.
		}
	}

	public Drawable getIcon(String packageName) {
		if (selectedPackage == null) {
			return null;
		}
		Intent intent = packageManager.getLaunchIntentForPackage(packageName);
		if (intent == null) {
			return null;
		}
		ComponentName componentName = intent.getComponent();
		if (componentName == null) {
			return null;
		}
		String drawableName = componentToDrawableNames.get(
				componentName.toString());
		if (drawableName == null || drawableName.isEmpty()) {
			return null;
		}
		@SuppressLint("DiscouragedApi")
		int id = iconPackRes.getIdentifier(drawableName, "drawable",
				selectedPackage);
		return id > 0 ? iconPackRes.getDrawable(id) : null;
	}

	private static List<ResolveInfo> queryIntentActivities(
			PackageManager pm,
			Intent intent) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return pm.queryIntentActivities(intent,
					PackageManager.ResolveInfoFlags.of(
							PackageManager.GET_META_DATA));
		} else {
			return pm.queryIntentActivities(intent,
					PackageManager.GET_META_DATA);
		}
	}

	private static ApplicationInfo getApplicationInfo(
			PackageManager pm,
			String packageName)
			throws PackageManager.NameNotFoundException {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return pm.getApplicationInfo(packageName,
					PackageManager.ApplicationInfoFlags.of(
							PackageManager.GET_META_DATA));
		} else {
			return pm.getApplicationInfo(packageName,
					PackageManager.GET_META_DATA);
		}
	}

	private static void addComponentAndDrawableNames(
			HashMap<String, String> map,
			InputStream is)
			throws XmlPullParserException, IOException {
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(new InputStreamReader(is));
		for (int eventType = parser.getEventType();
				eventType != XmlPullParser.END_DOCUMENT;
				eventType = parser.next()) {
			if (eventType == XmlPullParser.START_TAG &&
					"item".equals(parser.getName())) {
				map.put(
						parser.getAttributeValue(null, "component"),
						parser.getAttributeValue(null, "drawable"));
			}
		}
	}
}
