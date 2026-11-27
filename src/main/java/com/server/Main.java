package com.server;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Servidor WebSocket del Sudoku multijugador.
 *
 * Missatges client → servidor:
 *  - join      {name}               entra a la partida amb un nom
 *  - move      {row, col, value}    intenta posar un valor (files/columnes 0..8)
 *  - playAgain {}                   si la partida ha acabat, en comença una de nova
 *
 * Missatges servidor → client:
 *  - state     {you, board[81], status[81], players[{name,score}], finished}
 *  - wrong     {row, col}           el valor no era correcte (-1 punt)
 *  - error     {message}
 */
public class Main extends WebSocketServer {

    public static final int DEFAULT_PORT = 3000;

    private static final int POINTS_CORRECT = 2;
    private static final int POINTS_WRONG = -1;

    private final ClientRegistry clients = new ClientRegistry();
    private final Object gameLock = new Object();
    private SudokuGame game = new SudokuGame();

    public Main(InetSocketAddress address) {
        super(address);
    }

    // ----------------- Helpers JSON -----------------

    private static JSONObject msg(String type) {
        return new JSONObject().put("type", type);
    }

    private void sendSafe(WebSocket to, String payload) {
        if (to == null) return;
        try {
            to.send(payload);
        } catch (WebsocketNotConnectedException e) {
            System.out.println("Client desconnectat durant send: " + clients.remove(to));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Estat complet de la partida, personalitzat amb el nom del destinatari. */
    private JSONObject stateFor(String name) {
        return msg("state")
                .put("you", name)
                .put("board", new JSONArray(game.boardCopy()))
                .put("status", new JSONArray(game.statusCopy()))
                .put("players", clients.ranking())
                .put("finished", game.isFinished());
    }

    /** Envia l'estat a tots els jugadors que ja han fet join. */
    private void broadcastState() {
        for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {
            sendSafe(e.getKey(), stateFor(e.getValue()).toString());
        }
    }

    // ----------------- WebSocketServer overrides -----------------

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Connexió oberta, esperant join...");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String name = clients.remove(conn);
        System.out.println("Client desconnectat: " + name);
        if (name != null) {
            synchronized (gameLock) {
                broadcastState();
            }
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        JSONObject obj;
        try {
            obj = new JSONObject(message);
        } catch (Exception ex) {
            sendSafe(conn, msg("error").put("message", "JSON invàlid").toString());
            return;
        }

        switch (obj.optString("type", "")) {
            case "join" -> handleJoin(conn, obj);
            case "move" -> handleMove(conn, obj);
            case "playAgain" -> handlePlayAgain(conn);
            default -> sendSafe(conn, msg("error")
                    .put("message", "Tipus desconegut: " + obj.optString("type", "")).toString());
        }
    }

    private void handleJoin(WebSocket conn, JSONObject obj) {
        String name = clients.join(conn, obj.optString("name", ""));
        System.out.println("Jugador: " + name);
        synchronized (gameLock) {
            broadcastState(); // el nou jugador rep l'estat i la resta veu la llista actualitzada
        }
    }

    private void handleMove(WebSocket conn, JSONObject obj) {
        if (clients.nameOf(conn) == null) {
            sendSafe(conn, msg("error").put("message", "Primer cal fer join").toString());
            return;
        }
        int row = obj.optInt("row", -1);
        int col = obj.optInt("col", -1);
        int value = obj.optInt("value", 0);

        synchronized (gameLock) {
            if (game.isFinished()) return;
            int result = game.play(row, col, value);
            if (result == SudokuGame.CORRECT) {
                clients.addScore(conn, POINTS_CORRECT);
                broadcastState();
            } else if (result == SudokuGame.WRONG) {
                clients.addScore(conn, POINTS_WRONG);
                sendSafe(conn, msg("wrong").put("row", row).put("col", col).toString());
                broadcastState();
            }
            // IGNORED (casella ja bloquejada o dades invàlides): no fem res
        }
    }

    private void handlePlayAgain(WebSocket conn) {
        if (clients.nameOf(conn) == null) return;
        synchronized (gameLock) {
            if (game.isFinished()) {
                game = new SudokuGame();
                clients.resetScores();
                System.out.println("Nova partida");
            }
            broadcastState();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Servidor WebSocket engegat al port: " + getPort());
        setConnectionLostTimeout(100);
    }

    // ----------------- Lifecycle util -----------------

    private static void registerShutdownHook(Main server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Aturant servidor (shutdown hook)...");
            try {
                server.stop(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            System.out.println("Servidor aturat.");
        }));
    }

    private static void awaitForever() {
        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();
        registerShutdownHook(server);
        System.out.println("Servidor Sudoku en execució al port " + DEFAULT_PORT + ". Prem Ctrl+C per aturar-lo.");
        awaitForever();
    }
}
