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

		float touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		touchSlopSq = touchSlop * touchSlop;
		tapTimeout = ViewConfiguration.getTapTimeout();

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
		iconsBeforeEdit.clear();
		iconToEdit = null;
		editMode = false;
		invalidateView();
		drawView();
	}

	private void editIcon(AppMenu.Icon icon) {
		appMenu.icons.remove(icon);
		iconsBeforeEdit.clear();
		iconsBeforeEdit.addAll(appMenu.icons);
		iconToEdit = icon;
		editMode = true;
		invalidateView();
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
					invalidateView();
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
						hideMenu();
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
						hideMenu();
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

	private void editIconAt(Point point) {
		for (int i = 0, size = appMenu.icons.size(); i < size; ++i) {
			AppMenu.Icon icon = appMenu.icons.get(i);
			float sizeSq = Math.round(icon.size * icon.size);
			if (distSq(point.x, point.y, icon.x, icon.y) < sizeSq) {
				editIcon(icon);
				break;
			}
		}
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
		if (shouldShowMenu() || editMode) {
			if (iconToEdit != null) {
				int size = iconsBeforeEdit.size();
				double step = AppMenu.TAU / (size + 1);
				double angle = AppMenu.getPositiveAngle(Math.atan2(
						touch.y - appMenu.getCenterY(),
						touch.x - appMenu.getCenterX()) + step * .5);
				int insertAt = (int) Math.floor(angle / step);
				appMenu.icons.clear();
				appMenu.icons.addAll(iconsBeforeEdit);
				appMenu.icons.add(Math.min(size, insertAt), iconToEdit);
				appMenu.calculate(touch.x, touch.y);
				iconToEdit.x = touch.x;
				iconToEdit.y = touch.y;
			} else if (editMode) {
				appMenu.calculate(appMenu.getCenterX(), appMenu.getCenterY());
			} else {
				appMenu.calculate(touch.x, touch.y);
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

	private void invalidateView() {
		lastTouch.set(-2, -2);
	}

	private void hideMenu() {
		touch.set(-1, -1);
	}

	private boolean shouldShowMenu() {
		return touch.x > -1;
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
