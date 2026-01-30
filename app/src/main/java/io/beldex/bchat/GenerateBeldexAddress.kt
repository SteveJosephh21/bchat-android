package io.beldex.bchat

import org.bouncycastle.crypto.digests.KeccakDigest
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import java.io.ByteArrayOutputStream
import java.math.BigInteger

data class KeyPairResult(
    val sec: ByteArray,
    val pub: ByteArray
)

data class AddressKeys(
    val spend: KeyPairResult,
    val view: KeyPairResult,
    val address: String
)

fun generateKeyPairFromScalar(secret: ByteArray): KeyPairResult {

    val reduced = BeldexCrypto.scReduce32(secret)

    val priv = Ed25519PrivateKeyParameters(reduced, 0)
    val pub = priv.generatePublicKey().encoded   // 32 bytes, Ed25519

    return KeyPairResult(reduced, pub)
}

/* ===================== CRYPTO ===================== */

object BeldexCrypto {

    private val L = BigInteger(
        "723700557733226221397318656304299424085711635937990760600195093828545425857"
    )

    fun scReduce32(input: ByteArray): ByteArray {
        // little-endian
        val reversed = input.reversedArray()
        val n = BigInteger(1, reversed).mod(L)
        val out = n.toByteArray().let {
            if (it.size > 32) it.copyOfRange(it.size - 32, it.size) else it
        }
        return out.reversedArray().let {
            if (it.size < 32) it + ByteArray(32 - it.size) else it
        }
    }

    fun keccak256(input: ByteArray): ByteArray {
        val d = KeccakDigest(256)
        d.update(input, 0, input.size)
        return ByteArray(32).also { d.doFinal(it, 0) }
    }
}

/* ===================== ADDRESS ===================== */

object BeldexAddress {

    const val BELDEX_PREFIX = 0xD1

    fun fromSeed(seed: ByteArray): AddressKeys {
        require(seed.size == 32)

        // spend key: sc_reduce32(seed)
        val spend = generateKeyPairFromScalar(seed)

        // view key: sc_reduce32(keccak(spend_secret))
        val viewSeed = BeldexCrypto.keccak256(spend.sec)
        val view = generateKeyPairFromScalar(viewSeed)

        val address = Base58.encodeAddress(spend.pub, view.pub)
        return AddressKeys(spend, view, address)
    }
}


/* ===================== BASE58 ===================== */

object Base58 {

    private const val ALPHABET =
        "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    private val ENCODED_BLOCK_SIZES = intArrayOf(0, 2, 3, 5, 6, 7, 9, 10, 11)

    fun encodeAddress(spend: ByteArray, view: ByteArray): String {
        val data = ByteArrayOutputStream().apply {
            write(encodeVarInt(BeldexAddress.BELDEX_PREFIX))
            write(spend)
            write(view)
        }.toByteArray()

        val checksum = BeldexCrypto.keccak256(data).copyOfRange(0, 4)
        return base58Encode(data + checksum)
    }

    fun base58Encode(data: ByteArray): String {
        val result = StringBuilder()
        var offset = 0

        while (offset < data.size) {
            val blockSize = minOf(8, data.size - offset)
            val block = data.copyOfRange(offset, offset + blockSize)

            // little-endian block
            val num = BigInteger(1, block.reversedArray())
            var n = num
            val sb = StringBuilder()

            while (n > BigInteger.ZERO) {
                val divRem = n.divideAndRemainder(BigInteger.valueOf(58))
                sb.append(ALPHABET[divRem[1].toInt()])
                n = divRem[0]
            }

            result.append(
                sb.reverse().toString()
                    .padStart(ENCODED_BLOCK_SIZES[blockSize], ALPHABET[0])
            )

            offset += blockSize
        }
        return result.toString()
    }

    fun encodeVarInt(value: Int): ByteArray {
        var v = value
        val out = ByteArrayOutputStream()

        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                out.write(v)
                break
            } else {
                out.write((v and 0x7F) or 0x80)
                v = v ushr 7
            }
        }
        return out.toByteArray()
    }


}