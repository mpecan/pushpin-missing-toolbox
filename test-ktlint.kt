package test

import java.util.*  // Wildcard import - ktlint violation

class TestKtlint {
    fun test() {
        println("This line is way too long and will exceed the maximum line length of 120 characters which should trigger a ktlint violation")
    }
}