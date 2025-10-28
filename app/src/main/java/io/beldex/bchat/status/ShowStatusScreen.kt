package io.beldex.bchat.status

import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ShowStatusScreen(
    activity: ShowStatusActivity,
    viewModel: Status2ViewModel,
    statusUrl: String,
    statusKey: ByteArray
) {
    val context = LocalContext.current
    val statusImage by viewModel.statusBitmapImage.observeAsState()
    if (TextSecurePreferences.isScreenSecurityEnabled(context))
        activity.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else {
        activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    // Download bitmap on first composition
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val bitmap = activity.downloadStatusImage(statusUrl, statusKey)
            viewModel.updateStatusImage(bitmap)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.ime.asPaddingValues())
    ) {

        if (statusImage != null) {
            Image(
                bitmap = statusImage!!.asImageBitmap(),
                contentDescription = "Status image",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentScale = ContentScale.Fit
            )
        } else {
            // fallback if bitmap is null
            Image(
                painter = painterResource(id = R.drawable.empty_inbox_1),
                contentDescription = "Default image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}