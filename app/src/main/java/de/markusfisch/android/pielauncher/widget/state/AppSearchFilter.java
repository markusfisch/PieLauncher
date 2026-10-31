package de.markusfisch.android.pielauncher.widget.state;

import android.content.Context;
import android.text.TextUtils;

import java.util.List;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.AppSearch;
import de.markusfisch.android.pielauncher.content.Apps;

public class AppSearchFilter {

    public interface Listener {
        void onFilterChanged(List<Apps.AppIcon> newAppList, boolean isFiltered, int selectedApp);
    }

    private final Listener listener;
    private String query = "";

    public AppSearchFilter(Listener listener) {
        this.listener = listener;
    }

    public String getQuery() {
        return query;
    }

    public boolean isFiltered() {
        return !TextUtils.isEmpty(query);
    }

    public void filterAppList(Context context, String query, boolean doubleSpaceLaunch) {
        this.query = query != null ? query : "";
        List<Apps.AppIcon> newAppList = AppSearch.filterAppsBy(
                PieLauncherApp.apps, context, this.query);

        boolean isFiltered = isFiltered();
        int selectedApp = doubleSpaceLaunch
                ? (isFiltered ? 0 : -1)
                : getSelectedAppFromTrailingSpace(query);

        listener.onFilterChanged(newAppList, isFiltered, selectedApp);
    }

    public static int getSelectedAppFromTrailingSpace(String s) {
        int l = s == null ? 0 : s.length();
        if (l < 1) {
            return -1; // No selection.
        }
        --l;
        int i = l;
        for (; i > -1; --i) {
            if (s.charAt(i) != ' ') {
                break;
            }
        }
        return i == -1
                ? l // Only spaces, so treat first space as show icon.
                : l - i; // Return number of trailing spaces.
    }
}
