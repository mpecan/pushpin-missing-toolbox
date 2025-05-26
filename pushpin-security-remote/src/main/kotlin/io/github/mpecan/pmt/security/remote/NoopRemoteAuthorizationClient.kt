package io.github.mpecan.pmt.security.remote

import jakarta.servlet.http.HttpServletRequest

/**
 * No-op implementation of RemoteAuthorizationClient used when remote authorization is disabled.
 */
class NoopRemoteAuthorizationClient : RemoteAuthorizationClient {
    
    override fun canSubscribe(request: HttpServletRequest, channelId: String): Boolean {
        // Always return false, rely on local authorization service
        return false
    }
    
    override fun getSubscribableChannels(request: HttpServletRequest): List<String> {
        // Return empty list, rely on local authorization service
        return emptyList()
    }
    
    override fun getSubscribableChannelsByPattern(request: HttpServletRequest, pattern: String): List<String> {
        // Return empty list, rely on local authorization service
        return emptyList()
    }
}