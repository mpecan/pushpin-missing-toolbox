package io.github.mpecan.pmt.test

import java.util.concurrent.atomic.AtomicReference

object PortProvider {

    private var ports = AtomicReference((10000..12000).toList())

    fun getPort(): Int {
        var result: Int = -1
        ports.getAndUpdate { currentPorts ->
            result = currentPorts.random()
            currentPorts.filter { it != result }
        }
        return result
    }
}
