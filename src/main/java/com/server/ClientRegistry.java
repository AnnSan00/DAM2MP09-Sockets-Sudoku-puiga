package com.server;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registre de jugadors connectats: socket → nom i puntuació.
 * Un client només hi apareix després d'enviar el missatge "join".
 */
final class ClientRegistry {

    private static final int MAX_NAME = 16;

    private final Map<WebSocket, String> names = new LinkedHashMap<>();
    private final Map<WebSocket, Integer> scores = new LinkedHashMap<>();

    /** Registra el jugador. Si el nom ja existeix, afegeix " (2)", " (3)"... */
    synchronized String join(WebSocket socket, String requested) {
        String existing = names.get(socket);
        if (existing != null) return existing; // join repetit: ho ignorem

        String base = requested == null ? "" : requested.trim();
        if (base.isEmpty()) base = "Jugador";
        if (base.length() > MAX_NAME) base = base.substring(0, MAX_NAME);

        String name = base;
        int n = 2;
        while (isTaken(name)) {
            name = base + " (" + n++ + ")";
        }
        names.put(socket, name);
        scores.put(socket, 0);
        return name;
    }

    private boolean isTaken(String name) {
        for (String n : names.values()) {
            if (n.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    synchronized String remove(WebSocket socket) {
        scores.remove(socket);
        return names.remove(socket);
    }

    synchronized String nameOf(WebSocket socket) {
        return names.get(socket);
    }

    synchronized void addScore(WebSocket socket, int delta) {
        scores.computeIfPresent(socket, (s, v) -> v + delta);
    }

    synchronized void resetScores() {
        scores.replaceAll((s, v) -> 0);
    }

    /** Còpia dels sockets i noms (per iterar sense bloquejar). */
    synchronized Map<WebSocket, String> snapshot() {
        return new LinkedHashMap<>(names);
    }

    /** Jugadors ordenats per punts (de més a menys) i després per nom. */
    synchronized JSONArray ranking() {
        List<WebSocket> sockets = new ArrayList<>(names.keySet());
        sockets.sort(Comparator
                .comparing((WebSocket s) -> scores.get(s)).reversed()
                .thenComparing(s -> names.get(s).toLowerCase()));
        JSONArray arr = new JSONArray();
        for (WebSocket s : sockets) {
            arr.put(new JSONObject().put("name", names.get(s)).put("score", scores.get(s)));
        }
        return arr;
    }
}
