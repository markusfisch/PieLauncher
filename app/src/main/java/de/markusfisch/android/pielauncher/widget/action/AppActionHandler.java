package de.markusfisch.android.pielauncher.widget.action;

import android.content.Context;

import java.util.List;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.AppLauncher;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.content.Database;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.widget.AppPieView;
import de.markusfisch.android.pielauncher.widget.touch.TapFlags;

public class AppActionHandler {

    public interface Listener {
        void onLaunchingIcon(Apps.AppIcon icon);

        boolean isListFiltered();

        String getFilterQuery();
    }

    private final Preferences prefs;
    private final Listener listener;
    private AppPieView.ActionCallback actionCallback;
    private AppPieView.ListListener listListener;

    public AppActionHandler(Preferences prefs, Listener listener) {
        this.prefs = prefs;
        this.listener = listener;
    }

    public void setActionCallback(AppPieView.ActionCallback actionCallback) {
        this.actionCallback = actionCallback;
    }

    public AppPieView.ActionCallback getActionCallback() {
        return actionCallback;
    }

    public void requestEditOptions(Apps.AppIcon icon) {
        if (actionCallback != null) {
            actionCallback.onEditOptionsRequested(icon);
        }
    }

    public void requestAppInfo(Apps.AppIcon icon) {
        if (actionCallback != null) {
            actionCallback.onAppInfoRequested(icon);
        }
    }

    public void setListListener(AppPieView.ListListener listListener) {
        this.listListener = listListener;
    }

    public void launchApp(Context context, Apps.AppIcon appIcon) {
        if (listener != null) {
            listener.onLaunchingIcon(appIcon);
        }
        if (actionCallback != null) {
            actionCallback.onAppTapped(appIcon);
        } else if (AppLauncher.launchApp(context, appIcon)) {
            Database database = PieLauncherApp.getDatabase(context);
            database.recordLaunchAsync(appIcon);
            if (listener != null && listener.isListFiltered() &&
                    prefs.rememberSearchPicks()) {
                database.recordSearchPickAsync(listener.getFilterQuery(), appIcon);
            }
        }
    }

    public boolean performPieAction(
            Context context,
            TapFlags tapFlags,
            Apps.AppIcon appIcon,
            boolean pieVisible) {
        boolean result = false;
        boolean openList = false;
        switch (prefs.openListWith()) {
            case Preferences.OPEN_LIST_WITH_ANY_TOUCH:
                openList = tapFlags.wasTap || appIcon == null;
                break;
            case Preferences.OPEN_LIST_WITH_ICON:
                result = openList =
                        PieLauncherApp.apps.isDrawerIcon(appIcon);
                break;
            case Preferences.OPEN_LIST_WITH_LONG_PRESS:
                openList = tapFlags.longPress;
                break;
            case Preferences.OPEN_LIST_WITH_DOUBLE_TAP:
                openList = tapFlags.doubleTap;
                break;
            case Preferences.OPEN_LIST_WITH_TWO_FINGER_TAP:
                openList = tapFlags.twoFingerTap;
                break;
            case Preferences.OPEN_LIST_WITH_FOUR_FINGER_TAP:
                openList = tapFlags.fourFingerTap;
                break;
            case Preferences.OPEN_LIST_WITH_TAP:
            default:
                openList = tapFlags.wasTap;
                break;
        }
        if (openList) {
            if (listListener != null) {
                listListener.onOpenList(false);
            }
        } else if (pieVisible && appIcon != null) {
            launchApp(context, appIcon);
            result = true;
        }
        return result;
    }

    public boolean performShortcutAction(Context context, Apps.AppIcon shortcut) {
        if (shortcut != null && AppLauncher.launchApp(context, shortcut)) {
            if (listListener != null) {
                listListener.onHideList();
            }
            return true;
        }
        return false;
    }

    public boolean performListAction(Context context, Apps.AppIcon listIcon) {
        if (listIcon == null) {
            switch (prefs.openListWith()) {
                case Preferences.OPEN_LIST_WITH_TAP:
                case Preferences.OPEN_LIST_WITH_ANY_TOUCH:
                case Preferences.OPEN_LIST_WITH_DOUBLE_TAP:
                case Preferences.OPEN_LIST_WITH_TWO_FINGER_TAP:
                case Preferences.OPEN_LIST_WITH_FOUR_FINGER_TAP:
                    if (listListener != null) {
                        listListener.onHideList();
                    }
                    break;
            }
            return false;
        }
        launchApp(context, listIcon);
        if (listListener != null) {
            listListener.onHideList();
        }
        return true;
    }

    public boolean performListAction(
            Context context,
            Apps.AppIcon shortcutIcon,
            Apps.AppIcon listIcon) {
        if (shortcutIcon != null) {
            return performShortcutAction(context, shortcutIcon);
        }
        return performListAction(context, listIcon);
    }

    public void launchSelectedAppFromList(
            Context context,
            int selectedApp,
            List<Apps.AppIcon> appList) {
        if (selectedApp < 0 || appList == null || appList.isEmpty()) {
            return;
        }
        int index = Math.max(0, Math.min(appList.size() - 1, selectedApp));
        launchApp(context, appList.get(index));
    }
}
