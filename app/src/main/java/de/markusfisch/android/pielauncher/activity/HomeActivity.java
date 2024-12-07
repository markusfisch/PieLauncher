package de.markusfisch.android.pielauncher.activity;

import java.util.Set;
import java.util.HashSet;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.graphics.ToolbarBackground;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.view.SoftKeyboard;
import de.markusfisch.android.pielauncher.view.SystemBars;
import de.markusfisch.android.pielauncher.widget.AlphabetSidebar;
import de.markusfisch.android.pielauncher.widget.AppPieView;

public class HomeActivity extends Activity {
	private Preferences prefs;
	private SoftKeyboard kb;
	private GestureDetector gestureDetector;
	private ToolbarBackground toolbarBackground;
	private AppPieView pieView;
	private EditText searchInput;
	private ImageView prefsButton;
	private AlphabetSidebar alphabetSidebar;
	private boolean updateAfterTextChange = true;
	private boolean showAllAppsOnResume = false;
	private boolean immersiveMode = false;
	private long pausedAt = 0L;
	private String savedSearchText = "";
	private boolean isAlphabetFiltering = false;

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
		if (ev == null) {
			return false;
		}
		if (pieView.inListMode() && gestureDetector.onTouchEvent(ev)) {
			return true;
		}
		if (ev.getActionMasked() == MotionEvent.ACTION_UP) {
			pieView.resetDragDownList();
		}
		try {
			return super.dispatchTouchEvent(ev);
		} catch (IllegalStateException e) {
			// Never saw this happen on any of my devices but had two reports
			// from Google so it's probably better to catch and ignore this
			// exception than letting the launcher crash.
			return false;
		}
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		prefs = PieLauncherApp.getPrefs(this);
		kb = new SoftKeyboard(this);
		gestureDetector = new GestureDetector(this, new FlingListener(
				ViewConfiguration.get(this).getScaledMinimumFlingVelocity()));

		setContentView(R.layout.activity_home);
		if (!PreferencesActivity.isReady(this)) {
			PreferencesActivity.startWelcome(this);
		}

		toolbarBackground = new ToolbarBackground(getResources());
		pieView = findViewById(R.id.pie);
		searchInput = findViewById(R.id.search);
		prefsButton = findViewById(R.id.preferences);
		alphabetSidebar = findViewById(R.id.alphabet_sidebar);

		initPieView();
		initSearchInput();
		initAlphabetSidebar();

		SystemBars.listenForWindowInsets(pieView,
				(left, top, right, bottom) -> pieView.setPadding(
						left,
						// Never set a top padding because the list should
						// appear under the status bar.
						0,
						right,
						bottom));

		immersiveMode = prefs.isImmersiveMode();
		SystemBars.setTransparentSystemBars(getWindow(), immersiveMode);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.preferences) {
			showPreferences();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// Because this activity has the launch mode "singleTask", it'll get
		// an onNewIntent() when the activity is re-launched.
		if (intent != null &&
				(intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) !=
						Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) {
			// Deal with the gesture navigation/recent apps bug in Android 14:
			// * make the home gesture twice
			// * enter list of recent apps
			// * now an empty, broken screen is in the list of recent apps
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
					isGestureNavigationEnabled()) {
				finish();
				return;
			}
			// If this activity is re-launched but _not_ brought to front,
			// the home button was pressed while this activity was on screen.
			if (pieView.inEditMode()) {
				pieView.endEditMode();
			} else if (!isSearchVisible() &&
					!isGestureNavigationEnabled() &&
					// Only show all apps if the activity was recently paused
					// (by pressing the home button) and _not_ if onPause()
					// was triggered by pressing the overview button and
					// *then* the home button.
					System.currentTimeMillis() - pausedAt < 100) {
				// onNewIntent() is always followed by onResume()
				showAllAppsOnResume = true;
			}
		}
	}

	@Override
	public void onRestart() {
		super.onRestart();
		if (prefs.forceRelaunch()) {
			finish();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		setRequestedOrientation(prefs.getOrientation());
	}

	@Override
	protected void onResume() {
		super.onResume();
		updatePrefsButton();
		updateSystemBars();
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

	private void initPieView() {
		pieView.setWindow(getWindow());
		pieView.setListListener(new AppPieView.ListListener() {
			@Override
			public void onOpenList(boolean resume) {
				showAllAppsOnResume = resume;
				showAllApps();
				showAlphabetSidebar();
			}

			@Override
			public void onHideList() {
				hideAllApps();
			}

			@Override
			public void onScrollList(int y, boolean isScrolling) {
				if (isScrolling && y != 0) {
					kb.hideFrom(searchInput);
					hidePrefsButton();
				}
				searchInput.setBackgroundColor(toolbarBackground.getColor(y));
			}
		});
		PieLauncherApp.appMenu.setUpdateListener(() -> {
			searchInput.getText().clear();
			updateAppList();
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
				if (!updateAfterTextChange) {
					return;
				} else if (e.length() > 0) {
					hidePrefsButton();
				}
				String s = e.toString();
				if (s.equals("..")) {
					e.clear();
					showPreferences();
					return;
				} else if (s.equals(",,")) {
					e.clear();
					showEditor();
					return;
				}
				// Replace ". " before updateAppList().
				if (endsWithDoubleSpace(e)) {
					pieView.launchSelectedApp();
				}
				updateAppList();
				// Check icon count after the list was updated.
				if (prefs.autoLaunchMatching() &&
						pieView.getIconCount() == 1) {
					pieView.launchSelectedApp();
				}
			}
		});
		searchInput.setOnEditorActionListener((v, actionId, event) -> {
			switch (actionId) {
				case EditorInfo.IME_ACTION_GO:
				case EditorInfo.IME_ACTION_SEND:
				case EditorInfo.IME_ACTION_DONE:
				case EditorInfo.IME_ACTION_NEXT:
				case EditorInfo.IME_ACTION_SEARCH:
				case EditorInfo.IME_NULL:
					if (!searchInput.getText().toString().isEmpty()) {
						pieView.launchSelectedApp();
					}
					hideAllApps();
					return true;
				default:
					return false;
			}
		});

		// Clear alphabet filtering on any search input interaction
		View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
			if (hasFocus && isAlphabetFiltering) {
				clearAlphabetFiltering();
			}
		};
		searchInput.setOnFocusChangeListener(focusListener);
		searchInput.setOnClickListener(v -> {
			if (isAlphabetFiltering) {
				clearAlphabetFiltering();
			}
		});
	}

	private void clearAlphabetFiltering() {
		isAlphabetFiltering = false;
		PieLauncherApp.appMenu.setAlphabetFiltering(false);
		if (alphabetSidebar != null) {
			alphabetSidebar.clearSelection();
		}
		updateAppList();
	}

	private void initAlphabetSidebar() {
		alphabetSidebar.setOnLetterSelectedListener(new AlphabetSidebar.OnLetterSelectedListener() {
			@Override
			public void onLetterSelected(String letter, Set<String> aliases) {
				if (!isAlphabetFiltering) {
					isAlphabetFiltering = true;
					PieLauncherApp.appMenu.setAlphabetFiltering(true);
				}
				// Hide keyboard if visible
				InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				if (imm != null) {
					imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
				}
				pieView.filterAppList(letter, true, aliases);
			}

			@Override
			public void onLetterDeselected() {
				isAlphabetFiltering = false;
				PieLauncherApp.appMenu.setAlphabetFiltering(false);
				pieView.filterAppList("", false, new HashSet<>());
			}
		});
	}

	private boolean endsWithDoubleSpace(Editable e) {
		boolean doubleSpaceLaunch = prefs.doubleSpaceLaunch();
		String s = e.toString();
		if ((doubleSpaceLaunch && s.endsWith("  ")) ||
				// Some keyboards auto-replace two spaces with ". ",
				// which means a new, different search result.
				s.endsWith(". ")) {
			updateAfterTextChange = false;
			e.clear();
			e.append(s.substring(0, s.length() - 2));
			if (!doubleSpaceLaunch) {
				// Restore the two spaces for moving the selection.
				e.append("  ");
			}
			updateAfterTextChange = true;
			return doubleSpaceLaunch;
		}
		return false;
	}

	private void updatePrefsButton() {
		if (prefs.getIconPress() == Preferences.ICON_PRESS_MENU) {
			prefsButton.setImageResource(R.drawable.ic_edit);
			prefsButton.setOnClickListener(v -> showEditor());
			prefsButton.setOnLongClickListener(v -> {
				showPreferences();
				return true;
			});
		} else {
			prefsButton.setImageResource(R.drawable.ic_preferences);
			prefsButton.setOnClickListener(v -> showPreferences());
			prefsButton.setOnLongClickListener(v -> {
				showEditor();
				return true;
			});
		}
	}

	private void updateSystemBars() {
		boolean newImmersiveMode = prefs.isImmersiveMode();
		if (immersiveMode != newImmersiveMode) {
			immersiveMode = newImmersiveMode;
			SystemBars.setSystemUIVisibility(getWindow(), immersiveMode);
		}
	}

	private void showPreferences() {
		// Hide searchInput to avoid inactive InputConnection.
		hideAllApps();
		PreferencesActivity.start(this);
		showAllAppsOnResume = true;
	}

	private void showEditor() {
		hideAllApps();
		pieView.showEditor();
	}

	private void hidePrefsButton() {
		if (prefsButton.getVisibility() == View.VISIBLE) {
			prefsButton.setVisibility(View.GONE);
		}
	}

	private void showAlphabetSidebar() {
		alphabetSidebar.setVisibility(View.VISIBLE);
	}

	private void hideAlphabetSidebar() {
		alphabetSidebar.setVisibility(View.GONE);
	}

	private void showAllApps() {
		if (isSearchVisible()) {
			return;
		}

		searchInput.setVisibility(View.VISIBLE);
		prefsButton.setVisibility(View.VISIBLE);
		if (prefs.displayKeyboard()) {
			kb.showFor(searchInput);
		}

		// Clear search input.
		Editable editable = searchInput.getText();
		boolean searchWasEmpty = editable.toString().isEmpty();
		updateAfterTextChange = false;
		editable.clear();
		updateAfterTextChange = true;

		// Remove filter and reset last scroll position.
		if (!searchWasEmpty || pieView.isEmpty()) {
			updateAppList();
		}

		pieView.showList();
	}

	private void hideAllApps() {
		if (isSearchVisible()) {
			searchInput.setVisibility(View.GONE);
			kb.hideFrom(searchInput);
			hidePrefsButton();
			hideAlphabetSidebar();
		}
		// Ensure the pie menu is initially hidden because on some devices
		// there's not always a matching ACTION_UP/_CANCEL event for every
		// ACTION_DOWN event.
		pieView.hideList();
	}

	private boolean isSearchVisible() {
		return searchInput.getVisibility() == View.VISIBLE;
	}

	private boolean isGestureNavigationEnabled() {
		return Settings.Secure.getInt(getContentResolver(),
				"navigation_mode", 0) == 2;
	}

	private void updateAppList() {
		pieView.filterAppList(searchInput.getText().toString(), isAlphabetFiltering, new HashSet<>());
	}

	private class FlingListener extends GestureDetector.SimpleOnGestureListener {
		private final int minimumVelocity;

		private FlingListener(int minimumVelocity) {
			this.minimumVelocity = minimumVelocity;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			if (pieView.appListNotScrolled() &&
					Math.abs(distanceY) > Math.abs(distanceX)) {
				pieView.dragDownListBy(distanceY);
			}
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2,
				float velocityX, float velocityY) {
			if (pieView.appListNotScrolled() &&
					velocityY > velocityX &&
					velocityY >= minimumVelocity &&
					e1 != null && e2 != null &&
					e2.getY() - e1.getY() > 0) {
				pieView.resetScroll();
				hideAllApps();
				return true;
			}
			return false;
		}
	}
}
