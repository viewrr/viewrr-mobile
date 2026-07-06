package com.makd.afinity.shared.identity

import java.security.SecureRandom
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

/**
 * Android / JVM-host Ed25519 via BouncyCastle (pure-Java, RFC 8032). This is also the
 * implementation exercised by :shared:testAndroidHostTest, so the golden vectors run here.
 */
internal actual object Ed25519 {

    actual fun publicKeyFromSeed(seed: ByteArray): ByteArray {
        require(seed.size == 32) { "seed must be 32 bytes, got ${seed.size}" }
        return Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded
    }

    actual fun sign(message: ByteArray, seed: ByteArray): ByteArray {
        require(seed.size == 32) { "seed must be 32 bytes, got ${seed.size}" }
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(seed, 0))
        signer.update(message, 0, message.size)
        return signer.generateSignature()
    }

    actual fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        require(publicKey.size == 32) { "publicKey must be 32 bytes, got ${publicKey.size}" }
        val signer = Ed25519Signer()
        signer.init(false, Ed25519PublicKeyParameters(publicKey, 0))
        signer.update(message, 0, message.size)
        return signer.verifySignature(signature)
    }
}

private val secureRandom by lazy { SecureRandom() }

internal actual fun secureRandomBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    secureRandom.nextBytes(bytes)
    return bytes
}
