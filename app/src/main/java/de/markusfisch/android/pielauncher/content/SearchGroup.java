package de.markusfisch.android.pielauncher.content;

import android.graphics.Bitmap;

import java.util.ArrayList;

// A group of search results that is listed above the app icons,
// e.g. the shortcuts of one app.
public class SearchGroup {
	public static final int MAX_ITEMS = 3;

	public final String title;
	public final Bitmap icon;
	public final ArrayList<Apps.AppIcon> items = new ArrayList<>();

	SearchGroup(String title, Bitmap icon) {
		this.title = title;
		this.icon = icon;
	}
}
