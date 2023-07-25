package de.markusfisch.android.pielauncher.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.activity.SettingsActivity;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.graphics.Converter;
import de.markusfisch.android.pielauncher.graphics.Ripple;

public class AppPieView extends View {
	public interface ListListener {
		void onOpenList();

		void onHideList();

		void onScrollList(int y, boolean isScrolling);
	}

	private static final int HAPTIC_FEEDBACK_CONFIRM =
			Build.VERSION.SDK_INT < Build.VERSION_CODES.M
					? HapticFeedbackConstants.KEYBOARD_TAP
					: Build.VERSION.SDK_INT < Build.VERSION_CODES.R
					? HapticFeedbackConstants.CONTEXT_CLICK
					: HapticFeedbackConstants.CONFIRM;
	private static final int HAPTIC_FEEDBACK_DOWN =
			Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
					? HapticFeedbackConstants.KEYBOARD_TAP
					: HapticFeedbackConstants.CLOCK_TICK;
	private static final int MODE_PIE = 0;
	private static final int MODE_LIST = 1;
	private static final int MODE_EDIT = 2;
	private static final float FADE_DURATION = 150f;

	private final ArrayList<AppMenu.Icon> backup = new ArrayList<>();
	private final ArrayList<AppMenu.Icon> ungrabbedIcons = new ArrayList<>();
	private final Paint paintActive = new Paint(Paint.FILTER_BITMAP_FLAG);
	private final TextPaint paintText = new TextPaint(Paint.ANTI_ALIAS_FLAG);
	private final Point touch = new Point();
	private final Ripple ripple = new Ripple();
	private final Rect drawRect = new Rect();
	private final Rect iconStartRect = new Rect();
	private final Rect iconCenterRect = new Rect();
	private final Rect iconEndRect = new Rect();
	private final Bitmap iconAdd;
	private final Bitmap iconRemove;
	private final Bitmap iconDetails;
	private final Bitmap iconDone;
	private final Bitmap iconPreferences;
	private final Bitmap iconLaunchFirst;
	private final String numberOfIconsTip;
	private final String dragToOrderTip;
	private final String pinchZoomTip;
	private final ScaleGestureDetector scaleDetector;
	private final long tapTimeout;
	private final long minLongPressDuration;
	private final int editorPadding;
	private final int listPadding;
	private final int searchInputHeight;
	private final int iconSize;
	private final int iconTextPadding;
	private final int iconLaunchFirstHalf;
	private final int translucentBackgroundColor;
	private final float dp;
	private final float textHeight;
	private final float textOffset;
	private final float touchSlopSq;

	private Runnable rippleRunnable;
	private int viewWidth;
	private int viewHeight;
	private int minRadius;
	private int maxRadius;
	private int radius;
	private int maxScrollY;
	private int lastScrollY;
	private int lastInsertAt;
	private int mode = MODE_PIE;
	private ListListener listListener;
	private List<AppMenu.AppIcon> appList;
	private AppMenu.Icon grabbedIcon;
	private long fadeInFrom;
	private long fadeOutFrom;
	private boolean showLaunchFirst = false;

	public AppPieView(Context context, AttributeSet attr) {
		super(context, attr);

		scaleDetector = new ScaleGestureDetector(context,
				new ScaleListener());

		Resources res = context.getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
		dp = dm.density;
		float sp = dm.scaledDensity;
		editorPadding = Math.round(80f * dp);
		listPadding = Math.round(16f * dp);
		searchInputHeight = Math.round(112f * dp);
		iconSize = Math.round(48f * dp);
		iconTextPadding = Math.round(12f * dp);

		numberOfIconsTip = context.getString(R.string.tip_number_of_icons);
		dragToOrderTip = context.getString(R.string.tip_drag_to_order);
		pinchZoomTip = context.getString(R.string.tip_pinch_zoom);

		paintText.setColor(res.getColor(R.color.text_color));
		paintText.setTextAlign(Paint.Align.CENTER);
		paintText.setTextSize(14f * sp);
		textHeight = paintText.descent() - paintText.ascent();
		textOffset = (textHeight / 2) - paintText.descent();
		translucentBackgroundColor = res.getColor(R.color.bg_ui);

		iconAdd = getBitmapFromDrawable(res, R.drawable.ic_add);
		iconRemove = getBitmapFromDrawable(res, R.drawable.ic_remove);
		iconDetails = getBitmapFromDrawable(res, R.drawable.ic_details);
		iconDone = getBitmapFromDrawable(res, R.drawable.ic_done);
		iconPreferences = getBitmapFromDrawable(res, R.drawable.ic_preferences);
		iconLaunchFirst = getBitmapFromDrawable(res,
				R.drawable.ic_launch_first);
		iconLaunchFirstHalf = iconLaunchFirst.getWidth() >> 1;

		ViewConfiguration configuration = ViewConfiguration.get(context);
		float touchSlop = configuration.getScaledTouchSlop();
		touchSlopSq = touchSlop * touchSlop;
		tapTimeout = ViewConfiguration.getTapTimeout();
		minLongPressDuration = ViewConfiguration.getLongPressTimeout();
		ripple.setDuration(minLongPressDuration);

		if (PieLauncherApp.appMenu.isEmpty()) {
			PieLauncherApp.appMenu.indexAppsAsync(context);
		}
		initTouchListener();
	}

	public void setListListener(ListListener listener) {
		listListener = listener;
	}

	public void showList() {
		mode = MODE_LIST;
		cancelRipple();
		scrollList(lastScrollY, false);
		setVerticalScrollBarEnabled(true);
		hidePieMenu();
		resetFadeOutPieMenu();
		invalidate();
	}

	public void hideList() {
		mode = MODE_PIE;
		resetScrollWithoutAnimation();
		setVerticalScrollBarEnabled(false);
		hidePieMenu();
		resetFadeOutPieMenu();
		invalidate();
	}

	public boolean isEmpty() {
		return appList == null || appList.size() < 1;
	}

	public boolean isAppListScrolled() {
		return mode == MODE_LIST && getScrollY() != 0;
	}

	public boolean inEditMode() {
		return mode == MODE_EDIT;
	}

	public boolean inListMode() {
		return mode == MODE_LIST;
	}

	public void filterAppList(String query) {
		appList = PieLauncherApp.appMenu.filterAppsBy(query);
		showLaunchFirst = !TextUtils.isEmpty(query);
		scrollList(0, false);
		lastScrollY = 0;
		hidePieMenu();
		resetFadeOutPieMenu();
		invalidate();
	}

	public void launchFirstApp() {
		if (isEmpty()) {
			return;
		}
		AppMenu.AppIcon appIcon = appList.get(0);
		PieLauncherApp.appMenu.launchApp(getContext(), appIcon);
	}

	public void endEditMode() {
		Context context = getContext();
		if (context != null) {
			PieLauncherApp.appMenu.store(context);
		}
		PieLauncherApp.prefs.setRadius(radius);
		backup.clear();
		ungrabbedIcons.clear();
		grabbedIcon = null;
		mode = MODE_PIE;
		invalidate();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (changed) {
			initMenu(right - left, bottom - top);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		switch (mode) {
			default:
			case MODE_PIE:
				drawPieMenu(canvas);
				break;
			case MODE_LIST:
				drawList(canvas);
				break;
			case MODE_EDIT:
				drawEditor(canvas);
				break;
		}
		if (ripple.draw(canvas)) {
			invalidate();
		}
		if (PieLauncherApp.appMenu.isIndexing()) {
			drawTip(canvas, "Loading…");
		}
	}

	@Override
	protected int computeVerticalScrollRange() {
		return maxScrollY + getHeight();
	}

	@Override
	protected int computeVerticalScrollExtent() {
		return getHeight();
	}

	@Override
	protected int computeVerticalScrollOffset() {
		return getScrollY();
	}

	private static Bitmap getBitmapFromDrawable(Resources res, int resId) {
		return Converter.getBitmapFromDrawable(getDrawable(res, resId));
	}

	private static Drawable getDrawable(Resources res, int resId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return res.getDrawable(resId, null);
		} else {
			//noinspection deprecation
			return res.getDrawable(resId);
		}
	}

	private void initTouchListener() {
		setOnTouchListener(new OnTouchListener() {
			private final FlingRunnable flingRunnable = new FlingRunnable();
			private final SparseArray<TouchReference> touchReferences =
					new SparseArray<>();

			private VelocityTracker velocityTracker;
			private int primaryId;
			private int scrollOffset;
			private Runnable longPressRunnable;
			private Runnable performActionRunnable;

			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (mode == MODE_EDIT && grabbedIcon == null) {
					scaleDetector.onTouchEvent(event);
				}
				touch.set(Math.round(event.getX()), Math.round(event.getY()));
				switch (event.getActionMasked()) {
					default:
						break;
					case MotionEvent.ACTION_POINTER_DOWN:
						if (mode == MODE_PIE) {
							fadeOutPieMenu();
							hidePieMenu();
							invalidate();
						} else if (mode == MODE_LIST) {
							cancelLongPress();
							addTouch(event);
							initScroll(event);
						}
						break;
					case MotionEvent.ACTION_POINTER_UP:
						if (mode == MODE_LIST) {
							updateReferences(event);
							initScroll(event);
						}
						break;
					case MotionEvent.ACTION_DOWN:
						if (cancelPerformAction()) {
							// Ignore additional ACTION_DOWN's when there
							// was a pending action.
							break;
						}
						addTouch(event);
						switch (mode) {
							case MODE_PIE:
								setCenter(touch);
								performHapticFeedback(HAPTIC_FEEDBACK_DOWN);
								break;
							case MODE_LIST:
								initScroll(event);
								initLongPress();
								break;
							case MODE_EDIT:
								editIconAt(touch);
								break;
						}
						fadeInFrom = event.getEventTime();
						resetFadeOutPieMenu();
						invalidate();
						break;
					case MotionEvent.ACTION_MOVE:
						if (mode == MODE_LIST) {
							if (!isTap(event, Long.MAX_VALUE)) {
								cancelLongPress();
							}
							scroll(event);
						}
						invalidate();
						break;
					case MotionEvent.ACTION_UP:
						if (mode == MODE_LIST) {
							cancelLongPress();
							keepScrolling(event);
						}
						postPerformAction(v, event);
						break;
					case MotionEvent.ACTION_CANCEL:
						if (mode == MODE_PIE) {
							fadeOutPieMenu();
						} else if (mode == MODE_LIST) {
							cancelLongPress();
							recycleVelocityTracker();
						}
						grabbedIcon = null;
						hidePieMenu();
						invalidate();
						break;
				}
				return true;
			}

			private void addTouch(MotionEvent event) {
				int index = event.getActionIndex();
				primaryId = event.getPointerId(index);
				addTouchReference(event, primaryId, index);
			}

			private void updateReferences(MotionEvent event) {
				// Unfortunately, and contrary to the docs, some (old?) touch
				// screen drivers don't return the index of the touch that has
				// gone up in ACTION_POINTER_UP for getActionIndex() but an
				// index of a pointer that is still down.
				// So the only option is to update all references.
				for (int i = 0, l = event.getPointerCount(); i < l; ++i) {
					int id = event.getPointerId(i);
					addTouchReference(event, id, i);
				}
			}

			private void addTouchReference(MotionEvent event, int id,
					int index) {
				touchReferences.put(id, new TouchReference(
						event.getX(index),
						event.getY(index),
						event.getEventTime()));
			}

			private TouchReference getTouchReference(MotionEvent event,
					int index) {
				return index > -1 && index < event.getPointerCount()
						? touchReferences.get(event.getPointerId(index))
						: null;
			}

			private void initScroll(MotionEvent event) {
				flingRunnable.stop();
				recycleVelocityTracker();
				velocityTracker = VelocityTracker.obtain();
				velocityTracker.addMovement(event);
				scrollOffset = getScrollY();
			}

			private int getPrimaryIndex(MotionEvent event) {
				int count = event.getPointerCount();
				int id = -1;
				for (int i = 0; i < count; ++i) {
					id = event.getPointerId(i);
					if (id == primaryId) {
						return i;
					}
				}
				// If the ID wasn't found, the pointer must have gone up.
				primaryId = id;
				return count - 1;
			}

			private void scroll(MotionEvent event) {
				int index = getPrimaryIndex(event);
				if (index < 0) {
					return;
				}
				TouchReference tr = getTouchReference(event, index);
				if (tr == null) {
					return;
				}
				if (velocityTracker != null) {
					velocityTracker.addMovement(event);
				}
				int y = Math.round(event.getY(index));
				int scrollY = scrollOffset + (tr.pos.y - y);
				lastScrollY = clamp(scrollY, 0, maxScrollY);
				scrollList(lastScrollY, true);
				if (lastScrollY == 0 || lastScrollY == maxScrollY) {
					// Move reference to current coordinate if this
					// event wouldn't move the list to make movements
					// in the opposite direction immediate.
					tr.pos.y = y;
					scrollOffset = getScrollY();
				}
			}

			private void keepScrolling(MotionEvent event) {
				if (event.getPointerCount() < 2 && velocityTracker != null) {
					// 1000 means getYVelocity() will return pixels
					// per second.
					velocityTracker.computeCurrentVelocity(1000);
					flingRunnable.start(Math.round(
							velocityTracker.getYVelocity()));
					recycleVelocityTracker();
				}
			}

			private void recycleVelocityTracker() {
				if (velocityTracker != null) {
					velocityTracker.recycle();
					velocityTracker = null;
				}
			}

			private void initLongPress() {
				cancelLongPress();
				final AppMenu.Icon appIcon = getListIconAt(touch.x, touch.y);
				if (appIcon == null) {
					return;
				}
				initRipple();
				longPressRunnable = () -> {
					performHapticFeedback(
							HapticFeedbackConstants.LONG_PRESS);
					addIconInteractively(appIcon);
					longPressRunnable = null;
				};
				postDelayed(longPressRunnable, minLongPressDuration);
			}

			private void cancelLongPress() {
				cancelRipple();
				if (longPressRunnable != null) {
					removeCallbacks(longPressRunnable);
					longPressRunnable = null;
				}
			}

			private void initRipple() {
				cancelRipple();
				final Point at = new Point(touch.x, touch.y + getScrollY());
				rippleRunnable = () -> {
					ripple.set(at.x, at.y);
					invalidate();
				};
				postDelayed(rippleRunnable, tapTimeout);
			}

			private boolean isTap(MotionEvent event, long timeOut) {
				TouchReference tr = getTouchReference(event, 0);
				if (tr == null) {
					return false;
				}
				return SystemClock.uptimeMillis() - tr.time <= timeOut &&
						distSq(tr.pos, touch) <= touchSlopSq;
			}

			private void postPerformAction(final View v, MotionEvent event) {
				cancelPerformAction();
				final boolean wasTap = isTap(event, tapTimeout);
				final Point at = new Point(touch.x, touch.y);
				performActionRunnable = () -> {
					v.performClick();
					if (performAction(v.getContext(), at, wasTap)) {
						v.performHapticFeedback(HAPTIC_FEEDBACK_CONFIRM);
					}
					performActionRunnable = null;
					hidePieMenu();
					invalidate();
				};
				// Wait a short time before performing the action
				// because some touch screen drivers send
				// ACTION_UP/ACTION_DOWN pairs for very short
				// touch interruptions what makes the menu jump
				// and execute multiple actions unintentionally.
				postDelayed(performActionRunnable, 16);
			}

			private boolean cancelPerformAction() {
				if (performActionRunnable != null) {
					removeCallbacks(performActionRunnable);
					performActionRunnable = null;
					return true;
				}
				return false;
			}
		});
	}

	private void initMenu(int width, int height) {
		int min = Math.min(width, height);
		float maxIconSize = 64f * dp;
		if (Math.floor(min * .28f) > maxIconSize) {
			min = Math.round(maxIconSize / .28f);
		}
		maxRadius = Math.round(min * .5f);
		minRadius = Math.round(maxRadius * .5f);
		radius = clampRadius(PieLauncherApp.prefs.getRadius(maxRadius));
		viewWidth = width;
		viewHeight = height;
		layoutEditorControls(height > width);
	}

	private void layoutEditorControls(boolean portrait) {
		Bitmap[] icons = new Bitmap[]{iconAdd, iconPreferences, iconDone};
		Rect[] rects = new Rect[]{iconStartRect, iconCenterRect, iconEndRect};
		int length = icons.length;
		int totalWidth = 0;
		int totalHeight = 0;
		int largestWidth = 0;
		int largestHeight = 0;
		// Initialize rects and calculate totals.
		for (int i = 0; i < length; ++i) {
			Bitmap icon = icons[i];
			int w = icon.getWidth();
			int h = icon.getHeight();
			rects[i].set(0, 0, w, h);
			largestWidth = Math.max(largestWidth, w);
			largestHeight = Math.max(largestHeight, h);
			totalWidth += w;
			totalHeight += h;
		}
		if (portrait) {
			int step = Math.round(
					(float) (viewWidth - totalWidth) / (length + 1));
			int x = step;
			int y = viewHeight - largestHeight - editorPadding;
			for (Rect rect : rects) {
				rect.offset(x, y);
				x += step + rect.width();
			}
		} else {
			int step = Math.round(
					(float) (viewHeight - totalHeight) / (length + 1));
			int x = viewWidth - largestWidth - editorPadding;
			int y = step;
			for (Rect rect : rects) {
				rect.offset(x, y);
				y += step + rect.height();
			}
		}
	}

	private void addIconInteractively(AppMenu.Icon appIcon) {
		if (appIcon == null) {
			return;
		}
		if (listListener != null) {
			listListener.onHideList();
		}
		editIcon(appIcon);
		resetScrollWithoutAnimation();
		invalidate();
	}

	private void editIcon(AppMenu.Icon icon) {
		backup.clear();
		backup.addAll(PieLauncherApp.appMenu.icons);
		PieLauncherApp.appMenu.icons.remove(icon);
		ungrabbedIcons.clear();
		ungrabbedIcons.addAll(PieLauncherApp.appMenu.icons);
		CanvasPieMenu.paint.setAlpha(255);
		grabbedIcon = icon;
		lastInsertAt = -1;
		mode = MODE_EDIT;
	}

	private void resetScrollWithoutAnimation() {
		cancelRipple();
		scrollTo(0, 0);
	}

	private void scrollList(int y, boolean isScrolling) {
		scrollTo(0, y);
		if (listListener != null) {
			listListener.onScrollList(y, isScrolling);
		}
	}

	private void cancelRipple() {
		ripple.cancel();
		if (rippleRunnable != null) {
			removeCallbacks(rippleRunnable);
			rippleRunnable = null;
		}
	}

	private boolean performAction(Context context, Point at, boolean wasTap) {
		if (mode == MODE_PIE && isPieVisible()) {
			boolean result = false;
			if (wasTap) {
				if (listListener != null) {
					listListener.onOpenList();
				}
			} else {
				if (PieLauncherApp.appMenu.launchSelectedApp(context)) {
					ripple.set(at);
					result = true;
				}
			}
			fadeOutPieMenu();
			return result;
		} else if (mode == MODE_LIST && wasTap) {
			return performListAction(context, at);
		} else if (mode == MODE_EDIT) {
			boolean result = performEditAction(context);
			grabbedIcon = null;
			PieLauncherApp.appMenu.updateSmoothing();
			return result;
		}
		return false;
	}

	private boolean performListAction(Context context, Point at) {
		AppMenu.AppIcon appIcon = getListIconAt(at.x, at.y);
		if (appIcon != null) {
			PieLauncherApp.appMenu.launchApp(context, appIcon);
			if (listListener != null) {
				listListener.onHideList();
			}
			ripple.set(at);
			return true;
		}
		return false;
	}

	private AppMenu.AppIcon getListIconAt(int x, int y) {
		y += getScrollY();
		for (int i = 0, l = appList.size(); i < l; ++i) {
			AppMenu.AppIcon appIcon = appList.get(i);
			if (appIcon.hitRect.contains(x, y)) {
				return appIcon;
			}
		}
		return null;
	}

	private boolean performEditAction(Context context) {
		if (iconStartRect.contains(touch.x, touch.y)) {
			if (grabbedIcon == null) {
				((Activity) context).onBackPressed();
			} else {
				ripple.set(touch);
				PieLauncherApp.appMenu.icons.remove(grabbedIcon);
				// Undo any rotation if the menu has not otherwise changed.
				if (sameOrder(backup, PieLauncherApp.appMenu.icons)) {
					rollback();
				}
			}
			return true;
		} else if (iconCenterRect.contains(touch.x, touch.y) &&
				grabbedIcon == null) {
			SettingsActivity.start(context);
			return true;
		} else if (iconEndRect.contains(touch.x, touch.y)) {
			if (grabbedIcon == null) {
				endEditMode();
			} else {
				ripple.set(touch);
				rollback();
				PieLauncherApp.appMenu.launchAppInfo(context,
						(AppMenu.AppIcon) grabbedIcon);
			}
			return true;
		}
		return false;
	}

	private void rollback() {
		PieLauncherApp.appMenu.icons.clear();
		PieLauncherApp.appMenu.icons.addAll(backup);
	}

	private static boolean sameOrder(List<AppMenu.Icon> a,
			List<AppMenu.Icon> b) {
		int size = a.size();
		if (size != b.size()) {
			return false;
		}
		if (size == 0) {
			return true;
		}
		AppMenu.Icon icon = a.get(0);
		int i;
		for (i = 0; i < size; ++i) {
			if (b.get(i) == icon) {
				break;
			}
		}
		if (i >= size) {
			return false;
		}
		for (int j = 1; j < size; ++j) {
			if (a.get(j) != b.get(++i % size)) {
				return false;
			}
		}
		return true;
	}

	private void setCenter(Point point) {
		setCenter(point.x, point.y);
	}

	private void setCenter(int x, int y) {
		PieLauncherApp.appMenu.set(
				clamp(x, radius, viewWidth - radius),
				clamp(y, radius, viewHeight - radius),
				radius);
	}

	private void editIconAt(Point point) {
		int size = PieLauncherApp.appMenu.icons.size();
		for (int i = 0; i < size; ++i) {
			AppMenu.Icon icon = PieLauncherApp.appMenu.icons.get(i);
			float sizeSq = Math.round(icon.size * icon.size);
			if (distSq(point.x, point.y, icon.x, icon.y) < sizeSq) {
				editIcon(icon);
				performHapticFeedback(HAPTIC_FEEDBACK_DOWN);
				break;
			}
		}
	}

	private void drawList(Canvas canvas) {
		// Manually draw an icon grid because GridView doesn't perform too
		// well on low-end devices and doing it manually gives us more control.
		canvas.drawColor(translucentBackgroundColor, PorterDuff.Mode.SRC);
		int innerWidth = viewWidth - listPadding * 2;
		int columns = Math.min(5, innerWidth / iconSize);
		int iconAndTextHeight = iconSize + iconTextPadding +
				Math.round(textHeight);
		int cellWidth = innerWidth / columns;
		int cellHeight = iconAndTextHeight + iconTextPadding * 2;
		int maxTextWidth = cellWidth - iconTextPadding;
		int hpad = (cellWidth - iconSize) / 2;
		int vpad = (cellHeight - iconAndTextHeight) / 2;
		int labelX = cellWidth >> 1;
		int labelY = cellHeight - vpad - Math.round(textOffset);
		int scrollY = getScrollY();
		int bottomPadding = getPaddingBottom();
		int viewHeightMinusPadding = viewHeight - bottomPadding;
		int viewTop = scrollY - cellHeight;
		int viewBottom = scrollY + viewHeight;
		int x = listPadding;
		int y = listPadding + searchInputHeight;
		int wrapX = listPadding + cellWidth * columns;
		int size = appList.size();
		if (showLaunchFirst && size > 0) {
			canvas.drawBitmap(iconLaunchFirst,
					x + labelX - iconLaunchFirstHalf,
					y - listPadding, paintActive);
		}
		for (int i = 0; i < size; ++i) {
			if (y > viewTop && y < viewBottom) {
				AppMenu.AppIcon appIcon = appList.get(i);
				appIcon.hitRect.set(x, y, x + cellWidth, y + cellHeight);
				int ix = x + hpad;
				int iy = y + vpad;
				drawRect.set(ix, iy, ix + iconSize, iy + iconSize);
				canvas.drawBitmap(appIcon.bitmap, null, drawRect, paintActive);
				CharSequence label = TextUtils.ellipsize(appIcon.label,
						paintText, maxTextWidth, TextUtils.TruncateAt.END);
				canvas.drawText(label, 0, label.length(),
						x + labelX, y + labelY, paintText);
			}
			x += cellWidth;
			if (x >= wrapX) {
				x = listPadding;
				y += cellHeight;
			}
		}
		int maxHeight = y + listPadding + (x > listPadding ? cellHeight : 0);
		maxScrollY = Math.max(maxHeight - viewHeightMinusPadding, 0);
	}

	private void drawEditor(Canvas canvas) {
		canvas.drawColor(translucentBackgroundColor, PorterDuff.Mode.SRC);
		boolean hasIcon = grabbedIcon != null;
		// Only draw tips in portrait orientation.
		// There's probably not enough space in landscape.
		if (canvas.getWidth() < canvas.getHeight()) {
			drawTip(canvas, getTip(hasIcon));
		}
		if (hasIcon) {
			drawIcon(canvas, iconRemove, iconStartRect);
			drawIcon(canvas, iconDetails, iconEndRect);
		} else {
			drawIcon(canvas, iconAdd, iconStartRect);
			drawIcon(canvas, iconPreferences, iconCenterRect);
			drawIcon(canvas, iconDone, iconEndRect);
		}
		int centerX = viewWidth >> 1;
		int centerY = viewHeight >> 1;
		setCenter(centerX, centerY);
		if (hasIcon) {
			int lastIndex = ungrabbedIcons.size();
			double step = AppMenu.TAU / (lastIndex + 1);
			double angle = AppMenu.getPositiveAngle(Math.atan2(
					touch.y - centerY,
					touch.x - centerX) + step * .5);
			int insertAt = Math.min(lastIndex, (int) Math.floor(angle / step));
			if (insertAt != lastInsertAt) {
				// Avoid (visible) rotation of the menu when the first item
				// changes. From the user's point of view, it is not clear
				// why the menu rotates all of a sudden. The technical
				// reason was to keep the menu unchanged when the grabbed
				// icon is a newly added icon that is removed again. To
				// prevent this, the menu is now rolled back if it has the
				// sameOrder() as before (see below).
				if (lastInsertAt == 0 && insertAt == lastIndex) {
					Collections.rotate(ungrabbedIcons, 1);
				} else if (lastInsertAt == lastIndex && insertAt == 0) {
					Collections.rotate(ungrabbedIcons, -1);
				}
				PieLauncherApp.appMenu.icons.clear();
				PieLauncherApp.appMenu.icons.addAll(ungrabbedIcons);
				PieLauncherApp.appMenu.icons.add(insertAt, grabbedIcon);
				PieLauncherApp.appMenu.updateSmoothing();
				if (lastInsertAt < 0) {
					PieLauncherApp.appMenu.calculate(centerX, centerY);
					grabbedIcon.x = touch.x;
					grabbedIcon.y = touch.y;
					PieLauncherApp.appMenu.initSmoothing();
				}
				lastInsertAt = insertAt;
			}
			PieLauncherApp.appMenu.calculate(touch.x, touch.y);
			grabbedIcon.x = touch.x;
			grabbedIcon.y = touch.y;
		} else {
			PieLauncherApp.appMenu.calculate(centerX, centerY);
		}
		if (PieLauncherApp.appMenu.drawSmoothed(canvas)) {
			invalidate();
		}
	}

	private void drawPieMenu(Canvas canvas) {
		float f = 0;
		long now = SystemClock.uptimeMillis();
		if (fadeInFrom > 0) {
			f = Math.min(1f, (now - fadeInFrom) / FADE_DURATION);
		} else {
			long delta = now - fadeOutFrom;
			if (delta < FADE_DURATION) {
				// Ensure f < 1f so invalidate() is invoked.
				f = Math.min(.99999f, 1f - delta / FADE_DURATION);
			}
		}
		if (f > 0) {
			canvas.drawColor(0, PorterDuff.Mode.CLEAR);
			CanvasPieMenu.paint.setAlpha(Math.round(f * 255f));
			PieLauncherApp.appMenu.calculate(touch.x, touch.y);
			PieLauncherApp.appMenu.draw(canvas);
			if (f < 1f) {
				invalidate();
			}
		}
	}

	private void resetFadeOutPieMenu() {
		fadeOutFrom = 0;
	}

	private void fadeOutPieMenu() {
		fadeOutFrom = isPieVisible() ? SystemClock.uptimeMillis() : 0;
	}

	private void hidePieMenu() {
		fadeInFrom = 0;
	}

	private boolean isPieVisible() {
		return fadeInFrom > 0;
	}

	private String getTip(boolean hasIcon) {
		if (hasIcon) {
			return dragToOrderTip;
		}
		int iconsInMenu = PieLauncherApp.appMenu.icons.size();
		if (iconsInMenu != 4 && iconsInMenu != 6 && iconsInMenu != 8) {
			return numberOfIconsTip;
		} else {
			return pinchZoomTip;
		}
	}

	private void drawTip(Canvas canvas, String tip) {
		if (tip != null) {
			canvas.drawText(tip, viewWidth >> 1, editorPadding + textOffset,
					paintText);
		}
	}

	private void drawIcon(Canvas canvas, Bitmap icon, Rect rect) {
		canvas.drawBitmap(icon, null, rect, paintActive);
	}

	private static float distSq(Point a, Point b) {
		return distSq(a.x, a.y, b.x, b.y);
	}

	private static float distSq(int ax, int ay, int bx, int by) {
		float dx = ax - bx;
		float dy = ay - by;
		return dx * dx + dy * dy;
	}

	private int clampRadius(int r) {
		return clamp(r, minRadius, maxRadius);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private void scaleRadius(float factor) {
		radius *= factor;
		radius = clampRadius(radius);
		PieLauncherApp.appMenu.setRadius(radius);
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			if (!detector.isInProgress()) {
				return false;
			}
			scaleRadius(detector.getScaleFactor());
			invalidate();
			return true;
		}
	}

	private class FlingRunnable implements Runnable {
		private final OverScroller scroller;
		private int maxY;
		private int pps;

		@Override
		public void run() {
			if (mode != MODE_LIST || !scroller.computeScrollOffset()) {
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

	private static final class TouchReference {
		private final Point pos = new Point();
		private final long time;

		private TouchReference(float x, float y, long time) {
			this.pos.set(Math.round(x), Math.round(y));
			this.time = time;
		}
	}
}
