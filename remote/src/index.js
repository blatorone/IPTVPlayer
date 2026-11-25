// StreamWatch relay: pairs one device ("tv") and one phone ("phone") per room and forwards
// their end-to-end encrypted frames. It never sees the key (it stays in the QR code's URL
// fragment), stores nothing and logs nothing.
import { DurableObject } from "cloudflare:workers";

const ROOM_PATH = /^\/v1\/room\/([A-Za-z0-9_-]{22})$/;
const MAX_FRAME_CHARS = 512 * 1024;
// A room closes after this long without any message (the device ends idle sessions much earlier).
const ROOM_IDLE_LIMIT_MS = 6 * 60 * 60 * 1000;
const ROLES = ["tv", "phone"];

const other = (role) => (role === "tv" ? "phone" : "tv");

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const match = url.pathname.match(ROOM_PATH);
    if (!match) return new Response("Not found", { status: 404 });
    if (request.headers.get("Upgrade")?.toLowerCase() !== "websocket") {
      return new Response("WebSocket expected", { status: 426 });
    }
    if (!ROLES.includes(url.searchParams.get("role"))) {
      return new Response("Unknown role", { status: 400 });
    }
    const room = env.ROOMS.get(env.ROOMS.idFromName(match[1]));
    return room.fetch(request);
  },
};

export class Room extends DurableObject {
  async fetch(request) {
    const role = new URL(request.url).searchParams.get("role");
    // One connection per role: a reconnect replaces the previous one.
    for (const previous of this.ctx.getWebSockets(role)) close(previous, 4001, "replaced");
    const [client, server] = Object.values(new WebSocketPair());
    this.ctx.acceptWebSocket(server, [role]);
    const peerPresent = this.open(other(role)).length > 0;
    send(server, { relay: "peer", present: peerPresent });
    for (const peer of this.open(other(role))) send(peer, { relay: "peer", present: true });
    if ((await this.ctx.storage.getAlarm()) === null) {
      await this.ctx.storage.setAlarm(Date.now() + ROOM_IDLE_LIMIT_MS);
    }
    return new Response(null, { status: 101, webSocket: client });
  }

  async webSocketMessage(socket, message) {
    // Only encrypted frames ("1.<seq>.<nonce>.<ciphertext>") are forwarded.
    if (typeof message !== "string" || message.length > MAX_FRAME_CHARS || !message.startsWith("1.")) {
      close(socket, 4002, "invalid frame");
      return;
    }
    const role = this.ctx.getTags(socket)[0];
    for (const peer of this.open(other(role))) {
      try { peer.send(message); } catch { /* peer is going away */ }
    }
    // Active rooms stay open; only idle rooms expire.
    await this.ctx.storage.setAlarm(Date.now() + ROOM_IDLE_LIMIT_MS);
  }

  webSocketClose(socket) { this.left(socket); }

  webSocketError(socket) { this.left(socket); }

  async alarm() {
    for (const socket of this.ctx.getWebSockets()) close(socket, 4004, "expired");
    await this.ctx.storage.deleteAll();
  }

  left(socket) {
    const role = this.ctx.getTags(socket)[0];
    close(socket, 1000, "bye");
    // Replaced by a newer connection of the same role: nothing changes for the peer.
    if (this.open(role).some((s) => s !== socket)) return;
    if (role === "tv") {
      // The device ended the session (or went away): the phone has nothing left to talk to.
      for (const phone of this.open("phone")) close(phone, 4003, "device left");
    } else {
      for (const tv of this.open("tv")) send(tv, { relay: "peer", present: false });
    }
  }

  open(role) {
    return this.ctx.getWebSockets(role).filter((s) => s.readyState === WebSocket.OPEN);
  }
}

function send(socket, value) {
  try { socket.send(JSON.stringify(value)); } catch { /* closed */ }
}

function close(socket, code, reason) {
  try { socket.close(code, reason); } catch { /* already closed */ }
}
