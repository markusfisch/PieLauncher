package de.markusfisch.android.pielauncher.activity;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.adapter.AppsAdapter;
import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.receiver.PackageEventReceiver;
import de.markusfisch.android.pielauncher.widget.AppPieView;
import de.markusfisch.android.pielauncher.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Resources;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class HomeActivity extends Activity {
	private static final PackageEventReceiver packageEventReceiver =
			new PackageEventReceiver();

	private final Point touch = new Point();

	private InputMethodManager imm;
	private GestureDetector gestureDetector;
	private AppPieView pieView;
	private View allAppsContainer;
	private ListView appsListView;
	private EditText searchInput;
	private AppsAdapter appsAdapter;
	private boolean isScrolled = false;
	private boolean updateAfterTextChange = true;
	private int searchBarBackgroundColor;

	@Override
	public void onBackPressed() {
		if (pieView.isEditMode()) {
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

		imm = (InputMethodManager) getSystemService(
				Context.INPUT_METHOD_SERVICE);
		gestureDetector = new GestureDetector(this, new FlingListener(
				ViewConfiguration.get(this).getScaledMinimumFlingVelocity()));
		searchBarBackgroundColor = res.getColor(
				R.color.background_search_bar);

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
		registerPackageEventReceiver();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// end edit mode when HOME is pressed
		if (pieView.isEditMode() && intent != null &&
				(intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) !=
						Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) {
			pieView.endEditMode();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		hideAllApps();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterPackageEventReceiver();
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
				if (pieView.isEditMode()) {
					pieView.dispatchTouchEvent(event);
					return true;
				}
				if (gestureDetector.onTouchEvent(event)) {
					return true;
				}
				switch (event.getActionMasked()) {
					default: break; // make FindBugs happy
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
					pieView.addIconInteractive(appIcon, touch);
					hideAllApps();
				}
				return false;
			}
		});
		appsListView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScroll(final AbsListView view,
					final int firstVisibleItem,
					final int visibleItemCount,
					final int totalItemCount) {
				// give Android some time to settle down before running this;
				// not putting it on the queue makes it only work sometimes
				view.post(new Runnable() {
					@Override
					public void run() {
						isScrolled = firstVisibleItem > 0 ||
								(totalItemCount > 0 && getTopOfFirstChild(view) < 0);
						searchInput.setBackgroundColor(
								isScrolled ? searchBarBackgroundColor : 0);
					}
				});
			}

			@Override
			public void onScrollStateChanged(AbsListView view,
					int scrollState) {
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

	private void launchFirstApp() {
		AppMenu.AppIcon icon;
		if (appsAdapter.getCount() > 0 &&
				(icon = appsAdapter.getItem(0)) != null) {
			PieLauncherApp.appMenu.launchApp(this, icon);
		}
	}

	private void showAllApps() {
		pieView.setVisibility(View.GONE);
		allAppsContainer.setVisibility(View.VISIBLE);
		showSoftKeyboardFor(searchInput);

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
		if (allAppsContainer.getVisibility() == View.VISIBLE) {
			allAppsContainer.setVisibility(View.GONE);
			hideSoftKeyboardFrom(searchInput);
		}
		pieView.setVisibility(View.VISIBLE);
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

	private void registerPackageEventReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_LOCALE_CHANGED);
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
		filter.addDataScheme("package");
		filter.addDataScheme("file");
		registerReceiver(packageEventReceiver, filter);
	}

	private void unregisterPackageEventReceiver() {
		unregisterReceiver(packageEventReceiver);
	}

	private void showSoftKeyboardFor(EditText editText) {
		editText.requestFocus();
		imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
	}

	private void hideSoftKeyboardFrom(View view) {
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
			if (isScrolled) {
				return false;
			}
			boolean hide = velocityY > velocityX &&
					velocityY >= minimumVelocity &&
					e2.getY() - e1.getY() > 0;
			if (hide) {
				hideAllApps();
			}
			return hide;
		}
	}
}
