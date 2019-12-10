package de.markusfisch.android.pielauncher.widget;

import de.markusfisch.android.pielauncher.content.AppMenu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;

public class AppPieView extends SurfaceView {
	public interface OpenListListener {
		void onOpenList();
	}

	public static final AppMenu appMenu = new AppMenu();

	private final ArrayList<AppMenu.Icon> iconsBeforeEdit = new ArrayList<>();
	private final Point touch = new Point();
	private final Point lastTouch = new Point();
	private final SurfaceHolder surfaceHolder;
	private final float dp;

	private int viewWidth;
	private int viewHeight;
	private int radius;
	private int tapTimeout;
	private float touchSlopSq;
	private OpenListListener listListener;
	private AppMenu.Icon iconToEdit;
	private boolean editMode = false;

	public AppPieView(Context context, AttributeSet attr) {
		super(context, attr);

		dp = context.getResources().getDisplayMetrics().density;
		surfaceHolder = getHolder();
		appMenu.indexAppsAsync(context);

		ViewConfiguration vc = ViewConfiguration.get(context);
		float touchSlop = vc.getScaledTouchSlop();
		touchSlopSq = touchSlop * touchSlop;
		tapTimeout = vc.getTapTimeout();

		initSurfaceHolder(surfaceHolder);
		initTouchListener();

		setZOrderOnTop(true);
	}

	public void setOpenListListener(OpenListListener listener) {
		listListener = listener;
	}

	public void addIconInteractive(AppMenu.Icon appIcon, Point from) {
		editIcon(appIcon);
		clear(lastTouch);
		touch.set(from.x, from.y);
		setCenter(viewWidth >> 1, viewHeight >> 1);
		drawView();
	}

	public boolean isEditMode() {
		return editMode;
	}

	public void endEditMode() {
		iconsBeforeEdit.clear();
		iconToEdit = null;
		editMode = false;
		drawView();
	}

	private void editIcon(AppMenu.Icon icon) {
		appMenu.icons.remove(icon);
		iconsBeforeEdit.clear();
		iconsBeforeEdit.addAll(appMenu.icons);
		iconToEdit = icon;
		editMode = true;
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
					clear(lastTouch);
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
		radius = Math.round(min * .5f);
		viewWidth = width;
		viewHeight = height;
	}

	private void initTouchListener() {
		setOnTouchListener(new View.OnTouchListener() {
			private Point down = new Point();
			private long downAt;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				touch.set(Math.round(event.getRawX()),
						Math.round(event.getRawY()));
				switch (event.getActionMasked()) {
					default:
						break;
					case MotionEvent.ACTION_CANCEL:
						clear(touch);
						iconToEdit = null;
						drawView();
						break;
					case MotionEvent.ACTION_MOVE:
						drawView();
						break;
					case MotionEvent.ACTION_DOWN:
						if (editMode) {
							editIconAt(touch);
						} else {
							down.set(touch.x, touch.y);
							downAt = event.getEventTime();
							setCenter(touch);
						}
						drawView();
						break;
					case MotionEvent.ACTION_UP:
						v.performClick();
						if (editMode) {
							iconToEdit = null;
						} else {
							if (SystemClock.uptimeMillis() - downAt <= tapTimeout &&
									distSq(down, touch) <= touchSlopSq) {
								if (listListener != null) {
									listListener.onOpenList();
								}
							} else {
								appMenu.launch(v.getContext());
							}
						}
						clear(touch);
						drawView();
						break;
				}
				return true;
			}
		});
	}

	private void setCenter(Point point) {
		setCenter(point.x, point.y);
	}

	private void setCenter(int x, int y) {
		appMenu.set(
				Math.max(radius, Math.min(viewWidth - radius, x)),
				Math.max(radius, Math.min(viewHeight - radius, y)),
				radius);
	}

	private boolean editIconAt(Point point) {
		for (int i = 0, size = appMenu.icons.size(); i < size; ++i) {
			AppMenu.Icon icon = appMenu.icons.get(i);
			float sizeSq = Math.round(icon.size * icon.size);
			if (distSq(point.x, point.y, icon.x, icon.y) < sizeSq) {
				editIcon(icon);
				return true;
			}
		}
		return false;
	}

	private void drawView() {
		if (touch.equals(lastTouch)) {
			return;
		}
		Canvas canvas = surfaceHolder.lockCanvas();
		if (canvas == null) {
			return;
		}
		canvas.drawColor(0, PorterDuff.Mode.CLEAR);
		if (isTouchValid() || editMode) {
			int cx = appMenu.getCenterX();
			int cy = appMenu.getCenterY();
			{
				int focusX = touch.x;
				int focusY = touch.y;
				if (editMode && iconToEdit == null) {
					focusX = cx;
					focusY = cy;
				}
				appMenu.calculate(focusX, focusY);
			}
			if (iconToEdit != null) {
				double angle = AppMenu.getPositiveAngle(Math.atan2(
						touch.y - cy,
						touch.x - cx));
				double step = AppMenu.TAU / appMenu.icons.size();
				int insertAt = (int) Math.floor(angle / step);
				appMenu.icons.clear();
				appMenu.icons.addAll(iconsBeforeEdit);
				appMenu.icons.add(insertAt, iconToEdit);
				iconToEdit.x = touch.x;
				iconToEdit.y = touch.y;
			}
			appMenu.draw(canvas);
			/*if (editMode) {
				// draw delete from menu icon
				// draw app info icon
				// draw close editor
			}*/
		}
		lastTouch.set(touch.x, touch.y);
		surfaceHolder.unlockCanvasAndPost(canvas);
	}

	private boolean isTouchValid() {
		return touch.x > -1;
	}

	private static void clear(Point point) {
		point.set(-1, -1);
	}

	private static float distSq(Point a, Point b) {
		return distSq(a.x, a.y, b.x, b.y);
	}

	private static float distSq(int ax, int ay, int bx, int by) {
		float dx = ax - bx;
		float dy = ay - by;
		return dx*dx + dy*dy;
	}
}
