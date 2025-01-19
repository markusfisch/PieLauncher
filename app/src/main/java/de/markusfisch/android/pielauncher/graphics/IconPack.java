package de.markusfisch.android.pielauncher.graphics;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import de.markusfisch.android.pielauncher.io.IconMappings;

public class IconPack {
	public static class Pack {
		public final String packageName;
		public final String name;
		public final Resources resources;

		public Pack(String packageName, String name, Resources resources) {
			this.packageName = packageName;
			this.name = name;
			this.resources = resources;
		}

		public Drawable getDrawable(String drawableName) {
			if (drawableName == null) {
				return null;
			}
			@SuppressLint("DiscouragedApi")
			int id = resources.getIdentifier(drawableName, "drawable",
					packageName);
			try {
				return id > 0 ? resources.getDrawable(id) : null;
			} catch (NotFoundException e) {
				return null;
			}
		}

		public ArrayList<String> getDrawableNames() {
			LinkedHashMap<String, String> map = new LinkedHashMap<>();
			loadComponentAndDrawableNames(map);
			return new ArrayList<>(new LinkedHashSet<>(map.values()));
		}

		public void loadComponentAndDrawableNames(
				LinkedHashMap<String, String> map) {
			InputStream is = null;
			try {
				is = resources.getAssets().open("appfilter.xml");
				XmlPullParser parser = Xml.newPullParser();
				parser.setInput(new InputStreamReader(is));
				for (int eventType = parser.getEventType();
						eventType != XmlPullParser.END_DOCUMENT;
						eventType = parser.next()) {
					if (eventType == XmlPullParser.START_TAG &&
							"item".equals(parser.getName())) {
						String component = parser.getAttributeValue(
								null, "component");
						String drawable = parser.getAttributeValue(
								null, "drawable");
						if (component != null && !component.isEmpty() &&
								drawable != null && !drawable.isEmpty()) {
							map.put(component, drawable);
						}
					}
				}
			} catch (XmlPullParserException | IOException e) {
				// Ignore.
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// Ignore.
					}
				}
			}
		}
	}

	public static class PackAndDrawable implements Serializable {
		public final String packageName;
		public final String drawableName;

		public PackAndDrawable(String packageName, String drawableName) {
			this.packageName = packageName;
			this.drawableName = drawableName;
		}
	}

	public final LinkedHashMap<String, Pack> packs = new LinkedHashMap<>();
	public final LinkedHashMap<String, String> componentToDrawableNames =
			new LinkedHashMap<>();

	private final HashMap<ComponentName, PackAndDrawable> mappings =
			new HashMap<>();

	private PackageManager packageManager;
	private IconPack.Pack selectedPack;

	public boolean hasPacks() {
		return !packs.isEmpty();
	}

	public void restoreMappings(Context context) {
		IconMappings.restore(context, getSelectedIconPackageName(), mappings);
	}

	public void storeMappings(Context context) {
		IconMappings.store(context, getSelectedIconPackageName(), mappings);
	}

	public boolean hasMapping(ComponentName componentName) {
		return mappings.containsKey(componentName);
	}

	public void addMapping(String iconPackageName,
			ComponentName componentName, String drawableName) {
		mappings.put(componentName,
				new PackAndDrawable(iconPackageName, drawableName));
	}

	public void removeMapping(ComponentName componentName) {
		mappings.remove(componentName);
	}

	public void clearMappings() {
		mappings.clear();
	}

	public String getSelectedIconPackageName() {
		return selectedPack != null ? selectedPack.packageName : null;
	}

	public HashMap<String, String> getIconPacks() {
		HashMap<String, String> map = new HashMap<>();
		for (Pack pack : packs.values()) {
			map.put(pack.packageName, pack.name);
		}
		return map;
	}

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
					packs.put(packageName, new Pack(
							packageName,
							pm.getApplicationLabel(getApplicationInfo(
									pm, packageName)).toString(),
							pm.getResourcesForApplication(packageName)));
				} catch (PackageManager.NameNotFoundException e) {
					// Ignore.
				}
			}
		}
	}

	public void selectPack(PackageManager pm, String packageName) {
		selectedPack = null;
		packageManager = null;
		componentToDrawableNames.clear();
		if (pm == null) {
			return;
		}
		// Always update because packs may have been added/removed.
		updatePacks(pm);
		if (packageName == null || packageName.isEmpty()) {
			return;
		}
		selectedPack = packs.get(packageName);
		if (selectedPack == null) {
			return;
		}
		// Always reload packages and drawables as the pack may have
		// been updated.
		selectedPack.loadComponentAndDrawableNames(componentToDrawableNames);
		packageManager = pm;
	}

	public Drawable getIcon(ComponentName componentName) {
		String drawableName = null;
		PackAndDrawable pad = mappings.get(componentName);
		if (pad != null) {
			if (selectedPack != null &&
					pad.packageName.equals(selectedPack.packageName)) {
				drawableName = pad.drawableName;
			} else {
				Pack pack = packs.get(pad.packageName);
				if (pack != null) {
					return pack.getDrawable(pad.drawableName);
				}
			}
		}
		if (selectedPack == null) {
			return null;
		}
		if (drawableName == null) {
			Intent intent = packageManager.getLaunchIntentForPackage(
					componentName.getPackageName());
			if (intent == null) {
				return null;
			}
			drawableName = componentToDrawableNames.get(
					componentName.toString());
		}
		return selectedPack.getDrawable(drawableName);
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
}
