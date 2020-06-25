package de.markusfisch.android.pielauncher.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.adapter.AppsAdapter;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.view.SoftKeyboard;
import de.markusfisch.android.pielauncher.widget.AppPieView;

public class HomeActivity extends Activity {
	private final Point touch = new Point();

	private SoftKeyboard kb;
	private GestureDetector gestureDetector;
	private AppPieView pieView;
	private View allAppsContainer;
	private ListView appsListView;
	private EditText searchInput;
	private AppsAdapter appsAdapter;
	private boolean isScrolled = false;
	private boolean updateAfterTextChange = true;
	private boolean showAllAppsOnResume = false;
	private long pausedAt = 0L;

	@Override
	public void onBackPressed() {
		if (pieView.inEditMode()) {
			pieView.endEditMode();
			showAllApps();
		} else {
			hideAllApps();
		}
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		Resources res = getResources();

		// restrict to current orientation, whatever that is;
		// makes it possible to use it on tablets in landscape and
		// in portrait for phones; should become a choice at some point
		setRequestedOrientation(res.getConfiguration().orientation);

		kb = new SoftKeyboard(this);
		gestureDetector = new GestureDetector(this, new FlingListener(
				ViewConfiguration.get(this).getScaledMinimumFlingVelocity()));

		setContentView(R.layout.activity_home);

		pieView = findViewById(R.id.pie);
		allAppsContainer = findViewById(R.id.all_apps);
		appsListView = findViewById(R.id.apps);
		searchInput = findViewById(R.id.name);

		pieView.setOpenListListener(new AppPieView.OpenListListener() {
			@Override
			public void onOpenList() {
				showAllApps();
			}
		});
		PieLauncherApp.appMenu.setUpdateListener(new AppMenu.UpdateListener() {
			@Override
			public void onUpdate() {
				searchInput.setText(null);
				updateAppsAdapter();
			}
		});
		initSearchInput();
		initAppListView();

		setTransparentSystemBars(getWindow());
		listenForWindowInsets(appsListView);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// because this activity has the launch mode "singleTask", it'll get
		// an onNewIntent() when the activity is re-launched
		if (intent != null &&
				(intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) !=
						Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) {
			// if this activity is re-launched but _not_ brought to front,
			// the home button was pressed while this activity was on screen
			if (pieView.inEditMode()) {
				pieView.endEditMode();
			} else if (!isAllAppsVisible() &&
					// only show all apps if the activity was recently paused
					// (by pressing the home button) and _not_ if onPause()
					// was triggered by pressing the overview button and
					// *then* the home button
					System.currentTimeMillis() - pausedAt < 100) {
				// onNewIntent() is always followed by onResume()
				showAllAppsOnResume = true;
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
			// unfortunately, on Android Nougat, it's neccessary to
			// re-register the receiver on a regular basis or it will
			// silently stop working after a while
			((PieLauncherApp) getApplicationContext())
					.registerPackageEventReceiver();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (showAllAppsOnResume) {
			showAllApps();
			showAllAppsOnResume = false;
		} else {
			hideAllApps();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		pausedAt = System.currentTimeMillis();
	}

	private void initSearchInput() {
		searchInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start,
					int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start,
					int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable e) {
				if (updateAfterTextChange) {
					updateAppsAdapter();
				}
			}
		});
		searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				switch (actionId) {
					case EditorInfo.IME_ACTION_GO:
					case EditorInfo.IME_ACTION_SEND:
					case EditorInfo.IME_ACTION_DONE:
					case EditorInfo.IME_ACTION_NEXT:
					case EditorInfo.IME_NULL:
						if (searchInput.getText().toString().length() > 0) {
							launchFirstApp();
						}
						hideAllApps();
						return true;
					default:
						return false;
				}
			}
		});
	}

	// this onTouchListener is just for dispatching events
	@SuppressLint("ClickableViewAccessibility")
	private void initAppListView() {
		appsListView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (pieView.inEditMode()) {
					pieView.dispatchTouchEvent(event);
					return true;
				}
				if (gestureDetector.onTouchEvent(event)) {
					return true;
				}
				switch (event.getActionMasked()) {
					default:
						break; // make FindBugs happy
					case MotionEvent.ACTION_DOWN:
					case MotionEvent.ACTION_MOVE:
						touch.set((int) event.getRawX(),
								(int) event.getRawY());
						break;
				}
				return false;
			}
		});
		appsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(
					AdapterView<?> parent,
					View view,
					int position,
					long id) {
				PieLauncherApp.appMenu.launchApp(HomeActivity.this,
						appsAdapter.getItem(position - 1));
				hideAllApps();
			}
		});
		appsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(
					AdapterView<?> parent,
					View view,
					int position,
					long id) {
				AppMenu.AppIcon appIcon = appsAdapter.getItem(position - 1);
				if (appIcon != null) {
					// it's important to call hideAllApps() first because
					// it invalidates the touch position addIconInteractive()
					// tries to set
					hideAllApps();
					pieView.addIconInteractive(appIcon, touch);
				}
				return false;
			}
		});
		appsListView.setOnScrollListener(new AbsListView.OnScrollListener() {
			private int searchBarBackgroundColor;
			private float searchBarThreshold;

			@Override
			public void onScroll(final AbsListView view,
					final int firstVisibleItem,
					final int visibleItemCount,
					final int totalItemCount) {
				if (searchBarBackgroundColor == 0) {
					init(view.getResources());
				}
				// give Android some time to settle down before running this;
				// not putting it on the queue makes it only work sometimes
				view.post(new Runnable() {
					@Override
					public void run() {
						int color;
						int y;
						if (firstVisibleItem > 0) {
							isScrolled = true;
							color = searchBarBackgroundColor;
						} else if (totalItemCount > 0 &&
								(y = getTopOfFirstChild(view)) < 0) {
							isScrolled = true;
							y = Math.abs(y);
							if (y < searchBarThreshold) {
								kb.hideFrom(searchInput);
							}
							color = fadeColor(searchBarBackgroundColor,
									y / searchBarThreshold);
						} else {
							isScrolled = false;
							color = 0;
						}
						searchInput.setBackgroundColor(color);
					}
				});
			}

			@Override
			public void onScrollStateChanged(AbsListView view,
					int scrollState) {
			}

			private void init(Resources res) {
				searchBarBackgroundColor = res.getColor(
						R.color.background_search_bar);
				searchBarThreshold = res.getDisplayMetrics().density * 48f;
			}
		});
		LayoutInflater inflater = getLayoutInflater();
		appsListView.addHeaderView(inflater.inflate(R.layout.list_header,
				appsListView, false), null, false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			appsListView.addFooterView(inflater.inflate(R.layout.list_footer,
					appsListView, false), null, false);
		}
	}

	private static int getTopOfFirstChild(AbsListView view) {
		View child = view != null ? view.getChildAt(0) : null;
		return child != null ? child.getTop() : 0;
	}

	private static int fadeColor(int argb, float fraction) {
		fraction = Math.max(0f, Math.min(1f, fraction));
		int alpha = (argb >> 24) & 0xff;
		int rgb = argb & 0xffffff;
		return rgb | Math.round(fraction * alpha) << 24;
	}

	private void launchFirstApp() {
		AppMenu.AppIcon icon;
		if (appsAdapter.getCount() > 0 &&
				(icon = appsAdapter.getItem(0)) != null) {
			PieLauncherApp.appMenu.launchApp(this, icon);
		}
	}

	private void showAllApps() {
		if (isAllAppsVisible()) {
			return;
		}

		pieView.setVisibility(View.GONE);
		allAppsContainer.setVisibility(View.VISIBLE);
		kb.showFor(searchInput);

		// clear search input
		boolean searchWasEmpty = searchInput.getText().toString().isEmpty();
		updateAfterTextChange = false;
		searchInput.setText(null);
		updateAfterTextChange = true;

		// keeps list state if possible
		if (!searchWasEmpty || appsListView.getAdapter() == null) {
			updateAppsAdapter();
		}
	}

	private void hideAllApps() {
		if (isAllAppsVisible()) {
			allAppsContainer.setVisibility(View.GONE);
			kb.hideFrom(searchInput);
		}
		// ensure the pie menu is initially hidden because on some devices
		// there's not always a matching ACTION_UP/_CANCEL event for every
		// ACTION_DOWN event
		pieView.hideMenu();
		pieView.setVisibility(View.VISIBLE);
	}

	private boolean isAllAppsVisible() {
		return allAppsContainer.getVisibility() == View.VISIBLE;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static void setTransparentSystemBars(Window window) {
		if (window == null ||
				Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return;
		}
		window.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
						View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		window.setStatusBarColor(0);
		window.setNavigationBarColor(0);
	}

	@TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
	private static void listenForWindowInsets(View view) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) {
			return;
		}
		view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
			@Override
			public WindowInsets onApplyWindowInsets(View v,
					WindowInsets insets) {
				if (insets.hasSystemWindowInsets()) {
					v.setPadding(
							insets.getSystemWindowInsetLeft(),
							// never set a top padding because the list should
							// appear under the status bar
							0,
							insets.getSystemWindowInsetRight(),
							insets.getSystemWindowInsetBottom());
				}
				return insets.consumeSystemWindowInsets();
			}
		});
	}

	private void updateAppsAdapter() {
		String query = searchInput.getText().toString();
		appsAdapter = new AppsAdapter(
				PieLauncherApp.appMenu.filterAppsBy(query));
		appsListView.setAdapter(appsAdapter);
	}

	private class FlingListener extends GestureDetector.SimpleOnGestureListener {
		private int minimumVelocity;

		private FlingListener(int minimumVelocity) {
			this.minimumVelocity = minimumVelocity;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2,
				float velocityX, float velocityY) {
			if (!isScrolled &&
					velocityY > velocityX &&
					velocityY >= minimumVelocity &&
					e2.getY() - e1.getY() > 0) {
				hideAllApps();
				return true;
			}
			return false;
		}
	}
}
