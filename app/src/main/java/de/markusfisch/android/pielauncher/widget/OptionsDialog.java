package de.markusfisch.android.pielauncher.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class OptionsDialog {
	public static void show(Context context,
			int titleId,
			CharSequence[] items,
			DialogInterface.OnClickListener onClickListener) {
		new AlertDialog.Builder(context)
				.setTitle(titleId)
				.setItems(items, onClickListener)
				.show();
	}
}
