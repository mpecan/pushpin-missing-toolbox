package io.github.mpecan.pmt.testcontainers

/**
 * Configuration for Pushpin container with sensible defaults.
 * * This data class represents all available Pushpin configuration parameters that can be
 * customized when running Pushpin in a test container. Each parameter maps directly to
 * Pushpin's configuration file options.
 * * The configuration is divided into several sections:
 * - Global settings: General Pushpin behavior
 * - Runner settings: HTTP/HTTPS server configuration
 * - Proxy settings: Request routing and handling
 * - Handler settings: Message publishing and ZMQ transport
 * * Most parameters have sensible defaults that work well for testing scenarios.
 * Use [PushpinContainerBuilder] or [PushpinPresets] for convenient configuration.
 * * @see PushpinContainerBuilder
 * @see PushpinPresets
 */
data class PushpinConfiguration(
    // Global settings
    val runDir: String = "run",
    val ipcPrefix: String = "pushpin-",
    val portOffset: Int = 0,
    val statsConnectionTtl: Int = 120,
    val statsConnectionSend: Boolean = true,

    // Runner settings
    val services: String = "condure,pushpin-proxy,pushpin-handler",
    val httpPort: Int = 7999,
    val httpsPort: Int? = null,
    val localPorts: String? = null,
    val logDir: String = "log",
    val logLevel: Int = 5,
    val clientBufferSize: Int = 8192,
    val clientMaxConn: Int = 50000,
    val allowCompression: Boolean = false,

    // Proxy settings
    val routesFile: String = "routes",
    val debug: Boolean = false,
    val autoCrossOrigin: Boolean = false,
    val acceptXForwardedProtocol: Boolean = false,
    val setXForwardedProtocol: String = "proto-only",
    val xForwardedFor: String = "",
    val xForwardedForTrusted: String = "",
    val origHeadersNeedMark: String = "",
    val acceptPushpinRoute: Boolean = false,
    val cdnLoop: String = "",
    val logFrom: Boolean = false,
    val logUserAgent: Boolean = false,
    val sigIss: String = "pushpin",
    val sigKey: String = "changeme",
    val upstreamKey: String = "",
    val sockjsUrl: String = "http://cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js",
    val updatesCheck: String = "off",
    val organizationName: String = "",

    // Handler settings
    val ipcFileMode: String? = null,
    val pushInPort: Int = 5560,
    val pushInSpec: String = "tcp://*:$pushInPort",
    val pushInSubPort: Int = 5562,
    val pushInSubSpec: String = "tcp://*:$pushInSubPort",
    val pushInSubConnect: Boolean = false,
    val pushInHttpAddr: String = "0.0.0.0",
    val pushInHttpPort: Int = 5561,
    val pushInHttpMaxHeadersSize: Int = 10000,
    val pushInHttpMaxBodySize: Int = 1000000,
    val statsSpec: String = "ipc://{rundir}/{ipc_prefix}stats",
    val commandPort: Int = 5563,
    val commandSpec: String = "tcp://*:$commandPort",
    val messageRate: Int = 2500,
    val messageHwm: Int = 25000,
    val messageBlockSize: String? = null,
    val messageWait: Int = 5000,
    val idCacheTtl: Int = 60,
    val updateOnFirstSubscription: Boolean = true,
    val connectionSubscriptionMax: Int = 20,
    val subscriptionLinger: Int = 60,
    val statsSubscriptionTtl: Int = 60,
    val statsReportInterval: Int = 10,
    val statsFormat: String = "tnetstring",
) {
    /**
     * Generate the pushpin.conf content based on the configuration.
     */
    fun toConfigString(): String {
        return buildString {
            appendLine("[global]")
            appendLine("include={libdir}/internal.conf")
            appendLine()
            appendLine("# directory to save runtime files")
            appendLine("rundir=$runDir")
            appendLine()
            appendLine("# prefix for zmq ipc specs")
            appendLine("ipc_prefix=$ipcPrefix")
            appendLine()
            appendLine("# port offset for zmq tcp specs and http control server")
            appendLine("port_offset=$portOffset")
            appendLine()
            appendLine("# TTL (seconds) for connection stats")
            appendLine("stats_connection_ttl=$statsConnectionTtl")
            appendLine()
            appendLine("# whether to send individual connection stats")
            appendLine("stats_connection_send=$statsConnectionSend")
            appendLine()
            appendLine()
            appendLine("[runner]")
            appendLine("# services to start")
            appendLine("services=$services")
            appendLine()
            appendLine("# plain HTTP port to listen on for client connections")
            appendLine("http_port=$httpPort")
            appendLine()

            httpsPort?.let {
                appendLine("# list of HTTPS ports to listen on for client connections")
                appendLine("https_ports=$it")
                appendLine()
            }

            localPorts?.let {
                appendLine("# list of unix socket paths to listen on for client connections")
                appendLine("local_ports=$it")
                appendLine()
            }

            appendLine("# directory to save log files")
            appendLine("logdir=$logDir")
            appendLine()
            appendLine("# logging level. 2 = info, >2 = verbose")
            appendLine("log_level=$logLevel")
            appendLine()
            appendLine("# client full request header must fit in this buffer")
            appendLine("client_buffer_size=$clientBufferSize")
            appendLine()
            appendLine("# maximum number of client connections")
            appendLine("client_maxconn=$clientMaxConn")
            appendLine()
            appendLine("# whether connections can use compression")
            appendLine("allow_compression=$allowCompression")
            appendLine()
            appendLine("# paths")
            appendLine("mongrel2_bin=mongrel2")
            appendLine("m2sh_bin=m2sh")
            appendLine("zurl_bin=zurl")
            appendLine()
            appendLine()
            appendLine("[proxy]")
            appendLine("# routes config file (path relative to location of this file)")
            appendLine("routesfile=$routesFile")
            appendLine()
            appendLine("# enable debug mode to get informative error responses")
            appendLine("debug=$debug")
            appendLine()
            appendLine("# whether to use automatic CORS and JSON-P wrapping")
            appendLine("auto_cross_origin=$autoCrossOrigin")
            appendLine()
            appendLine("# whether to accept x-forwarded-proto")
            appendLine("accept_x_forwarded_protocol=$acceptXForwardedProtocol")
            appendLine()
            appendLine("# whether to assert x-forwarded-proto")
            appendLine("set_x_forwarded_protocol=$setXForwardedProtocol")
            appendLine()
            appendLine("# how to treat x-forwarded-for. example: \"truncate:0,append\"")
            appendLine("x_forwarded_for=$xForwardedFor")
            appendLine()
            appendLine("# how to treat x-forwarded-for if grip-signed")
            appendLine("x_forwarded_for_trusted=$xForwardedForTrusted")
            appendLine()
            appendLine("# the following headers must be marked in order to qualify as orig")
            appendLine("orig_headers_need_mark=$origHeadersNeedMark")
            appendLine()
            appendLine("# whether to accept Pushpin-Route header")
            appendLine("accept_pushpin_route=$acceptPushpinRoute")
            appendLine()
            appendLine("# value to append to the CDN-Loop header")
            appendLine("cdn_loop=$cdnLoop")
            appendLine()
            appendLine("# include client IP address in logs")
            appendLine("log_from=$logFrom")
            appendLine()
            appendLine("# include client user agent in logs")
            appendLine("log_user_agent=$logUserAgent")
            appendLine()
            appendLine("# for signing proxied requests")
            appendLine("sig_iss=$sigIss")
            appendLine()
            appendLine("# for signing proxied requests. use \"base64:\" prefix for binary key")
            appendLine("sig_key=$sigKey")
            appendLine()
            appendLine("# use this to allow grip to be forwarded upstream (e.g. to fanout.io)")
            appendLine("upstream_key=$upstreamKey")
            appendLine()
            appendLine("# for the sockjs iframe transport")
            appendLine("sockjs_url=$sockjsUrl")
            appendLine()
            appendLine("# updates check has three modes:")
            appendLine("#   report: check for new pushpin version and report anonymous usage info to")
            appendLine("#           the pushpin developers")
            appendLine("#   check:  check for new pushpin version only, don't report anything")
            appendLine("#   off:    don't do any reporting or checking")
            appendLine("updates_check=$updatesCheck")
            appendLine()
            appendLine("# use this field to identify your organization in updates requests")
            appendLine("organization_name=$organizationName")
            appendLine()
            appendLine()
            appendLine("[handler]")

            ipcFileMode?.let {
                appendLine("# ipc permissions (octal)")
                appendLine("ipc_file_mode=$it")
                appendLine()
            }

            appendLine("# bind PULL for receiving publish commands")
            appendLine("push_in_spec=$pushInSpec")
            appendLine()
            appendLine("# list of bind SUB for receiving published messages")
            appendLine("push_in_sub_spec=$pushInSubSpec")
            appendLine()
            appendLine("# whether the above SUB socket should connect instead of bind")
            appendLine("push_in_sub_connect=$pushInSubConnect")
            appendLine()
            appendLine("# addr/port to listen on for receiving publish commands via HTTP")
            appendLine("push_in_http_addr=$pushInHttpAddr")
            appendLine("push_in_http_port=$pushInHttpPort")
            appendLine()
            appendLine("# maximum headers and body size in bytes when receiving publish commands via HTTP")
            appendLine("push_in_http_max_headers_size=$pushInHttpMaxHeadersSize")
            appendLine("push_in_http_max_body_size=$pushInHttpMaxBodySize")
            appendLine()
            appendLine("# bind PUB for sending stats (metrics, subscription info, etc)")
            appendLine("stats_spec=$statsSpec")
            appendLine()
            appendLine("# bind REP for responding to commands")
            appendLine("command_spec=$commandSpec")
            appendLine()
            appendLine("# max messages per second")
            appendLine("message_rate=$messageRate")
            appendLine()
            appendLine("# max rate-limited messages")
            appendLine("message_hwm=$messageHwm")
            appendLine()

            messageBlockSize?.let {
                appendLine("# set to report blocks counts in stats (content size / block size)")
                appendLine("message_block_size=$it")
                appendLine()
            }

            appendLine("# max time (milliseconds) for out-of-order messages to wait")
            appendLine("message_wait=$messageWait")
            appendLine()
            appendLine("# time (seconds) to cache message ids")
            appendLine("id_cache_ttl=$idCacheTtl")
            appendLine()
            appendLine("# retry/recover sessions soon after the first subscription to a channel")
            appendLine("update_on_first_subscription=$updateOnFirstSubscription")
            appendLine()
            appendLine("# max subscriptions per connection")
            appendLine("connection_subscription_max=$connectionSubscriptionMax")
            appendLine()
            appendLine("# time (seconds) to linger response mode subscriptions")
            appendLine("subscription_linger=$subscriptionLinger")
            appendLine()
            appendLine("# TTL (seconds) for subscription stats")
            appendLine("stats_subscription_ttl=$statsSubscriptionTtl")
            appendLine()
            appendLine("# interval (seconds) to send report stats")
            appendLine("stats_report_interval=$statsReportInterval")
            appendLine()
            appendLine("# stats output format")
            appendLine("stats_format=$statsFormat")
        }
    }
}
