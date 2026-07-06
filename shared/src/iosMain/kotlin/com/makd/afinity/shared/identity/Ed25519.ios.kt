package com.makd.afinity.shared.identity

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.signature.InvalidSignatureException
import com.ionspin.kotlin.crypto.signature.Signature
import com.ionspin.kotlin.crypto.util.LibsodiumRandom

/**
 * iOS Ed25519 via ionspin's libsodium bindings — the exact crypto stack the server/worklet
 * use (`crypto_sign_seed_keypair` / detached sign+verify), so a mnemonic yields one identity
 * usable in both app auth and the P2P swarm.
 *
 * libsodium's native init is synchronous, so `initializeWithCallback` completes inline here.
 */
private fun ensureSodium() {
    if (!LibsodiumInitializer.isInitialized()) {
        LibsodiumInitializer.initializeWithCallback { }
    }
}

internal actual object Ed25519 {

    actual fun publicKeyFromSeed(seed: ByteArray): ByteArray {
        require(seed.size == 32) { "seed must be 32 bytes, got ${seed.size}" }
        ensureSodium()
        return Signature.seedKeypair(seed.toUByteArray()).publicKey.toByteArray()
    }

    actual fun sign(message: ByteArray, seed: ByteArray): ByteArray {
        require(seed.size == 32) { "seed must be 32 bytes, got ${seed.size}" }
        ensureSodium()
        val keyPair = Signature.seedKeypair(seed.toUByteArray())
        return Signature.detached(message.toUByteArray(), keyPair.secretKey).toByteArray()
    }

    actual fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        require(publicKey.size == 32) { "publicKey must be 32 bytes, got ${publicKey.size}" }
        ensureSodium()
        return try {
            Signature.verifyDetached(signature.toUByteArray(), message.toUByteArray(), publicKey.toUByteArray())
            true
        } catch (e: InvalidSignatureException) {
            false
        }
    }
}

internal actual fun secureRandomBytes(size: Int): ByteArray {
    ensureSodium()
    return LibsodiumRandom.buf(size).toByteArray()
}
