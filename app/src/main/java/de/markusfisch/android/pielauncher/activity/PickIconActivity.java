package de.markusfisch.android.pielauncher.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.adapter.PickIconAdapter;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.graphics.BackgroundBlur;
import de.markusfisch.android.pielauncher.graphics.IconPack;
import de.markusfisch.android.pielauncher.graphics.ToolbarBackground;
import de.markusfisch.android.pielauncher.view.SoftKeyboard;
import de.markusfisch.android.pielauncher.view.SystemBars;
import de.markusfisch.android.pielauncher.widget.OptionsDialog;

public class PickIconActivity extends Activity {
	public interface OnHideListener {
		void onHide();
	}

	private static final String PACKAGE_NAME = "package_name";

	private final Handler handler = new Handler(Looper.getMainLooper());

	private SoftKeyboard kb;
	private ToolbarBackground toolbarBackground;
	private View progressView;
	private String iconPackPackageName;
	private GridView gridView;
	private EditText searchInput;
	private ArrayList<String> drawableNames;
	private PickIconAdapter iconAdapter;

	public static void start(Context context, String packageName) {
		Intent intent = new Intent(context, PickIconActivity.class);
		intent.putExtra(PACKAGE_NAME, packageName);
		context.startActivity(intent);
	}

	public static void askToHide(Context context, String packageName) {
		askToHide(context, packageName, null);
	}

	public static void askToHide(Context context, String packageName,
			OnHideListener hideListener) {
		new AlertDialog.Builder(context)
				.setTitle(R.string.hide_app)
				.setMessage(R.string.want_to_hide_app)
				.setPositiveButton(android.R.string.ok, (d, w) -> {
					PieLauncherApp.appMenu.hiddenApps.addAndStore(context,
							packageName);
					PieLauncherApp.appMenu.updateIconsAsync(context);
					if (hideListener != null) {
						hideListener.onHide();
					}
				})
				.setNegativeButton(android.R.string.cancel, (d, w) -> {
				})
				.show();
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		Intent intent = getIntent();
		String packageName;
		if (intent == null || (packageName = intent.getStringExtra(
				PACKAGE_NAME)) == null) {
			finish();
			return;
		}

		setContentView(R.layout.activity_pick_icon);

		BackgroundBlur.blurIfTrue(getWindow(),
				PieLauncherApp.getPrefs(this).blurBackground());
		kb = new SoftKeyboard(this);
		toolbarBackground = new ToolbarBackground(getResources());
		View toolbar = findViewById(R.id.toolbar);
		progressView = findViewById(R.id.progress);

		iconPackPackageName = PieLauncherApp.iconPack
				.getSelectedIconPackageName();
		if (iconPackPackageName == null) {
			IconPack.Pack pack =
					PieLauncherApp.iconPack.packs.values().iterator().next();
			if (pack != null) {
				iconPackPackageName = pack.packageName;
			}
		}
		if (iconPackPackageName == null) {
			finish();
			return;
		}

		initGridView(packageName);
		initSearch();
		initHide(packageName);
		initReset(packageName);
		initSwitchPack();

		Window window = getWindow();
		gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
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
					if (y != 0) {
						kb.hideFrom(searchInput);
					}
				}
				toolbar.setBackgroundColor(toolbarBackground.getColor(y));
			}
		});
		SystemBars.addPaddingFromWindowInsets(toolbar, gridView);
		SystemBars.setTransparentSystemBars(window);
		SystemBars.setNavigationBarColor(window,
				toolbarBackground.backgroundColor);
	}

	private void initGridView(String packageName) {
		gridView = findViewById(R.id.icons);
		gridView.setOnItemClickListener((parent, view, position, id) -> {
			PieLauncherApp.iconPack.addMapping(
					iconPackPackageName,
					packageName,
					iconAdapter.getItem(position));
			PieLauncherApp.iconPack.storeMappings(this);
			PieLauncherApp.appMenu.updateIconsAsync(this);
			finish();
		});
		loadPack(iconPackPackageName);
	}

	private void initSearch() {
		searchInput = findViewById(R.id.search);
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
				if (iconAdapter == null) {
					return;
				}
				iconAdapter.clear();
				String query = e.toString();
				Locale defaultLocale = Locale.getDefault();
				for (int i = 0, size = drawableNames.size(); i < size; ++i) {
					if (drawableNames.get(i).toLowerCase(defaultLocale)
							.contains(query.toLowerCase(defaultLocale))) {
						iconAdapter.add(drawableNames.get(i));
					}
				}
			}
		});
		searchInput.post(searchInput::requestFocus);
	}

	private void initReset(String packageName) {
		View resetButton = findViewById(R.id.reset);
		if (PieLauncherApp.iconPack.hasMapping(packageName)) {
			resetButton.setOnClickListener((v) -> {
				askToRestore(packageName);
			});
		} else {
			resetButton.setVisibility(View.INVISIBLE);
		}
	}

	private void askToRestore(String packageName) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.change_icon)
				.setMessage(R.string.want_to_restore_icon)
				.setPositiveButton(android.R.string.ok, (d, w) -> {
					PieLauncherApp.iconPack.removeMapping(packageName);
					PieLauncherApp.iconPack.storeMappings(this);
					PieLauncherApp.appMenu.updateIconsAsync(this);
					finish();
				})
				.setNeutralButton(R.string.all, (d, w) -> {
					PieLauncherApp.iconPack.clearMappings();
					PieLauncherApp.iconPack.storeMappings(this);
					PieLauncherApp.appMenu.updateIconsAsync(this);
					finish();
				})
				.setNegativeButton(android.R.string.cancel, (d, w) -> {
				})
				.show();
	}

	private void initHide(String packageName) {
		View hideButton = findViewById(R.id.hide_app);
		hideButton.setOnClickListener((v) -> askToHide(
				this, packageName, this::finish));
	}

	private void initSwitchPack() {
		View switchButton = findViewById(R.id.switch_pack);
		HashMap<String, String> map = PieLauncherApp.iconPack.getIconPacks();
		if (map.size() > 0) {
			List<String> packageNames = new ArrayList<>(map.keySet());
			List<String> names = new ArrayList<>(map.values());
			switchButton.setOnClickListener((v) -> {
				OptionsDialog.show(this, R.string.icon_pack,
						names.toArray(new CharSequence[0]),
						(view, which) -> {
							loadPack(packageNames.get(which));
						});
			});
		} else {
			switchButton.setVisibility(View.GONE);
		}
	}

	private void loadPack(String packageName) {
		if (iconPackPackageName == null) {
			return;
		}
		progressView.setVisibility(View.VISIBLE);
		Executors.newSingleThreadExecutor().execute(() -> {
			IconPack.Pack pack = PieLauncherApp.iconPack.packs.get(
					packageName);
			if (pack != null) {
				iconPackPackageName = packageName;
				drawableNames = pack.getDrawableNames();
			}
			handler.post(() -> {
				progressView.setVisibility(View.GONE);
				if (pack == null) {
					return;
				}
				iconAdapter = new PickIconAdapter(this,
						iconPackPackageName,
						new ArrayList<>(drawableNames));
				gridView.setAdapter(iconAdapter);
				searchInput.getText().clear();
			});
		});
	}
}
