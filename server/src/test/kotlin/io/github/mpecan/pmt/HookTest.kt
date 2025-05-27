package io.github.mpecan.pmt

import java.util.*  // This is a wildcard import that violates ktlint rules

class HookTest {
    fun test() {
        println("This is an extremely long line that definitely exceeds the 120 character limit and should cause ktlint to fail when we try to commit this file")
    }
}