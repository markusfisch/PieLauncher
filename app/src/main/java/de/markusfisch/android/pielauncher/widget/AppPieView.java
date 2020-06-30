package de.markusfisch.android.pielauncher.widget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.graphics.Converter;

public class AppPieView extends View {
	public interface ListListener {
		void onOpenList();

		void onHideList();

		void onScrollList(int y);
	}

	private static final int MODE_PIE = 0;
	private static final int MODE_LIST = 1;
	private static final int MODE_EDIT = 2;

	private final ArrayList<AppMenu.Icon> backup = new ArrayList<>();
	private final ArrayList<AppMenu.Icon> ungrabbedIcons = new ArrayList<>();
	private final Paint paintActive = new Paint(Paint.FILTER_BITMAP_FLAG);
	private final Paint paintDeactive = new Paint(Paint.FILTER_BITMAP_FLAG);
	private final TextPaint paintText = new TextPaint(Paint.ANTI_ALIAS_FLAG);
	private final Point touch = new Point();
	private final Rect drawRect = new Rect();
	private final Rect iconAddRect = new Rect();
	private final Bitmap iconAdd;
	private final Rect iconRemoveRect = new Rect();
	private final Bitmap iconRemove;
	private final Rect iconInfoRect = new Rect();
	private final Bitmap iconInfo;
	private final Rect iconDoneRect = new Rect();
	private final Bitmap iconDone;
	private final int translucentBackgroundColor;
	private final float dp;
	private final String numberOfIconsTip;
	private final String dragToOrderTip;
	private final String pinchZoomTip;

	private ScaleGestureDetector scaleDetector;
	private int viewWidth;
	private int viewHeight;
	private int minRadius;
	private int maxRadius;
	private int radius;
	private long tapTimeout;
	private long longPressTimeout;
	private int editorPadding;
	private int listPadding;
	private int searchInputHeight;
	private int maxScrollY;
	private int lastScrollY;
	private int iconSize;
	private int iconTextPadding;
	private float textHeight;
	private float textOffset;
	private float touchSlopSq;
	private ListListener listListener;
	private AppMenu.Icon grabbedIcon;
	private List<AppMenu.AppIcon> appList;
	private int mode = MODE_PIE;

	public AppPieView(Context context, AttributeSet attr) {
		super(context, attr);

		scaleDetector = new ScaleGestureDetector(context,
				new ScaleListener());

		Resources res = context.getResources();
		dp = res.getDisplayMetrics().density;
		float sp = res.getDisplayMetrics().scaledDensity;
		editorPadding = Math.round(80f * dp);
		listPadding = Math.round(16f * dp);
		searchInputHeight = Math.round(112f * dp);
		iconSize = Math.round(48f * dp);
		iconTextPadding = Math.round(12f * dp);

		numberOfIconsTip = context.getString(R.string.tip_number_of_icons);
		dragToOrderTip = context.getString(R.string.tip_drag_to_order);
		pinchZoomTip = context.getString(R.string.tip_pinch_zoom);

		paintDeactive.setAlpha(40);
		paintText.setColor(res.getColor(R.color.text_color));
		paintText.setTextAlign(Paint.Align.CENTER);
		paintText.setTextSize(14f * sp);
		textHeight = paintText.descent() - paintText.ascent();
		textOffset = (textHeight / 2) - paintText.descent();
		translucentBackgroundColor = res.getColor(R.color.background_ui);

		iconAdd = getBitmapFromDrawable(res, R.drawable.ic_add);
		iconRemove = getBitmapFromDrawable(res, R.drawable.ic_remove);
		iconInfo = getBitmapFromDrawable(res, R.drawable.ic_info);
		iconDone = getBitmapFromDrawable(res, R.drawable.ic_done);

		ViewConfiguration configuration = ViewConfiguration.get(context);
		float touchSlop = configuration.getScaledTouchSlop();
		touchSlopSq = touchSlop * touchSlop;
		tapTimeout = ViewConfiguration.getTapTimeout();
		longPressTimeout = ViewConfiguration.getLongPressTimeout();

		PieLauncherApp.appMenu.indexAppsAsync(context);
		initTouchListener();
	}

	public void setListListener(ListListener listener) {
		listListener = listener;
	}

	public void showList() {
		mode = MODE_LIST;
		scrollList(lastScrollY);
		setVerticalScrollBarEnabled(true);
		invalidateTouch();
		invalidate();
	}

	public void hideList() {
		mode = MODE_PIE;
		resetScrollSilently();
		setVerticalScrollBarEnabled(false);
		invalidateTouch();
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
		scrollList(0);
		invalidateTouch();
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
						if (mode == MODE_LIST) {
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
							// Ignore ACTION_DOWN's when there was
							// an action pending.
							break;
						}
						addTouch(event);
						switch (mode) {
							case MODE_PIE:
								setCenter(touch);
								break;
							case MODE_LIST:
								initScroll(event);
								initLongPress();
								break;
							case MODE_EDIT:
								editIconAt(touch);
								break;
						}
						invalidate();
						break;
					case MotionEvent.ACTION_MOVE:
						if (mode == MODE_LIST) {
							if (!isTap(event, longPressTimeout)) {
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
						if (mode == MODE_LIST) {
							cancelLongPress();
							if (event.getPointerCount() < 1) {
								recycleVelocityTracker();
							}
						}
						grabbedIcon = null;
						invalidateTouch();
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
				// if the ID wasn't found, the pointer must have gone up
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
				scrollList(lastScrollY);
			}

			private void keepScrolling(MotionEvent event) {
				if (event.getPointerCount() < 2 && velocityTracker != null) {
					// 1000 means getYVelocity() will return pixels
					// per second
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
				final Point pos = touch;
				longPressRunnable = new Runnable() {
					@Override
					public void run() {
						addIconInteractively(pos);
						longPressRunnable = null;
					}
				};
				postDelayed(longPressRunnable, longPressTimeout);
			}

			private void cancelLongPress() {
				if (longPressRunnable != null) {
					removeCallbacks(longPressRunnable);
					longPressRunnable = null;
				}
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
				final Point pos = touch;
				performActionRunnable = new Runnable() {
					@Override
					public void run() {
						v.performClick();
						performAction(v.getContext(), pos, wasTap);
						performActionRunnable = null;
						invalidateTouch();
						invalidate();
					}
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
		Bitmap[] icons = new Bitmap[]{iconAdd, iconRemove, iconInfo, iconDone};
		Rect[] rects = new Rect[]{iconAddRect, iconRemoveRect, iconInfoRect,
				iconDoneRect};
		int length = icons.length;
		int totalWidth = 0;
		int totalHeight = 0;
		int largestWidth = 0;
		int largestHeight = 0;
		// initialize rects and calculate totals
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

	private void addIconInteractively(Point from) {
		AppMenu.Icon appIcon = getListIconAt(from.x, from.y);
		if (appIcon == null) {
			return;
		}
		if (listListener != null) {
			listListener.onHideList();
		}
		editIcon(appIcon);
		touch.set(from.x, from.y);
		setCenter(viewWidth >> 1, viewHeight >> 1);
		resetScrollSilently();
		invalidate();
	}

	private void editIcon(AppMenu.Icon icon) {
		backup.clear();
		backup.addAll(PieLauncherApp.appMenu.icons);
		PieLauncherApp.appMenu.icons.remove(icon);
		ungrabbedIcons.clear();
		ungrabbedIcons.addAll(PieLauncherApp.appMenu.icons);
		grabbedIcon = icon;
		mode = MODE_EDIT;
	}

	private void resetScrollSilently() {
		scrollTo(0, 0);
	}

	private void scrollList(int y) {
		scrollTo(0, y);
		if (listListener != null) {
			listListener.onScrollList(y);
		}
	}

	private void performAction(Context context, Point at, boolean wasTap) {
		if (mode == MODE_PIE) {
			if (wasTap) {
				if (listListener != null) {
					listListener.onOpenList();
				}
			} else {
				PieLauncherApp.appMenu.launchApp(context);
			}
		} else if (mode == MODE_LIST && wasTap) {
			performListAction(context, at.x, at.y);
		} else if (mode == MODE_EDIT) {
			performEditAction(context);
			grabbedIcon = null;
		}
	}

	private void performListAction(Context context, int x, int y) {
		AppMenu.AppIcon appIcon = getListIconAt(x, y);
		if (appIcon != null) {
			PieLauncherApp.appMenu.launchApp(context, appIcon);
		}
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

	private void performEditAction(Context context) {
		if (iconAddRect.contains(touch.x, touch.y)) {
			if (grabbedIcon != null) {
				rollback();
			} else {
				((Activity) context).onBackPressed();
			}
		} else if (iconRemoveRect.contains(touch.x, touch.y)) {
			if (grabbedIcon != null) {
				PieLauncherApp.appMenu.icons.remove(grabbedIcon);
			}
		} else if (iconInfoRect.contains(touch.x, touch.y)) {
			if (grabbedIcon != null) {
				rollback();
				startAppInfo(((AppMenu.AppIcon) grabbedIcon)
						.componentName.getPackageName());
			}
		} else if (iconDoneRect.contains(touch.x, touch.y)) {
			if (grabbedIcon != null) {
				rollback();
			} else {
				endEditMode();
			}
		}
	}

	private void rollback() {
		PieLauncherApp.appMenu.icons.clear();
		PieLauncherApp.appMenu.icons.addAll(backup);
	}

	private void startAppInfo(String packageName) {
		Intent intent = new Intent(
				Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.setData(Uri.parse("package:" + packageName));
		getContext().startActivity(intent);
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
				break;
			}
		}
	}

	private void drawList(Canvas canvas) {
		// manually draw an icon grid because GridView doesn't perform too
		// well on low-end devices and doing it manually gives us more control
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
		for (int i = 0, l = appList.size(); i < l; ++i) {
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
		drawTip(canvas, getTip(hasIcon));
		drawIcon(canvas, iconAdd, iconAddRect, !hasIcon);
		drawIcon(canvas, iconRemove, iconRemoveRect, hasIcon);
		drawIcon(canvas, iconInfo, iconInfoRect, hasIcon);
		drawIcon(canvas, iconDone, iconDoneRect, !hasIcon);
		if (hasIcon) {
			int size = ungrabbedIcons.size();
			double step = AppMenu.TAU / (size + 1);
			double angle = AppMenu.getPositiveAngle(Math.atan2(
					touch.y - PieLauncherApp.appMenu.getCenterY(),
					touch.x - PieLauncherApp.appMenu.getCenterX()) +
					step * .5);
			int insertAt = (int) Math.floor(angle / step);
			PieLauncherApp.appMenu.icons.clear();
			PieLauncherApp.appMenu.icons.addAll(ungrabbedIcons);
			PieLauncherApp.appMenu.icons.add(Math.min(size, insertAt),
					grabbedIcon);
			PieLauncherApp.appMenu.calculate(touch.x, touch.y);
			grabbedIcon.x = touch.x;
			grabbedIcon.y = touch.y;
		} else {
			PieLauncherApp.appMenu.calculate(
					PieLauncherApp.appMenu.getCenterX(),
					PieLauncherApp.appMenu.getCenterY());
		}
		PieLauncherApp.appMenu.draw(canvas);
	}

	private void drawPieMenu(Canvas canvas) {
		canvas.drawColor(0, PorterDuff.Mode.CLEAR);
		if (shouldShowMenu()) {
			PieLauncherApp.appMenu.calculate(touch.x, touch.y);
			PieLauncherApp.appMenu.draw(canvas);
		}
	}

	private boolean shouldShowMenu() {
		return touch.x > -1;
	}

	private void invalidateTouch() {
		touch.set(-1, -1);
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

	private void drawIcon(Canvas canvas, Bitmap icon, Rect rect,
			boolean active) {
		canvas.drawBitmap(icon, null, rect,
				active ? paintActive : paintDeactive);
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
		private OverScroller scroller;
		private int maxY;

		@Override
		public void run() {
			if (!scroller.computeScrollOffset()) {
				return;
			}
			if (maxY != maxScrollY) {
				maxY = maxScrollY;
				scroller.springBack(
						0, getScrollY(),
						0, 0,
						0, maxY);
			}
			scrollList(scroller.getCurrY());
			update();
		}

		private FlingRunnable() {
			scroller = new OverScroller(getContext());
		}

		private void start(int pixelsPerSecond) {
			maxY = maxScrollY;
			scroller.fling(
					0, getScrollY(),
					0, -pixelsPerSecond,
					0, 0,
					0, maxY,
					0, listPadding);
			update();
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
