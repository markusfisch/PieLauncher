package de.markusfisch.android.pielauncher.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;

public class Dialog {
	public static AlertDialog.Builder newDialog(Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ||
				!PieLauncherApp.getPrefs(context).useLightDialogs()) {
			return new AlertDialog.Builder(context);
		}
		return new AlertDialog.Builder(context, R.style.LightDialog);
	}
}
