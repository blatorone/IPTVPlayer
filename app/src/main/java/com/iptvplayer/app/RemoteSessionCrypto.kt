package com.iptvplayer.app

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end encryption for the temporary remote settings session (phone ↔ TV).
 *
 * The TV creates a random 32-byte session secret. It only travels inside the QR code, in the
 * URL fragment (after `#`), which browsers never send to a server. From the secret both sides
 * derive, with HKDF-SHA256:
 * - the room id the relay uses to pair the two connections (reveals nothing about the key),
 * - the AES-256-GCM key for every message.
 *
 * The relay therefore only sees a room id and ciphertext. A frame is
 * `1.<seq>.<nonce>.<ciphertext>` (base64url, no padding). The sender's role and the sequence
 * number are authenticated as associated data, so frames cannot be reflected back to their
 * sender, replayed or reordered unnoticed.
 *
 * The same format is implemented by the phone page with WebCrypto, and can be implemented by
 * future iOS/Linux builds with any AES-GCM and HKDF implementation.
 */
internal class RemoteSessionCrypto private constructor(val secret: ByteArray) {
    enum class Role(val wire: String) { TV("tv"), PHONE("phone") }

    val roomId: String = encode(hkdf(secret, ROOM_INFO, 16))
    private val key = SecretKeySpec(hkdf(secret, KEY_INFO, 32), "AES")
    val encodedSecret: String get() = encode(secret)

    /** Highest sequence number accepted per sending role; frames must strictly increase. */
    private val lastSeen = mutableMapOf<Role, Long>()
    private var nextSeq = 1L

    fun seal(sender: Role, plaintext: ByteArray, random: SecureRandom = SECURE_RANDOM): String {
        val seq = nextSeq++
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(associatedData(sender, seq))
        val sealed = cipher.doFinal(plaintext)
        return "$VERSION.$seq.${encode(nonce)}.${encode(sealed)}"
    }

    /** Returns the plaintext, or null for anything forged, replayed, reordered or malformed. */
    fun open(sender: Role, frame: String): ByteArray? {
        if (frame.length > MAX_FRAME_CHARS) return null
        val parts = frame.split('.')
        if (parts.size != 4 || parts[0] != VERSION) return null
        val seq = parts[1].toLongOrNull()?.takeIf { it > 0 } ?: return null
        if (seq <= (lastSeen[sender] ?: 0L)) return null
        val nonce = decode(parts[2])?.takeIf { it.size == NONCE_BYTES } ?: return null
        val sealed = decode(parts[3])?.takeIf { it.size >= TAG_BITS / 8 } ?: return null
        val plaintext = runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(associatedData(sender, seq))
            cipher.doFinal(sealed)
        }.getOrNull() ?: return null
        lastSeen[sender] = seq
        return plaintext
    }

    /**
     * A reconnecting peer starts counting at 1 again. The device then announces a fresh epoch that
     * every later command must carry, so frames of an earlier connection are still rejected.
     */
    fun resetPeer(sender: Role) {
        lastSeen.remove(sender)
    }

    private fun associatedData(sender: Role, seq: Long) =
        "$AAD_PREFIX|${sender.wire}|$seq".toByteArray(Charsets.UTF_8)

    companion object {
        private const val VERSION = "1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128
        private const val NONCE_BYTES = 12
        private const val SECRET_BYTES = 32
        private const val AAD_PREFIX = "streamwatch-remote/1"
        private val ROOM_INFO = "streamwatch-remote/1 room".toByteArray(Charsets.UTF_8)
        private val KEY_INFO = "streamwatch-remote/1 key".toByteArray(Charsets.UTF_8)
        /** Large enough for a portal list or category selection, small enough to bound memory. */
        const val MAX_FRAME_CHARS = 512 * 1024
        private val SECURE_RANDOM = SecureRandom()

        fun create(random: SecureRandom = SECURE_RANDOM) =
            RemoteSessionCrypto(ByteArray(SECRET_BYTES).also(random::nextBytes))

        /** The phone side: rebuilds the session from the secret in the QR code. */
        fun fromEncodedSecret(encoded: String): RemoteSessionCrypto? =
            decode(encoded)?.takeIf { it.size == SECRET_BYTES }?.let(::RemoteSessionCrypto)

        internal fun hkdf(secret: ByteArray, info: ByteArray, length: Int): ByteArray {
            // RFC 5869 with an empty salt (= 32 zero bytes); the secret is already uniformly random.
            val extract = Mac.getInstance("HmacSHA256")
            extract.init(SecretKeySpec(ByteArray(32), "HmacSHA256"))
            val prk = extract.doFinal(secret)
            val expand = Mac.getInstance("HmacSHA256")
            expand.init(SecretKeySpec(prk, "HmacSHA256"))
            val output = ByteArray(length)
            var previous = ByteArray(0)
            var offset = 0
            var counter = 1
            while (offset < length) {
                expand.update(previous)
                expand.update(info)
                expand.update(counter.toByte())
                previous = expand.doFinal()
                val count = minOf(previous.size, length - offset)
                System.arraycopy(previous, 0, output, offset, count)
                offset += count
                counter++
            }
            return output
        }

        private fun encode(bytes: ByteArray): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        private fun decode(text: String): ByteArray? =
            runCatching { Base64.getUrlDecoder().decode(text) }.getOrNull()
    }
}
