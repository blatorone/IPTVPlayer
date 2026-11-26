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
  socket.onopen = () => { retry = 0; setStatus(t("status.connectedWaiting"), "warn"); };
  socket.onmessage = (event) => {
    if (typeof event.data !== "string") return;
    incoming = incoming.then(() => receive(event.data));
  };
  socket.onclose = (event) => {
    socket = null;
    deviceThere = false;
    if (event.code === 4003) {
      setStatus(t("status.accessEnded"), "warn");
      $("finish").hidden = true;
      notice(t("notice.accessEnded"));
    } else setStatus(t("status.reconnecting"), "warn");
    if (ended) return;
    retry++;
    if (retry > 40) { setStatus(t("status.noConnection"), "warn"); return; }
    setTimeout(connect, Math.min(1000 * 2 ** Math.min(retry - 1, 4), 15000));
  };
}

async function receive(data) {
  if (data.startsWith("{")) {
    const message = JSON.parse(data);
    if (message.relay === "peer") {
      deviceThere = !!message.present;
      if (!deviceThere) setStatus(t("status.deviceAbsent"), "warn");
    }
    return;
  }
  const text = await session.open(data);
  if (!text) return;
  const message = JSON.parse(text);
  // The device tells its app language; texts of this page follow it.
  if (message.t === "state" && message.language) setLanguage(message.language);
  if (message.t === "state" && (message.mode === "pin" || message.mode === "pinNew")) {
    state = message;
    deviceThere = true;
    renderPinOnly();
  } else if (message.t === "state" && message.mode === "unlock") {
    // A PIN or password protects the settings: nothing is shown before it was entered.
    state = message;
    deviceThere = true;
    renderUnlock();
  } else if (message.t === "result" && state && state.mode === "unlock") {
    if (!message.ok) toast(message.message || t("result.wrong"), true);
  } else if (message.t === "state") {
    state = message;
    applyAccent(message.accent);
    deviceThere = true;
    setStatus(t("status.connectedTo", message.device), "ok");
    $("finish").hidden = false;
    show();
    render();
  } else if (message.t === "result" && state && (state.mode === "pin" || state.mode === "pinNew")) {
    pinResult(message);
  } else if (message.t === "result") {
    if (message.data && message.data.sections) { portalCategories = message.data; renderPortals(); }
    if (message.data && message.data.file) downloadBackup(message.data.name, message.data.file);
    if (message.data && message.data.backupParts) { backupImport = { ...backupImport, info: message.data, parts: message.data.backupParts.slice() }; renderBackup(); }
    if (message.message || !message.ok) toast(message.message || t("result.failed"), !message.ok);
  }
}

async function send(command) {
  if (!socket || socket.readyState !== WebSocket.OPEN || !state) {
    toast(t("toast.noDevice"), true);
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
  const box = $("toast");
  box.textContent = text;
  box.className = "show" + (error ? " error" : "");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { box.className = ""; }, 3500);
}
function notice(text) { $("portal-notice").replaceChildren(el("div", { class: "notice" }, text)); }
function show() {
  $("intro").hidden = true; $("tabs").hidden = false; $("app").hidden = false;
  if (!$("page-title").textContent) switchTab(tab);
}

function askPin(title) {
  return new Promise((resolve) => {
    const dialog = $("pin-dialog");
    $("pin-title").textContent = title;
    const input = $("pin-input");
    input.value = "";
    // Digits get the number pad; a password (letters allowed) the normal keyboard.
    input.setAttribute("inputmode", state && state.parental && state.parental.pinPassword ? "text" : "numeric");
    dialog.onclose = () => {
      const pin = input.value;
      input.value = "";
      input.blur();
      resolve(dialog.returnValue === "ok" && /^[^\x00-\x1f]{4,64}$/.test(pin) ? pin : null);
    };
    dialog.showModal();
    input.focus();
  });
}
async function askNewPin(title) {
  const first = await askPin(t("pin.withMin", title));
  if (!first) return null;
  const second = await askPin(t("pin.repeatNew"));
  if (first !== second) { toast(t("pin.mismatch"), true); return null; }
  return first;
}

// ---------- rendering ----------
let tab = "portals";
let categorySection = "LIVE";

function render() {
  if (!state) return;
  // Keep the title in the current language (it may change with the app language).
  const active = [...$("tabs").children].find((b) => b.dataset.tab === tab);
  if (active) $("page-title").textContent = active.textContent.trim();
  renderPortals();
  renderCountries();
  renderCategories();
  renderParental();
  renderSettings();
  renderBackup();
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
         ? t("portal.mergeOn")
         : t("portal.mergeOff"))]
    : []));
  if (!state.portals.length) {
    $("portal-list").replaceChildren(el("p", { class: "hint" }, t("portal.none")));
    return;
  }
  $("portal-list").replaceChildren(...state.portals.map((p) => el("div", { class: "card" },
    el("div", { class: "row", style: "border:none;padding:0;margin:0;background:none" },
      el("span", { class: "label" }, el("b", {}, p.label), p.active ? " · " + t("portal.active") : "",
        el("span", { class: "sub" }, p.address),
        el("span", { class: "sub" }, t("portal.macValue", p.mac)),
        el("span", { class: "sub" }, p.expiry ? t("portal.expiry", p.expiry) : t("portal.expiryUnknown"))),
      switchButton(p.enabled, p.enabled ? t("portal.disable") : t("portal.enable"),
        () => send({ t: "portalEnabled", id: p.id, enabled: !p.enabled }))),
    el("div", { class: "row-actions" },
      el("span", { class: "badge " + (p.reachable === true ? "ok" : p.reachable === false ? "bad" : "") },
        p.reachable === true ? t("portal.reachable") : p.reachable === false ? t("portal.unreachable") : t("portal.unknown")),
      el("button", { class: p.preferred ? "preferred" : "", "aria-pressed": String(!!p.preferred),
        title: t("portal.preferredTip"),
        onclick: () => send({ t: "portalPreferred", id: p.id, preferred: !p.preferred }) },
        p.preferred ? t("portal.preferred") : t("portal.prefer")),
      el("button", { onclick: () => {
        if (portalCategories && portalCategories.id === p.id) { portalCategories = null; renderPortals(); }
        else send({ t: "portalCategories", id: p.id });
      } }, portalCategories && portalCategories.id === p.id ? t("portal.closeCategories") : t("portal.categories")),
      el("button", { onclick: () => { editingPortal = editingPortal === p.id ? null : p.id; renderPortals(); } },
        editingPortal === p.id ? t("portal.close") : t("portal.edit")),
      el("button", { class: "danger", onclick: () => {
        if (confirm(t("portal.removeConfirm", p.label))) send({ t: "portalRemove", id: p.id });
      } }, t("portal.remove"))),
    editingPortal === p.id ? portalEditForm(p) : null,
    portalCategories && portalCategories.id === p.id ? categoryOverview(portalCategories) : null)));
}

// The provider's own categories of one portal (read-only), to see which countries it offers.
let portalCategories = null;
let overviewSection = "LIVE";
function categoryOverview(data) {
  const names = { LIVE: t("cat.live"), MOVIES: t("cat.movies"), SERIES: t("cat.series") };
  const list = data.sections[overviewSection] || [];
  const countries = new Map();
  for (const c of list) countries.set(c.country, (countries.get(c.country) || 0) + 1);
  const summary = [...countries].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0])).map(([c, n]) => `${c} ${n}`).join(" · ");
  return el("div", { class: "edit-form" },
    el("div", { class: "segments" }, Object.keys(names).map((key) => el("button", {
      class: key === overviewSection ? "active" : "",
      onclick: () => { overviewSection = key; renderPortals(); } }, `${names[key]} (${(data.sections[key] || []).length})`))),
    list.length
      ? [el("p", { class: "hint" }, summary),
         el("div", { class: "category-overview" }, list.map((c) => el("div", { class: "overview-row" },
           el("span", {}, c.name), el("span", { class: "sub" }, [c.country, c.count].filter((v) => v != null).join(" · ")))))]
      : el("p", { class: "hint" }, t("overview.none")));
}

let editingPortal = null;
function portalEditForm(p) {
  const address = el("input", { type: "url", inputmode: "url", value: p.address });
  const mac = el("input", { value: p.mac, autocapitalize: "characters" });
  return el("form", { class: "edit-form", autocomplete: "off", onsubmit: (e) => {
    e.preventDefault();
    const a = address.value.trim();
    if (!/^https?:\/\//i.test(a)) { toast(t("portal.addrInvalid"), true); return; }
    editingPortal = null;
    send({ t: "portalEdit", id: p.id, address: a, mac: mac.value.trim() });
  } },
  el("label", {}, t("portal.address"), address),
  el("label", {}, t("portal.mac"), mac),
  el("button", { type: "submit", class: "primary" }, t("portal.saveReload")));
}

// Countries are chosen here first and sent to the device only with "Übernehmen".
let countryDraft = null;
let askedBefore = false;
const savedCountries = () => state.countries.filter((c) => c.selected).map((c) => c.code);

function switchTab(name) {
  tab = name;
  for (const b of $("tabs").children) b.classList.toggle("active", b.dataset.tab === name);
  for (const n of ["portals", "countries", "categories", "parental", "settings", "backup"]) $(`tab-${n}`).hidden = n !== name;
  // The page title follows the tab (like the title bar of an app); settings return to their list.
  const label = [...$("tabs").children].find((b) => b.dataset.tab === name);
  $("page-title").textContent = label ? label.textContent.trim() : "";
  if (name === "settings") { settingsSection = null; if (state) renderSettings(); }
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
    t("country.ask"),
    el("div", { class: "row-actions" }, el("button", { onclick: () => {
      countryDraft = null;
      send({ t: "countriesLater" });
    } }, t("country.later"))))] : []));
  const selected = countryDraft ?? savedCountries();
  const changed = countryDraft !== null && countryDraft.join() !== savedCountries().join();
  $("country-apply").disabled = !changed && !state.askCountries;
  $("country-reset").disabled = !changed;
  const query = $("country-search").value.trim().toLowerCase();
  const items = state.countries.filter((c) => !query || c.name.toLowerCase().includes(query) || c.code.toLowerCase() === query);
  $("country-list").replaceChildren(...items.map((c) => {
    const on = selected.includes(c.code);
    return el("div", { class: "row" },
      el("span", { class: "label" }, c.name, on ? el("span", { class: "sub" }, t("country.position", selected.indexOf(c.code) + 1)) : null),
      switchButton(on, c.name, () => {
        countryDraft = on ? selected.filter((x) => x !== c.code) : [...selected, c.code];
        renderCountries();
      }));
  }));
  if (!items.length) $("country-list").replaceChildren(el("p", { class: "hint" }, t("country.none")));
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
    canLock ? el("button", { class: "lock", "aria-pressed": String(c.locked), title: t("cat.lockTip"),
      onclick: async () => {
        const pin = c.locked ? await askPin(t("pin.unlockTitle")) : undefined;
        if (c.locked && !pin) return;
        send({ t: "categoryLock", section: categorySection, id: c.id, locked: !c.locked, pin });
      } }, c.locked ? "🔒" : "🔓") : null,
    switchButton(!c.hidden, t("cat.showAria", c.name),
      () => send({ t: "categories", section: categorySection, ids: [c.id], hidden: !c.hidden })))));
  if (!items.length) $("category-list").replaceChildren(el("p", { class: "hint" }, t("cat.none")));
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
      el("button", { "aria-label": t("stepper.less"), disabled: s.value - s.step < s.min, onclick: () => onChange(s.value - s.step) }, "−"),
      el("span", {}, `${s.value} ${s.unit}`),
      el("button", { "aria-label": t("stepper.more"), disabled: s.value + s.step > s.max, onclick: () => onChange(s.value + s.step) }, "+"));
  }
  return el("div", { class: "row" }, el("span", { class: "label" }, s.label), control);
}

function renderParental() {
  const p = state.parental;
  const box = $("parental");
  if (!p.hasPin) {
    box.replaceChildren(
      el("p", { class: "hint" }, t("parental.setPinFirst")),
      el("button", { class: "primary", onclick: async () => {
        const pin = await askNewPin(t("pin.new"));
        if (pin) send({ t: "pin", pin });
      } }, t("parental.setPin")));
    return;
  }
  const fields = state.settings.filter((s) => s.id.startsWith("parental."));
  // Like on the device: lockAdult off and leaving kids mode need the PIN; with a settings PIN,
  // every change of the Einstellungsschutz and of the areas needs that one.
  const settingsField = (f) => ["protectSettings", "lockPortals", "lockCategories"].includes(f) || f.startsWith("area.");
  const needsPin = (field, value) =>
    (field === "lockAdult" && value === false) || (field === "kidsMode" && value === false) ||
    (p.hasSettingsPin && settingsField(field) && !(field === "protectSettings" && value === true));
  box.replaceChildren(
    p.kidsMode ? el("div", { class: "notice" }, t("parental.kidsActive")) : null,
    ...fields.map((s) => settingRow(s, async (value) => {
      const field = s.id.slice("parental.".length);
      let pin;
      if (needsPin(field, value)) {
        pin = await askPin(field === "kidsMode" ? t("pin.endKids")
          : settingsField(field) ? t("pin.settings") : t("pin.enter"));
        if (!pin) return;
      }
      send({ t: "parental", field, value, pin });
    })),
    el("h3", {}, t("pin.heading")),
    el("div", { class: "row-actions" },
      el("button", { onclick: async () => {
        const current = await askPin(t("pin.current")); if (!current) return;
        const pin = await askNewPin(t("pin.new")); if (pin) send({ t: "pin", pin, current });
      } }, t("parental.changePin")),
      el("button", { onclick: async () => {
        const current = await askPin(t("pin.current")); if (!current) return;
        if (p.separateKidsPin) send({ t: "kidsPin", pin: null, current });
        else { const pin = await askNewPin(t("pin.endKids")); if (pin) send({ t: "kidsPin", pin, current }); }
      } }, p.separateKidsPin ? t("parental.kidsPinRemove") : t("parental.kidsPinSet")),
      el("button", { class: "danger", onclick: async () => {
        if (!confirm(t("parental.removePinConfirm"))) return;
        const current = await askPin(t("pin.current")); if (current) send({ t: "pinRemove", current });
      } }, t("parental.removePin"))),
    p.hasSettingsPin ? null : el("p", { class: "hint" }, t("parental.settingsPinHint")),
    el("h3", {}, t("parental.kidsAccountHeading")),
    el("p", { class: "hint" }, p.kidsAccount
      ? t("parental.kidsAccountInfo", p.kidsAccount)
      : t("parental.kidsAccountNone")),
    el("div", { class: "row-actions" },
      el("button", { onclick: () => {
        const name = (prompt(t("parental.kidsNamePrompt"), p.kidsAccount || "") || "").trim();
        if (name) send({ t: "parental", field: "kidsName", value: name });
      } }, p.kidsAccount ? t("parental.rename") : t("parental.createKids")),
      p.kidsAccount ? el("button", { class: "danger", onclick: async () => {
        if (!confirm(t("parental.removeKidsConfirm", p.kidsAccount))) return;
        const pin = await askPin(t("pin.enter")); if (pin) send({ t: "parental", field: "kidsRemove", value: true, pin });
      } }, t("parental.removeKids")) : null),
    el("p", { class: "hint" }, t("parental.categoriesHint")));
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
  // Like the app: first the list of sections, a tap opens the section as its own page.
  const names = [...groups.keys()];
  if (settingsSection && !names.includes(settingsSection)) settingsSection = null;
  if (!settingsSection) {
    $("settings").replaceChildren(el("div", { class: "list-group" }, names.map((name) => el("button", {
      class: "list-item", onclick: () => { settingsSection = name; window.scrollTo(0, 0); renderSettings(); },
    }, el("span", {}, name), el("span", { class: "chevron", "aria-hidden": "true" }, "›")))));
    return;
  }
  $("settings").replaceChildren(
    el("button", { class: "back-link", onclick: () => { settingsSection = null; window.scrollTo(0, 0); renderSettings(); } },
      el("span", { "aria-hidden": "true" }, "‹"), " ", settingsSection),
    el("div", { class: "list-group" },
      (groups.get(settingsSection) || []).map((s) => settingRow(s, (value) => send({ t: "set", id: s.id, value })))));
}
let settingsSection = null;

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
  if (ids.length && confirm(t("cat.hideConfirm", ids.length))) send({ t: "categories", section: categorySection, ids, hidden: true });
});
$("portal-form").addEventListener("submit", (e) => {
  e.preventDefault();
  const form = e.target;
  const address = form.address.value.trim();
  const mac = form.mac.value.trim();
  if (!/^https?:\/\//i.test(address)) { toast(t("portal.addrInvalid"), true); return; }
  send({ t: "portalAdd", address, mac });
  form.reset();
});

// ---------- start ----------
(async () => {
  setStatus(t("status.connecting"));
  if (!window.isSecureContext || !crypto.subtle) {
    setStatus(t("status.unsupported"), "warn");
    return;
  }
  const fromHash = new URLSearchParams(location.hash.slice(1)).get("s");
  if (fromHash) sessionStorage.setItem("sw-secret", fromHash);
  // Keep the secret out of the address bar, history and screenshots.
  if (location.hash) history.replaceState(null, "", location.pathname);
  const secret = sessionStorage.getItem("sw-secret");
  if (!secret) { setStatus(t("status.noAccess")); return; }
  try {
    session = await Session.create(secret);
  } catch {
    sessionStorage.removeItem("sw-secret");
    setStatus(t("status.invalidQr"), "warn");
    return;
  }
  connect();
})();

// "Fertig": ends the session on the device, which then returns to Live-TV.
$("finish").addEventListener("click", async () => {
  if (!confirm(t("finish.confirm"))) return;
  await send({ t: "finish" });
  ended = true;
  sessionStorage.removeItem("sw-secret");
  $("finish").hidden = true;
  $("tabs").hidden = true;
  $("app").hidden = true;
  $("intro").hidden = false;
  $("intro").replaceChildren(el("h1", {}, t("common.done")),
    el("p", {}, t("finish.doneText")));
  setStatus(t("status.ended"));
});

// ---------- PIN only (from the PIN window on the TV) ----------
// Only a PIN field: the TV sends no settings; the PIN never appears on the TV screen.
function renderPinOnly() {
  setStatus(t("status.connected"), "ok");
  $("finish").hidden = true;
  $("tabs").hidden = true;
  $("app").hidden = true;
  $("intro").hidden = false;
  const newPin = state.mode === "pinNew";
  const hint = el("p", { class: "hint" }, t("pin.hintInput"));
  // A password (letters allowed) is typed with the keyboard.
  if (state.password && !newPin) {
    $("intro").replaceChildren(el("h1", {}, t("pin.enterPassword")), hint,
      passwordForm(t("pw.label"), (pin) => send({ t: "pinEntry", pin })));
    return;
  }
  const newPassword = () => {
    $("intro").replaceChildren(el("h1", {}, t("pin.newPasswordTitle")), hint,
      passwordForm(t("pw.new"), (first) => {
        $("intro").replaceChildren(el("h1", {}, t("pw.repeat")), hint,
          passwordForm(t("pw.repeat"), (second) => {
            if (first !== second) { toast(t("pw.mismatch"), true); newPassword(); return; }
            send({ t: "pinEntry", pin: first });
          }));
      }));
  };
  // The same digit pad as on the TV: dots for the digits, 1–9, ⌫, 0, OK.
  // Like on the device: a new PIN first asks for its length (4, 6, 8 or own length);
  // with a fixed length the PIN is taken as soon as it is reached.
  let pin = "", first = null, length = null, choosing = newPin;
  const dots = el("div", { class: "pin-dots" });
  const title = el("h1", {});
  const draw = () => {
    title.textContent = !newPin ? t("pin.enter") : first === null ? t("pin.newTitle") : t("pin.repeatNew");
    dots.textContent = Array.from({ length: length || Math.max(4, pin.length) }, (_, i) => (i < pin.length ? "●" : "–")).join(" ");
    lengths.hidden = !choosing;
    dots.hidden = choosing;
    pad.hidden = choosing;
    if (choosing) title.textContent = t("pin.lengthQ");
  };
  const submit = () => {
    if (pin.length < 4) { toast(t("pin.min4"), true); return; }
    if (newPin && first === null) { first = pin; pin = ""; draw(); return; }
    // A new PIN is typed twice here, so a typo cannot lock the device.
    if (newPin && first !== pin) { toast(t("pin.mismatchBoth"), true); first = null; pin = ""; draw(); return; }
    send({ t: "pinEntry", pin });
    pin = ""; first = null; draw();
  };
  const key = (label) => el("button", { class: "pin-key", onclick: () => {
    if (label === "⌫") pin = pin.slice(0, -1);
    else if (label === "OK") return submit();
    else if (pin.length < (length || 12)) pin += label;
    draw();
    if (length && pin.length === length) submit();
  } }, label === "OK" ? t("common.ok") : label);
  const pad = el("div", { class: "pin-pad" }, ["1","2","3","4","5","6","7","8","9","⌫","0","OK"].map(key));
  const lengths = el("div", { class: "pin-lengths" }, [[4, t("pin.digits", 4)], [6, t("pin.digits", 6)], [8, t("pin.digits", 8)], [null, t("pin.lenCustom")], ["pw", t("pin.lenPassword")]]
    .map(([n, label]) => el("button", { class: "pin-key", onclick: () => {
      if (n === "pw") { newPassword(); return; }
      length = n; choosing = false; draw();
    } }, label)));
  // Checking an existing PIN: no fixed length (OK confirms).
  $("intro").replaceChildren(title,
    el("p", { class: "hint" }, t("pin.hintPin")),
    lengths, dots, pad);
  draw();
}

// A text field for a password (or a PIN typed with the keyboard).
function passwordForm(label, onSubmit, numeric = false) {
  const input = el("input", { type: "password", autocomplete: "off", inputmode: numeric ? "numeric" : "text", maxlength: "64" });
  const form = el("form", { class: "card" }, el("label", {}, label, input), el("button", { type: "submit", class: "primary" }, t("common.ok")));
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const value = input.value;
    if (!/^[^\x00-\x1f]{4,64}$/.test(value)) { toast(t("pw.min4"), true); return; }
    input.value = "";
    input.blur();
    onSubmit(value);
  });
  setTimeout(() => input.focus(), 0);
  return form;
}

// Settings protected by a PIN or password: unlock first (checked on the device).
function renderUnlock() {
  setStatus(t("status.connected"), "ok");
  $("finish").hidden = true;
  $("tabs").hidden = true;
  $("app").hidden = true;
  $("intro").hidden = false;
  $("intro").replaceChildren(
    el("h1", {}, t("unlock.title")),
    el("p", { class: "hint" }, state.password
      ? t("unlock.hintPw")
      : t("unlock.hintPin")),
    passwordForm(state.password ? t("pw.label") : t("pin.heading"), (pin) => send({ t: "pinEntry", pin }), !state.password));
}

// ---------- backup ----------
let backupChoice = null;
let backupImport = null;
function downloadBackup(name, file) {
  const bytes = Uint8Array.from(atob(file), (c) => c.charCodeAt(0));
  const url = URL.createObjectURL(new Blob([bytes], { type: "application/octet-stream" }));
  const link = el("a", { href: url, download: name || "StreamWatch-Backup.swbackup" });
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
  toast(t("backup.downloaded"));
}
function partChoice(parts, chosen, onChange) {
  const labels = new Map((state.backupParts || []).map((p) => [p.value, p.label]));
  const all = parts.length > 0 && parts.every((p) => chosen.includes(p));
  return [
    el("div", { class: "row" }, el("span", { class: "label" }, el("b", {}, t("backup.all"))),
      switchButton(all, t("backup.all"), () => onChange(all ? [] : parts.slice()))),
    ...parts.map((p) => el("div", { class: "row" }, el("span", { class: "label" }, labels.get(p) || p),
      switchButton(chosen.includes(p), labels.get(p) || p,
        () => onChange(chosen.includes(p) ? chosen.filter((x) => x !== p) : [...chosen, p])))),
  ];
}
function renderBackup() {
  if (!state || !$("backup")) return;
  const parts = (state.backupParts || []).map((p) => p.value);
  if (!parts.length) { $("backup").replaceChildren(el("p", { class: "hint" }, t("backup.unavailable"))); return; }
  if (!backupChoice) backupChoice = parts.slice();
  const fileInput = el("input", { type: "file", accept: ".swbackup,.json,application/octet-stream,application/json" });
  fileInput.addEventListener("change", () => {
    const file = fileInput.files && fileInput.files[0];
    if (!file) return;
    if (file.size > 270000) { toast(t("backup.tooBig"), true); return; }
    const reader = new FileReader();
    reader.onload = () => {
      const bytes = new Uint8Array(reader.result);
      let text = "";
      for (let i = 0; i < bytes.length; i += 8192) text += String.fromCharCode(...bytes.subarray(i, i + 8192));
      backupImport = { file: btoa(text), name: file.name };
      send({ t: "backupInspect", file: backupImport.file });
    };
    reader.readAsArrayBuffer(file);
  });
  const imported = backupImport && backupImport.info;
  $("backup").replaceChildren(
    el("div", { class: "card" },
      el("h2", {}, t("backup.createTitle")),
      el("p", { class: "hint" }, t("backup.createHint")),
      ...partChoice(parts, backupChoice, (next) => { backupChoice = next; renderBackup(); }),
      el("button", { class: "primary", disabled: !backupChoice.length,
        onclick: () => send({ t: "backupCreate", parts: backupChoice }) }, t("backup.download"))),
    el("div", { class: "card" },
      el("h2", {}, t("backup.importTitle")),
      el("label", {}, t("backup.chooseFile"), fileInput),
      imported ? [
        el("p", { class: "hint" }, imported.createdAt ? t("backup.info", imported.createdAt, imported.appVersion) : backupImport.name),
        ...partChoice(imported.backupParts, backupImport.parts, (next) => { backupImport.parts = next; renderBackup(); }),
        el("p", { class: "hint" }, t("backup.importHint")),
        el("button", { class: "primary", disabled: !backupImport.parts.length, onclick: () => {
          if (confirm(t("backup.importConfirm"))) send({ t: "backupRestore", file: backupImport.file, parts: backupImport.parts });
        } }, t("backup.importNow")),
      ] : null));
}

function pinResult(message) {
  if (message.ok) {
    sessionStorage.removeItem("sw-secret");
    $("intro").replaceChildren(el("h1", {}, state.mode === "pinNew" ? t("pin.set") : t("pin.confirmed")), el("p", {}, t("pin.closePage")));
    setStatus(t("common.done"));
  } else toast(message.message || t("pin.wrong"), true);
}
