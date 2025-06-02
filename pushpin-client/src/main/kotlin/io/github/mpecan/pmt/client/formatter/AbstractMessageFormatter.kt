package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.exception.MessageFormattingException
import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.model.PushpinFormat

/**
 * Abstract base class for message formatters.
 * Provides common functionality for message formatting.
 */
abstract class AbstractMessageFormatter<T : PushpinFormat>(
    protected val serializationService: MessageSerializationService,
    protected val options: FormatterOptions = FormatterOptions(),
) {
    /**
     * Formats a message for a specific transport.
     * Applies pre-processing and post-processing hooks.
     *
     * @param message The message to format
     * @return The formatted message
     * @throws MessageFormattingException If there is an error during formatting
     */
    @Throws(MessageFormattingException::class)
    fun formatWithHooks(message: Message): T {
        try {
            val processedMessage = preProcessMessage(message)
            val result = doFormat(processedMessage)
            return postProcessResult(result, processedMessage)
        } catch (e: Exception) {
            if (e is MessageFormattingException) throw e
            throw MessageFormattingException("Error formatting message", e)
        }
    }

    /**
     * Hook for pre-processing the message before formatting.
     * Can be overridden by subclasses.
     *
     * @param message The message to pre-process
     * @return The pre-processed message
     */
    protected open fun preProcessMessage(message: Message): Message =
        if (options.applyCustomPreProcessors) {
            options.preProcessors.fold(message) { acc, processor -> processor(acc) }
        } else {
            message
        }

    /**
     * Actual formatting implementation.
     * Must be implemented by subclasses.
     *
     * @param message The message to format
     * @return The formatted message
     */
    protected abstract fun doFormat(message: Message): T

    /**
     * Hook for post-processing the result after formatting.
     * Can be overridden by subclasses.
     *
     * @param result The formatted message
     * @param originalMessage The original message
     * @return The post-processed result
     */
    protected open fun postProcessResult(
        result: T,
        originalMessage: Message,
    ): T =
        if (options.applyCustomPostProcessors) {
            @Suppress("UNCHECKED_CAST")
            options.postProcessors.fold(result) { acc, processor -> processor(acc, originalMessage) as T }
        } else {
            result
        }
}

/**
 * Configuration options for message formatters.
 */
data class FormatterOptions(
    /**
     * Whether to apply custom pre-processors. Default is false.
     */
    val applyCustomPreProcessors: Boolean = false,
    /**
     * Whether to apply custom post-processors. Default is false.
     */
    val applyCustomPostProcessors: Boolean = false,
    /**
     * Custom pre-processors to apply to messages before formatting.
     * Each processor takes a Message and returns a Message.
     */
    val preProcessors: List<(Message) -> Message> = emptyList(),
    /**
     * Custom post-processors to apply to results after formatting.
     * Each processor takes a PushpinFormat and the original Message,
     * and returns a PushpinFormat.
     */
    val postProcessors: List<(PushpinFormat, Message) -> PushpinFormat> = emptyList(),
    /**
     * Additional options specific to the formatter.
     */
    val additionalOptions: Map<String, Any> = emptyMap(),
) {
    /**
     * Creates a new options instance with an additional pre-processor.
     */
    fun withPreProcessor(processor: (Message) -> Message): FormatterOptions =
        copy(
            applyCustomPreProcessors = true,
            preProcessors = preProcessors + processor,
        )

    /**
     * Creates a new options instance with an additional post-processor.
     */
    fun withPostProcessor(processor: (PushpinFormat, Message) -> PushpinFormat): FormatterOptions =
        copy(
            applyCustomPostProcessors = true,
            postProcessors = postProcessors + processor,
        )

    /**
     * Creates a new options instance with an additional option.
     */
    fun withOption(
        key: String,
        value: Any,
    ): FormatterOptions =
        copy(
            additionalOptions = additionalOptions + (key to value),
        )
}
