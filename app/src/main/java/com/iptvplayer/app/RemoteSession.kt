package com.iptvplayer.app

import android.os.SystemClock
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Address of the page and relay (Cloudflare Worker in `remote/`). It is the only place that
 * names the Cloudflare account; after moving to another account only this changes.
 */
internal const val REMOTE_BASE_URL = "https://streamwatch-remote.streamwatchiptv.workers.dev"

/** Ends a session nobody used for this long. */
internal const val REMOTE_IDLE_LIMIT_MS = 15 * 60 * 1000L

internal enum class RemoteStatus { CONNECTING, WAITING, CONNECTED, OFFLINE, ENDED }

/** What the session needs from the app; always called on the main thread. */
internal interface RemoteHandler {
    fun remoteState(): RemoteState
    /** Applies a command; returns a message for the phone (null = success without message). */
    fun applyRemote(command: RemoteCommand): RemoteReply
}

internal data class RemoteReply(val ok: Boolean, val message: String? = null)

/**
 * One temporary remote settings session of this device. Holds the secret only in memory; after
 * [end] the secret is gone and the QR code is worthless. Nothing is logged.
 */
internal class RemoteSession(
    private val scope: CoroutineScope,
    private val handler: RemoteHandler,
    private val onFinished: () -> Unit = {},
    baseUrl: String = REMOTE_BASE_URL
) {
    private val crypto = RemoteSessionCrypto.create()
    val pageUrl = "$baseUrl/#s=${crypto.encodedSecret}"
    private val socketUrl = baseUrl.replaceFirst("https://", "wss://") + "/v1/room/${crypto.roomId}?role=tv"

    private val _status = MutableStateFlow(RemoteStatus.CONNECTING)
    val status: StateFlow<RemoteStatus> = _status
    private val _remainingMs = MutableStateFlow(REMOTE_IDLE_LIMIT_MS)
    /** Time until the session ends for lack of use. */
    val remainingMs: StateFlow<Long> = _remainingMs

    private val startedAt = SystemClock.elapsedRealtime()
    @Volatile private var lastActivity = startedAt
    private val inbox = Channel<String>(Channel.UNLIMITED)
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    @Volatile private var socket: WebSocket? = null
    private var epoch = newEpoch()
    private var lastSentState: String? = null
    private val jobs = mutableListOf<Job>()

    fun start() {
        jobs += scope.launch(Dispatchers.Main) { process() }
        jobs += scope.launch(Dispatchers.Main) { connectLoop() }
        jobs += scope.launch(Dispatchers.Main) { watchLimits() }
    }

    /** Ends the session on this device: closes the connection and forgets the secret. */
    fun end() {
        if (_status.value == RemoteStatus.ENDED) return
        _status.value = RemoteStatus.ENDED
        socket?.close(1000, "ended")
        socket = null
        jobs.forEach { it.cancel() }
        inbox.close()
    }

    /** Sends the current state to the phone if it changed (called whenever app state changes). */
    fun publish() {
        if (_status.value != RemoteStatus.CONNECTED) return
        val text = runCatching { RemoteProtocol.encodeState(handler.remoteState(), epoch) }.getOrNull() ?: return
        if (text == lastSentState) return
        lastSentState = text
        send(text)
    }

    private suspend fun connectLoop() {
        var attempt = 0
        while (scope.isActive && _status.value != RemoteStatus.ENDED) {
            val closed = kotlinx.coroutines.CompletableDeferred<Unit>()
            val request = Request.Builder().url(socketUrl).build()
            socket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    attempt = 0
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    inbox.trySend(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    closed.complete(Unit)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    closed.complete(Unit)
                }
            })
            closed.await()
            socket = null
            if (_status.value == RemoteStatus.ENDED) return
            _status.value = RemoteStatus.OFFLINE
            attempt++
            delay((1_000L shl (attempt - 1).coerceAtMost(4)).coerceAtMost(15_000L))
            if (_status.value != RemoteStatus.ENDED) _status.value = RemoteStatus.CONNECTING
        }
    }

    private suspend fun process() {
        for (text in inbox) {
            if (text.startsWith("{")) {
                relayMessage(text)
                continue
            }
            val plain = withContext(Dispatchers.Default) {
                crypto.open(RemoteSessionCrypto.Role.PHONE, text)
            }?.toString(Charsets.UTF_8) ?: continue
            val command = RemoteProtocol.decodeCommand(plain, epoch) ?: continue
            lastActivity = SystemClock.elapsedRealtime()
            if (command == RemoteCommand.Finish) {
                send(RemoteProtocol.encodeResult(true, "Zugriff beendet."))
                end()
                onFinished()
                return
            }
            if (command == RemoteCommand.Hello) {
                lastSentState = null
                publish()
                continue
            }
            val reply = runCatching { handler.applyRemote(command) }
                .getOrElse { RemoteReply(false, "Die Änderung ist fehlgeschlagen.") }
            send(RemoteProtocol.encodeResult(reply.ok, reply.message))
            lastSentState = null
            publish()
        }
    }

    private fun relayMessage(text: String) {
        val message = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull() ?: return
        if (message.get("relay")?.asString != "peer") return
        val present = message.get("present")?.asBoolean == true
        if (present) {
            // A (re)connected phone starts a new epoch; frames of an earlier connection stay invalid.
            crypto.resetPeer(RemoteSessionCrypto.Role.PHONE)
            epoch = newEpoch()
            lastActivity = SystemClock.elapsedRealtime()
            _status.value = RemoteStatus.CONNECTED
            lastSentState = null
            publish()
        } else if (_status.value != RemoteStatus.ENDED) {
            _status.value = RemoteStatus.WAITING
        }
    }

    private suspend fun watchLimits() {
        while (scope.isActive && _status.value != RemoteStatus.ENDED) {
            val now = SystemClock.elapsedRealtime()
            val idleLeft = REMOTE_IDLE_LIMIT_MS - (now - lastActivity)

            val left = idleLeft
            _remainingMs.value = left.coerceAtLeast(0)
            if (left <= 0) {
                end()
                return
            }
            delay(1_000)
        }
    }

    private fun send(plain: String) {
        val frame = crypto.seal(RemoteSessionCrypto.Role.TV, plain.toByteArray(Charsets.UTF_8))
        socket?.send(frame)
    }

    private fun newEpoch(): String = ByteArray(9).also(SecureRandom()::nextBytes)
        .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
}
