package io.github.mpecan.pmt.security.remote

import io.github.mpecan.pmt.security.model.ChannelPermission
import io.github.mpecan.pmt.security.model.ChannelPermissions
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * No-op implementation of RemoteAuthorizationClient used when remote authorization is disabled.
 */
@Component
@ConditionalOnProperty(
    name = ["pushpin.security.jwt.remoteAuthorization.enabled"],
    havingValue = "false",
    matchIfMissing = true
)
class NoopRemoteAuthorizationClient : RemoteAuthorizationClient {
    
    override fun hasPermission(
        request: HttpServletRequest, 
        channelId: String, 
        permission: ChannelPermission
    ): Boolean {
        // Always return false, rely on local authorization service
        return false
    }
    
    override fun getChannelsWithPermission(
        request: HttpServletRequest, 
        permission: ChannelPermission
    ): List<String> {
        // Return empty list, rely on local authorization service
        return emptyList()
    }
    
    override fun getAllChannelPermissions(request: HttpServletRequest): List<ChannelPermissions> {
        // Return empty list, rely on local authorization service
        return emptyList()
    }
}