package io.beldex.bchat.status

import com.beldex.libbchat.utilities.recipients.Recipient

sealed interface StatusEvents {
    data class RecipientSelectionChanged(val recipient: Recipient, val isSelected: Boolean):
        StatusEvents
    data class SearchQueryChanged(val query: String): StatusEvents
}