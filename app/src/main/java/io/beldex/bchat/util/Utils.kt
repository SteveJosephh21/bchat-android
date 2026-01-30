package io.beldex.bchat.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import java.util.regex.Pattern

class Utils {
    companion object {
        val namePattern: Pattern = Pattern.compile("[A-Za-z0-9\\s]+")

        fun showToast(context: Context?, @StringRes resId: Int) {
            Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
        }
    }
}