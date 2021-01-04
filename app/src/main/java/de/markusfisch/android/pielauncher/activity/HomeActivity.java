package de.markusfisch.android.pielauncher.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.os.BatteryOptimization;
import de.markusfisch.android.pielauncher.view.SoftKeyboard;
import de.markusfisch.android.pielauncher.view.SystemBars;
import de.markusfisch.android.pielauncher.widget.AppPieView;

public class HomeActivity extends Activity {
	private SoftKeyboard kb;
	private GestureDetector gestureDetector;
	private AppPieView pieView;
	private EditText searchInput;
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
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (pieView.inListMode() && gestureDetector.onTouchEvent(ev)) {
			return true;
		}
		try {
			return super.dispatchTouchEvent(ev);
		} catch (IllegalStateException e) {
			// never saw this happen on my devices but had two reportings
			// from Google so it's probably better to catch and ignore this
			// exception than letting the launcher crash
			return false;
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
		showBatteryOptimizationInfoIfNecessary();

		pieView = findViewById(R.id.pie);
		searchInput = findViewById(R.id.search);

		initPieView(res);
		initSearchInput();

		SystemBars.listenForWindowInsets(pieView);
		SystemBars.setTransparentSystemBars(getWindow());
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
			} else if (!isSearchVisible() &&
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

	private void initPieView(Resources res) {
		final int searchBarBackgroundColor = res.getColor(
				R.color.background_search_bar);
		final float searchBarThreshold = res.getDisplayMetrics().density * 48f;
		pieView.setListListener(new AppPieView.ListListener() {
			@Override
			public void onOpenList() {
				showAllApps();
			}

			@Override
			public void onHideList() {
				hideAllApps();
			}

			@Override
			public void onScrollList(int y) {
				y = Math.abs(y);
				if (y > 0) {
					kb.hideFrom(searchInput);
				}
				int color = fadeColor(searchBarBackgroundColor,
						y / searchBarThreshold);
				searchInput.setBackgroundColor(
						pieView.isAppListScrolled() ? color : 0);
			}
		});
		PieLauncherApp.appMenu.setUpdateListener(new AppMenu.UpdateListener() {
			@Override
			public void onUpdate() {
				searchInput.setText(null);
				updateAppList();
			}
		});
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
					updateAppList();
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
							pieView.launchFirstApp();
						}
						hideAllApps();
						return true;
					default:
						return false;
				}
			}
		});
	}

	private void showAllApps() {
		if (isSearchVisible()) {
			return;
		}

		searchInput.setVisibility(View.VISIBLE);
		kb.showFor(searchInput);

		// clear search input
		boolean searchWasEmpty = searchInput.getText().toString().isEmpty();
		updateAfterTextChange = false;
		searchInput.setText(null);
		updateAfterTextChange = true;

		// keeps list state if possible
		if (!searchWasEmpty || pieView.isEmpty()) {
			updateAppList();
		}

		pieView.showList();
	}

	private void hideAllApps() {
		if (isSearchVisible()) {
			searchInput.setVisibility(View.GONE);
			kb.hideFrom(searchInput);
		}
		// ensure the pie menu is initially hidden because on some devices
		// there's not always a matching ACTION_UP/_CANCEL event for every
		// ACTION_DOWN event
		pieView.hideList();
	}

	private boolean isSearchVisible() {
		return searchInput.getVisibility() == View.VISIBLE;
	}

	private void showBatteryOptimizationInfoIfNecessary() {
		if (!BatteryOptimization.isIgnoringBatteryOptimizations(this)) {
			startActivity(new Intent(this, BatteryOptimizationActivity.class));
		}
	}

	private void updateAppList() {
		pieView.filterAppList(searchInput.getText().toString());
	}

	private static int fadeColor(int argb, float fraction) {
		fraction = Math.max(0f, Math.min(1f, fraction));
		int alpha = (argb >> 24) & 0xff;
		int rgb = argb & 0xffffff;
		return rgb | Math.round(fraction * alpha) << 24;
	}

	private class FlingListener extends GestureDetector.SimpleOnGestureListener {
		private int minimumVelocity;

		private FlingListener(int minimumVelocity) {
			this.minimumVelocity = minimumVelocity;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2,
				float velocityX, float velocityY) {
			if (!pieView.isAppListScrolled() &&
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
