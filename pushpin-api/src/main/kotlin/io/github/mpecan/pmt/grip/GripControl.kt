package io.github.mpecan.pmt.grip

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Base interface for GRIP control messages.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = GripSubscribeControl::class, name = "subscribe"),
    JsonSubTypes.Type(value = GripUnsubscribeControl::class, name = "unsubscribe"),
    JsonSubTypes.Type(value = GripDetachControl::class, name = "detach"),
    JsonSubTypes.Type(value = GripKeepAliveControl::class, name = "keep-alive"),
    JsonSubTypes.Type(value = GripSetHoldControl::class, name = "set-hold"),
    JsonSubTypes.Type(value = GripAckControl::class, name = "ack"),
    JsonSubTypes.Type(value = GripCloseControl::class, name = "close"),
)
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed interface GripControl

/**
 * Subscribe to a channel.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GripSubscribeControl(
    val channel: String,
    val filters: List<String>? = null,
    @get:JsonProperty("prev-id")
    val prevId: String? = null,
) : GripControl

/**
 * Unsubscribe from a channel.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GripUnsubscribeControl(
    val channel: String,
) : GripControl

/**
 * Detach the backend connection while keeping the client connection alive.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class GripDetachControl : GripControl

/**
 * Configure keep-alive behavior.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GripKeepAliveControl(
    val timeout: Int? = null,
    val content: String? = null,
    @get:JsonProperty("content-bin")
    val contentBin: String? = null,
    val format: String? = null,
) : GripControl

/**
 * Change the hold mode.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GripSetHoldControl(
    val mode: String,
    val timeout: Int? = null,
    val channels: List<GripChannelConfig>? = null,
) : GripControl

/**
 * Acknowledge a message.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GripAckControl(
    val channel: String,
    val id: String,
) : GripControl

/**
 * Close the connection.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GripCloseControl(
    val code: Int? = null,
    val reason: String? = null,
) : GripControl

/**
 * Channel configuration for set-hold control.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GripChannelConfig(
    val name: String,
    val filters: List<String>? = null,
    @get:JsonProperty("prev-id")
    val prevId: String? = null,
)
