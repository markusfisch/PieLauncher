package de.markusfisch.android.pielauncher.view;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class SoftKeyboard {
	private final InputMethodManager imm;

	public SoftKeyboard(Context context) {
		imm = (InputMethodManager) context.getSystemService(
				Context.INPUT_METHOD_SERVICE);
	}

	public void showFor(EditText editText) {
		editText.requestFocus();
		try {
			imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
		} catch (Exception e) {
			// Catch DeadSystemException, which is only thrown at API > 24.
		}
	}

	public void hideFrom(View view) {
		try {
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		} catch (Exception e) {
			// Catch DeadSystemException, which is only thrown at API > 24.
		}
	}
}
