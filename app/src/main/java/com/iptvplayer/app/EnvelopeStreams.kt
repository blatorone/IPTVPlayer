package com.iptvplayer.app

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/*
 * Envelope format for large local snapshots.
 *
 * A fresh random AES-256 data key encrypts the payload in independent AES-GCM
 * chunks; only the 32-byte data key is encrypted ("wrapped") with the Android
 * Keystore key. Large payloads are therefore processed in the app process with
 * constant memory instead of being sent through the Keystore in one piece, which
 * was slow and briefly needed several hundred megabytes on TV devices.
 *
 * Layout: magic | wrapped key length | wrapped key | 12-byte base nonce |
 * chunks of (final flag | sealed length | sealed bytes). Each chunk nonce is the
 * base nonce combined with the chunk index, and index plus final flag are
 * authenticated, so reordered, altered or truncated files are rejected.
 */
private const val ENVELOPE_MAGIC = 0x53575032 // "SWP2"
private const val ENVELOPE_CHUNK = 256 * 1024
private const val TAG_BITS = 128

internal fun newEnvelopeDataKey(): SecretKey =
    KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

private fun chunkNonce(base: ByteArray, index: Long): ByteArray = base.copyOf().also { nonce ->
    for (byte in 0 until 8) {
        nonce[4 + byte] = (nonce[4 + byte].toInt() xor (index ushr (56 - byte * 8)).toInt()).toByte()
    }
}

private fun chunkAad(index: Long, final: Boolean): ByteArray = ByteArray(9).also { aad ->
    for (byte in 0 until 8) aad[byte] = (index ushr (56 - byte * 8)).toByte()
    aad[8] = if (final) 1 else 0
}

/** Encrypts everything written to it; [close] writes the final chunk but leaves [output] open. */
internal class EnvelopeOutputStream(
    output: OutputStream,
    private val key: SecretKey,
    wrappedKey: ByteArray,
    random: SecureRandom = SecureRandom()
) : OutputStream() {
    private val data = DataOutputStream(output)
    private val baseNonce = ByteArray(12).also(random::nextBytes)
    private val buffer = ByteArray(ENVELOPE_CHUNK)
    private var size = 0
    private var index = 0L
    private var closed = false

    init {
        require(wrappedKey.isNotEmpty() && wrappedKey.size <= 4_096)
        data.writeInt(ENVELOPE_MAGIC)
        data.writeInt(wrappedKey.size)
        data.write(wrappedKey)
        data.write(baseNonce)
    }

    override fun write(b: Int) {
        check(!closed)
        if (size == buffer.size) seal(final = false)
        buffer[size++] = b.toByte()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        check(!closed)
        var offset = off
        var remaining = len
        while (remaining > 0) {
            if (size == buffer.size) seal(final = false)
            val count = minOf(remaining, buffer.size - size)
            System.arraycopy(b, offset, buffer, size, count)
            size += count
            offset += count
            remaining -= count
        }
    }

    private fun seal(final: Boolean) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, chunkNonce(baseNonce, index)))
        cipher.updateAAD(chunkAad(index, final))
        val sealed = cipher.doFinal(buffer, 0, size)
        data.writeByte(if (final) 1 else 0)
        data.writeInt(sealed.size)
        data.write(sealed)
        index++
        size = 0
    }

    override fun flush() = data.flush()

    override fun close() {
        if (closed) return
        seal(final = true)
        closed = true
        data.flush()
    }
}

/** Decrypts an envelope chunk by chunk; [unwrap] turns the stored wrapped key into the data key. */
internal class EnvelopeInputStream(
    private val input: InputStream,
    unwrap: (ByteArray) -> SecretKey
) : InputStream() {
    private val data = DataInputStream(input)
    private val key: SecretKey
    private val baseNonce = ByteArray(12)
    private var chunk = ByteArray(0)
    private var position = 0
    private var index = 0L
    private var finished = false

    init {
        if (data.readInt() != ENVELOPE_MAGIC) throw IOException("Unknown snapshot format")
        val wrappedSize = data.readInt()
        if (wrappedSize !in 1..4_096) throw IOException("Invalid snapshot header")
        val wrapped = ByteArray(wrappedSize).also(data::readFully)
        key = unwrap(wrapped)
        data.readFully(baseNonce)
    }

    private fun nextChunk(): Boolean {
        if (finished) return false
        val final = when (data.readByte().toInt()) {
            0 -> false
            1 -> true
            else -> throw IOException("Invalid snapshot chunk")
        }
        val sealedSize = data.readInt()
        if (sealedSize !in TAG_BITS / 8..ENVELOPE_CHUNK + TAG_BITS / 8) throw IOException("Invalid snapshot chunk")
        val sealed = ByteArray(sealedSize).also(data::readFully)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, chunkNonce(baseNonce, index)))
        cipher.updateAAD(chunkAad(index, final))
        chunk = try {
            cipher.doFinal(sealed)
        } catch (error: java.security.GeneralSecurityException) {
            throw IOException("Snapshot could not be authenticated", error)
        }
        position = 0
        index++
        finished = final
        return true
    }

    override fun read(): Int {
        while (position == chunk.size) if (!nextChunk()) return -1
        return chunk[position++].toInt() and 0xff
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        while (position == chunk.size) if (!nextChunk()) return -1
        val count = minOf(len, chunk.size - position)
        System.arraycopy(chunk, position, b, off, count)
        position += count
        return count
    }

    override fun close() = input.close()
}
