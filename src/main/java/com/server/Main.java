package com.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main extends WebSocketServer {

    private static final int PORT = 3000;
    private final Map<WebSocket, ClientRegistry> clients = new ConcurrentHashMap<>();
    private int clientCounter = 1;

    // Tablero inicial (0 representa casilla vacía)
    private final int[][] initialBoard = {
        {5,3,0, 0,7,0, 0,0,0},
        {6,0,0, 1,9,5, 0,0,0},
        {0,9,8, 0,0,0, 0,6,0},
        {8,0,0, 0,6,0, 0,0,3},
        {4,0,0, 8,0,3, 0,0,1},
        {7,0,0, 0,2,0, 0,0,6},
        {0,6,0, 0,0,0, 2,8,0},
        {0,0,0, 4,1,9, 0,0,5},
        {0,0,0, 0,8,0, 0,7,9}
    };

    // Solución correcta del Sudoku
    private final int[][] solutionBoard = {
        {5,3,4, 6,7,8, 9,1,2},
        {6,7,2, 1,9,5, 3,4,8},
        {1,9,8, 3,4,2, 5,6,7},
        {8,5,9, 7,6,1, 4,2,3},
        {4,2,6, 8,5,3, 7,9,1},
        {7,1,3, 9,2,4, 8,5,6},
        {9,6,1, 5,3,7, 2,8,4},
        {2,8,7, 4,1,9, 6,3,5},
        {3,4,5, 2,8,6, 1,7,9}
    };

    private int[][] currentBoard = new int[9][9];
    private boolean[][] lockedBoard = new boolean[9][9];

    public Main(int port) {
        super(new InetSocketAddress(port));
        resetBoard();
    }

    private void resetBoard() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                currentBoard[r][c] = initialBoard[r][c];
                lockedBoard[r][c] = (initialBoard[r][c] != 0);
            }
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String clientId = "clientX" + (clientCounter++);
        ClientRegistry client = new ClientRegistry(clientId, conn);
        clients.put(conn, client);
        System.out.println("Cliente conectado: " + clientId);
        sendGameState(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        broadcastGameState();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JSONObject msg = new JSONObject(message);
            String type = msg.getString("type");
            ClientRegistry client = clients.get(conn);

            if (client == null) return;

            switch (type) {
                case "join":
                    String nick = msg.optString("nick", client.getId());
                    client.setNick(nick);
                    broadcastGameState();
                    break;

                case "move":
                    int r = msg.getInt("row");
                    int c = msg.getInt("col");
                    int val = msg.getInt("val");

                    if (!lockedBoard[r][c]) {
                        if (solutionBoard[r][c] == val) {
                            currentBoard[r][c] = val;
                            lockedBoard[r][c] = true; // Bloquear casilla
                            client.addScore(2);      // +2 puntos por acierto
                        } else {
                            client.addScore(-1);     // -1 punto por error
                        }
                        broadcastGameState();
                    }
                    break;

                case "restart":
                    resetBoard();
                    for (ClientRegistry cReg : clients.values()) {
                        cReg.resetScore();
                    }
                    broadcastGameState();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastGameState() {
        for (WebSocket conn : clients.keySet()) {
            sendGameState(conn);
        }
    }

    private void sendGameState(WebSocket conn) {
        JSONObject state = new JSONObject();
        state.put("type", "game_state");
        state.put("board", new JSONArray(currentBoard));
        state.put("locked", new JSONArray(lockedBoard));

        // Lista de jugadores ordenada por puntuación descendente
        List<ClientRegistry> playerList = new ArrayList<>(clients.values());
        playerList.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        JSONArray playersArray = new JSONArray();
        for (ClientRegistry p : playerList) {
            JSONObject pObj = new JSONObject();
            pObj.put("nick", p.getNick());
            pObj.put("score", p.getScore());
            playersArray.put(pObj);
        }
        state.put("players", playersArray);

        // Verificar si se ha completado el tablero
        boolean finished = true;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (!lockedBoard[r][c]) {
                    finished = false;
                    break;
                }
            }
        }
        state.put("gameFinished", finished);

        conn.send(state.toString());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Servidor de Sudoku corriendo en el puerto " + PORT);
    }

    public static void main(String[] args) {
        Main server = new Main(PORT);
        server.start();
    }
}