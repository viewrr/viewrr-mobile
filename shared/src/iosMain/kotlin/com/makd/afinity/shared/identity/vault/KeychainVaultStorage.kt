package com.makd.afinity.shared.identity.vault

import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

/**
 * iOS [VaultStorage] backed by the Keychain (#142 rung 2). The (already password-encrypted)
 * [VaultBlob] is stored as a generic-password item accessible only after first unlock and only on
 * this device (`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`) — defense-in-depth over the
 * master-password encryption. The plaintext seed is never stored; only the AEAD ciphertext blob.
 */
@OptIn(ExperimentalForeignApi::class)
class KeychainVaultStorage(
    private val service: String = DEFAULT_SERVICE,
    private val account: String = DEFAULT_ACCOUNT,
) : VaultStorage {

    override fun read(): ByteArray? {
        val svc = CFBridgingRetain(service)
        val acc = CFBridgingRetain(account)
        try {
            val q = buildQuery(
                listOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to svc,
                    kSecAttrAccount to acc,
                    kSecReturnData to kCFBooleanTrue,
                    kSecMatchLimit to kSecMatchLimitOne,
                ),
            )
            val data = memScoped {
                val out = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(q, out.ptr)
                q?.let { CFRelease(it) }
                if (status != errSecSuccess) null else CFBridgingRelease(out.value) as? NSData
            }
            return data?.toByteArray()
        } finally {
            CFBridgingRelease(svc)
            CFBridgingRelease(acc)
        }
    }

    override fun write(blob: ByteArray) {
        clear()
        val data = CFBridgingRetain(blob.toNSData())
        val svc = CFBridgingRetain(service)
        val acc = CFBridgingRetain(account)
        try {
            val q = buildQuery(
                listOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to svc,
                    kSecAttrAccount to acc,
                    kSecValueData to data,
                    kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                ),
            )
            val status = SecItemAdd(q, null)
            q?.let { CFRelease(it) }
            check(status == errSecSuccess) { "keychain write failed: $status" }
        } finally {
            CFBridgingRelease(data)
            CFBridgingRelease(svc)
            CFBridgingRelease(acc)
        }
    }

    override fun exists(): Boolean = read() != null

    override fun clear() {
        val svc = CFBridgingRetain(service)
        val acc = CFBridgingRetain(account)
        try {
            val q = buildQuery(
                listOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to svc,
                    kSecAttrAccount to acc,
                ),
            )
            SecItemDelete(q)
            q?.let { CFRelease(it) }
        } finally {
            CFBridgingRelease(svc)
            CFBridgingRelease(acc)
        }
    }

    /**
     * Builds a transient CFDictionary with null key/value callbacks. The caller keeps every value
     * alive (constant CFStringRefs, or CFBridgingRetain'd NSData/NSString) until after the SecItem
     * call, so the dictionary needs no retain callbacks of its own.
     */
    private fun buildQuery(pairs: List<Pair<CFStringRef?, CFTypeRef?>>): CFDictionaryRef? = memScoped {
        val n = pairs.size
        val keys = allocArray<COpaquePointerVar>(n)
        val values = allocArray<COpaquePointerVar>(n)
        pairs.forEachIndexed { i, (k, v) ->
            keys[i] = k
            values[i] = v
        }
        CFDictionaryCreate(kCFAllocatorDefault, keys, values, n.toLong(), null, null)
    }

    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        val src = bytes
        val out = ByteArray(size)
        if (size > 0 && src != null) {
            out.usePinned { memcpy(it.addressOf(0), src, size.toULong()) }
        }
        return out
    }

    companion object {
        const val DEFAULT_SERVICE = "stream.viewrr.identity"
        const val DEFAULT_ACCOUNT = "self-custody-vault"
    }
}
