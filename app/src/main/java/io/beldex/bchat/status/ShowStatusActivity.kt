package io.beldex.bchat.status

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.beldex.libbchat.utilities.DownloadUtilities.downloadFile
import com.beldex.libsignal.streams.ProfileCipherInputStream
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.ui.ScreenContainer
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


enum class ShowStatus(val destination: String) {
    ShowStatus("status_class")
}

@AndroidEntryPoint
class ShowStatusActivity: ComponentActivity() {
    private var tempFile: File? = null
    val TAG = "ShowStatus"

    val viewModel:Status2ViewModel by viewModels()

    fun downloadStatusImage(statusUrl: String, statusKey: ByteArray): Bitmap? {
        Log.d(TAG, "url ")
        Log.d(TAG, "status key ")
        val downloadDestination = File.createTempFile("status_enc", ".jpg", cacheDir)
        val decryptDestination = File.createTempFile("status_dec", ".jpg", cacheDir)

        try {
            if (statusUrl.isEmpty() || statusKey.isEmpty()) {
                Log.e(TAG, "Missing status URL or key — cannot download")
                return null
            }
            Log.d(TAG, "url "+statusUrl)
            Log.d(TAG, "status key "+statusKey.toString())
            downloadFile(downloadDestination, statusUrl)

            ProfileCipherInputStream(FileInputStream(downloadDestination), statusKey).use { avatarStream ->
                FileOutputStream(decryptDestination).use { output ->
                    avatarStream.copyTo(output)
                }
            }

            val bitmap = BitmapFactory.decodeFile(decryptDestination.absolutePath)
            Log.d(TAG, "Decoded bitmap = $bitmap")
            return bitmap
        } finally {
            Log.d(TAG, "finally")
            downloadDestination.delete()
            decryptDestination.deleteOnExit()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val destination = intent?.getStringExtra(EXTRA_DESTINATION) ?: Status2.Status2.destination
        val statusUrl = intent?.getStringExtra(ConversationFragmentV2.STATUS_URL) ?: ""
        val statusKey = intent?.getByteArrayExtra(ConversationFragmentV2.STATUS_KEY) ?: byteArrayOf()
        setContent {
            val context = this
            BChatTheme(
                darkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            ) {
                // A surface container using the 'background' color from the theme
                Surface {
                    Scaffold {
                        val navController = rememberNavController()
                        NavHost(
                            navController = navController,
                            startDestination = destination,
                            modifier = Modifier
                                .padding(it)
                        ) {

                            composable(
                                route = Status.Status.destination
                            ) {
                                ScreenContainer(
                                    title = "Status",
                                    onBackClick = { finish() },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                ) {
                                    ShowStatusScreen(
                                        context,
                                        viewModel,
                                        statusUrl,
                                        statusKey
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "io.beldex.bchat.DESTINATION"
    }
}