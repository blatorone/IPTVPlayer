package com.iptvplayer.app

import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.nio.file.Files

class TimeshiftRecorderTest {
    @Test
    fun recordingIsReadBackUnchangedAndSeeksLandOnPackets() {
        val data = ByteArray(9 * 1024 * 1024) { (it * 31 % 251).toByte() } // more than one segment
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/live") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.use { out ->
                var sent = 0
                while (sent < data.size) {
                    val part = minOf(256 * 1024, data.size - sent)
                    out.write(data, sent, part)
                    out.flush()
                    sent += part
                    Thread.sleep(20)
                }
                Thread.sleep(3_000) // a live stream stays open
            }
        }
        server.start()
        val directory = Files.createTempDirectory("timeshift").toFile()
        val recorder = TimeshiftRecorder(directory, "http://127.0.0.1:${server.address.port}/live", 60_000L)
        try {
            recorder.start()
            val copy = ByteArray(data.size)
            var position = 0
            while (position < data.size) {
                position += recorder.read(position.toLong(), copy, position, data.size - position) { false }
            }
            assertArrayEquals(data, copy)
            val (oldest, live) = recorder.window()
            assertTrue(live >= oldest)
            val middle = recorder.offsetAt((oldest + live) / 2)
            assertEquals(0L, middle % 188)
            assertTrue(middle in 0L..data.size.toLong())
        } finally {
            recorder.stop()
            server.stop(0)
        }
    }
}
