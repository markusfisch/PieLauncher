package de.markusfisch.android.pielauncher.widget;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.graphics.Converter;
import de.markusfisch.android.pielauncher.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;

public class AppPieView extends SurfaceView {
	public interface OpenListListener {
		void onOpenList();
	}

	private final ArrayList<AppMenu.Icon> backup = new ArrayList<>();
	private final ArrayList<AppMenu.Icon> ungrabbedIcons = new ArrayList<>();
	private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
	private final Paint selectedPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
	private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Point inset = new Point();
	private final Point touch = new Point();
	private final SurfaceHolder surfaceHolder;
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
	private int tapTimeout;
	private int padding;
	private float textOffset;
	private float touchSlopSq;
	private OpenListListener listListener;
	private AppMenu.Icon grabbedIcon;
	private boolean editMode = false;

	public AppPieView(Context context, AttributeSet attr) {
		super(context, attr);

		scaleDetector = new ScaleGestureDetector(context,
				new ScaleListener());

		Resources res = context.getResources();
		dp = res.getDisplayMetrics().density;
		float sp = res.getDisplayMetrics().scaledDensity;
		padding = Math.round(dp * 80f);

		numberOfIconsTip = context.getString(R.string.tip_number_of_icons);
		dragToOrderTip = context.getString(R.string.tip_drag_to_order);
		pinchZoomTip = context.getString(R.string.tip_pinch_zoom);

		selectedPaint.setColorFilter(new PorterDuffColorFilter(
				res.getColor(R.color.selected),
				PorterDuff.Mode.SRC_IN));
		textPaint.setColor(res.getColor(R.color.text_color));
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTextSize(14f * sp);
		float textHeight = textPaint.descent() - textPaint.ascent();
		textOffset = (textHeight / 2) - textPaint.descent();
		translucentBackgroundColor = res.getColor(
				R.color.background_transparent);

		iconAdd = getBitmapFromDrawable(res, R.drawable.ic_add);
		iconRemove = getBitmapFromDrawable(res, R.drawable.ic_remove);
		iconInfo = getBitmapFromDrawable(res, R.drawable.ic_info);
		iconDone = getBitmapFromDrawable(res, R.drawable.ic_done);

		float touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		touchSlopSq = touchSlop * touchSlop;
		tapTimeout = ViewConfiguration.getTapTimeout();

		surfaceHolder = getHolder();
		PieLauncherApp.appMenu.indexAppsAsync(context);

		initSurfaceHolder(surfaceHolder);
		initTouchListener();

		setZOrderOnTop(true);
	}

	public void setOpenListListener(OpenListListener listener) {
		listListener = listener;
	}

	public void addIconInteractive(AppMenu.Icon appIcon, Point from) {
		editIcon(appIcon);
		touch.set(from.x, from.y);
		setCenter(viewWidth >> 1, viewHeight >> 1);
		drawView();
	}

	public boolean isEditMode() {
		return editMode;
	}

	public void endEditMode() {
		Context context = getContext();
		if (context != null) {
			PieLauncherApp.appMenu.store(context);
			PieLauncherApp.prefs.setRadius(radius);
		}
		backup.clear();
		ungrabbedIcons.clear();
		grabbedIcon = null;
		editMode = false;
		drawView();
	}

	private void editIcon(AppMenu.Icon icon) {
		backup.clear();
		backup.addAll(PieLauncherApp.appMenu.icons);
		PieLauncherApp.appMenu.icons.remove(icon);
		ungrabbedIcons.clear();
		ungrabbedIcons.addAll(PieLauncherApp.appMenu.icons);
		grabbedIcon = icon;
		editMode = true;
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

	private void initSurfaceHolder(SurfaceHolder holder) {
		holder.setFormat(PixelFormat.TRANSPARENT);
		holder.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceChanged(
					SurfaceHolder holder,
					int format,
					int width,
					int height) {
				initMenu(width, height);
				if (editMode) {
					drawView();
				}
			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
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
		layoutTouchTargets(height > width);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			// because views can't go below the status bar before Lollipop
			int[] pos = new int[2];
			getLocationOnScreen(pos);
			inset.set(pos[0], pos[1]);
		}
	}

	private void layoutTouchTargets(boolean portrait) {
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
			int y = viewHeight - largestHeight - padding;
			for (Rect rect : rects) {
				rect.offset(x, y);
				x += step + rect.width();
			}
		} else {
			int step = Math.round(
					(float) (viewHeight - totalHeight) / (length + 1));
			int x = viewWidth - largestWidth - padding;
			int y = step;
			for (Rect rect : rects) {
				rect.offset(x, y);
				y += step + rect.height();
			}
		}
	}

	private void initTouchListener() {
		setOnTouchListener(new View.OnTouchListener() {
			private final Point down = new Point();

			private long downAt;
			private Runnable performActionRunnable;

			@Override
			public boolean onTouch(final View v, MotionEvent event) {
				if (editMode && grabbedIcon == null) {
					scaleDetector.onTouchEvent(event);
				}
				touch.set(Math.round(event.getRawX() - inset.x),
						Math.round(event.getRawY() - inset.y));
				switch (event.getActionMasked()) {
					default:
						break;
					case MotionEvent.ACTION_DOWN:
						if (performActionRunnable != null) {
							removeCallbacks(performActionRunnable);
							// Ignore ACTION_DOWN's when there was
							// an action pending.
							break;
						}
						if (editMode) {
							editIconAt(touch);
						} else {
							down.set(touch.x, touch.y);
							downAt = event.getEventTime();
							setCenter(touch);
						}
						drawView();
						break;
					case MotionEvent.ACTION_MOVE:
						drawView();
						break;
					case MotionEvent.ACTION_UP:
						if (performActionRunnable != null) {
							removeCallbacks(performActionRunnable);
						}
						final long downTime =
								SystemClock.uptimeMillis() - downAt;
						performActionRunnable = new Runnable() {
							@Override
							public void run() {
								v.performClick();
								performAction(v.getContext(), down, downTime);
								performActionRunnable = null;
								invalidateTouch();
								drawView();
							}
						};
						// Wait a short time before performing the action
						// because some touch screen drivers send
						// ACTION_UP/ACTION_DOWN pairs for very short
						// touch interruptions what makes the menu jump
						// and execute multiple actions unintentionally.
						postDelayed(performActionRunnable, 16);
						break;
					case MotionEvent.ACTION_CANCEL:
						invalidateTouch();
						grabbedIcon = null;
						drawView();
						break;
				}
				return true;
			}
		});
	}

	private void performAction(Context context, Point down, long downTime) {
		if (editMode) {
			performEditAction(context);
			grabbedIcon = null;
		} else if (downTime <= tapTimeout &&
				distSq(down, touch) <= touchSlopSq) {
			if (listListener != null) {
				listListener.onOpenList();
			}
		} else {
			PieLauncherApp.appMenu.launchApp(context);
		}
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
				Math.max(radius, Math.min(viewWidth - radius, x)),
				Math.max(radius, Math.min(viewHeight - radius, y)),
				radius);
	}

	private void editIconAt(Point point) {
		for (int i = 0, size = PieLauncherApp.appMenu.icons.size();
				i < size; ++i) {
			AppMenu.Icon icon = PieLauncherApp.appMenu.icons.get(i);
			float sizeSq = Math.round(icon.size * icon.size);
			if (distSq(point.x, point.y, icon.x, icon.y) < sizeSq) {
				editIcon(icon);
				break;
			}
		}
	}

	private void drawView() {
		Canvas canvas = surfaceHolder.lockCanvas();
		if (canvas == null) {
			return;
		}
		if (editMode) {
			canvas.drawColor(translucentBackgroundColor,
					PorterDuff.Mode.SRC);
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
		} else {
			canvas.drawColor(0, PorterDuff.Mode.CLEAR);
			if (shouldShowMenu()) {
				PieLauncherApp.appMenu.calculate(touch.x, touch.y);
				PieLauncherApp.appMenu.draw(canvas);
			}
		}
		surfaceHolder.unlockCanvasAndPost(canvas);
	}

	private void invalidateTouch() {
		touch.set(-1, -1);
	}

	private boolean shouldShowMenu() {
		return touch.x > -1;
	}

	private void drawTip(Canvas canvas, String tip) {
		if (tip != null) {
			drawText(canvas, tip);
		}
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

	private void drawIcon(Canvas canvas, Bitmap icon, Rect rect,
			boolean active) {
		canvas.drawBitmap(icon, null, rect,
				active && rect.contains(touch.x, touch.y)
						? selectedPaint
						: bitmapPaint);
	}

	private void drawText(Canvas canvas, String text) {
		canvas.drawText(text, viewWidth >> 1, padding + textOffset, textPaint);
	}

	private static float distSq(Point a, Point b) {
		return distSq(a.x, a.y, b.x, b.y);
	}

	private static float distSq(int ax, int ay, int bx, int by) {
		float dx = ax - bx;
		float dy = ay - by;
		return dx*dx + dy*dy;
	}

	private int clampRadius(int r) {
		return Math.max(minRadius, Math.min(r, maxRadius));
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
			drawView();
			return true;
		}
	}
}
