package de.markusfisch.android.pielauncher.widget.action;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.widget.AppPieView;
import de.markusfisch.android.pielauncher.widget.touch.TapFlags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AppActionHandlerTest {

    private Context context;
    private Preferences prefs;
    private AppActionHandler actionHandler;
    private Apps.AppIcon testAppIcon;
    private Apps.AppIcon testShortcutIcon;

    private boolean openedList = false;
    private boolean hidList = false;
    private Apps.AppIcon tappedApp = null;
    private Apps.AppIcon launchingIcon = null;
    private Apps.AppIcon infoRequestedApp = null;
    private Apps.AppIcon editOptionsApp = null;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        prefs = PieLauncherApp.getPrefs(context);

        Constructor<Apps.AppIcon> constructor = Apps.AppIcon.class.getDeclaredConstructor(
                android.content.ComponentName.class, String.class, Bitmap.class, android.os.UserHandle.class);
        constructor.setAccessible(true);

        Bitmap bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
        testAppIcon = constructor.newInstance(
                new android.content.ComponentName("com.test.app", "MainActivity"),
                "Test App", bitmap, android.os.Process.myUserHandle());
        testShortcutIcon = constructor.newInstance(
                new android.content.ComponentName("com.test.app", "Shortcut"),
                "Test Shortcut", bitmap, android.os.Process.myUserHandle());

        openedList = false;
        hidList = false;
        tappedApp = null;
        launchingIcon = null;
        infoRequestedApp = null;
        editOptionsApp = null;

        actionHandler = new AppActionHandler(prefs, new AppActionHandler.Listener() {
            @Override
            public void onLaunchingIcon(Apps.AppIcon icon) {
                launchingIcon = icon;
            }

            @Override
            public boolean isListFiltered() {
                return false;
            }

            @Override
            public String getFilterQuery() {
                return "";
            }
        });

        actionHandler.setActionCallback(new AppPieView.ActionCallback() {
            @Override
            public void onAppTapped(Apps.AppIcon icon) {
                tappedApp = icon;
            }

            @Override
            public void onAppInfoRequested(Apps.AppIcon icon) {
                infoRequestedApp = icon;
            }

            @Override
            public void onEditOptionsRequested(Apps.AppIcon icon) {
                editOptionsApp = icon;
            }
        });

        actionHandler.setListListener(new AppPieView.ListListener() {
            @Override
            public void onOpenList(boolean resume) {
                openedList = true;
            }

            @Override
            public void onHideList() {
                hidList = true;
            }

            @Override
            public void onScrollList(int y, boolean isScrolling) {}

            @Override
            public void onDragDown(float alpha) {}
        });
    }

    @Test
    public void performPieActionLaunchesAppWhenPieVisibleAndNotOpenList() {
        prefs.setOpenListWith(Preferences.OPEN_LIST_WITH_ICON);
        TapFlags flags = new TapFlags();
        flags.wasTap = false;

        boolean result = actionHandler.performPieAction(context, flags, testAppIcon, true);

        assertTrue(result);
        assertEquals(testAppIcon, tappedApp);
        assertEquals(testAppIcon, launchingIcon);
        assertFalse(openedList);
    }

    @Test
    public void performPieActionOpensListOnTapWhenConfigured() {
        prefs.setOpenListWith(Preferences.OPEN_LIST_WITH_TAP);
        TapFlags flags = new TapFlags();
        flags.wasTap = true;

        boolean result = actionHandler.performPieAction(context, flags, testAppIcon, true);

        assertFalse(result);
        assertNull(tappedApp);
        assertTrue(openedList);
    }

    @Test
    public void performListActionLaunchesAppAndHidesList() {
        boolean result = actionHandler.performListAction(context, null, testAppIcon);

        assertTrue(result);
        assertEquals(testAppIcon, tappedApp);
        assertTrue(hidList);
    }

    @Test
    public void performListActionHidesListOnBackgroundTap() {
        prefs.setOpenListWith(Preferences.OPEN_LIST_WITH_TAP);

        boolean result = actionHandler.performListAction(context, null, null);

        assertFalse(result);
        assertNull(tappedApp);
        assertTrue(hidList);
    }

    @Test
    public void launchSelectedAppFromListLaunchesAppAtValidIndex() {
        List<Apps.AppIcon> list = new ArrayList<>();
        list.add(testAppIcon);

        actionHandler.launchSelectedAppFromList(context, 0, list);

        assertEquals(testAppIcon, tappedApp);
    }

    @Test
    public void launchSelectedAppFromListIgnoresInvalidIndex() {
        List<Apps.AppIcon> list = new ArrayList<>();
        list.add(testAppIcon);

        actionHandler.launchSelectedAppFromList(context, -1, list);
        assertNull(tappedApp);

        actionHandler.launchSelectedAppFromList(context, 0, new ArrayList<>());
        assertNull(tappedApp);
    }

    @Test
    public void requestEditOptionsDelegatesToActionCallback() {
        actionHandler.requestEditOptions(testAppIcon);
        assertEquals(testAppIcon, editOptionsApp);
    }

    @Test
    public void requestAppInfoDelegatesToActionCallback() {
        actionHandler.requestAppInfo(testAppIcon);
        assertEquals(testAppIcon, infoRequestedApp);
    }

    @Test
    public void performShortcutActionReturnsFalseWhenShortcutNull() {
        boolean result = actionHandler.performShortcutAction(context, null);
        assertFalse(result);
        assertFalse(hidList);
    }
}
