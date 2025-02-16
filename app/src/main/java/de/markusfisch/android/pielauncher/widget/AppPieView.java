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
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.activity.PickIconActivity;
import de.markusfisch.android.pielauncher.activity.PreferencesActivity;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.graphics.BackgroundBlur;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.graphics.Converter;
import de.markusfisch.android.pielauncher.graphics.PieMenu;
import de.markusfisch.android.pielauncher.graphics.Ripple;
import de.markusfisch.android.pielauncher.preference.Preferences;

public class AppPieView extends View {
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
	private static final int MODE_PIE = 0;
	private static final int MODE_LIST = 1;
	private static final int MODE_EDIT = 2;

	private final Fade fadePie = new Fade();
	private final Fade fadeList = new Fade();
	private final Fade fadeEdit = new Fade();
	private final ArrayList<AppMenu.Icon> backup = new ArrayList<>();
	private final ArrayList<AppMenu.Icon> ungrabbedIcons = new ArrayList<>();
	private final Paint paintList = new Paint(Paint.FILTER_BITMAP_FLAG);
	private final Paint paintDropZone = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint paintPressed = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint paintAction = new Paint(Paint.FILTER_BITMAP_FLAG);
	private final TextPaint paintText = new TextPaint(Paint.ANTI_ALIAS_FLAG);
	private final Point touch = new Point();
	private final Ripple ripple = Ripple.newFadingRipple();
	private final Rect drawRect = new Rect();
	private final Rect iconStartRect = new Rect();
	private final Rect iconCenterRect = new Rect();
	private final Rect iconEndRect = new Rect();
	private final Rect iconChangeTwistRect = new Rect();
	private final Rect iconChangeIconScaleRect = new Rect();
	private final Rect iconChangeRadiusRect = new Rect();
	private final Bitmap iconAdd;
	private final Bitmap iconRemove;
	private final Bitmap iconEdit;
	private final Bitmap iconHide;
	private final Bitmap iconDetails;
	private final Bitmap iconDone;
	private final Bitmap iconPreferences;
	private final Bitmap iconLaunchFirst;
	private final String loadingTip;
	private final String dragToOrderTip;
	private final String removeIconTip;
	private final String editAppTip;
	private final String hideAppTip;
	private final String removeAppTip;
	private final Preferences prefs;
	private final long tapOrScrollTimeout;
	private final long longPressTimeout;
	private final long doubleTapTimeout;
	private final int listPadding;
	private final int searchInputHeight;
	private final int iconSize;
	private final int iconTextPadding;
	private final int spaceBetween;
	private final int iconLaunchFirstHalf;
	private final int translucentBackgroundColor;
	private final int alphaDropZone;
	private final int alphaPressed;
	private final int alphaText;
	private final float dp;
	private final float textHeight;
	private final float textOffset;
	private final float touchSlopSq;

	private Window window;
	private Runnable rippleRunnable;
	private int viewWidth;
	private int viewHeight;
	private int controlsPadding;
	private int actionSize;
	private int actionSizeSq;
	private int deadZoneTop;
	private int deadZoneBottom;
	private int deadZoneLeft;
	private int deadZoneRight;
	private int listFadeHeight;
	private int minRadius;
	private int medRadius;
	private int maxRadius;
	private int radius;
	private float twist;
	private float minIconScale;
	private float iconScale;
	private float dragDistance;
	private float dragProgress;
	private int maxScrollY;
	private int lastScrollY;
	private int lastInsertAt;
	private int lastSelectedIcon;
	private int lastBlur;
	private int selectedApp = -1;
	private int mode = MODE_PIE;
	private ListListener listListener;
	private List<AppMenu.AppIcon> appList;
	private Rect highlightedAction;
	private AppMenu.Icon grabbedIcon;
	private AppMenu.Icon highlightedIcon;
	private long highlightedFrom;
	private long grabbedIconAt;
	private long lastTapUpTime;
	private Bitmap iconChangeTwist;
	private Bitmap iconChangeIconScale;
	private Bitmap iconChangeRadius;
	private boolean neverDropped = false;
	private boolean appListIsFiltered = false;

	public AppPieView(Context context, AttributeSet attr) {
		super(context, attr);

		prefs = PieLauncherApp.getPrefs(context);

		Resources res = context.getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
		dp = dm.density;
		float sp = dm.scaledDensity;

		controlsPadding = Math.round(80f * dp);
		listPadding = Math.round(16f * dp);
		searchInputHeight = Math.round(112f * dp);
		iconSize = Math.round(48f * dp);
		iconTextPadding = Math.round(12f * dp);
		spaceBetween = Math.round(4f * dp);

		loadingTip = context.getString(R.string.tip_loading);
		dragToOrderTip = context.getString(R.string.tip_drag_to_order);
		removeIconTip = context.getString(R.string.tip_remove_icon);
		editAppTip = context.getString(R.string.change_icon);
		hideAppTip = context.getString(R.string.hide_app);
		removeAppTip = context.getString(R.string.tip_remove_app);

		paintDropZone.setColor(res.getColor(R.color.bg_drop_zone));
		paintDropZone.setStyle(Paint.Style.FILL);
		alphaDropZone = paintDropZone.getAlpha();
		paintPressed.setColor(res.getColor(R.color.bg_action_pressed));
		paintPressed.setStyle(Paint.Style.FILL);
		alphaPressed = paintPressed.getAlpha();

		paintText.setColor(res.getColor(R.color.text_color));
		alphaText = paintText.getAlpha();
		paintText.setTextAlign(Paint.Align.CENTER);
		paintText.setTextSize(14f * sp);
		textHeight = paintText.descent() - paintText.ascent();
		textOffset = (textHeight / 2) - paintText.descent();
		translucentBackgroundColor = res.getColor(R.color.bg_ui);

		iconAdd = Converter.getBitmapFromDrawable(res, R.drawable.ic_add);
		iconEdit = Converter.getBitmapFromDrawable(res, R.drawable.ic_edit);
		iconHide = Converter.getBitmapFromDrawable(res, R.drawable.ic_hide);
		iconRemove = Converter.getBitmapFromDrawable(res,
				R.drawable.ic_remove);
		iconDetails = Converter.getBitmapFromDrawable(res,
				R.drawable.ic_details);
		iconDone = Converter.getBitmapFromDrawable(res, R.drawable.ic_done);
		iconPreferences = Converter.getBitmapFromDrawable(res,
				R.drawable.ic_preferences);
		iconLaunchFirst = Converter.getBitmapFromDrawable(res,
				R.drawable.ic_launch_first);
		iconLaunchFirstHalf = iconLaunchFirst.getWidth() >> 1;

		ViewConfiguration configuration = ViewConfiguration.get(context);
		float touchSlop = configuration.getScaledTouchSlop();
		touchSlopSq = touchSlop * touchSlop;
		tapOrScrollTimeout = ViewConfiguration.getTapTimeout();
		longPressTimeout = ViewConfiguration.getLongPressTimeout() *
				(prefs.getIconPress() == Preferences.ICON_PRESS_LONGER
						? 2L : 1L);
		doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout();

		if (PieLauncherApp.appMenu.isEmpty()) {
			PieLauncherApp.appMenu.postIndexApps(context);
		}
		initTouchListener();
	}

	public void setWindow(Window window) {
		this.window = window;
	}

	public void setListListener(ListListener listener) {
		listListener = listener;
	}

	public void resetScroll() {
		lastScrollY = 0;
	}

	public void showList() {
		if (mode == MODE_LIST) {
			return;
		}
		mode = MODE_LIST;
		cancelRipple();
		scrollList(lastScrollY, false);
		setVerticalScrollBarEnabled(true);
		resetDragDownList();
		fadeList.fadeIn();
		invalidate();
	}

	public void hideList() {
		if (mode == MODE_PIE) {
			return;
		}
		fadeList.maxIn = dragProgress;
		fadeOutMode();
		mode = MODE_PIE;
		resetScrollWithoutAnimation();
		setVerticalScrollBarEnabled(false);
		invalidate();
	}

	public void dragDownListBy(float y) {
		if (mode != MODE_LIST) {
			return;
		}
		dragDistance -= y;
		dragProgress = clamp(1f - (dragDistance / listFadeHeight), 0f, 1f);
		invalidate();
	}

	public void showEditor() {
		editIcon(null);
		invalidate();
	}

	public boolean isEmpty() {
		return appList == null || appList.isEmpty();
	}

	public int getIconCount() {
		return appList == null ? 0 : appList.size();
	}

	public boolean appListNotScrolled() {
		return mode != MODE_LIST || getScrollY() == 0;
	}

	public boolean inEditMode() {
		return mode == MODE_EDIT;
	}

	public boolean inListMode() {
		return mode == MODE_LIST;
	}

	public void filterAppList(String query) {
		List<AppMenu.AppIcon> newAppList =
				PieLauncherApp.appMenu.filterAppsBy(getContext(), query);
		if (newAppList != null) {
			appList = newAppList;
		}
		appListIsFiltered = !TextUtils.isEmpty(query);
		selectedApp = prefs.doubleSpaceLaunch()
				? (appListIsFiltered ? 0 : -1)
				: getSelectedAppFromTrailingSpace(query);
		scrollList(0, false);
		lastScrollY = 0;
		invalidate();
	}

	public void launchSelectedAppFromList() {
		if (selectedApp < 0 || isEmpty()) {
			return;
		}
		PieLauncherApp.appMenu.launchApp(getContext(), appList.get(
				clamp(selectedApp, 0, getIconCount() - 1)));
	}

	public void endEditMode() {
		Context context = getContext();
		if (context != null) {
			PieLauncherApp.appMenu.store(context);
		}
		PieLauncherApp.appMenu.unlock();
		prefs.setRadius(radius);
		prefs.setTwist(twist);
		prefs.setIconScale(iconScale);
		backup.clear();
		ungrabbedIcons.clear();
		releaseIcon();
		fadeOutMode();
		mode = MODE_PIE;
		invalidate();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
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
		float fPie = fadePie.get(now, ad);
		float fList = Math.min(fadeList.get(now, ad), dragProgress);
		float fEdit = fadeEdit.get(now, ad);
		float fMax = Math.max(fPie, Math.max(fList, fEdit));
		if (prefs.blurBackground()) {
			int blur = Math.round(fMax * BackgroundBlur.BLUR);
			if (blur != lastBlur) {
				BackgroundBlur.setBlurRadius(window, blur);
				lastBlur = blur;
			}
		}
		if (mode != MODE_PIE || prefs.darkenBackground()) {
			int max = (translucentBackgroundColor >> 24) & 0xff;
			int alpha = Math.round(fMax * max);
			if (alpha == 0) {
				canvas.drawColor(0, PorterDuff.Mode.CLEAR);
			} else {
				canvas.drawColor(
						(alpha << 24) | (translucentBackgroundColor & 0xffffff),
						PorterDuff.Mode.SRC);
			}
		}
		boolean invalidate = drawPieMenu(canvas, fPie);
		invalidate |= drawList(canvas, fList);
		invalidate |= drawEditor(canvas, fEdit);
		if (ripple.draw(canvas, prefs) || invalidate) {
			invalidate();
		}
		if (PieLauncherApp.appMenu.isIndexing()) {
			drawTip(canvas, loadingTip);
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

	private void initTouchListener() {
		setOnTouchListener(new OnTouchListener() {
			private final FlingRunnable flingRunnable = new FlingRunnable();
			private final SparseArray<TouchReference> touchReferences =
					new SparseArray<>();

			private VelocityTracker velocityTracker;
			private int primaryId;
			private int spinId = -1;
			private double spinAngleDown;
			private double spinInitialTwist;
			private int scrollOffset;
			private Runnable longPressRunnable;
			private Runnable performActionRunnable;

			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				touch.set(Math.round(event.getX()), Math.round(event.getY()));
				switch (event.getActionMasked()) {
					case MotionEvent.ACTION_POINTER_DOWN:
						if (mode == MODE_PIE) {
							startSpin(event);
							invalidate();
						} else if (mode == MODE_LIST) {
							cancelLongPress();
							addTouch(event);
							initScroll(event);
						}
						break;
					case MotionEvent.ACTION_POINTER_UP:
						if (mode == MODE_PIE) {
							stopSpin(event);
						} else if (mode == MODE_LIST) {
							updateReferences(event);
							initScroll(event);
						}
						break;
					case MotionEvent.ACTION_DOWN:
						if (cancelPerformAction()) {
							// Ignore additional ACTION_DOWN's when there
							// is a pending action.
							break;
						}
						addTouch(event);
						long eventTime = event.getEventTime();
						switch (mode) {
							case MODE_PIE:
								if (inDeadZone()) {
									return false;
								}
								setCenter(touch.x, touch.y);
								fadePie.fadeIn(eventTime);
								performHapticFeedbackIfAllowed(
										HAPTIC_FEEDBACK_DOWN);
								break;
							case MODE_LIST:
								// Ignore ACTION_DOWN during animation to
								// prevent starting a long press from the
								// wrong coordinates.
								if (fadeList.isFadingIn(eventTime)) {
									return false;
								}
								initScroll(event);
								initLongPress(v.getContext());
								break;
							case MODE_EDIT:
								editIconAt(touch);
								break;
						}
						invalidate();
						break;
					case MotionEvent.ACTION_MOVE:
						if (mode == MODE_PIE) {
							spin(event);
						} else if (mode == MODE_LIST) {
							if (isScroll(event)) {
								cancelLongPress();
							}
							scroll(event);
						}
						invalidate();
						break;
					case MotionEvent.ACTION_UP:
						if (mode == MODE_PIE) {
							cancelSpin();
						} else if (mode == MODE_LIST) {
							cancelLongPress();
							cancelDragDownList();
							keepScrolling(event);
						}
						postPerformAction(v, event);
						break;
					case MotionEvent.ACTION_CANCEL:
						if (mode == MODE_PIE) {
							cancelSpin();
							fadeOutMode();
						} else if (mode == MODE_LIST) {
							cancelLongPress();
							cancelDragDownList();
							recycleVelocityTracker();
						}
						releaseIcon();
						invalidate();
						break;
					default:
						break;
				}
				return true;
			}

			private void addTouch(MotionEvent event) {
				int index = event.getActionIndex();
				primaryId = event.getPointerId(index);
				setTouchReference(event, primaryId, index);
			}

			private void updateReferences(MotionEvent event) {
				// Unfortunately, and contrary to the docs, some (old?) touch
				// screen drivers don't return the index of the touch that has
				// gone up in ACTION_POINTER_UP for getActionIndex() but an
				// index of a pointer that is still down.
				// So the only option is to update all references.
				for (int i = 0, l = event.getPointerCount(); i < l; ++i) {
					int id = event.getPointerId(i);
					setTouchReference(event, id, i);
				}
			}

			private void setTouchReference(MotionEvent event, int id,
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

			private boolean inDeadZone() {
				switch (prefs.getDeadZone()) {
					case Preferences.DEAD_ZONE_TOP:
						return inTopDeadZone();
					case Preferences.DEAD_ZONE_BOTTOM:
						return inBottomDeadZone();
					case Preferences.DEAD_ZONE_TOP_BOTTOM:
						return inTopDeadZone() || inBottomDeadZone();
					case Preferences.DEAD_ZONE_ALL:
						return inTopDeadZone() || inBottomDeadZone() ||
								inLeftOrRightDeadZone();
					default:
						return false;
				}
			}

			private boolean inTopDeadZone() {
				return touch.y < deadZoneTop;
			}

			private boolean inBottomDeadZone() {
				return touch.y > deadZoneBottom;
			}

			private boolean inLeftOrRightDeadZone() {
				return touch.x < deadZoneLeft || touch.x > deadZoneRight;
			}

			private void initScroll(MotionEvent event) {
				flingRunnable.stop();
				recycleVelocityTracker();
				velocityTracker = VelocityTracker.obtain();
				velocityTracker.addMovement(event);
				scrollOffset = getScrollY();
			}

			private void scroll(MotionEvent event) {
				if (isDraggingDownList()) {
					if (listListener != null) {
						listListener.onDragDown(dragProgress);
					}
					return;
				}
				int index = getPrimaryIndex(event);
				TouchReference tr = getTouchReference(event, index);
				if (tr == null || isTap(tr, event.getEventTime(),
						tapOrScrollTimeout)) {
					return;
				}
				if (velocityTracker != null) {
					velocityTracker.addMovement(event);
				}
				int y = Math.round(event.getY(index));
				int scrollY = scrollOffset + (tr.scrollRef - y);
				lastScrollY = clamp(scrollY, 0, maxScrollY);
				scrollList(lastScrollY, true);
				if (lastScrollY == 0 || lastScrollY == maxScrollY) {
					// Move reference to current coordinate if no scrolling
					// (in that direction) was possible so moving into the
					// opposite direction is immediate.
					tr.scrollRef = y;
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

			private void initLongPress(Context context) {
				if (prefs.getIconPress() == Preferences.ICON_LOCK_MENU) {
					return;
				}
				cancelLongPress();
				final AppMenu.Icon appIcon = getListIconAt(touch.x, touch.y);
				if (appIcon == null) {
					return;
				}
				initLongPressFeedback(appIcon);
				longPressRunnable = () -> {
					performHapticFeedbackIfAllowed(
							HapticFeedbackConstants.LONG_PRESS);
					if (prefs.getIconPress() == Preferences.ICON_PRESS_MENU) {
						showIconOptions(context, appIcon);
					} else {
						addIconInteractively(appIcon);
					}
					longPressRunnable = null;
					cancelHighlight();
				};
				postDelayed(longPressRunnable, longPressTimeout);
			}

			private void cancelLongPress() {
				cancelHighlight();
				cancelRipple();
				if (longPressRunnable != null) {
					removeCallbacks(longPressRunnable);
					longPressRunnable = null;
				}
			}

			private void cancelHighlight() {
				highlightedIcon = null;
				highlightedFrom = 0;
			}

			private void initLongPressFeedback(AppMenu.Icon appIcon) {
				cancelRipple();
				final Point at = new Point(touch.x, touch.y + getScrollY());
				rippleRunnable = () -> {
					highlightedIcon = appIcon;
					highlightedFrom = SystemClock.uptimeMillis();
					ripple.set(at.x, at.y);
					invalidate();
				};
				// Delay touch feedback to not make it feel too sensitive.
				postDelayed(rippleRunnable, longPressTimeout >> 1);
			}

			private boolean isScroll(MotionEvent event) {
				return !isTap(event, Long.MAX_VALUE);
			}

			private boolean isTap(MotionEvent event, long timeOut) {
				TouchReference tr = getTouchReference(event,
						getPrimaryIndex(event));
				return isTap(tr, event.getEventTime(), timeOut);
			}

			private boolean isTap(TouchReference tr, long time, long timeOut) {
				return tr != null &&
						time - tr.time <= timeOut &&
						distSq(tr.x, tr.y, touch.x, touch.y) <= touchSlopSq;
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

			private void postPerformAction(final View v, MotionEvent event) {
				cancelPerformAction();
				// Any duration shorter than the long press timeout is
				// considered to be a press/tap.
				final boolean wasTap = isTap(event, longPressTimeout);
				final boolean wasLongPress = !wasTap &&
						isTap(event, Long.MAX_VALUE);
				long eventTime = event.getEventTime();
				final boolean wasDoubleTap = wasTap &&
						eventTime - lastTapUpTime < doubleTapTimeout;
				lastTapUpTime = eventTime;
				final Point at = new Point(touch.x, touch.y);
				performActionRunnable = () -> {
					v.performClick();
					if (performAction(v.getContext(), at,
							wasTap, wasDoubleTap, wasLongPress)) {
						performHapticFeedbackIfAllowed(
								HAPTIC_FEEDBACK_CONFIRM);
					}
					performActionRunnable = null;
					resetHighlightedAction();
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

			private void startSpin(MotionEvent event) {
				if (fadePie.isVisible() && spinId < 0) {
					initSpin(event, event.getActionIndex());
				}
			}

			private void spin(MotionEvent event) {
				if (!fadePie.isVisible()) {
					return;
				}
				int count = event.getPointerCount();
				// Hide pie once the primary pointer was lifted. Can only be
				// done in ACTION_MOVE since ACTION_POINTER_UP doesn't give
				// the correct index everywhere. See updateReferences().
				if (count == 1 && event.getPointerId(0) != primaryId) {
					cancelSpin();
					primaryId = -1;
					fadeOutMode();
					return;
				}
				if (count < 2) {
					cancelSpin();
					return;
				}
				for (int i = 0; i < count; ++i) {
					if (event.getPointerId(i) == spinId) {
						PieLauncherApp.appMenu.setTwist(spinInitialTwist +
								PieMenu.getAngleDifference(
										angleOf(event.getX(i), event.getY(i)),
										spinAngleDown));
						return;
					}
				}
			}

			private void stopSpin(MotionEvent event) {
				cancelSpin();
				int indexUp = event.getActionIndex();
				for (int i = 0, l = event.getPointerCount(); i < l; ++i) {
					if (i == indexUp) {
						continue;
					}
					int id = event.getPointerId(i);
					if (id == primaryId) {
						continue;
					}
					initSpin(event, i);
					break;
				}
			}

			private void cancelSpin() {
				spinId = -1;
			}

			private void initSpin(MotionEvent event, int idx) {
				spinId = event.getPointerId(idx);
				spinAngleDown = angleOf(event.getX(idx), event.getY(idx));
				spinInitialTwist = PieLauncherApp.appMenu.getTwist();
			}
		});
	}

	private void cancelDragDownList() {
		if (isDraggingDownList()) {
			resetDragDownList();
			invalidate();
			if (listListener != null) {
				listListener.onDragDown(dragProgress);
			}
		}
	}

	private boolean isDraggingDownList() {
		return dragProgress < 1f;
	}

	private void resetDragDownList() {
		dragDistance = 0f;
		dragProgress = 1f;
	}

	private void layoutView(int width, int height) {
		int viewMin = Math.min(width, height);
		int viewMax = Math.max(width, height);
		viewWidth = width;
		viewHeight = height;

		float maxIconSize = 64f * dp;
		if (Math.floor(viewMin * .28f) > maxIconSize) {
			viewMin = Math.round(maxIconSize / .28f);
		}

		maxRadius = Math.round(viewMin * .5f);
		minRadius = Math.round(maxRadius * .5f);
		medRadius = minRadius + (maxRadius - minRadius) / 2;
		radius = clampRadius(prefs.getRadius(maxRadius));
		twist = prefs.getTwist();
		minIconScale = (48f * dp) / maxIconSize;
		iconScale = prefs.getIconScale();

		updateChangeTwistIcon();
		updateChangeIconScaleIcon();
		updateChangeRadiusIcon();

		int pieBottom = viewMax / 2 + maxRadius;
		controlsPadding = (viewMax - pieBottom) / 2;

		deadZoneTop = Math.min(height / 10, Math.round(64f * dp));
		deadZoneBottom = height - deadZoneTop;
		deadZoneLeft = Math.min(width / 10, Math.round(48f * dp));
		deadZoneRight = width - deadZoneLeft;

		listFadeHeight = Math.round(viewHeight * .5f);

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

		// Evenly distribute rects in parent.
		int spaces = length + 1;
		int max;
		if (portrait) {
			int step = Math.round((float) (viewWidth - totalWidth) / spaces);
			max = largestWidth + step;
			int x = step;
			int y = viewHeight - controlsPadding - largestHeight / 2;
			for (Rect rect : rects) {
				rect.offset(x, y);
				x += step + rect.width();
			}
		} else {
			int step = Math.round((float) (viewHeight - totalHeight) / spaces);
			max = largestHeight + step;
			int x = viewWidth - controlsPadding - largestWidth / 2;
			int y = step;
			for (Rect rect : rects) {
				rect.offset(x, y);
				y += step + rect.height();
			}
		}

		// Calculate top button rectangles.
		iconChangeTwistRect.set(iconStartRect);
		iconChangeIconScaleRect.set(iconCenterRect);
		iconChangeRadiusRect.set(iconEndRect);
		if (portrait) {
			int axis = viewHeight >> 1;
			iconChangeTwistRect.top = mirror(iconStartRect.bottom, axis);
			iconChangeTwistRect.bottom = mirror(iconStartRect.top, axis);
			iconChangeIconScaleRect.top = mirror(iconCenterRect.bottom, axis);
			iconChangeIconScaleRect.bottom = mirror(iconCenterRect.top, axis);
			iconChangeRadiusRect.top = mirror(iconEndRect.bottom, axis);
			iconChangeRadiusRect.bottom = mirror(iconEndRect.top, axis);
		} else {
			int axis = viewWidth >> 1;
			iconChangeTwistRect.left = mirror(iconStartRect.right, axis);
			iconChangeTwistRect.right = mirror(iconStartRect.left, axis);
			iconChangeIconScaleRect.left = mirror(iconCenterRect.right, axis);
			iconChangeIconScaleRect.right = mirror(iconCenterRect.left, axis);
			iconChangeRadiusRect.left = mirror(iconEndRect.right, axis);
			iconChangeRadiusRect.right = mirror(iconEndRect.left, axis);
		}

		// Calculate size of circular action buttons.
		actionSize = Math.min(iconSize, max / 2 - spaceBetween);
		actionSizeSq = actionSize * actionSize;
	}

	private void showIconOptions(Context context, AppMenu.Icon icon) {
		if (icon == null) {
			return;
		}
		ArrayList<String> list = new ArrayList<>();
		list.add(context.getString(R.string.add_to_pie_menu));
		list.add(context.getString(R.string.show_app_info));
		list.add(context.getString(R.string.hide_app));
		if (PieLauncherApp.iconPack.hasPacks()) {
			list.add(context.getString(R.string.change_icon));
		}
		OptionsDialog.show(context, R.string.edit_app,
				list.toArray(new CharSequence[0]),
				(view, which) -> {
					switch (which) {
						case 0:
							addIconInteractively(icon);
							postDelayed(this::releaseIcon, 100);
							break;
						case 1:
							PieLauncherApp.appMenu.launchAppInfo(context,
									(AppMenu.AppIcon) icon);
							break;
						case 2:
							PickIconActivity.askToHide(context,
									((AppMenu.AppIcon) icon).componentName);
							break;
						case 3:
							returnToList();
							changeIcon(context, icon);
							break;
					}
				});
	}

	private void addIconInteractively(AppMenu.Icon appIcon) {
		if (appIcon == null) {
			return;
		}
		if (listListener != null) {
			listListener.onHideList();
		}
		fadeList.fadeOut();
		neverDropped = true;
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
		grabbedIconAt = SystemClock.uptimeMillis();
		lastInsertAt = -1;
		if (mode != MODE_EDIT) {
			PieLauncherApp.appMenu.lock();
			fadeEdit.fadeIn();
			mode = MODE_EDIT;
		}
	}

	private void releaseIcon() {
		grabbedIcon = null;
		neverDropped = false;
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

	private boolean performAction(Context context, Point at, boolean wasTap,
			boolean wasDoubleTap, boolean wasLongPress) {
		if (mode == MODE_PIE && fadePie.isVisible()) {
			fadeOutMode();
			return performPieAction(context, at,
					wasTap, wasDoubleTap, wasLongPress);
		} else if (mode == MODE_LIST && wasTap) {
			return performListAction(context, at);
		} else if (mode == MODE_EDIT) {
			boolean result = performEditAction(context);
			releaseIcon();
			PieLauncherApp.appMenu.updateSmoothing();
			return result;
		}
		return false;
	}

	private boolean performPieAction(Context context, Point at,
			boolean wasTap, boolean wasDoubleTap, boolean wasLongPress) {
		boolean result = false;
		boolean openList = false;
		AppMenu.AppIcon appIcon = PieLauncherApp.appMenu.getSelectedApp();
		switch (prefs.openListWith()) {
			case Preferences.OPEN_LIST_WITH_ANY_TOUCH:
				openList = wasTap || appIcon == null;
				break;
			case Preferences.OPEN_LIST_WITH_ICON:
				result = openList =
						PieLauncherApp.appMenu.isDrawerIcon(appIcon);
				break;
			case Preferences.OPEN_LIST_WITH_LONG_PRESS:
				openList = wasLongPress;
				break;
			case Preferences.OPEN_LIST_WITH_DOUBLE_TAP:
				openList = wasDoubleTap;
				break;
			case Preferences.OPEN_LIST_WITH_TAP:
			default:
				openList = wasTap;
				break;
		}
		if (openList) {
			if (listListener != null) {
				listListener.onOpenList(false);
			}
		} else if (appIcon != null) {
			PieLauncherApp.appMenu.launchApp(context, appIcon);
			result = true;
		}
		if (result) {
			ripple.set(at);
		}
		return result;
	}

	private boolean performListAction(Context context, Point at) {
		AppMenu.AppIcon appIcon = getListIconAt(at.x, at.y);
		if (appIcon == null) {
			switch (prefs.openListWith()) {
				case Preferences.OPEN_LIST_WITH_TAP:
				case Preferences.OPEN_LIST_WITH_ANY_TOUCH:
				case Preferences.OPEN_LIST_WITH_DOUBLE_TAP:
					if (listListener != null) {
						listListener.onHideList();
					}
					break;
			}
			return false;
		}
		PieLauncherApp.appMenu.launchApp(context, appIcon);
		if (listListener != null) {
			listListener.onHideList();
		}
		ripple.set(at);
		return true;
	}

	private AppMenu.AppIcon getListIconAt(int x, int y) {
		y += getScrollY();
		for (int i = 0, l = getIconCount(); i < l; ++i) {
			AppMenu.AppIcon appIcon = appList.get(i);
			if (appIcon.hitRect.contains(x, y)) {
				return appIcon;
			}
		}
		return null;
	}

	private boolean performEditAction(Context context) {
		if (grabbedIcon == null && contains(iconChangeTwistRect, touch)) {
			twist = getTwistSegment(twist + AppMenu.HALF_PI) *
					(float) AppMenu.HALF_PI;
			updateChangeTwistIcon();
			ripple.set(touch);
			return true;
		} else if (grabbedIcon == null &&
				contains(iconChangeIconScaleRect, touch)) {
			iconScale = iconScale < 1f ? 1f : minIconScale;
			updateChangeIconScaleIcon();
			ripple.set(touch);
			return true;
		} else if (grabbedIcon == null &&
				contains(iconChangeRadiusRect, touch)) {
			radius = getNextRadius(radius);
			PieLauncherApp.appMenu.setRadius(radius);
			updateChangeRadiusIcon();
			ripple.set(touch);
			return true;
		} else if (contains(iconStartRect, touch)) {
			if (grabbedIcon == null) {
				((Activity) context).onBackPressed();
			} else {
				ripple.set(touch);
				removeIconFromPie(grabbedIcon,
						PieLauncherApp.appMenu.isDrawerIcon(
								(AppMenu.AppIcon) grabbedIcon));
			}
			return true;
		} else if (contains(iconCenterRect, touch)) {
			if (grabbedIcon == null) {
				PreferencesActivity.start(context);
			} else {
				ripple.set(touch);
				rollback();
				if (neverDropped) {
					fadeOutMode();
					returnToList();
				}
				if (PieLauncherApp.iconPack.hasPacks()) {
					changeIcon(context, grabbedIcon);
				} else if (PieLauncherApp.appMenu.isDrawerIcon(
						(AppMenu.AppIcon) grabbedIcon)) {
					removeIconFromPie(grabbedIcon, true);
				} else {
					PickIconActivity.askToHide(context,
							((AppMenu.AppIcon) grabbedIcon).componentName);
				}
			}
			return true;
		} else if (contains(iconEndRect, touch)) {
			if (grabbedIcon == null) {
				endEditMode();
			} else {
				ripple.set(touch);
				rollback();
				fadeOutMode();
				if (PieLauncherApp.appMenu.isDrawerIcon(
						(AppMenu.AppIcon) grabbedIcon)) {
					removeIconFromPie(grabbedIcon, true);
				} else {
					PieLauncherApp.appMenu.launchAppInfo(context,
							(AppMenu.AppIcon) grabbedIcon);
				}
			}
			return true;
		}
		return false;
	}

	private void removeIconFromPie(AppMenu.Icon icon, boolean isDrawerIcon) {
		if (isDrawerIcon) {
			prefs.setOpenListWith(Preferences.OPEN_LIST_WITH_TAP);
		}
		PieLauncherApp.appMenu.icons.remove(icon);
		// Undo any rotation if the menu has not otherwise changed.
		if (sameOrder(backup, PieLauncherApp.appMenu.icons)) {
			rollback();
		}
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

	private void updateChangeTwistIcon() {
		iconChangeTwist = Converter.getBitmapFromDrawable(getResources(),
				getDrawableForTwist(twist));
	}

	private static int getDrawableForTwist(float twist) {
		switch (getTwistSegment(twist)) {
			case 1:
				return R.drawable.ic_twist_90;
			case 2:
				return R.drawable.ic_twist_180;
			case 3:
				return R.drawable.ic_twist_270;
			default:
				return R.drawable.ic_twist_0;
		}
	}

	private static int getTwistSegment(double rad) {
		rad = (rad + AppMenu.TAU) % AppMenu.TAU;
		if (rad > 5.497 || rad < .785) {
			return 0;
		} else if (rad < 2.356) {
			return 1;
		} else if (rad < 3.926) {
			return 2;
		}
		return 3;
	}

	private void updateChangeIconScaleIcon() {
		iconChangeIconScale = Converter.getBitmapFromDrawable(getResources(),
				getDrawableForIconScale(iconScale));
	}

	private static int getDrawableForIconScale(float scale) {
		return scale == 1f
				? R.drawable.ic_icon_scale_large
				: R.drawable.ic_icon_scale_small;
	}

	private void updateChangeRadiusIcon() {
		iconChangeRadius = Converter.getBitmapFromDrawable(getResources(),
				getDrawableForRadius(radius));
	}

	private int getNextRadius(int r) {
		switch (getRadiusSegment(r)) {
			case 1:
				return maxRadius;
			case 2:
				return minRadius;
			default:
				return medRadius;
		}
	}

	private int getDrawableForRadius(int r) {
		switch (getRadiusSegment(r)) {
			case 1:
				return R.drawable.ic_radius_medium;
			case 2:
				return R.drawable.ic_radius_large;
			default:
				return R.drawable.ic_radius_small;
		}
	}

	private int getRadiusSegment(int r) {
		int dMin = Math.abs(r - minRadius);
		int dMed = Math.abs(r - medRadius);
		int dMax = Math.abs(r - maxRadius);
		if (dMin < dMed && dMin < dMax) {
			return 0;
		} else if (dMed < dMin && dMed < dMax) {
			return 1;
		}
		return 2;
	}

	private void changeIcon(Context context, AppMenu.Icon icon) {
		AppMenu.AppIcon appIcon = (AppMenu.AppIcon) icon;
		PickIconActivity.start(context, appIcon.componentName);
	}

	private void returnToList() {
		if (listListener != null) {
			listListener.onOpenList(true);
		}
	}

	private void setCenter(int x, int y) {
		PieLauncherApp.appMenu.set(
				clamp(x, radius, viewWidth - radius),
				clamp(y, radius, viewHeight - radius),
				radius,
				twist,
				iconScale);
		lastSelectedIcon = -1;
	}

	private void editIconAt(Point point) {
		int size = PieLauncherApp.appMenu.icons.size();
		for (int i = 0; i < size; ++i) {
			AppMenu.Icon icon = PieLauncherApp.appMenu.icons.get(i);
			float sizeSq = Math.round(icon.size * icon.size);
			if (distSq(point.x, point.y, icon.x, icon.y) < sizeSq) {
				editIcon(icon);
				performHapticFeedbackIfAllowed(HAPTIC_FEEDBACK_DOWN);
				return;
			}
		}
		editActionsFeedback();
	}

	private void editActionsFeedback() {
		if (contains(iconStartRect, touch)) {
			setHighlightedAction(iconStartRect);
		} else if (contains(iconCenterRect, touch)) {
			setHighlightedAction(iconCenterRect);
		} else if (contains(iconEndRect, touch)) {
			setHighlightedAction(iconEndRect);
		} else if (contains(iconChangeTwistRect, touch)) {
			setHighlightedAction(iconChangeTwistRect);
		} else if (contains(iconChangeIconScaleRect, touch)) {
			setHighlightedAction(iconChangeIconScaleRect);
		} else if (contains(iconChangeRadiusRect, touch)) {
			setHighlightedAction(iconChangeRadiusRect);
		} else {
			resetHighlightedAction();
		}
	}

	private boolean drawList(Canvas canvas, float f) {
		if (f <= 0) {
			return false;
		}

		// Manually draw an icon grid because GridView doesn't perform too
		// well on low-end devices and doing it manually gives us more control.
		int innerWidth = viewWidth - listPadding * 2;
		int columns = Math.min(5, innerWidth / (iconSize + spaceBetween));
		boolean showAppNames = showAppNames();
		int iconAndTextHeight = iconSize + (showAppNames
				? iconTextPadding + Math.round(textHeight)
				: 0);
		int cellWidth = innerWidth / columns;
		int cellHeight = iconAndTextHeight + iconTextPadding * 2;
		int wrapX = listPadding + cellWidth * columns;
		int size = getIconCount();

		// Calculate boundaries and offsets.
		int maxTextWidth = cellWidth - iconTextPadding;
		int hpad = (cellWidth - iconSize) / 2;
		int vpad = (cellHeight - iconAndTextHeight) / 2;
		int labelX = cellWidth >> 1;
		int labelY = cellHeight - vpad - Math.round(textOffset);
		int scrollY = getScrollY();
		int viewTop = scrollY - cellHeight;
		int viewBottom = scrollY + viewHeight;
		int x = listPadding;
		int y = searchInputHeight + listPadding;

		// Slide in/out animation.
		y += Math.round((1f - f) * listFadeHeight);
		paintList.setAlpha(Math.round(f * 255f));
		paintText.setAlpha(Math.round(f * alphaText));

		// Draw launch marker.
		if (selectedApp > -1 && size > 0) {
			int offset = Math.min(selectedApp, size - 1);
			int ix = x + offset % columns * cellWidth;
			int iy = y + offset / columns * cellHeight;
			canvas.drawBitmap(iconLaunchFirst,
					ix + labelX - iconLaunchFirstHalf,
					iy - listPadding,
					paintList);
		}

		// Calculate magnification of touched icon.
		int magSize = Math.round(Math.max(cellWidth, cellHeight) * .3f);
		boolean invalidate = f < 1f;
		if (highlightedFrom > 0) {
			float ad = prefs.getAnimationDuration();
			if (ad > 0) {
				long now = SystemClock.uptimeMillis();
				float t = Math.min(1f, (now - highlightedFrom) / ad);
				if (t < 1f) {
					invalidate = true;
				}
				magSize = Math.round(magSize * t);
			} else {
				magSize = 0;
			}
		}

		// Draw icons.
		for (int i = 0; i < size; ++i) {
			if (y > viewTop && y < viewBottom) {
				AppMenu.AppIcon appIcon = appList.get(i);
				appIcon.hitRect.set(x, y, x + cellWidth, y + cellHeight);
				int ix = x + hpad;
				int iy = y + vpad;
				int mag = appIcon == highlightedIcon ? magSize : 0;
				drawRect.set(ix - mag, iy - mag,
						ix + iconSize + mag, iy + iconSize + mag);
				canvas.drawBitmap(appIcon.bitmap, null, drawRect, paintList);
				if (showAppNames) {
					CharSequence label = TextUtils.ellipsize(appIcon.label,
							paintText, maxTextWidth, TextUtils.TruncateAt.END);
					canvas.drawText(label, 0, label.length(),
							x + labelX, y + labelY, paintText);
				}
			}
			x += cellWidth;
			if (x >= wrapX) {
				x = listPadding;
				y += cellHeight;
			}
		}

		int maxHeight = y + listPadding + (x > listPadding ? cellHeight : 0);
		int viewHeightMinusPadding = viewHeight - getPaddingBottom();
		maxScrollY = Math.max(maxHeight - viewHeightMinusPadding, 0);
		return invalidate;
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

	private boolean drawEditor(Canvas canvas, float f) {
		if (f <= 0) {
			return false;
		}

		int alpha = Math.round(f * 255f);
		CanvasPieMenu.paint.setAlpha(alpha);
		paintDropZone.setAlpha(Math.round(f * alphaDropZone));
		paintPressed.setAlpha(Math.round(f * alphaPressed));
		paintAction.setAlpha(alpha);
		paintText.setAlpha(Math.round(f * alphaText));

		boolean hasIcon = grabbedIcon != null;

		// Only draw tips in portrait orientation.
		// There's probably not enough space in landscape.
		if (canvas.getWidth() < canvas.getHeight()) {
			drawTip(canvas, getTip(hasIcon));
		}

		boolean invalidate = f < 1f;
		if (hasIcon) {
			float radius = actionSize;
			float ad = prefs.getAnimationDuration();
			if (ad > 0) {
				long now = SystemClock.uptimeMillis();
				float t = Math.min(1f, (now - grabbedIconAt) / ad);
				if (t < 1f) {
					invalidate = true;
				}
				radius *= t;
			}
			drawAction(canvas, iconRemove, iconStartRect, radius);
			drawAction(canvas, PieLauncherApp.iconPack.hasPacks()
					? iconEdit : iconHide, iconCenterRect, radius);
			drawAction(canvas, iconDetails, iconEndRect, radius);
		} else {
			drawAction(canvas, iconChangeTwist, iconChangeTwistRect);
			drawAction(canvas, iconChangeIconScale, iconChangeIconScaleRect);
			drawAction(canvas, iconChangeRadius, iconChangeRadiusRect);
			drawAction(canvas, iconAdd, iconStartRect);
			drawAction(canvas, iconPreferences, iconCenterRect);
			drawAction(canvas, iconDone, iconEndRect);
		}

		int centerX = viewWidth >> 1;
		int centerY = viewHeight >> 1;
		setCenter(centerX, centerY);

		if (hasIcon) {
			drawEditablePie(centerX, centerY);
		} else {
			PieLauncherApp.appMenu.calculate(centerX, centerY);
		}

		// Invoke drawSmoothed() first to make sure it's always run.
		return PieLauncherApp.appMenu.drawSmoothed(canvas) || invalidate;
	}

	private void drawEditablePie(int centerX, int centerY) {
		int lastIndex = ungrabbedIcons.size();
		double step = AppMenu.TAU / (lastIndex + 1);
		double angle = AppMenu.getPositiveAngle(Math.atan2(
				touch.y - centerY,
				touch.x - centerX) - twist + step * .5);
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
	}

	private boolean drawPieMenu(Canvas canvas, float f) {
		if (f <= 0) {
			return false;
		}

		CanvasPieMenu.paint.setAlpha(Math.round(f * 255f));
		PieLauncherApp.appMenu.calculate(touch.x, touch.y,
				prefs.animateInOut() ? easeSlowerOut(f) : 1f);
		PieLauncherApp.appMenu.draw(canvas);

		int selectedIcon = PieLauncherApp.appMenu.getSelectedIcon();
		if (selectedIcon != lastSelectedIcon) {
			lastSelectedIcon = selectedIcon;
			performHapticFeedbackIfAllowed(HAPTIC_FEEDBACK_CHOICE);
		}

		return f < 1f;
	}

	private String getTip(boolean hasIcon) {
		if (hasIcon) {
			if (contains(iconStartRect, touch)) {
				setHighlightedAction(iconStartRect);
				return removeIconTip;
			} else if (contains(iconCenterRect, touch)) {
				setHighlightedAction(iconCenterRect);
				return PieLauncherApp.iconPack.hasPacks()
						? editAppTip : hideAppTip;
			} else if (contains(iconEndRect, touch)) {
				setHighlightedAction(iconEndRect);
				return removeAppTip;
			}
			resetHighlightedAction();
			return dragToOrderTip;
		}
		return null;
	}

	private void resetHighlightedAction() {
		setHighlightedAction(null);
	}

	private void setHighlightedAction(Rect rect) {
		highlightedAction = rect;
	}

	private void drawTip(Canvas canvas, String tip) {
		if (tip != null) {
			canvas.drawText(tip, viewWidth >> 1,
					controlsPadding + textOffset, paintText);
		}
	}

	private void drawAction(Canvas canvas, Bitmap icon, Rect rect) {
		drawAction(canvas, icon, rect, 0f);
	}

	private void drawAction(Canvas canvas, Bitmap icon, Rect rect,
			float radius) {
		boolean pressed = rect == highlightedAction;
		Paint circlePaint = null;
		if (radius > 0) {
			circlePaint = pressed ? paintPressed : paintDropZone;
		} else if (pressed) {
			radius = actionSize;
			circlePaint = paintPressed;
		}
		if (circlePaint != null) {
			canvas.drawCircle(rect.centerX(), rect.centerY(),
					radius, circlePaint);
		}
		canvas.drawBitmap(icon, null, rect, paintAction);
	}

	private void fadeOutMode() {
		switch (mode) {
			case MODE_PIE:
				fadePie.fadeOut();
				break;
			case MODE_LIST:
				fadeList.fadeOut();
				break;
			case MODE_EDIT:
				fadeEdit.fadeOut();
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

	private boolean contains(Rect rect, Point point) {
		return distSq(rect.centerX(), rect.centerY(), point.x, point.y) <
				actionSizeSq;
	}

	private static float distSq(int ax, int ay, int bx, int by) {
		float dx = ax - bx;
		float dy = ay - by;
		return dx * dx + dy * dy;
	}

	private static double angleOf(float x, float y) {
		return PieMenu.getPositiveAngle(Math.atan2(
				y - PieLauncherApp.appMenu.getCenterY(),
				x - PieLauncherApp.appMenu.getCenterX()));
	}

	private int clampRadius(int r) {
		return clamp(r, minRadius, maxRadius);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int mirror(int v, int axis) {
		return axis - (v - axis);
	}

	private static int getSelectedAppFromTrailingSpace(String s) {
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
		private final int x;
		private final int y;
		private final long time;

		// Required to avoid modifying y, which interferes with isTap().
		private int scrollRef;

		private TouchReference(float x, float y, long time) {
			this.x = Math.round(x);
			this.scrollRef = this.y = Math.round(y);
			this.time = time;
		}
	}

	private static final class Fade {
		private long fadeInFrom;
		private long fadeInTo;
		private long fadeOutFrom;
		private float maxIn = 1f;
		private float minOut = 0f;

		private void fadeIn() {
			fadeIn(SystemClock.uptimeMillis());
		}

		private void fadeIn(long now) {
			fadeInFrom = now;
			fadeInTo = 0;
			fadeOutFrom = 0;
		}

		private void fadeOut() {
			fadeOut(SystemClock.uptimeMillis());
		}

		private void fadeOut(long now) {
			fadeInFrom = 0;
			fadeInTo = 0;
			fadeOutFrom = now;
		}

		private boolean isVisible() {
			return fadeInFrom > 0;
		}

		private boolean isFadingIn(long now) {
			return now < fadeInTo;
		}

		private float get(long now, float duration) {
			if (fadeInTo == 0) {
				fadeInTo = fadeInFrom + Math.round(duration);
			}
			if (duration == 0) {
				return fadeInFrom > 0 ? 1f : 0f;
			}
			if (fadeInFrom > 0) {
				float x = (now - fadeInFrom) / duration;
				maxIn = Math.min(1f, minOut + easeSlowerIn(x));
				return maxIn;
			}
			long delta = now - fadeOutFrom;
			if (delta < duration) {
				minOut = maxIn - easeSlowerOut(delta / duration);
				// Ensure < 1f so invalidate() is invoked one last time.
				minOut = Math.min(.999f, Math.max(0f, minOut));
				return minOut;
			}
			return 0;
		}
	}

	private static float easeSlowerIn(float x) {
		return x * x;
	}

	private static float easeSlowerOut(float x) {
		return 1f - (1f - x) * (1f - x);
	}
}
