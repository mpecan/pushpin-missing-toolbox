package io.github.mpecan.pmt

import java.util.*  // Wildcard import violation

class TestViolation {
    fun test() {
        println("This line is way too long and will exceed the maximum line length of 120 characters which should trigger a ktlint violation when we try to commit")
    }
}