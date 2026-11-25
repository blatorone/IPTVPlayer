package com.iptvplayer.app

import com.iptvplayer.app.RemoteSessionCrypto.Role
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteSessionCryptoTest {
    private fun pair(): Pair<RemoteSessionCrypto, RemoteSessionCrypto> {
        val tv = RemoteSessionCrypto.create()
        val phone = RemoteSessionCrypto.fromEncodedSecret(tv.encodedSecret)!!
        return tv to phone
    }

    @Test
    fun hkdfMatchesRfc5869TestCase3() {
        // RFC 5869 A.3: IKM = 22 × 0x0b, empty salt, empty info, L = 42.
        val okm = RemoteSessionCrypto.hkdf(ByteArray(22) { 0x0b }, ByteArray(0), 42)
        assertEquals(
            "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8",
            okm.joinToString("") { "%02x".format(it) }
        )
    }

    @Test
    fun phoneAndTvShareRoomAndExchangeMessages() {
        val (tv, phone) = pair()
        assertEquals(tv.roomId, phone.roomId)
        val frame = phone.seal(Role.PHONE, "hallo".toByteArray())
        assertArrayEquals("hallo".toByteArray(), tv.open(Role.PHONE, frame))
        val reply = tv.seal(Role.TV, "ok".toByteArray())
        assertArrayEquals("ok".toByteArray(), phone.open(Role.TV, reply))
    }

    @Test
    fun roomIdDoesNotRevealSecret() {
        val (tv, _) = pair()
        assertNotEquals(tv.encodedSecret, tv.roomId)
        assertEquals(22, tv.roomId.length)
    }

    @Test
    fun rejectsReplayReflectionAndTampering() {
        val (tv, phone) = pair()
        val frame = phone.seal(Role.PHONE, "pin".toByteArray())
        assertNotNull(tv.open(Role.PHONE, frame))
        assertNull("replay", tv.open(Role.PHONE, frame))
        // A phone frame echoed back as if the TV had sent it must fail authentication.
        val fresh = RemoteSessionCrypto.fromEncodedSecret(tv.encodedSecret)!!
        assertNull("reflection", fresh.open(Role.TV, phone.seal(Role.PHONE, "x".toByteArray())))
        val parts = phone.seal(Role.PHONE, "y".toByteArray()).split('.')
        val flipped = parts[3].replaceFirstChar { if (it == 'A') 'B' else 'A' }
        assertNull("tampered", tv.open(Role.PHONE, listOf(parts[0], parts[1], parts[2], flipped).joinToString(".")))
        assertNull("garbage", tv.open(Role.PHONE, "1.99.abc.def"))
    }

    @Test
    fun otherSessionCannotRead() {
        val (_, phone) = pair()
        val stranger = RemoteSessionCrypto.create()
        assertNull(stranger.open(Role.PHONE, phone.seal(Role.PHONE, "geheim".toByteArray())))
    }

    @Test
    fun rejectsMalformedSecrets() {
        assertNull(RemoteSessionCrypto.fromEncodedSecret("kurz"))
        assertNull(RemoteSessionCrypto.fromEncodedSecret("!!!"))
    }
}
