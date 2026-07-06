package com.makd.afinity.shared.identity.vault

/**
 * Versioned, self-describing binary layout for the at-rest vault blob (#142 rung 2). All the
 * parameters needed to re-derive the KEK and decrypt (KDF iterations, salt, nonce) travel with
 * the ciphertext, so a stored blob is decryptable on its own given the master password.
 *
 * Layout (all integers big-endian):
 *   [0]                 version (currently [VERSION])
 *   [1..4]              kdfIterations : Int
 *   [5]                 saltLen : UByte
 *   [6 .. 6+saltLen)    salt
 *   [.]                 nonceLen : UByte
 *   [. .. .+nonceLen)   nonce
 *   [4 bytes]           cipherLen : Int
 *   [cipherLen bytes]   ciphertext (AEAD output, includes auth tag)
 *
 * There is intentionally NO plaintext seed anywhere in this structure — only AEAD ciphertext.
 */
internal data class VaultBlob(
    val kdfIterations: Int,
    val salt: ByteArray,
    val nonce: ByteArray,
    val ciphertext: ByteArray,
) {
    fun encode(): ByteArray {
        require(salt.size in 1..255) { "salt length out of range: ${salt.size}" }
        require(nonce.size in 1..255) { "nonce length out of range: ${nonce.size}" }
        val out = ArrayList<Byte>(1 + 4 + 1 + salt.size + 1 + nonce.size + 4 + ciphertext.size)
        out.add(VERSION)
        out.addInt(kdfIterations)
        out.add(salt.size.toByte())
        salt.forEach { out.add(it) }
        out.add(nonce.size.toByte())
        nonce.forEach { out.add(it) }
        out.addInt(ciphertext.size)
        ciphertext.forEach { out.add(it) }
        return out.toByteArray()
    }

    companion object {
        const val VERSION: Byte = 1

        /** Parses a blob produced by [encode]. Throws [IllegalArgumentException] on malformed input. */
        fun decode(bytes: ByteArray): VaultBlob {
            val c = Cursor(bytes)
            val version = c.byte()
            require(version == VERSION) { "unsupported vault blob version: $version" }
            val iterations = c.int()
            require(iterations > 0) { "invalid kdf iterations: $iterations" }
            val salt = c.bytes(c.uByteLen())
            val nonce = c.bytes(c.uByteLen())
            val ciphertext = c.bytes(c.int())
            require(c.remaining() == 0) { "trailing bytes in vault blob" }
            return VaultBlob(iterations, salt, nonce, ciphertext)
        }
    }
}

private fun ArrayList<Byte>.addInt(value: Int) {
    add((value ushr 24).toByte())
    add((value ushr 16).toByte())
    add((value ushr 8).toByte())
    add(value.toByte())
}

private class Cursor(private val data: ByteArray) {
    private var pos = 0

    fun byte(): Byte {
        require(pos < data.size) { "vault blob truncated" }
        return data[pos++]
    }

    fun uByteLen(): Int = byte().toInt() and 0xff

    fun int(): Int {
        require(pos + 4 <= data.size) { "vault blob truncated (int)" }
        val v = ((data[pos].toInt() and 0xff) shl 24) or
            ((data[pos + 1].toInt() and 0xff) shl 16) or
            ((data[pos + 2].toInt() and 0xff) shl 8) or
            (data[pos + 3].toInt() and 0xff)
        pos += 4
        return v
    }

    fun bytes(len: Int): ByteArray {
        require(len >= 0 && pos + len <= data.size) { "vault blob truncated (len=$len)" }
        val out = data.copyOfRange(pos, pos + len)
        pos += len
        return out
    }

    fun remaining(): Int = data.size - pos
}
