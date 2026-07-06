package com.makd.afinity.shared.identity

/**
 * BIP39 mnemonic generation, validation, and seed derivation.
 *
 * FROZEN cross-repo contract (viewrr-web#1, viewrr #120/#135, worklet/identity.mjs):
 *   mnemonic --PBKDF2-HMAC-SHA512(salt="mnemonic"+passphrase, 2048 iters, 64 bytes)--> seed64
 *   seed64[0:32] is the Ed25519 keyPair seed.
 *
 * Only the English wordlist is supported (the frozen contract wordlist). Mnemonics are
 * canonicalised to single-space-separated NFKD; for the ASCII English list NFKD is the
 * identity, so UTF-8 bytes are already the normalised bytes.
 */
object Bip39 {

    /** Generates a 12-word (128-bit entropy) English mnemonic using the platform CSPRNG. */
    fun generateMnemonic(): String = entropyToMnemonic(secureRandomBytes(16))

    /** Validates wordlist membership, structure, and the BIP39 checksum. */
    fun validateMnemonic(mnemonic: String): Boolean {
        val words = normalizeWords(mnemonic)
        if (words.isEmpty() || words.size % 3 != 0 || words.size < 12 || words.size > 24) return false

        val indices = IntArray(words.size)
        for (i in words.indices) {
            val idx = BIP39_ENGLISH.indexOf(words[i])
            if (idx < 0) return false
            indices[i] = idx
        }

        val totalBits = words.size * 11
        val checksumBits = totalBits / 33
        val entropyBits = totalBits - checksumBits

        // Reconstruct the bit stream (11 bits per word, MSB first).
        val bits = BooleanArray(totalBits)
        for (wi in indices.indices) {
            val idx = indices[wi]
            for (b in 0 until 11) {
                bits[wi * 11 + b] = ((idx ushr (10 - b)) and 1) == 1
            }
        }

        val entropy = ByteArray(entropyBits / 8)
        for (i in entropy.indices) {
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or (if (bits[i * 8 + b]) 1 else 0)
            }
            entropy[i] = v.toByte()
        }

        val hash = sha256(entropy)
        for (i in 0 until checksumBits) {
            val expected = ((hash[i / 8].toInt() ushr (7 - i % 8)) and 1) == 1
            if (bits[entropyBits + i] != expected) return false
        }
        return true
    }

    /**
     * BIP39 seed derivation: PBKDF2-HMAC-SHA512 over the mnemonic with salt
     * "mnemonic" + [passphrase], 2048 iterations, producing a 64-byte seed.
     */
    fun mnemonicToSeed(mnemonic: String, passphrase: String = ""): ByteArray {
        val canonical = normalizeWords(mnemonic).joinToString(" ")
        val password = canonical.encodeToByteArray()
        val salt = ("mnemonic$passphrase").encodeToByteArray()
        return pbkdf2HmacSha512(password, salt, iterations = 2048, dkLenBytes = 64)
    }

    // ---- internals ----

    internal fun entropyToMnemonic(entropy: ByteArray): String {
        require(entropy.size in intArrayOf(16, 20, 24, 28, 32)) {
            "entropy must be 16/20/24/28/32 bytes, got ${entropy.size}"
        }
        val checksumBits = entropy.size * 8 / 32
        val hash = sha256(entropy)
        // entropy bits followed by the leading checksumBits of the hash.
        val combined = entropy + hash
        val totalBits = entropy.size * 8 + checksumBits
        val numWords = totalBits / 11
        val words = ArrayList<String>(numWords)
        for (w in 0 until numWords) {
            var index = 0
            for (b in 0 until 11) {
                val bitPos = w * 11 + b
                val bit = (combined[bitPos / 8].toInt() ushr (7 - bitPos % 8)) and 1
                index = (index shl 1) or bit
            }
            words.add(BIP39_ENGLISH[index])
        }
        return words.joinToString(" ")
    }

    private fun normalizeWords(mnemonic: String): List<String> =
        mnemonic.trim().split(WHITESPACE).filter { it.isNotEmpty() }

    private val WHITESPACE = Regex("\\s+")
}
