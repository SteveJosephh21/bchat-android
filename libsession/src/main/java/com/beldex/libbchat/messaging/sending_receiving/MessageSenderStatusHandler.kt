package com.beldex.libbchat.messaging.sending_receiving

import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.messages.signal.OutgoingMediaMessage
import com.beldex.libbchat.messaging.messages.signal.OutgoingTextMessage
import com.beldex.libbchat.messaging.messages.visible.VisibleMessage
import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment
import com.beldex.libbchat.messaging.sending_receiving.notifications.PushRegistryV1
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.Device
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.ThreadUtils
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import java.util.*

fun MessageSender.createStatusTextOnlyMessage(
    device: Device,
    status: String,
    userPublicKey: String,
    members: Collection<Recipient>,
): Promise<String, Exception> {
    val deferred = deferred<String, Exception>()
    ThreadUtils.queue {
        val context = MessagingModuleConfiguration.shared.context
        val storage = MessagingModuleConfiguration.shared.storage
        // Create the message
        val message = VisibleMessage()
        message.sentTimestamp = MnodeAPI.nowWithOffset
        message.text = status

        // Send it
        for (member in members) {
            message.recipient = member.address.toString()
            val outgoingTextMessage = OutgoingTextMessage.from(message, member)
            val threadId = storage.getThreadId(member)
            // Put the message in the database
            message.id = storage.getMessageIdFromSMSDatabase(threadId ?: -1, outgoingTextMessage, message.sentTimestamp)
            Log.d("Status-Message id -> ","${message.id}, ${message.recipient}")
            try {
                //MessageSender.send(message, member.address)
                sendStatusNonDurably(message, member.address)
                    .success {
                        Log.d("Status-Message success -> ","${message.id}")
                    }.fail {
                        Log.d("Status-Message fail -> ","${message.id}")
                    }
            } catch (e: Exception) {
                Log.d("Status-Message catch -> ","${message.id}")
                deferred.reject(e)
                return@queue
            }
        }
        // Fulfill the promise
        deferred.resolve("")
    }
    // Return
    return deferred.promise
}

fun MessageSender.createStatusAttachmentMessage(
    attachments: List<Attachment>,
    status: String?,
    userPublicKey: String,
    members: Collection<Recipient>,
): Promise<String, Exception> {
    val deferred = deferred<String, Exception>()
    ThreadUtils.queue {
        val context = MessagingModuleConfiguration.shared.context
        val storage = MessagingModuleConfiguration.shared.storage
        // Create the message
        val message = VisibleMessage()
        message.sentTimestamp = MnodeAPI.nowWithOffset
        message.text = status
        var address = ""

        // Send it
        for (member in members) {
            message.recipient = member.address.toString()
            address = member.address.toString()
            val outgoingMediaMessage = OutgoingMediaMessage.fromStatus(message, member, attachments, null)

            val threadId = storage.getThreadId(member)
            // Put the message in the database
            message.id = storage.getMessageIdFromMMSDatabase(threadId ?: -1, outgoingMediaMessage, message.sentTimestamp)
            Log.d("Status-Message id -> ","${message.id}, ${message.recipient}")
            try {
                //MessageSender.send(message, member.address)
                sendStatusAttachmentNonDurably(message, attachments, member.address)
                    /*.success {
                        Log.d("Status-Message success -> ","${message.id}")
                    }.fail {
                        Log.d("Status-Message fail -> ","${message.id}")
                    }*/
            } catch (e: Exception) {
                Log.d("Status-Message catch -> ","${message.id}")
                deferred.reject(e)
                return@queue
            }
        }
        // Fulfill the promise
        deferred.resolve(address)
    }
    // Return
    return deferred.promise
}