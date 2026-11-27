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

private const val UNLOCK_ATTEMPTS = 5

internal enum class RemoteStatus { CONNECTING, WAITING, CONNECTED, OFFLINE, ENDED }

/** What the session needs from the app; always called on the main thread. */
internal interface RemoteHandler {
    fun remoteState(): RemoteState
    /** Applies a command; returns a message for the phone (null = success without message). */
    fun applyRemote(command: RemoteCommand): RemoteReply
    /** Longer work (backup) off the main thread; null hands the command to [applyRemote]. */
    suspend fun applyRemoteSuspending(command: RemoteCommand): RemoteReply? = null
}

/** [data] is an optional answer for the phone (for example a portal's category list). */
internal data class RemoteReply(val ok: Boolean, val message: String? = null, val data: com.google.gson.JsonObject? = null)

/**
 * One temporary remote settings session of this device. Holds the secret only in memory; after
 * [end] the secret is gone and the QR code is worthless. Nothing is logged.
 */
internal class RemoteSession(
    private val scope: CoroutineScope,
    private val handler: RemoteHandler,
    private val endedMessage: String,
    private val failedMessage: String,
    private val onFinished: () -> Unit = {},
    baseUrl: String = REMOTE_BASE_URL,
    /**
     * "pin" (check a PIN) or "pinNew" (set a new one): only a PIN field on the phone, no settings
     * are sent, only the PIN comes back. Null for the normal settings session.
     */
    private var pinMode: String? = null,
    /** In a PIN-only session: the PIN is a password (the phone shows a text field). */
    private var pinPassword: Boolean = false,
    /** In a PIN-only session: what the device asks for (e.g. "PIN ändern"), shown above the entry. */
    private var pinTitle: String? = null,
    /**
     * A PIN or password protects the settings: the phone has to enter it before it gets any
     * setting (checked here on the device). Null when nothing is protected.
     */
    private val unlock: ((String) -> Boolean)? = null,
    private val unlockPassword: Boolean = false,
    private val wrongPinMessage: String = failedMessage
) {
    private var unlocked = unlock == null
    private var unlockAttempts = 0
    private val crypto = RemoteSessionCrypto.create()
    val pageUrl = "$baseUrl/#s=${crypto.encodedSecret}" + if (pinMode != null) "&m=pin" else ""
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

    /**
     * A PIN-only session goes on with the next step (e.g. after the current PIN the new one): the
     * phone switches to it without a new QR code.
     */
    fun continuePin(mode: String, password: Boolean, title: String?) {
        if (pinMode == null) return
        pinMode = mode
        pinPassword = password
        pinTitle = title
        publish()
    }

    /** Sends the current state to the phone if it changed (called whenever app state changes). */
    fun publish() {
        if (_status.value != RemoteStatus.CONNECTED) return
        val text = if (pinMode != null || !unlocked) {
            com.google.gson.JsonObject().apply {
                addProperty("t", "state")
                addProperty("epoch", epoch)
                addProperty("mode", pinMode ?: "unlock")
                addProperty("password", if (pinMode != null) pinPassword else unlockPassword)
                if (pinMode != null) pinTitle?.let { addProperty("title", it) }
            }.toString()
        } else runCatching { RemoteProtocol.encodeState(handler.remoteState(), epoch) }.getOrNull() ?: return
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
                send(RemoteProtocol.encodeResult(true, endedMessage))
                end()
                onFinished()
                return
            }
            if (command == RemoteCommand.Hello) {
                lastSentState = null
                publish()
                continue
            }
            if (!unlocked) {
                // Nothing but the PIN is accepted until it was right; a few tries per QR code.
                val entry = command as? RemoteCommand.PinEntry
                if (entry != null && unlock?.invoke(entry.pin) == true) {
                    unlocked = true
                    send(RemoteProtocol.encodeResult(true, null))
                    lastSentState = null
                    publish()
                } else {
                    send(RemoteProtocol.encodeResult(false, wrongPinMessage))
                    if (++unlockAttempts >= UNLOCK_ATTEMPTS) { end(); onFinished(); return }
                }
                continue
            }
            val reply = try {
                handler.applyRemoteSuspending(command) ?: handler.applyRemote(command)
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (_: Exception) {
                RemoteReply(false, failedMessage)
            }
            send(RemoteProtocol.encodeResult(reply.ok, reply.message, reply.data))
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
