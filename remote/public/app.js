"use strict";
// StreamWatch remote settings page. The session secret comes from the QR code's URL fragment
// (never sent to a server). Everything sent to the device is end-to-end encrypted with
// AES-256-GCM; the relay only forwards ciphertext. Format: see RemoteSessionCrypto.kt.

const enc = new TextEncoder();
const dec = new TextDecoder();
const $ = (id) => document.getElementById(id);

// ---------- crypto ----------
const b64url = {
  encode(bytes) {
    let s = "";
    for (const b of bytes) s += String.fromCharCode(b);
    return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  },
  decode(text) {
    if (!/^[A-Za-z0-9_-]*$/.test(text)) throw new Error("base64url");
    const s = atob(text.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((text.length + 3) % 4));
    return Uint8Array.from(s, (c) => c.charCodeAt(0));
  },
};

async function hkdf(secret, info, length) {
  const base = await crypto.subtle.importKey("raw", secret, "HKDF", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits(
    { name: "HKDF", hash: "SHA-256", salt: new Uint8Array(32), info: enc.encode(info) }, base, length * 8);
  return new Uint8Array(bits);
}

class Session {
  static async create(secretText) {
    const secret = b64url.decode(secretText);
    if (secret.length !== 32) throw new Error("secret");
    const s = new Session();
    s.roomId = b64url.encode(await hkdf(secret, "streamwatch-remote/1 room", 16));
    s.key = await crypto.subtle.importKey("raw", await hkdf(secret, "streamwatch-remote/1 key", 32),
      "AES-GCM", false, ["encrypt", "decrypt"]);
    s.seq = 0;
    s.lastSeen = 0;
    return s;
  }
  aad(role, seq) { return enc.encode(`streamwatch-remote/1|${role}|${seq}`); }
  async seal(text) {
    const seq = ++this.seq;
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const ct = new Uint8Array(await crypto.subtle.encrypt(
      { name: "AES-GCM", iv, additionalData: this.aad("phone", seq), tagLength: 128 }, this.key, enc.encode(text)));
    return `1.${seq}.${b64url.encode(iv)}.${b64url.encode(ct)}`;
  }
  async open(frame) {
    const parts = frame.split(".");
    if (parts.length !== 4 || parts[0] !== "1") return null;
    const seq = Number(parts[1]);
    if (!Number.isSafeInteger(seq) || seq <= this.lastSeen) return null;
    try {
      const plain = await crypto.subtle.decrypt(
        { name: "AES-GCM", iv: b64url.decode(parts[2]), additionalData: this.aad("tv", seq), tagLength: 128 },
        this.key, b64url.decode(parts[3]));
      this.lastSeen = seq;
      return dec.decode(plain);
    } catch { return null; }
  }
}

// ---------- connection ----------
let session = null;
let socket = null;
let state = null;
let deviceThere = false;
let retry = 0;
let ended = false;
let incoming = Promise.resolve();

function setStatus(text, kind = "") {
  const el = $("status");
  el.textContent = text;
  el.className = "status " + kind;
}

function connect() {
  const url = `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/v1/room/${session.roomId}?role=phone`;
  socket = new WebSocket(url);
  socket.onopen = () => { retry = 0; setStatus("Verbunden, warte auf das Gerät …", "warn"); };
  socket.onmessage = (event) => {
    if (typeof event.data !== "string") return;
    incoming = incoming.then(() => receive(event.data));
  };
  socket.onclose = (event) => {
    socket = null;
    deviceThere = false;
    if (event.code === 4003) {
      setStatus("Zugriff am Gerät beendet", "warn");
      $("finish").hidden = true;
      notice("Der Zugriff wurde am Gerät beendet oder ist abgelaufen. Starte ihn am Gerät neu und scanne den neuen QR-Code.");
    } else setStatus("Verbindung unterbrochen – verbinde neu …", "warn");
    if (ended) return;
    retry++;
    if (retry > 40) { setStatus("Keine Verbindung", "warn"); return; }
    setTimeout(connect, Math.min(1000 * 2 ** Math.min(retry - 1, 4), 15000));
  };
}

async function receive(data) {
  if (data.startsWith("{")) {
    const message = JSON.parse(data);
    if (message.relay === "peer") {
      deviceThere = !!message.present;
      if (!deviceThere) setStatus("Gerät nicht verbunden – App am Gerät geöffnet?", "warn");
    }
    return;
  }
  const text = await session.open(data);
  if (!text) return;
  const message = JSON.parse(text);
  if (message.t === "state") {
    state = message;
    applyAccent(message.accent);
    deviceThere = true;
    setStatus(`Verbunden mit ${message.device}`, "ok");
    $("finish").hidden = false;
    show();
    render();
  } else if (message.t === "result") {
    if (message.message || !message.ok) toast(message.message || "Das hat nicht geklappt.", !message.ok);
  }
}

async function send(command) {
  if (!socket || socket.readyState !== WebSocket.OPEN || !state) {
    toast("Keine Verbindung zum Gerät.", true);
    return;
  }
  socket.send(await session.seal(JSON.stringify({ ...command, epoch: state.epoch })));
}

// ---------- helpers ----------
// The page uses the accent colour set in the app.
function applyAccent(hex) {
  if (!/^#[0-9A-Fa-f]{6}$/.test(hex || "")) return;
  const root = document.documentElement.style;
  root.setProperty("--accent", hex);
  const n = parseInt(hex.slice(1), 16);
  const lum = (0.2126 * ((n >> 16) & 255) + 0.7152 * ((n >> 8) & 255) + 0.0722 * (n & 255)) / 255;
  root.setProperty("--on-accent", lum > 0.6 ? "#111" : "#fff");
}

function el(tag, props = {}, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(props)) {
    if (key.startsWith("on")) node.addEventListener(key.slice(2), value);
    else if (key === "class") node.className = value;
    else if (value === true) node.setAttribute(key, "");
    else if (value !== false && value != null) node.setAttribute(key, value);
  }
  for (const child of children.flat()) if (child != null) node.append(child);
  return node;
}
const switchButton = (on, label, onClick) =>
  el("button", { class: "switch", role: "switch", "aria-checked": String(!!on), "aria-label": label, onclick: onClick });
let toastTimer;
function toast(text, error = false) {
  const t = $("toast");
  t.textContent = text;
  t.className = "show" + (error ? " error" : "");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { t.className = ""; }, 3500);
}
function notice(text) { $("portal-notice").replaceChildren(el("div", { class: "notice" }, text)); }
function show() { $("intro").hidden = true; $("tabs").hidden = false; $("app").hidden = false; }

function askPin(title) {
  return new Promise((resolve) => {
    const dialog = $("pin-dialog");
    $("pin-title").textContent = title;
    const input = $("pin-input");
    input.value = "";
    dialog.onclose = () => {
      const pin = input.value.trim();
      input.value = "";
      resolve(dialog.returnValue === "ok" && /^[0-9]{4,12}$/.test(pin) ? pin : null);
    };
    dialog.showModal();
    input.focus();
  });
}
async function askNewPin(title) {
  const first = await askPin(`${title} (4–12 Ziffern)`);
  if (!first) return null;
  const second = await askPin("Neue PIN wiederholen");
  if (first !== second) { toast("Die PINs stimmen nicht überein.", true); return null; }
  return first;
}

// ---------- rendering ----------
let tab = "portals";
let categorySection = "LIVE";

function render() {
  if (!state) return;
  renderPortals();
  renderCountries();
  renderCategories();
  renderParental();
  renderSettings();
}

function renderPortals() {
  const notes = [];
  if (state.busy) notes.push(el("div", { class: "notice" }, state.busy));
  if (state.error) notes.push(el("div", { class: "notice error" }, state.error));
  $("portal-notice").replaceChildren(...notes);
  const merge = state.settings.find((s) => s.id === "merge");
  $("portal-merge").replaceChildren(...(merge && state.portals.length > 1
    ? [settingRow(merge, (value) => send({ t: "set", id: merge.id, value })),
       el("p", { class: "hint" }, merge.value
         ? "Alle aktiven Portale werden zu einer Liste zusammengeführt (Duplikate entfernt)."
         : "Es wird jeweils nur das aktive Portal angezeigt.")]
    : []));
  if (!state.portals.length) {
    $("portal-list").replaceChildren(el("p", { class: "hint" }, "Noch kein Portal eingerichtet."));
    return;
  }
  $("portal-list").replaceChildren(...state.portals.map((p) => el("div", { class: "card" },
    el("div", { class: "row", style: "border:none;padding:0;margin:0;background:none" },
      el("span", { class: "label" }, el("b", {}, p.label), p.active ? " · aktiv" : "",
        el("span", { class: "sub" }, p.address),
        el("span", { class: "sub" }, `MAC ${p.mac}`),
        el("span", { class: "sub" }, p.expiry ? `Gültig bis ${p.expiry}` : "Ablaufdatum unbekannt")),
      switchButton(p.enabled, p.enabled ? "Portal deaktivieren" : "Portal aktivieren",
        () => send({ t: "portalEnabled", id: p.id, enabled: !p.enabled }))),
    el("div", { class: "row-actions" },
      el("span", { class: "badge " + (p.reachable === true ? "ok" : p.reachable === false ? "bad" : "") },
        p.reachable === true ? "erreichbar" : p.reachable === false ? "nicht erreichbar" : "unbekannt"),
      el("button", { class: p.preferred ? "preferred" : "", "aria-pressed": String(!!p.preferred),
        title: "Wird für Live-TV, Filme und Serien zuerst verwendet",
        onclick: () => send({ t: "portalPreferred", id: p.id, preferred: !p.preferred }) },
        p.preferred ? "★ Bevorzugt" : "☆ Bevorzugen"),
      el("button", { onclick: () => { editingPortal = editingPortal === p.id ? null : p.id; renderPortals(); } },
        editingPortal === p.id ? "Schließen" : "Bearbeiten"),
      el("button", { class: "danger", onclick: () => {
        if (confirm(`${p.label} wirklich entfernen?`)) send({ t: "portalRemove", id: p.id });
      } }, "Entfernen")),
    editingPortal === p.id ? portalEditForm(p) : null)));
}

let editingPortal = null;
function portalEditForm(p) {
  const address = el("input", { type: "url", inputmode: "url", value: p.address });
  const mac = el("input", { value: p.mac, autocapitalize: "characters" });
  return el("form", { class: "edit-form", autocomplete: "off", onsubmit: (e) => {
    e.preventDefault();
    const a = address.value.trim();
    if (!/^https?:\/\//i.test(a)) { toast("Die Adresse muss mit http:// oder https:// beginnen.", true); return; }
    editingPortal = null;
    send({ t: "portalEdit", id: p.id, address: a, mac: mac.value.trim() });
  } },
  el("label", {}, "Portal-Adresse", address),
  el("label", {}, "MAC-Adresse", mac),
  el("button", { type: "submit", class: "primary" }, "Speichern und neu laden"));
}

// Countries are chosen here first and sent to the device only with "Übernehmen".
let countryDraft = null;
let askedBefore = false;
const savedCountries = () => state.countries.filter((c) => c.selected).map((c) => c.code);

function switchTab(name) {
  tab = name;
  for (const b of $("tabs").children) b.classList.toggle("active", b.dataset.tab === name);
  for (const n of ["portals", "countries", "categories", "parental", "settings"]) $(`tab-${n}`).hidden = n !== name;
  window.scrollTo(0, 0);
}

function renderCountries() {
  // After a portal was loaded the device asks for the countries: answer it here.
  if (state.askCountries && !askedBefore) {
    countryDraft = savedCountries();
    switchTab("countries");
  }
  askedBefore = state.askCountries;
  $("country-ask").replaceChildren(...(state.askCountries ? [el("div", { class: "notice ask" },
    "Das Portal ist geladen. Wähle die Länder, die angezeigt werden sollen, und tippe auf „Übernehmen“.",
    el("div", { class: "row-actions" }, el("button", { onclick: () => {
      countryDraft = null;
      send({ t: "countriesLater" });
    } }, "Später")))] : []));
  const selected = countryDraft ?? savedCountries();
  const changed = countryDraft !== null && countryDraft.join() !== savedCountries().join();
  $("country-apply").disabled = !changed && !state.askCountries;
  $("country-reset").disabled = !changed;
  const query = $("country-search").value.trim().toLowerCase();
  const items = state.countries.filter((c) => !query || c.name.toLowerCase().includes(query) || c.code.toLowerCase() === query);
  $("country-list").replaceChildren(...items.map((c) => {
    const on = selected.includes(c.code);
    return el("div", { class: "row" },
      el("span", { class: "label" }, c.name, on ? el("span", { class: "sub" }, `Position ${selected.indexOf(c.code) + 1}`) : null),
      switchButton(on, c.name, () => {
        countryDraft = on ? selected.filter((x) => x !== c.code) : [...selected, c.code];
        renderCountries();
      }));
  }));
  if (!items.length) $("country-list").replaceChildren(el("p", { class: "hint" }, "Keine Länder gefunden."));
}

function visibleCategories() {
  const query = $("category-search").value.trim().toLowerCase();
  return (state.categories[categorySection] || []).filter((c) => !query || c.name.toLowerCase().includes(query));
}

function renderCategories() {
  for (const b of $("category-sections").children) b.classList.toggle("active", b.dataset.section === categorySection);
  const items = visibleCategories();
  const canLock = state.parental.hasPin;
  $("category-list").replaceChildren(...items.map((c) => el("div", { class: "row" },
    el("span", { class: "label" }, c.name),
    canLock ? el("button", { class: "lock", "aria-pressed": String(c.locked), title: "Mit PIN sperren",
      onclick: async () => {
        const pin = c.locked ? await askPin("PIN zum Entsperren") : undefined;
        if (c.locked && !pin) return;
        send({ t: "categoryLock", section: categorySection, id: c.id, locked: !c.locked, pin });
      } }, c.locked ? "🔒" : "🔓") : null,
    switchButton(!c.hidden, `${c.name} anzeigen`,
      () => send({ t: "categories", section: categorySection, ids: [c.id], hidden: !c.hidden })))));
  if (!items.length) $("category-list").replaceChildren(el("p", { class: "hint" }, "Keine Kategorien."));
}

// A setting with "showIf" is shown only while the other setting has that value.
function visibleSetting(s) {
  if (!s.showIf) return true;
  const other = state.settings.find((o) => o.id === s.showIf.id);
  return !!other && String(other.value) === s.showIf.value;
}

function settingRow(s, onChange) {
  if (s.type === "info") {
    return el("div", { class: "row" }, el("span", { class: "label" }, s.label,
      el("span", { class: "sub" }, s.text),
      s.link ? el("a", { class: "sub", href: s.link, target: "_blank", rel: "noopener noreferrer" }, s.link) : null));
  }
  if (s.type === "action") {
    return el("div", { class: "row" }, el("span", { class: "label" }, ...s.label.split("\n").map((line, i) =>
      i === 0 ? line : el("span", { class: "sub" }, line))),
      el("button", { onclick: () => send({ t: "action", id: s.id }) }, s.button));
  }
  if (s.type === "input") {
    const input = el("input", { type: s.secret ? "password" : "url", placeholder: s.hint, autocomplete: "off",
      autocapitalize: "off", spellcheck: "false" });
    return el("form", { class: "row input-row", autocomplete: "off", onsubmit: (e) => {
      e.preventDefault();
      const value = input.value.trim();
      if (!value) return;
      input.value = "";
      input.blur();
      send({ t: "input", id: s.id, value });
    } }, el("label", { class: "label" }, s.label, input), el("button", { type: "submit", class: "primary" }, s.button));
  }
  if (s.type === "choice" && s.options.some((o) => o.color)) {
    const current = s.options.find((o) => o.value === s.value);
    return el("div", { class: "row color-row" },
      el("span", { class: "label" }, s.label, el("span", { class: "sub" }, current ? current.label : "")),
      el("div", { class: "swatches" }, s.options.map((o) => el("button", {
        class: "swatch", style: `background:${o.color}`, title: o.label, "aria-label": o.label,
        "aria-pressed": String(o.value === s.value), onclick: () => onChange(o.value) }))));
  }
  let control;
  if (s.type === "toggle") control = switchButton(s.value, s.label, () => onChange(!s.value));
  else if (s.type === "choice") {
    control = el("select", { "aria-label": s.label, onchange: (e) => onChange(e.target.value) },
      s.options.map((o) => el("option", { value: o.value, selected: o.value === s.value }, o.label)));
  } else {
    control = el("div", { class: "stepper" },
      el("button", { "aria-label": "weniger", disabled: s.value - s.step < s.min, onclick: () => onChange(s.value - s.step) }, "−"),
      el("span", {}, `${s.value} ${s.unit}`),
      el("button", { "aria-label": "mehr", disabled: s.value + s.step > s.max, onclick: () => onChange(s.value + s.step) }, "+"));
  }
  return el("div", { class: "row" }, el("span", { class: "label" }, s.label), control);
}

function renderParental() {
  const p = state.parental;
  const box = $("parental");
  if (!p.hasPin) {
    box.replaceChildren(
      el("p", { class: "hint" }, "Lege zuerst eine PIN fest. Damit kannst du Einstellungen schützen, Kategorien sperren und den Kindermodus nutzen."),
      el("button", { class: "primary", onclick: async () => {
        const pin = await askNewPin("Neue PIN");
        if (pin) send({ t: "pin", pin });
      } }, "PIN festlegen"));
    return;
  }
  const fields = state.settings.filter((s) => s.id.startsWith("parental."));
  const needsPin = { lockAdult: (v) => v === false, kidsMode: (v) => v === false };
  box.replaceChildren(
    p.kidsMode ? el("div", { class: "notice" }, "Der Kindermodus ist am Gerät aktiv.") : null,
    ...fields.map((s) => settingRow(s, async (value) => {
      const field = s.id.slice("parental.".length);
      let pin;
      if (needsPin[field]?.(value)) {
        pin = await askPin(field === "kidsMode" ? "PIN zum Beenden des Kindermodus" : "PIN eingeben");
        if (!pin) return;
      }
      send({ t: "parental", field, value, pin });
    })),
    el("h3", {}, "PIN"),
    el("div", { class: "row-actions" },
      el("button", { onclick: async () => {
        const current = await askPin("Aktuelle PIN"); if (!current) return;
        const pin = await askNewPin("Neue PIN"); if (pin) send({ t: "pin", pin, current });
      } }, "PIN ändern"),
      el("button", { onclick: async () => {
        const current = await askPin("Aktuelle PIN"); if (!current) return;
        if (p.separateKidsPin) send({ t: "kidsPin", pin: null, current });
        else { const pin = await askNewPin("PIN zum Beenden des Kindermodus"); if (pin) send({ t: "kidsPin", pin, current }); }
      } }, p.separateKidsPin ? "Eigene Kindermodus-PIN entfernen" : "Eigene Kindermodus-PIN festlegen"),
      el("button", { class: "danger", onclick: async () => {
        if (!confirm("PIN und Kindersicherung wirklich entfernen?")) return;
        const current = await askPin("Aktuelle PIN"); if (current) send({ t: "pinRemove", current });
      } }, "PIN entfernen")),
    el("p", { class: "hint" }, "Einzelne Kategorien sperrst du unter „Kategorien“ mit dem Schloss."));
}

function renderSettings() {
  // Do not rebuild the page while something is being typed; it is redrawn when the field is left.
  const typing = document.activeElement;
  if (typing && typing.tagName === "INPUT" && $("settings").contains(typing)) {
    typing.onblur = () => { typing.onblur = null; setTimeout(renderSettings, 0); };
    return;
  }
  const groups = new Map();
  for (const s of state.settings) {
    if (s.id.startsWith("parental.") || !visibleSetting(s)) continue;
    if (!groups.has(s.section)) groups.set(s.section, []);
    groups.get(s.section).push(s);
  }
  $("settings").replaceChildren(...[...groups].flatMap(([section, items]) => [
    el("h3", {}, section),
    ...items.map((s) => settingRow(s, (value) => send({ t: "set", id: s.id, value }))),
  ]));
}

// ---------- events ----------
$("tabs").addEventListener("click", (e) => {
  const button = e.target.closest("button[data-tab]");
  if (button) switchTab(button.dataset.tab);
});
$("country-apply").addEventListener("click", () => {
  send({ t: "countries", codes: countryDraft ?? savedCountries(), confirm: true });
  countryDraft = null;
});
$("country-reset").addEventListener("click", () => { countryDraft = null; renderCountries(); });
$("category-sections").addEventListener("click", (e) => {
  const button = e.target.closest("button[data-section]");
  if (!button) return;
  categorySection = button.dataset.section;
  renderCategories();
});
$("country-search").addEventListener("input", () => state && renderCountries());
$("category-search").addEventListener("input", () => state && renderCategories());
$("category-show-all").addEventListener("click", () => {
  const ids = visibleCategories().map((c) => c.id);
  if (ids.length) send({ t: "categories", section: categorySection, ids, hidden: false });
});
$("category-hide-all").addEventListener("click", () => {
  const ids = visibleCategories().map((c) => c.id);
  if (ids.length && confirm(`${ids.length} Kategorien ausblenden?`)) send({ t: "categories", section: categorySection, ids, hidden: true });
});
$("portal-form").addEventListener("submit", (e) => {
  e.preventDefault();
  const form = e.target;
  const address = form.address.value.trim();
  const mac = form.mac.value.trim();
  if (!/^https?:\/\//i.test(address)) { toast("Die Adresse muss mit http:// oder https:// beginnen.", true); return; }
  send({ t: "portalAdd", address, mac });
  form.reset();
});

// ---------- start ----------
(async () => {
  if (!window.isSecureContext || !crypto.subtle) {
    setStatus("Nicht unterstützt", "warn");
    return;
  }
  const fromHash = new URLSearchParams(location.hash.slice(1)).get("s");
  if (fromHash) sessionStorage.setItem("sw-secret", fromHash);
  // Keep the secret out of the address bar, history and screenshots.
  if (location.hash) history.replaceState(null, "", location.pathname);
  const secret = sessionStorage.getItem("sw-secret");
  if (!secret) { setStatus("Kein Zugang"); return; }
  try {
    session = await Session.create(secret);
  } catch {
    sessionStorage.removeItem("sw-secret");
    setStatus("Ungültiger QR-Code", "warn");
    return;
  }
  connect();
})();

// "Fertig": ends the session on the device, which then returns to Live-TV.
$("finish").addEventListener("click", async () => {
  if (!confirm("Alles erledigt? Der Zugriff wird beendet und das Gerät wechselt zu Live-TV.")) return;
  await send({ t: "finish" });
  ended = true;
  sessionStorage.removeItem("sw-secret");
  $("finish").hidden = true;
  $("tabs").hidden = true;
  $("app").hidden = true;
  $("intro").hidden = false;
  $("intro").replaceChildren(el("h1", {}, "Fertig"),
    el("p", {}, "Der Zugriff ist beendet. Für weitere Änderungen am Gerät erneut „Per Handy einstellen“ öffnen und den neuen QR-Code scannen."));
  setStatus("Zugriff beendet");
});
