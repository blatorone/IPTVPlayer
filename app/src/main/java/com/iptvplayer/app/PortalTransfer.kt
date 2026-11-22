package com.iptvplayer.app

import org.json.JSONArray
import org.json.JSONObject

internal data class PortalCredentials(val address: String, val mac: String)

internal fun exportPortalCredentials(portals: List<SavedPortal>): String =
    JSONObject().apply {
        put("format", "streamwatch-portals")
        put("version", 1)
        put("portals", JSONArray().apply {
            portals.forEach { portal ->
                put(JSONObject().put("address", portal.address).put("mac", portal.mac))
            }
        })
    }.toString(2)

internal fun importPortalCredentials(json: String): List<PortalCredentials> {
    val root = JSONObject(json)
    require(root.optString("format") == "streamwatch-portals" && root.optInt("version") == 1)
    val portals = root.getJSONArray("portals")
    require(portals.length() in 1..50)
    return List(portals.length()) { index ->
        val portal = portals.getJSONObject(index)
        val address = portal.getString("address").trim()
        val mac = normalizePortalMac(portal.getString("mac"))
        require(address.startsWith("http://") || address.startsWith("https://"))
        require(mac.isNotBlank())
        PortalCredentials(address, mac)
    }.distinctBy { SavedPortal(it.address, it.mac, emptyList(), emptyMap(), emptyMap()).identity }
}
