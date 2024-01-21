package de.markusfisch.android.pielauncher.view

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

class SoftKeyboard @JvmOverloads constructor(
    context: Context,
    private val imm: InputMethodManager = context.imm()
) {

    fun showFor(editText: EditText) {
        editText.requestFocus()
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideFrom(view: View) {
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

private fun Context.imm() = getSystemService(
    Context.INPUT_METHOD_SERVICE
) as InputMethodManager
