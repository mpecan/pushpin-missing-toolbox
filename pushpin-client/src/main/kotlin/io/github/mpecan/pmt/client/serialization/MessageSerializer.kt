package io.github.mpecan.pmt.client.serialization

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.model.PushpinMessage

/**
 * Interface for serializing messages to Pushpin format.
 */
interface MessageSerializer {
    /**
     * Converts a Message to a PushpinMessage.
     *
     * @param message The message to convert
     * @return The converted PushpinMessage
     */
    fun serialize(message: Message): PushpinMessage
}