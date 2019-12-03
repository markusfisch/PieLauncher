package de.markusfisch.android.pielauncher.activity;

import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.receiver.PackageEventReceiver;
import de.markusfisch.android.pielauncher.widget.AppPieView;
import de.markusfisch.android.pielauncher.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ListView;

public class HomeActivity extends Activity {
	private static final PackageEventReceiver packageEventReceiver =
			new PackageEventReceiver();

	private AppPieView pieView;
	private View allAppsContainer;
	private ListView appsListView;
	private EditText searchInput;

	@Override
	public void onBackPressed() {
		if (allAppsContainer.getVisibility() == View.VISIBLE) {
			allAppsContainer.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		setContentView(R.layout.activity_home);
		pieView = findViewById(R.id.pie);
		allAppsContainer = findViewById(R.id.all_apps);
		appsListView = findViewById(R.id.apps);
		searchInput = findViewById(R.id.name);

		pieView.setOpenListListener(new AppPieView.OpenListListener() {
			@Override
			public void onOpenList() {
				allAppsContainer.setVisibility(View.VISIBLE);
				searchInput.requestFocus();
			}
		});
		AppPieView.appMenu.setUpdateListener(new AppMenu.UpdateListener() {
			@Override
			public void onUpdate() {
			}
		});

		setTransparentSystemBars(getWindow());
		registerPackageEventReceiver();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterPackageEventReceiver();
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
}
