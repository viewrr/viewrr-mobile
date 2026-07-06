package com.makd.afinity.shared.identity

import org.kotlincrypto.hash.sha2.SHA256
import org.kotlincrypto.macs.hmac.sha2.HmacSHA512

/**
 * Pure-Kotlin (multiplatform) crypto primitives used by the identity core. SHA-256 and
 * HMAC-SHA512 come from KotlinCrypto (vetted, common code). PBKDF2 below is the standard
 * KDF construction layered on the vetted HMAC — data shuffling, not curve math.
 */

internal fun sha256(data: ByteArray): ByteArray = SHA256().digest(data)

/**
 * PBKDF2-HMAC-SHA512, per RFC 8018 / PBKDF2. Used by BIP39 mnemonicToSeed (2048 iterations,
 * 64-byte output). Reuses one keyed HMAC instance; KotlinCrypto's Mac resets after doFinal.
 */
internal fun pbkdf2HmacSha512(
    password: ByteArray,
    salt: ByteArray,
    iterations: Int,
    dkLenBytes: Int,
): ByteArray {
    require(iterations > 0) { "iterations must be > 0" }
    require(dkLenBytes > 0) { "dkLenBytes must be > 0" }
    val hLen = 64
    val numBlocks = (dkLenBytes + hLen - 1) / hLen
    val output = ByteArray(numBlocks * hLen)
    val mac = HmacSHA512(password)

    val saltWithIndex = ByteArray(salt.size + 4)
    salt.copyInto(saltWithIndex)

    for (blockIndex in 1..numBlocks) {
        // INT_32_BE(blockIndex) appended to salt for the first HMAC.
        saltWithIndex[salt.size] = (blockIndex ushr 24).toByte()
        saltWithIndex[salt.size + 1] = (blockIndex ushr 16).toByte()
        saltWithIndex[salt.size + 2] = (blockIndex ushr 8).toByte()
        saltWithIndex[salt.size + 3] = blockIndex.toByte()

        var u = mac.doFinal(saltWithIndex) // U1
        val t = u.copyOf()
        for (iteration in 2..iterations) {
            u = mac.doFinal(u) // U_iteration
            for (k in t.indices) {
                t[k] = (t[k].toInt() xor u[k].toInt()).toByte()
            }
        }
        t.copyInto(output, (blockIndex - 1) * hLen)
    }
    return output.copyOf(dkLenBytes)
}

// ---- hex helpers (lowercase, matching the frozen wire format) ----

internal fun ByteArray.toLowerHex(): String {
    val hexChars = "0123456789abcdef"
    val sb = StringBuilder(size * 2)
    for (b in this) {
        val v = b.toInt() and 0xff
        sb.append(hexChars[v ushr 4])
        sb.append(hexChars[v and 0x0f])
    }
    return sb.toString()
}

internal fun hexToBytes(hex: String): ByteArray {
    require(hex.length % 2 == 0) { "hex must have even length" }
    val out = ByteArray(hex.length / 2)
    for (i in out.indices) {
        val hi = hexNibble(hex[i * 2])
        val lo = hexNibble(hex[i * 2 + 1])
        out[i] = ((hi shl 4) or lo).toByte()
    }
    return out
}

private fun hexNibble(c: Char): Int = when (c) {
    in '0'..'9' -> c - '0'
    in 'a'..'f' -> c - 'a' + 10
    in 'A'..'F' -> c - 'A' + 10
    else -> throw IllegalArgumentException("invalid hex char: $c")
}
