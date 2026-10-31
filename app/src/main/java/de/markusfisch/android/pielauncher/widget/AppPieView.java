package de.markusfisch.android.pielauncher.widget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.activity.PickIconActivity;
import de.markusfisch.android.pielauncher.activity.PreferencesActivity;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.AppSearch;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.graphics.BackgroundBlur;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.graphics.Converter;
import de.markusfisch.android.pielauncher.graphics.PieMenu;
import de.markusfisch.android.pielauncher.widget.state.ViewStateManager;
import de.markusfisch.android.pielauncher.widget.state.PieEditor;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.view.SystemBars;
import de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer;
import de.markusfisch.android.pielauncher.widget.renderer.ListRenderer;
import de.markusfisch.android.pielauncher.widget.renderer.PieRenderer;
import de.markusfisch.android.pielauncher.widget.touch.TouchController;
import de.markusfisch.android.pielauncher.widget.touch.EditorTouchState;
import de.markusfisch.android.pielauncher.widget.touch.ListTouchState;
import de.markusfisch.android.pielauncher.widget.touch.PieTouchState;
import de.markusfisch.android.pielauncher.widget.touch.GeneralTouchListener;
import de.markusfisch.android.pielauncher.widget.touch.ListTouchListener;
import de.markusfisch.android.pielauncher.widget.touch.PieTouchListener;
import de.markusfisch.android.pielauncher.widget.touch.TapFlags;
import de.markusfisch.android.pielauncher.widget.shortcuts.AppShortcutsController;
import de.markusfisch.android.pielauncher.widget.state.EditorAction;
import de.markusfisch.android.pielauncher.widget.state.AppSearchFilter;
import de.markusfisch.android.pielauncher.widget.state.PieConfiguration;
import de.markusfisch.android.pielauncher.widget.action.AppActionHandler;

public class AppPieView extends View {

    public interface ActionCallback {
        void onAppTapped(Apps.AppIcon icon);

        void onAppInfoRequested(Apps.AppIcon icon);

        void onEditOptionsRequested(Apps.AppIcon icon);
    }

    public void setActionCallback(ActionCallback actionCallback) {
        actionHandler.setActionCallback(actionCallback);
    }

    public interface ListListener {
        void onOpenList(boolean resume);

        void onHideList();

        void onScrollList(int y, boolean isScrolling);

        void onDragDown(float alpha);
    }

    private static final int HAPTIC_FEEDBACK_DOWN =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    ? HapticFeedbackConstants.KEYBOARD_TAP
                    : HapticFeedbackConstants.CONTEXT_CLICK;
    private static final int HAPTIC_FEEDBACK_CONFIRM =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                    ? HAPTIC_FEEDBACK_DOWN
                    : HapticFeedbackConstants.CONFIRM;
    private static final int HAPTIC_FEEDBACK_CHOICE =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    ? HAPTIC_FEEDBACK_DOWN
                    : HapticFeedbackConstants.SEGMENT_TICK;

    private final CanvasPieMenu<Apps.AppIcon> pieMenu = new CanvasPieMenu<>();
    private final ArrayList<Apps.AppIcon> menuPrimary =
            PieLauncherApp.apps.menuPrimary;
    private final ArrayList<Apps.AppIcon> menuSecondary =
            PieLauncherApp.apps.menuSecondary;
    private final ViewStateManager stateManager = new ViewStateManager(new ViewStateManager.Listener() {
        @Override
        public void invalidate() {
            AppPieView.this.invalidate();
        }

        @Override
        public void setVerticalScrollBarEnabled(boolean enabled) {
            AppPieView.this.setVerticalScrollBarEnabled(enabled);
        }

        @Override
        public void scrollList(int y, boolean isScrolling) {
            AppPieView.this.scrollList(y, isScrolling);
        }

        @Override
        public void resetScrollWithoutAnimation() {
            AppPieView.this.resetScrollWithoutAnimation();
        }

        @Override
        public void fadeOutMode() {
            AppPieView.this.fadeOutMode();
        }

        @Override
        public void onModeChanged(int mode) {
            switch (mode) {
                case ViewStateManager.MODE_PIE:
                    if (touchController != null) touchController.setState(pieTouchState);
                    lastSelectedIcon = -1;
                    break;
                case ViewStateManager.MODE_LIST:
                    if (touchController != null) touchController.setState(listTouchState);
                    break;
                case ViewStateManager.MODE_EDIT:
                    if (touchController != null) touchController.setState(editorTouchState);
                    pieMenu.calculate(viewWidth >> 1, viewHeight >> 1);
                    break;
            }
        }
    });

    private final PieEditor pieEditor = new PieEditor(new PieEditor.Listener() {
        @Override
        public void invalidate() {
            AppPieView.this.invalidate();
        }

        @Override
        public void storeMenu() {
            AppPieView.this.storeMenu();
        }

        @Override
        public void releaseIcon() {
            AppPieView.this.releaseIcon();
        }
    }, pieMenu);
    private PieRenderer pieRenderer;
    private ListRenderer listRenderer;
    private EditorRenderer editorRenderer;

    private final android.graphics.Point touch = new android.graphics.Point();
    private final String loadingTip;
    private final Preferences prefs;
    private final long longPressTimeout;
    private final int listPadding;
    private final int searchInputHeight;
    private final int iconSize;
    private final int iconTextPadding;
    private final int spaceBetween;
    private final int translucentBackgroundColor;
    private final float maxBlurRadius;
    private final float dp;
    private final float sp;

    private Window window;
    private RenderNode pieRenderNode;
    private RenderNode listRenderNode;
    private int viewWidth;
    private int viewHeight;
    private int controlsPadding;
    private int deadZoneTop;
    private int deadZoneBottom;
    private int deadZoneLeft;
    private int deadZoneRight;
    private int listFadeHeight;
    private final PieConfiguration pieConfig = new PieConfiguration();
    private int maxScrollY;
    private int lastScrollY;
    private int lastSelectedIcon;
    private int currentBackgroundBlur;
    private int currentPieBlur;
    private int currentListBlur;
    private int selectedApp = -1;
    private ListListener listListener;
    private List<Apps.AppIcon> appList;
    private Rect highlightedAction;
    private Apps.AppIcon touchedIcon;
    private Apps.AppIcon highlightedIcon;
    private long highlightedFrom;
    private boolean appListIsFiltered = false;

    private final AppShortcutsController shortcutsController;
    private final AppActionHandler actionHandler;
    private int keyboardHeight = -1;

    public AppPieView(Context context, AttributeSet attr) {
        super(context, attr);

        prefs = PieLauncherApp.getPrefs(context);

        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        dp = dm.density;
        sp = dm.scaledDensity;

        pieMenu.icons = menuPrimary;
        pieMenu.onCircle(this::onCircle);

        controlsPadding = Math.round(80f * dp);
        listPadding = Math.round(16f * dp);
        searchInputHeight = Math.round(112f * dp);
        iconSize = Math.round(48f * dp);
        iconTextPadding = Math.round(12f * dp);
        spaceBetween = Math.round(4f * dp);
        maxBlurRadius = iconSize * .35f;

        shortcutsController = new AppShortcutsController(context, dp);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            trackKeyboardApi30();
        }

        loadingTip = context.getString(R.string.tip_loading);

        translucentBackgroundColor = res.getColor(R.color.bg_ui);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pieRenderNode = new RenderNode("PieMenu");
            listRenderNode = new RenderNode("List");
        }


        ViewConfiguration configuration = ViewConfiguration.get(context);
        longPressTimeout = ViewConfiguration.getLongPressTimeout() *
                (prefs.getIconPress() == Preferences.ICON_PRESS_LONGER
                        ? 2L : 1L);
        if (PieLauncherApp.apps.isEmpty()) {
            PieLauncherApp.apps.indexAppsAsync(context);
        }
        initTouchListener();

        pieRenderer = new PieRenderer(pieMenu, stateManager.getFadePie(), prefs);
        listRenderer = new ListRenderer(context);
        editorRenderer = new EditorRenderer(context);

        actionHandler = new AppActionHandler(prefs, new AppActionHandler.Listener() {
            @Override
            public void onLaunchingIcon(Apps.AppIcon icon) {
                pieRenderer.setLaunchingIcon(icon);
            }

            @Override
            public boolean isListFiltered() {
                return stateManager.inListMode() && appSearchFilter.isFiltered();
            }

            @Override
            public String getFilterQuery() {
                return appSearchFilter.getQuery();
            }
        });
    }

    public void setWindow(Window window) {
        this.window = window;
    }

    public void setListListener(ListListener listener) {
        listListener = listener;
        actionHandler.setListListener(listener);
    }

    public void resetScroll() {
        lastScrollY = 0;
    }

    public void showList() {
        if (maxScrollY > 0 && lastScrollY > maxScrollY) {
            lastScrollY = maxScrollY;
        }
        stateManager.setLastScrollY(lastScrollY);
        pieMenu.addScrollShift(lastScrollY);
        stateManager.showList();
    }

    public void hideList() {
        stateManager.hideList();
    }

    public void dragDownListBy(float y) {
        stateManager.dragDownListBy(y);
        if (listListener != null) {
            listListener.onDragDown(stateManager.getDragProgress());
        }
    }

    public void showEditor() {
        editIcon(null);
        stateManager.showEditor();
    }

    public boolean isEmpty() {
        return appList == null || appList.isEmpty();
    }

    public int getIconCount() {
        return appList == null ? 0 : appList.size();
    }

    public boolean appListNotScrolled() {
        return stateManager.getMode() != ViewStateManager.MODE_LIST || getScrollY() == 0;
    }

    public boolean inEditMode() {
        return stateManager.inEditMode();
    }

    public boolean inListMode() {
        return stateManager.inListMode();
    }

    public boolean isScrollingAppShortcuts() {
        return shortcutsController.isTouched();
    }

    public boolean isFiltered() {
        return appSearchFilter.isFiltered();
    }

    public String getQuery() {
        return appSearchFilter.getQuery();
    }

    private final AppSearchFilter appSearchFilter = new AppSearchFilter((newAppList, isFiltered, selected) -> {
        if (newAppList != null) {
            appList = newAppList;
        }
        appListIsFiltered = isFiltered;
        selectedApp = selected;
        invalidate();
    });

    public void filterAppList(String query, boolean resetScroll) {
        appSearchFilter.filterAppList(getContext(), query, prefs.doubleSpaceLaunch());
        if (resetScroll) {
            scrollList(0, false);
            lastScrollY = 0;
        }
        calculateMaxScrollY();
        updateAppShortcuts();
    }

    public void launchSelectedAppFromList() {
        actionHandler.launchSelectedAppFromList(getContext(), selectedApp, appList);
    }

    public void endEditMode() {
        pieEditor.storeMenu();
        pieEditor.commit();
        releaseIcon();
        stateManager.endEditMode();
        if (prefs.excludePie() != Preferences.EXCLUDE_PIE_NONE) {
            PieLauncherApp.apps.propagateUpdate();
        }
    }

    @Override
    protected void onLayout(
            boolean changed,
            int left,
            int top,
            int right,
            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            layoutView(right - left, bottom - top);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = SystemClock.uptimeMillis();
        float ad = prefs.getAnimationDuration();
        float fPie = stateManager.getFadePie().get(now,
                // Prolong fade out to improve touch feedback.
                stateManager.getFadePie().isFadingOut(now) ? ad * 3f : ad);
        float fList = Math.min(stateManager.getFadeList().get(now, ad), stateManager.getDragProgress());
        float fEdit = stateManager.getFadeEdit().get(now, ad);
        float fMax = easeSlowerIn(Math.max(fPie, Math.max(fList, fEdit)));

        int bbr = prefs.getBackgroundBlurRadius();
        if (bbr > 0) {
            int blur = Math.round(fMax * bbr);
            if (blur != currentBackgroundBlur) {
                BackgroundBlur.setBlurRadius(window, blur);
                currentBackgroundBlur = blur;
            }
        }

        if (stateManager.getMode() != ViewStateManager.MODE_PIE || prefs.darkenBackground() !=
                Preferences.DARKEN_BACKGROUND_NONE) {
            darkenBackground(canvas, fMax);
        }

        if (stateManager.inEditMode()) {
            int centerX = viewWidth >> 1;
            int centerY = viewHeight >> 1;
            setCenter(centerX, centerY);
            if (pieEditor.hasGrabbedIcon()) {
                pieEditor.setTouch(touch);
                pieEditor.setTwist(pieMenu.getTwist());
                pieEditor.calculateEditablePie(centerX, centerY);
            } else {
                pieMenu.calculate(centerX, centerY);
            }
        }

        Canvas pieCanvas = getRecordingCanvas(pieRenderNode, canvas);
        float pieBlurAlpha = stateManager.inEditMode() ? 1f : fPie;
        currentPieBlur = applyBlurOut(pieRenderNode, currentPieBlur, pieBlurAlpha);
        boolean invalidate = pieRenderer.draw(pieCanvas, touch, fPie, now, ad, stateManager.inEditMode());
        drawRenderNode(pieRenderNode, canvas);

        if (PieLauncherApp.apps.isIndexing()) {
            if (loadingTip != null) {
                canvas.drawText(loadingTip, viewWidth >> 1, controlsPadding + listRenderer.getTextOffset(), listRenderer.getPaintText());
            }
        }

        int shortcutsHeight = shortcutsController.getHeight(prefs.showAppShortcuts(), stateManager.inListMode());
        boolean shortcutsWereVisible = shortcutsController.isVisible();
        shortcutsController.setVisible(shortcutsHeight > 0 && fList > 0 &&
                !stateManager.isDraggingDownList());

        Canvas listCanvas = getRecordingCanvas(listRenderNode, canvas);
        currentListBlur = applyBlurOut(listRenderNode, currentListBlur, fList);
        invalidate |= listRenderer.draw(listCanvas, stateManager.getFadeList(),
                appList, getScrollY(), viewWidth, viewHeight,
                touchedIcon, highlightedIcon, isDraggingDownList(),
                selectedApp, highlightedFrom, ad, showAppNames(),
                prefs.listAnimationAppearance(), stateManager.getDragProgress(),
                listRenderNode, shortcutsHeight);

        if (shortcutsController.isVisible()) {
            if (!shortcutsWereVisible) {
                shortcutsController.setExpandFrom(now);
            }
            if (shortcutsController.getExpandFrom() > 0) {
                float t = ad > 0 ? (now - shortcutsController.getExpandFrom()) / ad : 1f;
                if (t < 1f) {
                    float x = t - 1f;
                    float spring = 1f + x * x * (2.5f * x + 1.5f);
                    shortcutsHeight = Math.round(shortcutsHeight * (.85f + .15f * spring));
                    invalidate = true;
                } else {
                    shortcutsController.setExpandFrom(0);
                }
            }
            shortcutsController.layout(viewWidth, viewHeight, getBottomInset(), listPadding, shortcutsHeight, getScrollY());
            shortcutsController.drawBackdrop(listRenderNode, canvas, getScrollY(), listPadding);
            invalidate |= shortcutsController.draw(canvas, fList, shortcutsHeight, getScrollY(), listRenderer.getTextOffset(), listPadding);
        } else {
            drawRenderNode(listRenderNode, canvas);
        }

        editorRenderer.setTouch(touch);
        invalidate |= editorRenderer.draw(canvas, stateManager.getFadeEdit(), pieMenu, pieEditor.getGrabbedIcon(), pieEditor.getGrabbedIconAt(), highlightedAction, ad, hasSecondaryMenu(), PieLauncherApp.iconPack.hasPacks());

        if (invalidate) {
            invalidate();
        }
    }

    @Override
    protected int computeVerticalScrollRange() {
        return maxScrollY + getHeight();
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return getScrollY();
    }

    private final FlingRunnable flingRunnable = new FlingRunnable();
    private Runnable highlightRunnable;
    private Runnable longPressRunnable;

    private TouchController touchController;
    private PieTouchState pieTouchState;
    private ListTouchState listTouchState;
    private EditorTouchState editorTouchState;

    private class AppGeneralTouchListener implements GeneralTouchListener {
        @Override
        public void performAction(View v, Point t, TapFlags tapFlags) {
            AppPieView.this.touch.set(t.x, t.y);
            if (AppPieView.this.performAction(v.getContext(), t, tapFlags)) {
                performHapticFeedbackIfAllowed(HAPTIC_FEEDBACK_CONFIRM);
            }
            resetHighlightedAction();
        }

        @Override
        public void postDelayed(Runnable r, long delayMillis) {
            AppPieView.this.postDelayed(r, delayMillis);
        }

        @Override
        public void removeCallbacks(Runnable r) {
            AppPieView.this.removeCallbacks(r);
        }

        @Override
        public void releaseIcon() {
            AppPieView.this.releaseIcon();
        }

        @Override
        public void setTouch(Point touch) {
            AppPieView.this.touch.set(touch.x, touch.y);
        }

        @Override
        public void invalidate() {
            AppPieView.this.invalidate();
        }

        @Override
        public Context getContext() {
            return AppPieView.this.getContext();
        }

        @Override
        public void editIconAt(Point touch) {
            AppPieView.this.editIconAt(touch);
        }

        @Override
        public void updateEditablePie(Point touch) {
            if (pieEditor.hasGrabbedIcon()) {
                pieEditor.setTouch(touch);
                pieEditor.setTwist(pieMenu.getTwist());
                pieEditor.calculateEditablePie(viewWidth >> 1, viewHeight >> 1);
            }
        }
    }

    private class AppPieTouchListener implements PieTouchListener {
        @Override
        public void openMenu(long eventTime) {
            if (prefs.splitPieMenu() != Preferences.ALTERNATE_MENU_NONE) {
                selectMenu(touch.x, touch.y);
                setCenter(touch.x, touch.y);
            } else {
                if (prefs.circleSwapsMenus() != Preferences.ALTERNATE_MENU_NONE && !isPrimaryMenu()) {
                    setMenu(menuPrimary);
                }
                setCenter(touch.x, touch.y);
            }
            stateManager.getFadePie().fadeIn(eventTime);
            performHapticFeedbackIfAllowed(HAPTIC_FEEDBACK_DOWN);
        }

        @Override
        public void startSpin() {
        }

        @Override
        public void spin(double twist) {
            if (!stateManager.getFadePie().isVisible()) return;
            pieMenu.setTwist(twist);
        }

        @Override
        public void stopSpin() {
        }

        @Override
        public double getPieTwist() {
            return pieMenu.getTwist();
        }

        @Override
        public Point getPieCenter() {
            return new Point(pieMenu.getCenterX(), pieMenu.getCenterY());
        }

        @Override
        public boolean inDeadZone(Point touch) {
            AppPieView.this.touch.set(touch.x, touch.y);
            switch (prefs.getDeadZone()) {
                case Preferences.DEAD_ZONE_TOP:
                    return touch.y < deadZoneTop;
                case Preferences.DEAD_ZONE_BOTTOM:
                    return touch.y > deadZoneBottom;
                case Preferences.DEAD_ZONE_TOP_BOTTOM:
                    return touch.y < deadZoneTop || touch.y > deadZoneBottom;
                case Preferences.DEAD_ZONE_ALL:
                    return touch.y < deadZoneTop || touch.y > deadZoneBottom || touch.x < deadZoneLeft || touch.x > deadZoneRight;
                default:
                    return false;
            }
        }

        @Override
        public boolean expandPanelInDeadZone() {
            return prefs.expandPanelInDeadZone();
        }

        @Override
        public void fadeOutMode() {
            AppPieView.this.fadeOutMode();
        }

        @Override
        public void evaluateSelectedIcon(Point touch) {
            pieMenu.calculate(touch.x, touch.y, 1f, pieRenderer.getLaunchingIcon());
            int selectedIcon = pieMenu.getSelectedIcon();
            if (selectedIcon != lastSelectedIcon) {
                lastSelectedIcon = selectedIcon;
                performHapticFeedbackIfAllowed(HAPTIC_FEEDBACK_CHOICE);
            }
        }
    }

    private class AppListTouchListener implements ListTouchListener {
        @Override
        public int getScrollY() {
            return AppPieView.this.getScrollY();
        }

        @Override
        public int getMaxScrollY() {
            return maxScrollY;
        }

        @Override
        public void scroll(int scrollY, boolean anim) {
            scrollList(scrollY, anim);
        }

        @Override
        public void keepScrolling(int velocityY) {
            if (shortcutsController.isTouched()) {
                shortcutsController.fling(velocityY);
                invalidate();
            } else {
                flingRunnable.start(velocityY);
            }
        }

        @Override
        public void initAppShortcutsScroll(Point touch) {
            shortcutsController.initScroll(touch, shortcutsController.isVisible());
        }

        @Override
        public void scrollAppShortcuts(int delta) {
            shortcutsController.scrollBy(delta);
        }

        @Override
        public boolean isAppShortcutsTouched() {
            return shortcutsController.isTouched();
        }

        @Override
        public void setAppShortcutsTouched(boolean touched) {
            shortcutsController.setTouched(touched);
        }

        @Override
        public void initLongPress(Context context, Point t) {
            if (prefs.getIconPress() == Preferences.ICON_LOCK_MENU) return;
            cancelLongPress();
            final Apps.AppIcon appIcon = getListIconAt(t.x, t.y);
            if (appIcon == null) return;
            touchedIcon = appIcon;
            highlightRunnable = () -> {
                highlightedIcon = appIcon;
                highlightedFrom = android.os.SystemClock.uptimeMillis();
            };
            postDelayed(highlightRunnable, longPressTimeout >> 1);
            longPressRunnable = () -> {
                performHapticFeedbackIfAllowed(android.view.HapticFeedbackConstants.LONG_PRESS);
                if (prefs.getIconPress() == Preferences.ICON_PRESS_MENU) {
                    actionHandler.requestEditOptions(appIcon);
                } else {
                    addIconInteractively(appIcon);
                }
                longPressRunnable = null;
                cancelHighlight();
            };
            postDelayed(longPressRunnable, longPressTimeout);
        }

        @Override
        public void cancelLongPress() {
            cancelHighlight();
            if (longPressRunnable != null) {
                removeCallbacks(longPressRunnable);
                longPressRunnable = null;
            }
        }

        @Override
        public boolean isFadingIn(long eventTime) {
            return stateManager.getFadeList().isFadingIn(eventTime);
        }

        @Override
        public boolean isDraggingDownList() {
            return stateManager.isDraggingDownList();
        }

        @Override
        public float getDragProgress() {
            return stateManager.getDragProgress();
        }

        @Override
        public void onDragDown(float dragProgress) {
            if (listListener != null) {
                listListener.onDragDown(dragProgress);
            }
        }

        @Override
        public void cancelDragDownList() {
            if (stateManager.isDraggingDownList()) {
                if (stateManager.getDragProgress() <= 0.5f) {
                    stateManager.resetDragDownList();
                    if (listListener != null) {
                        listListener.onHideList();
                    }
                } else {
                    stateManager.resetDragDownList();
                    invalidate();
                    if (listListener != null) {
                        listListener.onDragDown(1f);
                    }
                }
            }
        }

        @Override
        public void stopFling() {
            flingRunnable.stop();
        }
    }

    private void initTouchListener() {
        GeneralTouchListener generalTouchListener = new AppGeneralTouchListener();
        PieTouchListener pieTouchListener = new AppPieTouchListener();
        ListTouchListener listTouchListener = new AppListTouchListener();

        pieTouchState = new PieTouchState(pieTouchListener);
        listTouchState = new ListTouchState(listTouchListener);
        editorTouchState = new EditorTouchState(generalTouchListener);

        touchController = new TouchController(generalTouchListener);
        touchController.setState(pieTouchState); // Default state
        setOnTouchListener(touchController);
    }

    private void cancelHighlight() {
        touchedIcon = null;
        highlightedIcon = null;
        highlightedFrom = 0;
        if (highlightRunnable != null) {
            removeCallbacks(highlightRunnable);
            highlightRunnable = null;
        }
    }

    private void cancelDragDownList() {
        if (isDraggingDownList()) {
            resetDragDownList();
            invalidate();
            if (listListener != null) {
                listListener.onDragDown(stateManager.getDragProgress());
            }
        }
    }

    private boolean isDraggingDownList() {
        return stateManager.isDraggingDownList();
    }

    private void resetDragDownList() {
        stateManager.resetDragDownList();
    }

    private void calculateMaxScrollY() {
        if (viewWidth == 0 || appList == null || appList.isEmpty()) {
            maxScrollY = 0;
            return;
        }
        int innerWidth = viewWidth - listPadding * 2;
        int columns = Math.max(1, Math.min(5, innerWidth / (iconSize + spaceBetween)));
        boolean showAppNames = prefs.showAppNames() == Preferences.SHOW_APP_NAMES_ALWAYS ||
                (prefs.showAppNames() == Preferences.SHOW_APP_NAMES_SEARCH && appListIsFiltered);
        int iconAndTextHeight = iconSize + (showAppNames ? iconTextPadding + Math.round(listRenderer.getTextHeight()) : 0);
        int cellHeight = iconAndTextHeight + iconTextPadding * 2;
        int rows = (int) Math.ceil((double) appList.size() / columns);
        int shortcutsHeight = shortcutsController.getHeight(prefs.showAppShortcuts(), stateManager.inListMode());
        int bottomInset = shortcutsHeight > 0
                ? getBottomInset() + listPadding : getPaddingBottom();
        int maxHeight = searchInputHeight + listPadding + rows * cellHeight + listPadding + shortcutsHeight;
        int maxY = Math.max(0, maxHeight - (viewHeight - bottomInset));
        if (maxY != maxScrollY) {
            maxScrollY = maxY;
            if (getScrollY() > maxScrollY) {
                scrollList(maxScrollY, false); // Ensure we don't scroll past the new max
            }
        }
    }

    private void layoutView(int width, int height) {
        int viewMin = Math.min(width, height);
        int viewMax = Math.max(width, height);
        viewWidth = width;
        viewHeight = height;

        pieConfig.layout(viewMin, dp);
        loadPiePreferences();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                pieRenderNode != null) {
            pieRenderNode.setPosition(0, 0, viewWidth, viewHeight);
        }

        pieConfig.updateChangeIcons(getResources(), editorRenderer);
        pieConfig.updateSplitPieIcon(getResources(), editorRenderer, !isPrimaryMenu());

        int pieBottom = viewMax / 2 + pieConfig.getMaxRadius();
        controlsPadding = (viewMax - pieBottom) / 2;

        deadZoneTop = Math.min(height / 10, Math.round(64f * dp));
        deadZoneBottom = height - deadZoneTop;
        deadZoneLeft = Math.min(width / 10, Math.round(48f * dp));
        deadZoneRight = width - deadZoneLeft;

        listFadeHeight = Math.round(viewHeight * .5f);
        stateManager.setListFadeHeight(listFadeHeight);

        shortcutsController.setTextSize(sp);
        listRenderer.layout(listPadding, searchInputHeight, iconSize, spaceBetween, iconTextPadding, listFadeHeight, sp);
        editorRenderer.layout(viewWidth, viewHeight, controlsPadding, listRenderer.getTextOffset(), sp, spaceBetween, iconSize);
        calculateMaxScrollY();
    }

    public void addIconInteractively(Apps.AppIcon appIcon) {
        if (listListener != null) {
            listListener.onHideList();
        }
        stateManager.getFadeList().fadeOut();
        pieEditor.setNeverDropped(true);
        editIcon(appIcon);
        resetScrollWithoutAnimation();
        invalidate();
    }

    private void editIcon(Apps.AppIcon icon) {
        if (pieMenu.icons != menuPrimary &&
                pieMenu.icons != menuSecondary) {
            setMenu(menuPrimary);
        }
        pieConfig.updateSplitPieIcon(getResources(), editorRenderer, !isPrimaryMenu());
        setCenter(viewWidth >> 1, viewHeight >> 1);
        pieEditor.beginEdit(pieMenu.icons);
        pieMenu.icons.remove(icon);
        CanvasPieMenu.paint.setAlpha(255);
        pieEditor.editIcon(icon);
        if (stateManager.getMode() != ViewStateManager.MODE_EDIT) {
            stateManager.showEditor();
        }
        invalidate();
    }

    public void releaseIcon() {
        pieEditor.setNeverDropped(false);
        pieEditor.commitGrabbedItem();
    }

    private void resetScrollWithoutAnimation() {
        scrollTo(0, 0);
    }

    private void scrollList(int y, boolean isScrolling) {
        lastScrollY = y;
        scrollTo(0, y);
        if (listListener != null) {
            listListener.onScrollList(y, isScrolling);
        }
    }

    private boolean performAction(
            Context context,
            Point at,
            TapFlags tapFlags) {
        if (stateManager.getMode() == ViewStateManager.MODE_PIE) {
            Apps.AppIcon appIcon = null;
            int index = pieMenu.getSelectedIcon();
            if (index > -1 && index < pieMenu.icons.size()) {
                appIcon = pieMenu.icons.get(index);
            }
            boolean pieVisible = stateManager.getFadePie().isVisible();
            fadeOutMode();
            return actionHandler.performPieAction(context, tapFlags, appIcon, pieVisible);
        } else if (stateManager.inListMode() && tapFlags.wasTap) {
            if (shortcutsController.isVisible() && shortcutsController.contains(at.x, at.y)) {
                Apps.AppIcon shortcut = shortcutsController.getShortcutAt(at.x, at.y);
                return actionHandler.performShortcutAction(context, shortcut);
            }
            Apps.AppIcon listIcon = getListIconAt(at.x, at.y);
            return actionHandler.performListAction(context, listIcon);
        } else if (stateManager.inEditMode()) {
            boolean result = performEditAction(context);
            releaseIcon();
            pieMenu.updateSmoothing();
            return result;
        }
        return false;
    }

    private Apps.AppIcon getListIconAt(int x, int y) {
        if (shortcutsController.isVisible() && shortcutsController.contains(x, y)) {
            return null;
        }
        y += getScrollY();
        for (int i = 0, l = getIconCount(); i < l; ++i) {
            Apps.AppIcon appIcon = appList.get(i);
            if (appIcon.hitRect.contains(x, y)) {
                return appIcon;
            }
        }
        return null;
    }

    private boolean performEditAction(Context context) {
        EditorAction action = editorRenderer.getActionAt(touch, pieEditor.hasGrabbedIcon());
        switch (action) {
            case CHANGE_TWIST:
                pieConfig.cycleTwist();
                pieConfig.updateChangeTwistIcon(getResources(), editorRenderer);
                return true;
            case CHANGE_ICON_SCALE:
                pieConfig.cycleIconScale();
                pieConfig.updateChangeIconScaleIcon(getResources(), editorRenderer);
                return true;
            case CHANGE_RADIUS:
                pieConfig.cycleRadius();
                pieMenu.setRadius(pieConfig.getRadius());
                pieConfig.updateChangeRadiusIcon(getResources(), editorRenderer);
                return true;
            case BACK_OR_REMOVE_ICON:
                if (!pieEditor.hasGrabbedIcon()) {
                    ((Activity) context).onBackPressed();
                } else {
                    removeIconFromPie(pieEditor.getGrabbedIcon(),
                            PieLauncherApp.apps.isDrawerIcon(
                                    (Apps.AppIcon) pieEditor.getGrabbedIcon()));
                }
                return true;
            case EDIT_SETTINGS_OR_UNDO:
                if (!pieEditor.hasGrabbedIcon()) {
                    if (hasSecondaryMenu()) {
                        storeMenu();
                        setMenu(isPrimaryMenu() ? menuSecondary : menuPrimary);
                        centerSmoothCoordinatesAt(pieMenu.icons,
                                viewWidth >> 1, viewHeight >> 1);
                        pieConfig.updateSplitPieIcon(getResources(), editorRenderer, !isPrimaryMenu());
                        pieConfig.updateChangeIcons(getResources(), editorRenderer);
                        invalidate();
                    } else {
                        PreferencesActivity.start(context);
                    }
                } else {
                    pieEditor.rollback(pieMenu.icons);
                    if (pieEditor.isNeverDropped()) {
                        fadeOutMode();
                        returnToList();
                    }
                    if (PieLauncherApp.iconPack.hasPacks()) {
                        storeMenu();
                        changeIcon(context, pieEditor.getGrabbedIcon());
                    } else if (PieLauncherApp.apps.isDrawerIcon(
                            (Apps.AppIcon) pieEditor.getGrabbedIcon())) {
                        removeIconFromPie(pieEditor.getGrabbedIcon(), true);
                    } else {
                        PickIconActivity.askToHide(context,
                                ((Apps.AppIcon) pieEditor.getGrabbedIcon()).componentName);
                    }
                }
                return true;
            case END_EDIT_MODE:
                if (!pieEditor.hasGrabbedIcon()) {
                    endEditMode();
                } else {
                    pieEditor.rollback(pieMenu.icons);
                    fadeOutMode();
                    if (PieLauncherApp.apps.isDrawerIcon(
                            (Apps.AppIcon) pieEditor.getGrabbedIcon())) {
                        removeIconFromPie(pieEditor.getGrabbedIcon(), true);
                    } else if (Apps.isShortcut((Apps.AppIcon) pieEditor.getGrabbedIcon())) {
                        // The end target uninstalls apps; a pinned shortcut (e.g.
                        // a PWA) has nothing to uninstall, so remove it instead.
                        // Unlike launchAppInfo() this stays in-app, so we must end
                        // edit mode ourselves — otherwise the view is left in
                        // MODE_EDIT and the pie won't open until the activity is
                        // recreated (e.g. by pressing Home).
                        PieLauncherApp.apps.removePinnedShortcut(context,
                                (Apps.AppIcon) pieEditor.getGrabbedIcon());
                        endEditMode();
                    } else {
                        // Persist changes before launching app info because
                        // that can be used to uninstall an app which would
                        // trigger indexing what would reset any changes.
                        storeMenu();
                        actionHandler.requestAppInfo((Apps.AppIcon) pieEditor.getGrabbedIcon());
                    }
                }
                return true;
            case NONE:
            default:
                return false;
        }
    }

    private void updateAppShortcuts() {
        shortcutsController.update(
                getSelectedApp(),
                appSearchFilter.isFiltered(),
                prefs.showAppShortcuts(),
                getContext(),
                this::invalidate);
    }

    private Apps.AppIcon getSelectedApp() {
        if (!appSearchFilter.isFiltered() || selectedApp < 0 || isEmpty()) {
            return null;
        }
        return appList.get(clamp(selectedApp, 0, getIconCount() - 1));
    }

    private int getBottomInset() {
        int inset = getPaddingBottom();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            inset = Math.max(inset, keyboardHeight > -1
                    ? keyboardHeight
                    : getKeyboardHeightApi30(getRootWindowInsets()));
        }
        return inset;
    }

    @SuppressLint("UseRequiresApi")
    @TargetApi(Build.VERSION_CODES.R)
    private void trackKeyboardApi30() {
        setWindowInsetsAnimationCallback(new WindowInsetsAnimation.Callback(
                WindowInsetsAnimation.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
            @Override
            public WindowInsets onProgress(
                    WindowInsets insets,
                    List<WindowInsetsAnimation> animations) {
                keyboardHeight = getKeyboardHeightApi30(insets);
                invalidate();
                return insets;
            }

            @Override
            public void onEnd(WindowInsetsAnimation animation) {
                keyboardHeight = -1;
                invalidate();
            }
        });
    }

    @SuppressLint("UseRequiresApi")
    @TargetApi(Build.VERSION_CODES.R)
    private static int getKeyboardHeightApi30(WindowInsets insets) {
        return insets == null
                ? 0
                : insets.getInsets(WindowInsets.Type.ime()).bottom;
    }

    private void removeIconFromPie(Apps.AppIcon icon, boolean isDrawerIcon) {
        if (isDrawerIcon) {
            prefs.setOpenListWith(Preferences.OPEN_LIST_WITH_TAP);
        }
        pieMenu.icons.remove(icon);
        // Undo any rotation if the menu has not otherwise changed.
        if (!pieEditor.isMenuChanged(pieMenu.icons)) {
            pieEditor.rollback(pieMenu.icons);
        }
    }

    private static void centerSmoothCoordinatesAt(
            List<Apps.AppIcon> icons,
            int centerX,
            int centerY) {
        for (Apps.AppIcon ic : icons) {
            ic.setStartPosition(centerX, centerY);
        }
    }

    public void changeIcon(Context context, PieMenu.Icon icon) {
        Apps.AppIcon appIcon = (Apps.AppIcon) icon;
        PickIconActivity.start(context, appIcon.componentName);
    }

    public void returnToList() {
        if (listListener != null) {
            listListener.onOpenList(true);
        }
    }

    private void onCircle(int direction) {
        List<Apps.AppIcon> icons = getAlternateMenu(
                prefs.circleSwapsMenus());
        if (icons == null || icons.isEmpty() || icons.equals(pieMenu.icons)) {
            return;
        }
        if (prefs.animateInOut() && prefs.getAnimationDuration() > 0) {
            pieRenderer.startCircleTransition(direction, pieMenu.icons, SystemClock.uptimeMillis());
        }
        setMenu(icons);
        performHapticFeedbackIfAllowed(HapticFeedbackConstants.LONG_PRESS);
    }

    private void selectMenu(int x, int y) {
        boolean alternate = viewWidth > viewHeight
                ? x < viewWidth >> 1
                : y < viewHeight >> 1;
        List<Apps.AppIcon> icons = alternate
                ? getAlternateMenu(prefs.splitPieMenu())
                : menuPrimary;
        setMenu(icons == null || icons.isEmpty()
                ? menuPrimary
                : icons);
    }

    private List<Apps.AppIcon> getAlternateMenu(int option) {
        switch (option) {
            case Preferences.ALTERNATE_MENU_SECONDARY:
                return menuSecondary;
            case Preferences.ALTERNATE_MENU_ALL_APPS:
                List<Apps.AppIcon> all = AppSearch.filterAppsBy(
                        PieLauncherApp.apps, getContext(), null);
                return all != null ? new ArrayList<>(all) : null;
            case Preferences.ALTERNATE_MENU_LAST_USED:
                List<Apps.AppIcon> lastUsed = AppSearch.filterAppsByFrecency(
                        PieLauncherApp.apps, getContext());
                return lastUsed != null
                        ? new ArrayList<>(lastUsed.subList(0, Math.min(8, lastUsed.size())))
                        : null;
            case Preferences.ALTERNATE_MENU_NONE:
            default:
                return null;
        }
    }

    private void setMenu(List<Apps.AppIcon> icons) {
        pieMenu.icons = icons;
        applyPiePreferences();
    }

    private boolean isPrimaryMenu() {
        return pieMenu.icons == menuPrimary;
    }

    private void setCenter(int x, int y) {
        int radius = pieConfig.getRadius();
        pieMenu.set(
                clamp(x, radius, viewWidth - radius),
                clamp(y, radius, viewHeight - radius),
                radius,
                pieConfig.getTwist(),
                pieConfig.getIconScale());
        lastSelectedIcon = -1;
        pieRenderer.setLaunchingIcon(null);
    }

    private static float distSq(int ax, int ay, int bx, int by) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
    }

    private void editIconAt(Point point) {
        int size = pieMenu.icons.size();
        for (int i = 0; i < size; ++i) {
            Apps.AppIcon icon = (Apps.AppIcon) pieMenu.icons.get(i);
            double rad = icon.size * .5;
            float sizeSq = Math.round(rad * rad);
            if (distSq(point.x, point.y, icon.x, icon.y) < sizeSq) {
                editIcon(icon);
                performHapticFeedbackIfAllowed(HAPTIC_FEEDBACK_DOWN);
                return;
            }
        }
        editActionsFeedback();
    }

    private void editActionsFeedback() {
        EditorAction action = editorRenderer.getActionAt(touch, pieEditor.hasGrabbedIcon());
        switch (action) {
            case BACK_OR_REMOVE_ICON:
                setHighlightedAction(editorRenderer.iconStartRect);
                break;
            case EDIT_SETTINGS_OR_UNDO:
                setHighlightedAction(editorRenderer.iconCenterRect);
                break;
            case END_EDIT_MODE:
                setHighlightedAction(editorRenderer.iconEndRect);
                break;
            case CHANGE_TWIST:
                setHighlightedAction(editorRenderer.iconChangeTwistRect);
                break;
            case CHANGE_ICON_SCALE:
                setHighlightedAction(editorRenderer.iconChangeIconScaleRect);
                break;
            case CHANGE_RADIUS:
                setHighlightedAction(editorRenderer.iconChangeRadiusRect);
                break;
            case NONE:
            default:
                resetHighlightedAction();
                break;
        }
    }


    private boolean showAppNames() {
        switch (prefs.showAppNames()) {
            case Preferences.SHOW_APP_NAMES_ALWAYS:
                return true;
            case Preferences.SHOW_APP_NAMES_SEARCH:
                return appListIsFiltered;
            default:
                return false;
        }
    }


    private boolean hasSecondaryMenu() {
        return prefs.splitPieMenu() == Preferences.ALTERNATE_MENU_SECONDARY ||
                prefs.circleSwapsMenus() ==
                        Preferences.ALTERNATE_MENU_SECONDARY;
    }


    private Canvas getRecordingCanvas(RenderNode node, Canvas fallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                !fallback.isHardwareAccelerated()) {
            return fallback;
        }
        return node.beginRecording();
    }

    private void drawRenderNode(RenderNode node, Canvas canvas) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                node != null &&
                canvas.isHardwareAccelerated()) {
            node.endRecording();
            canvas.drawRenderNode(node);
        }
    }


    private void resetHighlightedAction() {
        setHighlightedAction(null);
    }

    private void setHighlightedAction(android.graphics.Rect rect) {
        highlightedAction = rect;
    }

    private void storeMenu() {
        Context context = getContext();
        if (context != null) {
            PieLauncherApp.apps.store(context);
        }
        pieConfig.savePreferences(prefs, isPrimaryMenu());
    }

    private void applyPiePreferences() {
        loadPiePreferences();
        pieConfig.applyTo(pieMenu);
    }

    private void loadPiePreferences() {
        pieConfig.loadPreferences(prefs, isPrimaryMenu());
    }

    private void fadeOutMode() {
        switch (stateManager.getMode()) {
            case ViewStateManager.MODE_PIE:
                stateManager.getFadePie().fadeOut();
                break;
            case ViewStateManager.MODE_LIST:
                stateManager.getFadeList().fadeOut();
                break;
            case ViewStateManager.MODE_EDIT:
                stateManager.getFadeEdit().fadeOut();
                break;
        }
    }

    private void performHapticFeedbackIfAllowed(int feedback) {
        switch (prefs.hapticFeedback()) {
            case Preferences.HAPTIC_FEEDBACK_DISABLE_ALL:
                return;
            case Preferences.HAPTIC_FEEDBACK_DISABLE_LAUNCH:
                if (feedback == HAPTIC_FEEDBACK_CONFIRM) {
                    return;
                }
                // Fall through.
            default:
                performHapticFeedback(feedback);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }


    private void darkenBackground(Canvas canvas, float f) {
        int max = (translucentBackgroundColor >> 24) & 0xff;
        if (stateManager.getMode() == ViewStateManager.MODE_PIE && prefs.darkenBackground() ==
                Preferences.DARKEN_BACKGROUND_LIGHT) {
            max /= 2;
        }
        int alpha = Math.round(f * max);
        if (alpha == 0) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        } else {
            canvas.drawColor(
                    (alpha << 24) | (translucentBackgroundColor & 0xffffff),
                    PorterDuff.Mode.SRC);
        }
    }

    private int applyBlurOut(RenderNode node, int current, float f) {
        return applyBlur(node, current, 1f - clamp(f, 0f, 1f));
    }

    private int applyBlur(RenderNode node, int current, float f) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                node == null ||
                !isHardwareAccelerated()) {
            return 0;
        }
        if (!prefs.blurMenu() || f <= 0f) {
            clearBlur(node);
            return 0;
        }
        int radius = Math.max(1, Math.round(f * maxBlurRadius));
        if (current != radius) {
            node.setRenderEffect(RenderEffect.createBlurEffect(
                    radius, radius, Shader.TileMode.CLAMP));
        }
        return radius;
    }

    private void clearBlur(RenderNode node) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                node != null) {
            node.setRenderEffect(null);
        }
    }

    private class FlingRunnable implements Runnable {
        private final OverScroller scroller;
        private int maxY;
        private int pps;

        @Override
        public void run() {
            if (stateManager.getMode() != ViewStateManager.MODE_LIST || !scroller.computeScrollOffset()) {
                return;
            }
            if (maxY != maxScrollY) {
                maxY = maxScrollY;
                // OverScroller.springBack() stops the animation so we
                // cannot use it and have to start a new fling instead.
                scroller.forceFinished(true);
                initFling(getScrollY());
            }
            scrollList(scroller.getCurrY(), true);
            update();
        }

        private FlingRunnable() {
            scroller = new OverScroller(getContext());
        }

        private void start(int pixelsPerSecond) {
            pps = -pixelsPerSecond;
            maxY = maxScrollY;
            initFling(getScrollY());
            update();
        }

        private void initFling(int y) {
            scroller.fling(
                    0, y,
                    0, pps,
                    0, 0,
                    0, maxY,
                    0, listPadding);
        }

        private void stop() {
            scroller.forceFinished(true);
            removeCallbacks(this);
        }

        private void update() {
            invalidate();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                post(this);
            } else {
                postOnAnimation(this);
            }
        }
    }

    private static float easeSlowerIn(float x) {
        return x * x;
    }
}
