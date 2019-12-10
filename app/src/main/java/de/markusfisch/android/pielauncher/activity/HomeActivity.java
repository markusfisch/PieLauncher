package de.markusfisch.android.pielauncher.activity;

import de.markusfisch.android.pielauncher.adapter.AppsAdapter;
import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.receiver.PackageEventReceiver;
import de.markusfisch.android.pielauncher.widget.AppPieView;
import de.markusfisch.android.pielauncher.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class HomeActivity extends Activity {
	private static final PackageEventReceiver packageEventReceiver =
			new PackageEventReceiver();

	private final Point touch = new Point();

	private InputMethodManager imm;
	private AppPieView pieView;
	private View allAppsContainer;
	private ListView appsListView;
	private EditText searchInput;
	private AppsAdapter appsAdapter;

	@Override
	public void onBackPressed() {
		hideAllApps();
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		imm = (InputMethodManager) getSystemService(
				Context.INPUT_METHOD_SERVICE);

		setContentView(R.layout.activity_home);

		pieView = findViewById(R.id.pie);
		allAppsContainer = findViewById(R.id.all_apps);
		appsListView = findViewById(R.id.apps);
		searchInput = findViewById(R.id.name);

		pieView.setOpenListListener(new AppPieView.OpenListListener() {
			@Override
			public void onOpenList() {
				allAppsContainer.setVisibility(View.VISIBLE);
				showSoftKeyboardFor(searchInput);
				updateApps();
			}
		});
		AppPieView.appMenu.setUpdateListener(new AppMenu.UpdateListener() {
			@Override
			public void onUpdate() {
				searchInput.setText(null);
				updateApps();
			}
		});
		initSearchInput();
		initAppListView();

		setTransparentSystemBars(getWindow());
		registerPackageEventReceiver();
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
				updateApps();
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
						return launchFirstApp();
					default:
						return false;
				}
			}
		});
	}

	private void initAppListView() {
		appsListView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getActionMasked()) {
					case MotionEvent.ACTION_MOVE:
						touch.set((int) event.getRawX(),
								(int) event.getRawX());
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
				appsAdapter.getItem(position).launch(HomeActivity.this);
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
				pieView.enterEditMode(touch);
				hideAllApps();
				return false;
			}
		});
	}

	private boolean launchFirstApp() {
		AppMenu.AppIcon icon = appsAdapter.getItem(0);
		if (icon == null) {
			return false;
		}
		icon.launch(this);
		hideAllApps();
		return true;
	}

	private void hideAllApps() {
		if (allAppsContainer.getVisibility() == View.VISIBLE) {
			allAppsContainer.setVisibility(View.GONE);
			searchInput.setText(null);
			hideSoftKeyboardFrom(searchInput);
		}
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static boolean setTransparentSystemBars(Window window) {
		if (window == null ||
				Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return false;
		}
		window.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		window.setStatusBarColor(0);
		window.setNavigationBarColor(0);
		return true;
	}

	private void registerPackageEventReceiver() {
		IntentFilter filter = new IntentFilter();
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

	private void updateApps() {
		String query = searchInput.getText().toString();
		appsAdapter = new AppsAdapter(pieView.appMenu.filterAppsBy(query));
		appsListView.setAdapter(appsAdapter);
	}
}
