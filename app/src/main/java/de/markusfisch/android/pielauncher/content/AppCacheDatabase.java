package de.markusfisch.android.pielauncher.content;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class AppCacheDatabase {
	private static final String FILE_NAME = "app_cache.db";
	private static final int VERSION = 1;

	private static final String APPS = "apps";
	private static final String COMPONENT_NAME = "component_name";
	private static final String PACKAGE_NAME = "package_name";
	private static final String USER_SERIAL = "user_serial";
	private static final String LABEL = "label";
	private static final String ICON = "icon";
	private static final String UPDATED_AT = "updated_at";
	private static final long NO_USER = -1L;

	private final OpenHelper openHelper;

	public AppCacheDatabase(Context context) {
		openHelper = new OpenHelper(context.getApplicationContext());
	}

	public void restoreApps(
			Context context,
			Map<LauncherItemKey, Apps.AppIcon> apps) {
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor cursor = db.query(APPS,
				new String[]{COMPONENT_NAME, USER_SERIAL, LABEL, ICON},
				null,
				null,
				null,
				null,
				null);
		try {
			while (cursor.moveToNext()) {
				ComponentName componentName =
						ComponentName.unflattenFromString(cursor.getString(0));
				String label = cursor.getString(2);
				byte[] icon = cursor.getBlob(3);
				if (componentName == null || label == null || icon == null) {
					continue;
				}
				Bitmap bitmap = BitmapFactory.decodeByteArray(
						icon, 0, icon.length);
				UserHandle userHandle = null;
				long serialNumber = cursor.getLong(1);
				if (AppLauncher.HAS_LAUNCHER_APP && serialNumber != NO_USER) {
					userHandle = getUserForSerialNumber(context, serialNumber);
					if (userHandle == null) {
						continue;
					}
					if (AppLauncher.isPrivateProfileLocked(context, userHandle)) {
						continue;
					}
				}
				if (bitmap == null) {
					continue;
				}
				Apps.AppIcon appIcon = new Apps.AppIcon(
						componentName,
						label,
						bitmap,
						userHandle);
				apps.put(new LauncherItemKey(componentName, userHandle),
						appIcon);
			}
		} finally {
			cursor.close();
		}
	}

	public void replaceAllApps(
			Context context,
			Map<LauncherItemKey, Apps.AppIcon> apps) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			db.delete(APPS, null, null);
			insertApps(context, db, apps);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void replacePackage(
			Context context,
			String packageName,
			UserHandle userHandle,
			Map<LauncherItemKey, Apps.AppIcon> apps) {
		Map<LauncherItemKey, Apps.AppIcon> packageApps = new HashMap<>();
		for (Map.Entry<LauncherItemKey, Apps.AppIcon> entry : apps.entrySet()) {
			Apps.AppIcon appIcon = entry.getValue();
			if (packageName.equals(appIcon.componentName.getPackageName()) &&
					(userHandle == null || userHandle.equals(appIcon.userHandle))) {
				packageApps.put(entry.getKey(), appIcon);
			}
		}
		SQLiteDatabase db = openHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			deletePackage(context, db, packageName, userHandle);
			insertApps(context, db, packageApps);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void removePackage(
			Context context,
			String packageName,
			UserHandle userHandle) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			deletePackage(context, db, packageName, userHandle);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	private void insertApps(
			Context context,
			SQLiteDatabase db,
			Map<LauncherItemKey, Apps.AppIcon> apps) {
		long now = System.currentTimeMillis();
		for (Apps.AppIcon appIcon : apps.values()) {
			byte[] icon = getIconBlob(appIcon);
			if (icon == null) {
				continue;
			}
			ContentValues values = new ContentValues();
			values.put(COMPONENT_NAME, appIcon.componentName.flattenToString());
			values.put(PACKAGE_NAME, appIcon.componentName.getPackageName());
			putUserSerial(context, values, appIcon.userHandle);
			values.put(LABEL, appIcon.label);
			values.put(ICON, icon);
			values.put(UPDATED_AT, now);
			db.insertWithOnConflict(APPS, null, values,
					SQLiteDatabase.CONFLICT_REPLACE);
		}
	}

	private void deletePackage(
			Context context,
			SQLiteDatabase db,
			String packageName,
			UserHandle userHandle) {
		if (AppLauncher.HAS_LAUNCHER_APP && userHandle != null) {
			db.delete(APPS,
					PACKAGE_NAME + "=? AND " + USER_SERIAL + "=?",
					new String[]{
							packageName,
							String.valueOf(getSerialNumberForUser(
									context, userHandle))
					});
		} else {
			db.delete(APPS, PACKAGE_NAME + "=?", new String[]{packageName});
		}
	}

	private static byte[] getIconBlob(Apps.AppIcon appIcon) {
		if (appIcon.iconBytes != null) {
			return appIcon.iconBytes;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (!appIcon.bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
			return null;
		}
		appIcon.iconBytes = out.toByteArray();
		return appIcon.iconBytes;
	}

	private static void putUserSerial(
			Context context,
			ContentValues values,
			UserHandle userHandle) {
		if (AppLauncher.HAS_LAUNCHER_APP && userHandle != null) {
			values.put(USER_SERIAL, getSerialNumberForUser(
					context, userHandle));
		} else {
			values.put(USER_SERIAL, NO_USER);
		}
	}

	@SuppressLint("UseRequiresApi")
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static long getSerialNumberForUser(
			Context context,
			UserHandle userHandle) {
		UserManager um = AppLauncher.getUserManager(context);
		return um != null ? um.getSerialNumberForUser(userHandle) : -1L;
	}

	@SuppressLint("UseRequiresApi")
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static UserHandle getUserForSerialNumber(
			Context context,
			long serialNumber) {
		UserManager um = AppLauncher.getUserManager(context);
		return um != null ? um.getUserForSerialNumber(serialNumber) : null;
	}

	private static class OpenHelper extends SQLiteOpenHelper {
		OpenHelper(Context context) {
			super(context, FILE_NAME, null, VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + APPS + " (" +
					COMPONENT_NAME + " TEXT NOT NULL," +
					PACKAGE_NAME + " TEXT NOT NULL," +
					USER_SERIAL + " INTEGER NOT NULL," +
					LABEL + " TEXT NOT NULL," +
					ICON + " BLOB NOT NULL," +
					UPDATED_AT + " INTEGER NOT NULL," +
					"PRIMARY KEY (" + COMPONENT_NAME + "," +
					USER_SERIAL + "));");
			db.execSQL("CREATE INDEX apps_package_name ON " + APPS +
					" (" + PACKAGE_NAME + ");");
		}

		@Override
		public void onUpgrade(
				SQLiteDatabase db,
				int oldVersion,
				int newVersion) {
		}

		@Override
		public void onDowngrade(
				SQLiteDatabase db,
				int oldVersion,
				int newVersion) {
		}
	}
}
