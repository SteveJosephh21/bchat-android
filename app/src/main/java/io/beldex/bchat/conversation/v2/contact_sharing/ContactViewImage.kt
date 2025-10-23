package io.beldex.bchat.conversation.v2.contact_sharing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.dependencies.DatabaseComponent


@Composable
fun ContactViewImage(
    contacts: List<ContactModel>,
    backgroundColor : Color,
) {
    val context = LocalContext.current

    data class ContactDisplay(val name: String, val address: String)

    val contactList: List<ContactDisplay> = contacts.firstOrNull()?.let { contact ->
        val names = flattenData(contact.name)
        val addresses = flattenData(contact.address.serialize())
        names.zip(addresses) { name, address -> ContactDisplay(name, address) }
    } ?: emptyList()

    fun getUserIsBNSHolderStatus(publicKey: String): Boolean? {
        return DatabaseComponent.get(context)
            .bchatContactDatabase()
            .getContactWithBchatID(publicKey)
            ?.isBnsHolder
    }

    if (contactList.isNotEmpty()) {
        val multiContact = contactList.size > 1
        Box(modifier = Modifier.padding(top = if(multiContact) 4.dp else 0.dp, end = if(multiContact) 4.dp else 0.dp)) {
            contactList.getOrNull(1)?.let { second ->
                key(second.address) {
                    ProfilePictureComponent(
                        publicKey=second.address,
                        displayName=second.name,
                        containerSize=30.dp,
                        pictureMode= ProfilePictureMode.SmallPicture,
                        modifier= Modifier
                            .align(Alignment.TopEnd)
                            .offset(x=3.dp, y=(-3).dp)
                    )
                }
            }

            contactList.getOrNull(0)?.let { first ->
                key(first.address) {
                    ProfilePictureComponent(
                        publicKey=first.address,
                        displayName=first.name,
                        containerSize=36.dp,
                        pictureMode= ProfilePictureMode.SmallPicture,
                        modifier= Modifier.then(
                            if (multiContact && getUserIsBNSHolderStatus(first.address) != true) {
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, backgroundColor, CircleShape)
                            } else Modifier
                        )
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.Green, RoundedCornerShape(100))
        )
    }
}