package com.server;

import org.java_websocket.WebSocket;

public class ClientRegistry {
    private final String id;
    private final WebSocket conn;
    private String nick;
    private int score;

    public ClientRegistry(String id, WebSocket conn) {
        this.id = id;
        this.conn = conn;
        this.nick = id; // Por defecto el nick es el ID
        this.score = 0;
    }

    public String getId() { return id; }
    public WebSocket getConn() { return conn; }
    public String getNick() { return nick; }
    public void setNick(String nick) { this.nick = nick; }
    public int getScore() { return score; }
    public void addScore(int points) { this.score += points; }
    public void resetScore() { this.score = 0; }
}