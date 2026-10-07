package com.clientFX;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ResourceBundle;

public class CtrlBoard implements Initializable {

    @FXML private Label lblCurrentPlayer;
    @FXML private GridPane gridSudoku;
    @FXML private ListView<String> listPlayers;

    private final TextField[][] cells = new TextField[9][9];

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initGrid();
    }

    private void initGrid() {
        gridSudoku.getChildren().clear();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                TextField tf = new TextField();
                tf.setPrefSize(42, 42);
                tf.setAlignment(Pos.CENTER);

                // Bordes para separar las subregiones de 3x3
                StringBuilder style = new StringBuilder("-fx-font-size: 14px; -fx-alignment: center; ");
                if (r % 3 == 0) style.append("-fx-border-top: 2px solid black; ");
                if (c % 3 == 0) style.append("-fx-border-left: 2px solid black; ");
                if (r == 8) style.append("-fx-border-bottom: 2px solid black; ");
                if (c == 8) style.append("-fx-border-right: 2px solid black; ");
                tf.setStyle(style.toString());

                final int row = r;
                final int col = c;

                // Enviar número al introducir un valor entre 1 y 9
                tf.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal.isEmpty() && newVal.matches("[1-9]")) {
                        sendMove(row, col, Integer.parseInt(newVal));
                    }
                });

                cells[r][c] = tf;
                gridSudoku.add(tf, c, r);
            }
        }
    }

    private void sendMove(int row, int col, int val) {
        JSONObject msg = new JSONObject();
        msg.put("type", "move");
        msg.put("row", row);
        msg.put("col", col);
        msg.put("val", val);
        Main.wsClient.safeSend(msg.toString());
    }

    public void updateGameState(JSONObject state) {
        lblCurrentPlayer.setText("Juga: " + Main.playerNick);

        JSONArray board = state.getJSONArray("board");
        JSONArray locked = state.getJSONArray("locked");

        for (int r = 0; r < 9; r++) {
            JSONArray rowBoard = board.getJSONArray(r);
            JSONArray rowLocked = locked.getJSONArray(r);
            for (int c = 0; c < 9; c++) {
                int val = rowBoard.getInt(c);
                boolean isLocked = rowLocked.getBoolean(c);
                TextField tf = cells[r][c];

                tf.setDisable(isLocked);

                if (val != 0) {
                    tf.setText(String.valueOf(val));
                } else {
                    tf.setText("");
                }

                // Casillas acertadas/bloqueadas en verde
                if (isLocked) {
                    tf.setStyle(tf.getStyle() + "-fx-background-color: #A9DFBF; -fx-opacity: 1.0; -fx-text-fill: black;");
                }
            }
        }

        // Actualizar lista de jugadores y sus puntuaciones
        listPlayers.getItems().clear();
        JSONArray players = state.getJSONArray("players");
        for (int i = 0; i < players.length(); i++) {
            JSONObject p = players.getJSONObject(i);
            listPlayers.getItems().add(p.getString("nick") + " \t " + p.getInt("score"));
        }
    }
}