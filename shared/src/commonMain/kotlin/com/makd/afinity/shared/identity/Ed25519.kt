package com.makd.afinity.shared.identity

/**
 * Platform-native Ed25519 (RFC 8032). Every RFC 8032 implementation yields the same
 * pubkey/signature for a given 32-byte seed, which is what keeps this deterministic
 * across platforms and byte-for-byte compatible with the server/worklet libsodium stack.
 *
 * - androidMain / JVM host: BouncyCastle (`Ed25519PrivateKeyParameters`).
 * - iosMain: ionspin libsodium bindings (`crypto_sign_seed_keypair` — the exact stack the
 *   Hub verifier accepts, proven by viewrr WorkletIdentityParityTest).
 *
 * The "secret" here is the 32-byte seed (the BIP39-derived root secret). libsodium's
 * 64-byte secretKey is reconstructed from it on demand inside the iOS actual.
 */
internal expect object Ed25519 {
    /** seed: 32 bytes. Returns the 32-byte raw Ed25519 public key. */
    fun publicKeyFromSeed(seed: ByteArray): ByteArray

    /** seed: 32 bytes. Returns a 64-byte detached signature over [message]. */
    fun sign(message: ByteArray, seed: ByteArray): ByteArray

    /** Verifies a 64-byte detached [signature] over [message] against a 32-byte [publicKey]. */
    fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean
}

/** Cryptographically secure random bytes from the platform CSPRNG. */
internal expect fun secureRandomBytes(size: Int): ByteArray
