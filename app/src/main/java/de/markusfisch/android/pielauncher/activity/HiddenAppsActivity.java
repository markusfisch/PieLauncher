package de.markusfisch.android.pielauncher.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.adapter.HiddenAppsAdapter;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.AppLauncher;
import de.markusfisch.android.pielauncher.graphics.BackgroundBlur;
import de.markusfisch.android.pielauncher.graphics.ToolbarBackground;
import de.markusfisch.android.pielauncher.view.SystemBars;
import de.markusfisch.android.pielauncher.widget.Dialog;
import de.markusfisch.android.pielauncher.widget.Toaster;

public class HiddenAppsActivity extends Activity {
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final ExecutorService executor =
			Executors.newSingleThreadExecutor();

	private ToolbarBackground toolbarBackground;
	private View progressView;
	private ListView listView;
	private HiddenAppsAdapter adapter;
	private AlertDialog optionsDialog;
	private int loadGeneration;
	private final BroadcastReceiver profileReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			loadHiddenApps();
		}
	};

	public static void start(Context context) {
		context.startActivity(new Intent(context, HiddenAppsActivity.class));
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		setContentView(R.layout.activity_hidden_apps);

		BackgroundBlur.setBlurRadius(getWindow(),
				PieLauncherApp.getPrefs(this).getBackgroundBlurRadius());
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

	@Override
	protected void onStart() {
		super.onStart();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
			filter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				filter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
				filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
				filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED);
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				filter.addAction(Intent.ACTION_PROFILE_ACCESSIBLE);
				filter.addAction(Intent.ACTION_PROFILE_INACCESSIBLE);
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
				filter.addAction(Intent.ACTION_PROFILE_ADDED);
				filter.addAction(Intent.ACTION_PROFILE_REMOVED);
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
				filter.addAction(Intent.ACTION_PROFILE_AVAILABLE);
				filter.addAction(Intent.ACTION_PROFILE_UNAVAILABLE);
			}
			registerReceiver(profileReceiver, filter);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadHiddenApps(); // Because the list may have changed.
	}

	@Override
	protected void onStop() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			unregisterReceiver(profileReceiver);
		}
		++loadGeneration;
		clearHiddenApps();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		executor.shutdownNow();
	}

	private void initListView() {
		listView = findViewById(R.id.apps);
		listView.setEmptyView(findViewById(R.id.no_hidden_apps));
		listView.setOnItemClickListener((parent, view, position, id) -> {
			HiddenAppsAdapter.HiddenApp hiddenApp = adapter != null
					? adapter.getItem(position) : null;
			if (hiddenApp != null && isProfileAvailable(hiddenApp.userHandle)) {
				showHiddenAppOptions(hiddenApp);
			}
		});
	}

	private void showHiddenAppOptions(HiddenAppsAdapter.HiddenApp hiddenApp) {
		ComponentName componentName = hiddenApp.componentName;
		ArrayList<String> list = new ArrayList<>();
		list.add(getString(R.string.unhide_app));
		list.add(getString(R.string.start_app));
		list.add(getString(R.string.show_app_info));
		optionsDialog = Dialog.newDialog(this)
				.setTitle(R.string.edit_app)
				.setItems(list.toArray(new CharSequence[0]), (view, which) -> {
					if (!isProfileAvailable(hiddenApp.userHandle)) {
						loadHiddenApps();
						return;
					}
					try {
						switch (which) {
							case 0:
								PieLauncherApp.apps.hiddenAppsStorage.removeAndStore(
										this, componentName.getPackageName());
								PieLauncherApp.apps.updateIconsAsync(this);
								loadHiddenApps();
								break;
							case 1:
								if (AppLauncher.HAS_LAUNCHER_APP) {
									AppLauncher.getLauncherApps(this).startMainActivity(
											componentName, hiddenApp.userHandle, null, null);
									break;
								}
								AppLauncher.launchPackage(this,
										componentName.getPackageName());
								break;
							case 2:
								if (AppLauncher.HAS_LAUNCHER_APP) {
									AppLauncher.getLauncherApps(this).startAppDetailsActivity(
											componentName, hiddenApp.userHandle, null, null);
									break;
								}
								AppLauncher.launchAppInfo(this,
										componentName.getPackageName());
								break;
						}
					} catch (Exception e) {
						Toaster.toast(this, R.string.activity_not_enabled);
					}
				}).show();
		optionsDialog.setOnDismissListener(d -> optionsDialog = null);
	}

	private void clearHiddenApps() {
		if (optionsDialog != null) {
			optionsDialog.dismiss();
			optionsDialog = null;
		}
		adapter = null;
		listView.setAdapter(null);
	}

	private void loadHiddenApps() {
		int generation = ++loadGeneration;
		clearHiddenApps();
		progressView.setVisibility(View.VISIBLE);
		executor.execute(() -> {
			PieLauncherApp.apps.hiddenAppsStorage.restore(this);
			List<UserHandle> profiles = AppLauncher.HAS_LAUNCHER_APP
					? AppLauncher.getProfiles(AppLauncher.getUserManager(this))
					: Collections.singletonList(null);
			final ArrayList<HiddenAppsAdapter.HiddenApp> hiddenApps =
					new ArrayList<>();
			for (ComponentName componentName :
					PieLauncherApp.apps.hiddenAppsStorage.copyComponentNames()) {
				for (UserHandle profile : profiles) {
					if (!isProfileAvailable(profile)) {
						continue;
					}
					Pair<String, Drawable> nameAndIcon = getAppNameAndIcon(
							this, componentName.getPackageName(), profile);
					if (nameAndIcon != null) {
						hiddenApps.add(new HiddenAppsAdapter.HiddenApp(
								componentName,
								nameAndIcon.first,
								nameAndIcon.second, profile));
					}
				}
			}
			handler.post(() -> {
				if (generation != loadGeneration) {
					return;
				}
				// A profile may have locked while its icons were loading.
				for (Iterator<HiddenAppsAdapter.HiddenApp> it = hiddenApps.iterator();
						it.hasNext(); ) {
					if (!isProfileAvailable(it.next().userHandle)) {
						it.remove();
					}
				}
				progressView.setVisibility(View.GONE);
				adapter = new HiddenAppsAdapter(this, hiddenApps);
				listView.setAdapter(adapter);
			});
		});
	}

	private boolean isProfileAvailable(UserHandle user) {
		if (user == null) {
			return true;
		}
		try {
			return !AppLauncher.isPrivateProfileLocked(this, user) &&
					(Build.VERSION.SDK_INT < Build.VERSION_CODES.N ||
							AppLauncher.getUserManager(this).isUserUnlocked(user));
		} catch (Exception e) {
			return false;
		}
	}

	private static Pair<String, Drawable> getAppNameAndIcon(
			Context context,
			String packageName,
			UserHandle userHandle) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			LauncherApps la = (LauncherApps) context.getSystemService(
					Context.LAUNCHER_APPS_SERVICE);
			try {
				for (LauncherActivityInfo info :
						AppLauncher.getActivityList(la, packageName, userHandle)) {
					return new Pair<>(
							info.getLabel().toString(),
							info.getBadgedIcon(0));
				}
			} catch (Exception e) {
				// The profile or package may have become unavailable.
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
