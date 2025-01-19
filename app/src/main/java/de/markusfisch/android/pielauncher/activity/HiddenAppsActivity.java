package de.markusfisch.android.pielauncher.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.adapter.HiddenAppsAdapter;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.graphics.BackgroundBlur;
import de.markusfisch.android.pielauncher.graphics.ToolbarBackground;
import de.markusfisch.android.pielauncher.view.SystemBars;
import de.markusfisch.android.pielauncher.widget.Dialog;

public class HiddenAppsActivity extends Activity {
	private final Handler handler = new Handler(Looper.getMainLooper());

	private ToolbarBackground toolbarBackground;
	private View progressView;
	private ListView listView;
	private HiddenAppsAdapter adapter;

	public static void start(Context context) {
		context.startActivity(new Intent(context, HiddenAppsActivity.class));
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		setContentView(R.layout.activity_hidden_apps);

		BackgroundBlur.blurIfTrue(getWindow(),
				PieLauncherApp.getPrefs(this).blurBackground());
		toolbarBackground = new ToolbarBackground(getResources());
		View toolbar = findViewById(R.id.toolbar);
		toolbar.setOnClickListener(v -> finish());
		progressView = findViewById(R.id.progress);

		initListView();

		Window window = getWindow();
		listView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if (visibleItemCount < 1) {
					return;
				}
				int y = 0xffff;
				if (firstVisibleItem == 0) {
					View child = view.getChildAt(firstVisibleItem);
					y = child.getTop() - view.getPaddingTop();
				}
				toolbar.setBackgroundColor(toolbarBackground.getColorForY(y));
			}
		});
		SystemBars.addPaddingFromWindowInsets(toolbar, listView);
		SystemBars.setTransparentSystemBars(window);
		SystemBars.setNavigationBarColor(window, toolbarBackground.backgroundColor);
	}

	private void initListView() {
		listView = findViewById(R.id.apps);
		listView.setEmptyView(findViewById(R.id.no_hidden_apps));
		listView.setOnItemClickListener((parent, view, position, id) -> {
			HiddenAppsAdapter.HiddenApp hiddenApp = adapter.getItem(position);
			if (hiddenApp != null) {
				askToShowApp(hiddenApp.componentName);
			}
		});
		loadHiddenApps();
	}

	private void askToShowApp(ComponentName componentName) {
		Dialog.newDialog(this)
				.setTitle(R.string.unhide_app)
				.setMessage(R.string.want_to_unhide_app)
				.setPositiveButton(android.R.string.ok, (d, w) -> {
					PieLauncherApp.appMenu.hiddenApps.removeAndStore(this,
							componentName.getPackageName());
					PieLauncherApp.appMenu.updateIconsAsync(this);
					loadHiddenApps();
				})
				.setNegativeButton(android.R.string.cancel, (d, w) -> {
				})
				.setNeutralButton(R.string.start_app, (d, w) ->
						AppMenu.launchPackage(this,
								componentName.getPackageName()))
				.show();
	}

	private void loadHiddenApps() {
		progressView.setVisibility(View.VISIBLE);
		Executors.newSingleThreadExecutor().execute(() -> {
			final ArrayList<HiddenAppsAdapter.HiddenApp> hiddenApps =
					new ArrayList<>();
			for (ComponentName componentName :
					PieLauncherApp.appMenu.hiddenApps.componentNames) {
				Pair<String, Drawable> nameAndIcon = getAppNameAndIcon(
						this, componentName.getPackageName());
				if (nameAndIcon != null) {
					hiddenApps.add(new HiddenAppsAdapter.HiddenApp(
							componentName,
							nameAndIcon.first,
							nameAndIcon.second));
				}
			}
			handler.post(() -> {
				progressView.setVisibility(View.GONE);
				adapter = new HiddenAppsAdapter(this, hiddenApps);
				listView.setAdapter(adapter);
			});
		});
	}

	private static Pair<String, Drawable> getAppNameAndIcon(Context context,
			String packageName) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			LauncherApps la = (LauncherApps) context.getSystemService(
					Context.LAUNCHER_APPS_SERVICE);
			for (LauncherActivityInfo info :
					la.getActivityList(packageName, Process.myUserHandle())) {
				return new Pair<>(
						info.getLabel().toString(),
						info.getIcon(0));
			}
		} else {
			PackageManager pm = context.getPackageManager();
			Intent intent = new Intent(Intent.ACTION_MAIN, null);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.setPackage(packageName);
			List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
			for (ResolveInfo info : activities) {
				return new Pair<>(
						info.loadLabel(pm).toString(),
						info.loadIcon(pm));
			}
		}
		return null;
	}
}
