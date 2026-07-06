package com.makd.afinity.shared.identity

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Golden vectors for the FROZEN cross-repo identity contract (#142 rung 1).
 *
 * Sources of every pinned value:
 *  - deriveHandle goldens: viewrr-web#1 src/identity/deriveHandle.test.ts.
 *  - zero-seed pubkey + signature: viewrr WorkletIdentityParityTest.kt (proves parity with the
 *    libsodium worklet + JDK Hub verifier). 00*32 is also RFC 8032 test vector 1.
 *  - Trezor BIP39 vector: the canonical BIP39 test vectors (passphrase "TREZOR").
 *  - full-chain (mnemonic -> seed32 -> pubkey -> handle): generated with @scure/bip39 +
 *    @noble/ed25519 and cross-checked to match the server libsodium stack byte-for-byte.
 */
class ViewrrIdentityTest {

    private val abandonMnemonic =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

    // ---- deriveHandle (frozen contract) ----

    @Test
    fun deriveHandleGoldenAllZero() {
        assertEquals("abandon-abandon-0000", ViewrrIdentity.deriveHandle("00".repeat(32)))
    }

    @Test
    fun deriveHandleSuffixFromLastTwoBytes() {
        assertEquals("abandon-abandon-9f2a", ViewrrIdentity.deriveHandle("00".repeat(30) + "9f2a"))
    }

    @Test
    fun deriveHandleRejectsNon32Byte() {
        var threw = false
        try {
            ViewrrIdentity.deriveHandle("00".repeat(16))
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw, "deriveHandle must reject keys that are not 32 bytes")
    }

    // ---- seed32 -> Ed25519 pubkey (server/worklet parity golden) ----

    @Test
    fun zeroSeedPublicKeyMatchesWorkletParityGolden() {
        val seed = ByteArray(32) // 00*32
        val kp = ViewrrIdentity.keyPairFromSeed(seed)
        assertEquals(
            "3b6a27bcceb6a42d62a3a8d02a6f0d73653215771de243a63ac048a18b59da29",
            kp.publicKeyHex,
        )
    }

    @Test
    fun zeroSeedRegisterSignatureMatchesWorkletParityGolden() {
        val seed = ByteArray(32)
        val message = ViewrrIdentity.REGISTER_MESSAGE.encodeToByteArray()
        val sig = ViewrrIdentity.sign(message, seed)
        assertEquals(
            "dead1e14fade052b9f644959fd7dbc198c4073cf07dda8002c7fa9efc66603e9" +
                "b13075c8f71a90b13096bdf9d2c011e550023eb5b18e4f72cc360d280d756609",
            sig.toLowerHex(),
        )
        val pub = hexToBytes("3b6a27bcceb6a42d62a3a8d02a6f0d73653215771de243a63ac048a18b59da29")
        assertTrue(ViewrrIdentity.verify(message, sig, pub))
    }

    // ---- BIP39 mnemonicToSeed (published + contract vectors) ----

    @Test
    fun bip39TrezorPublishedVector() {
        // Canonical BIP39 test vector, passphrase "TREZOR".
        val seed = ViewrrIdentity.mnemonicToSeed(abandonMnemonic, "TREZOR")
        assertEquals(
            "c55257c360c07c72029aebc1b53c05ed0362ada38ead3e3e9efa3708e5349553" +
                "1f09a6987599d18264c1e1c92f2cf141630c7a3c4ab7c81b2f001698e7463b04",
            seed.toLowerHex(),
        )
    }

    @Test
    fun bip39ContractSeedEmptyPassphrase() {
        val seed = ViewrrIdentity.mnemonicToSeed(abandonMnemonic)
        assertEquals(
            "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc1" +
                "9a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4",
            seed.toLowerHex(),
        )
    }

    // ---- full-chain determinism proof: mnemonic -> pubkey -> handle ----

    @Test
    fun fullChainMnemonicToPublicKeyAndHandle() {
        val kp = ViewrrIdentity.keyPairFromMnemonic(abandonMnemonic)
        assertContentEquals(
            hexToBytes("5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc1"),
            kp.seed,
        )
        assertEquals(
            "c5785e1865b708938aff8161d573006496663b1aa10834e396dc566869a2c66a",
            kp.publicKeyHex,
        )
        assertEquals("pyramid-seek-c66a", kp.handle)
    }

    // ---- BIP39 gen / validate ----

    @Test
    fun validateAcceptsCanonicalMnemonic() {
        assertTrue(ViewrrIdentity.validateMnemonic(abandonMnemonic))
    }

    @Test
    fun validateRejectsBadChecksum() {
        // Swap the last word for another valid wordlist word -> checksum breaks.
        val bad =
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon zoo"
        assertFalse(ViewrrIdentity.validateMnemonic(bad))
    }

    @Test
    fun validateRejectsUnknownWord() {
        val bad =
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon notaword"
        assertFalse(ViewrrIdentity.validateMnemonic(bad))
    }

    @Test
    fun generatedMnemonicIsTwelveWordsAndValid() {
        val mnemonic = ViewrrIdentity.generateMnemonic()
        assertEquals(12, mnemonic.split(" ").size)
        assertTrue(ViewrrIdentity.validateMnemonic(mnemonic))
    }

    // ---- sign / verify round-trip on a derived identity ----

    @Test
    fun signVerifyRoundTrip() {
        val kp = ViewrrIdentity.keyPairFromMnemonic(abandonMnemonic)
        val message = "hello viewrr".encodeToByteArray()
        val sig = ViewrrIdentity.sign(message, kp.seed)
        assertEquals(64, sig.size)
        assertTrue(ViewrrIdentity.verify(message, sig, kp.publicKey))
        // Tampered message must fail.
        assertFalse(ViewrrIdentity.verify("hello viewr".encodeToByteArray(), sig, kp.publicKey))
    }

    @Test
    fun entropyToMnemonicAllZeroEntropyIsAbandonAbout() {
        // 16 zero bytes -> the canonical all-"abandon" 12-word mnemonic ending in "about".
        assertEquals(abandonMnemonic, Bip39.entropyToMnemonic(ByteArray(16)))
    }
}
