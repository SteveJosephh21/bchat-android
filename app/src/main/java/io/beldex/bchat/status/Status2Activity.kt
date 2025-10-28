package io.beldex.bchat.status

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.DownloadUtilities.downloadFile
import com.beldex.libbchat.utilities.ProfileKeyUtil
import com.beldex.libbchat.utilities.ProfilePictureUtilities
import com.beldex.libbchat.utilities.StatusData
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.streams.ProfileCipherInputStream
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageContract
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.R
import io.beldex.bchat.avatar.AvatarSelection
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.ui.ScreenContainer
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.permissions.Permissions
import io.beldex.bchat.profiles.ProfileMediaConstraints
import io.beldex.bchat.util.BitmapDecodingException
import io.beldex.bchat.util.BitmapUtil
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.wallet.CheckOnline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.all
import nl.komponents.kovenant.ui.successUi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


enum class Status2(val destination: String) {
    Status2("status_class")
}

@AndroidEntryPoint
class Status2Activity: ComponentActivity() {
    private var tempFile: File? = null
    val TAG = "Status2Activity"

    var statusUrl: String = ""
    var statusKey: ByteArray = byteArrayOf()

    val viewModel:Status2ViewModel by viewModels()

    private val onAvatarCropped = registerForActivityResult(CropImageContract()) { result ->
        when {
            result.isSuccessful -> {
              /*  Log.d(TAG,result.getUriFilePath(this).toString())
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val statusImageToBeUploaded = BitmapUtil.createScaledBytes(this@Status2Activity, result.getUriFilePath(this@Status2Activity).toString(), ProfileMediaConstraints()).bitmap
                        launch(Dispatchers.Main) {
                            updateStatusImage(statusImageToBeUploaded)
                        }
                    } catch(ex: BitmapDecodingException) {
                        Log.e(TAG, ex.toString())
                    }
                }*/
            }
            result is CropImage.CancelledResult -> {
                Log.i(TAG, "Cropping image was cancelled by the user")
            }
            else -> {
                Log.e(TAG, "Cropping image failed")
            }
        }

    }

    private val onPickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        //val outputFile = Uri.fromFile(File(cacheDir, "cropped"))
        val inputFile: Uri? = result.data?.data ?: tempFile?.let(Uri::fromFile)
        val filePath = inputFile?.let { Utils.getFilePathFromUri(this, it, false) }
        Log.d(TAG,"path"+filePath.toString())
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val statusImageToBeUploaded = BitmapUtil.createScaledBytes(this@Status2Activity, filePath.toString(), ProfileMediaConstraints()).bitmap
                launch(Dispatchers.Main) {
                    updateStatusImage(statusImageToBeUploaded)
                }
            } catch(ex: BitmapDecodingException) {
                Log.e(TAG, ex.toString())
            }
        }
        //cropImage(inputFile, outputFile)
    }

    private fun cropImage(inputFile: Uri?, outputFile: Uri?) {
        avatarSelection.circularCropImage(
            inputFile = inputFile,
            outputFile = outputFile
        )
    }

    private val avatarSelection = AvatarSelection(this, onAvatarCropped, onPickImage)

    private fun updateStatusImage(statusImage: ByteArray? = null) {
        val promises = mutableListOf<Promise<*, Exception>>()
        val encodedStatusKey = ProfileKeyUtil.generateEncodedProfileKey(this)
        if(statusImage != null) {
            promises.add(ProfilePictureUtilities.uploadStatusImage(statusImage, encodedStatusKey))
        }
        val compoundPromise = all(promises)
        compoundPromise.successUi { result ->
            Toast.makeText(
                this,
                "Image uploaded successfully",
                Toast.LENGTH_SHORT
            ).show()
            val statusList = result as ArrayList<StatusData>
            val status = statusList[0]
            statusUrl = status.url
            statusKey = status.statusKey
            Log.d(TAG, "Fileserver url "+status.url)
            Log.d(TAG, "Fileserver status key "+status.statusKey.toString())
            val encodedKey = Base64.encodeToString(statusKey, Base64.NO_WRAP)
            Log.d(TAG, "Key length = ${statusKey.size}")
            Log.d(TAG, "Key (Base64) = ${encodedKey}")
            viewModel.updateMessage("$statusUrl$$encodedKey")
            //openConversationActivity("bdf9732ac2d32b90bf7fcb03169fcb37b08ecb3e30b94d5610484f0ed0961a166b")
        }
    }

    private fun downloadStatusImage(): Bitmap? {
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
                                val contactViewModel: Status2ViewModel = hiltViewModel()
                                val contacts by contactViewModel.recipients.collectAsState(initial = listOf())
                                val searchQuery by contactViewModel.searchQuery.collectAsState()
                                val selectedContact by contactViewModel.selectedRecipients.collectAsState()
                                ScreenContainer(
                                    title = "Status",
                                    onBackClick = { finish() },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                ) {
                                    Status2Screen(
                                        searchQuery = searchQuery,
                                        contacts = contacts,
                                        selectedContact = selectedContact,
                                        onEvent = contactViewModel::onEvent,
                                        activity = context,
                                        uploadImage = {
                                            uploadImage()
                                        },
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openConversationActivity(publicKey: String) {
        val encodedKey = Base64.encodeToString(statusKey, Base64.NO_WRAP)
        Log.d(TAG, "Key length = ${statusKey.size}")
        Log.d(TAG, "Key (Base64) = ${encodedKey}")
        val recipient = Recipient.from(this, Address.fromSerialized(publicKey), false)
        val threadId =
            DatabaseComponent.get(this).threadDatabase().getOrCreateThreadIdFor(
                recipient
            )
        val returnIntent = Intent()
        returnIntent.putExtra(ConversationFragmentV2.THREAD_ID,threadId)
        returnIntent.putExtra(ConversationFragmentV2.ADDRESS,recipient.address)
        returnIntent.putExtra(ConversationFragmentV2.STATUS_URL,statusUrl)
        returnIntent.putExtra(ConversationFragmentV2.STATUS_KEY,encodedKey)
        this.setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
        this.finish()
    }

    private fun uploadImage() {
        if (CheckOnline.isOnline(this)) {
            Permissions.with(this)
                .request(Manifest.permission.CAMERA)
                .onAnyResult {
                    tempFile=avatarSelection.startAvatarSelection(false, true)
                }
                .execute()
        } else {
            Toast.makeText(
                this,
                getString(R.string.please_check_your_internet_connection),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "io.beldex.bchat.DESTINATION"
    }
}