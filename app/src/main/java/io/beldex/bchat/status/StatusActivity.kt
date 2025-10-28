package io.beldex.bchat.status

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.ui.ScreenContainer
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.conversation.v2.ConversationFragmentV2.Companion.PICK_FROM_LIBRARY
import io.beldex.bchat.conversation.v2.ConversationFragmentV2.Companion.TAKE_PHOTO
import io.beldex.bchat.conversation.v2.utilities.AttachmentManager
import io.beldex.bchat.conversation_v2.openConversationActivity
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.mediasend.Media
import io.beldex.bchat.mediasend.MediaSendActivity
import io.beldex.bchat.mms.GifSlide
import io.beldex.bchat.mms.ImageSlide
import io.beldex.bchat.mms.SlideDeck
import io.beldex.bchat.mms.VideoSlide
import io.beldex.bchat.util.MediaUtil
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import timber.log.Timber

enum class Status(val destination: String) {
    Status("status_class")
}

@AndroidEntryPoint
class StatusActivity: ComponentActivity() {

    var selectedContacts: MutableList<Recipient> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val destination = intent?.getStringExtra(EXTRA_DESTINATION) ?: Status.Status.destination
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
                                val contactViewModel: StatusViewModel = hiltViewModel()
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
                                    StatusScreen(
                                        searchQuery = searchQuery,
                                        contacts = contacts,
                                        selectedContact = selectedContact,
                                        onEvent = contactViewModel::onEvent,
                                        context
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendAttachments(attachments: List<Attachment>, body: String?) {
        Log.d("Status-Message success ui -> ","${selectedContacts.size}")
        val userPublicKey = TextSecurePreferences.getLocalNumber(this)!!
        MessageSender.createStatusAttachment(attachments, body, userPublicKey, selectedContacts)
            .successUi { address ->
                Log.d("Status-Message success ui -> ","done $address")
                val threadID =
                    DatabaseComponent.get(this).threadDatabase().getOrCreateThreadIdFor(
                        Recipient.from(this, Address.fromSerialized(address), false)
                    )
                if (!this.isFinishing) {
                    openConversationActivity(
                        threadID,
                        Recipient.from(this, Address.fromSerialized(address), false),
                        this
                    )
                    this.finish()
                }
            }.failUi {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
            }
    }

    private fun openConversationActivity(threadId: Long, recipient: Recipient, activity: Activity) {
        val returnIntent = Intent()
        returnIntent.putExtra(ConversationFragmentV2.THREAD_ID,threadId)
        returnIntent.putExtra(ConversationFragmentV2.ADDRESS,recipient.address)
        activity.setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
    }

    fun pickFromLibrary(recipient: Recipient, text: String, selectedContact: List<Recipient>) {
        selectedContacts.clear()
        selectedContacts.addAll(selectedContact)
        AttachmentManager.selectGallery(
            this,
            PICK_FROM_LIBRARY, recipient, text
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        when (requestCode) {
            PICK_FROM_LIBRARY,
            TAKE_PHOTO -> {
                intent ?: return
                val body = intent.getStringExtra(MediaSendActivity.EXTRA_MESSAGE)
                val media = intent.getParcelableArrayListExtra<Media>(
                    MediaSendActivity.EXTRA_MEDIA
                ) ?: return
                val slideDeck = SlideDeck()
                for (item in media) {
                    when {
                        MediaUtil.isVideoType(item.mimeType) -> {
                            slideDeck.addSlide(
                                VideoSlide(
                                    this,
                                    item.uri,
                                    0,
                                    item.caption.orNull()
                                )
                            )
                        }
                        MediaUtil.isGif(item.mimeType) -> {
                            slideDeck.addSlide(
                                GifSlide(
                                    this,
                                    item.uri,
                                    0,
                                    item.width,
                                    item.height,
                                    item.caption.orNull()
                                )
                            )
                        }
                        MediaUtil.isImageType(item.mimeType) -> {
                            Log.d("Status-Message success mime -> ","image")
                            slideDeck.addSlide(
                                ImageSlide(
                                    this,
                                    item.uri,
                                    0,
                                    item.width,
                                    item.height,
                                    item.caption.orNull()
                                )
                            )
                        }
                        else -> {
                            Log.d("Status-Message success mime -> ","else")
                            Timber.tag("Beldex")
                                .d("Asked to send an unexpected media type: '" + item.mimeType + "'. Skipping.")
                        }
                    }
                }
                sendAttachments(slideDeck.asAttachments(), body)
            }
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "io.beldex.bchat.DESTINATION"
    }
}