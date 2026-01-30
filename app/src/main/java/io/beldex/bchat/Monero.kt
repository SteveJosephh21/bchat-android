package io.beldex.bchat

import com.iwebpp.crypto.TweetNaclFast

object Curve25519 {
    fun ge_scalarmult_base(scalar: ByteArray): ByteArray {
        require(scalar.size == 32) { "Scalar must be 32 bytes" }
        val pub = ByteArray(32)
        TweetNaclFast.crypto_scalarmult_base(pub, scalar)
        return pub
    }
}