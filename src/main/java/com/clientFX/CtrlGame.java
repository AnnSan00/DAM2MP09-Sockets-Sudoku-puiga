package com.clientFX;

import java.net.URL;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class CtrlGame implements Initializable {

    private static final int EMPTY = 0, GIVEN = 1, SOLVED = 2;
    private static final int CELL = 46;

    @FXML private BorderPane root;
    @FXML private Label txtPlayer, txtInfo;
    @FXML private GridPane gridBoard;
    @FXML private HBox boxPad;
    @FXML private VBox listPlayers;

    private final Label[][] cells = new Label[9][9];
    private final int[][] values = new int[9][9];
    private final int[][] status = new int[9][9];
    private int selRow = -1, selCol = -1;
    private String myName = "";
    private Integer lastScore = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Taulell 9x9
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                Label cell = new Label();
                cell.setPrefSize(CELL, CELL);
                cell.setMinSize(CELL, CELL);
                cell.setAlignment(Pos.CENTER);
                final int row = r, col = c;
                cell.setOnMouseClicked(e -> selectCell(row, col));
                cells[r][c] = cell;
                gridBoard.add(cell, c, r);
                renderCell(r, c);
            }
        }

        // Teclat numèric en pantalla (1..9)
        for (int n = 1; n <= 9; n++) {
            final int value = n;
            Button b = new Button(String.valueOf(n));
            b.setPrefSize(40, 36);
            b.setOnAction(e -> {
                submit(value);
                root.requestFocus();
            });
            boxPad.getChildren().add(b);
        }

        // Teclat físic: números 1..9
        root.setOnKeyPressed(e -> {
            String t = e.getText();
            if (t.length() == 1 && t.charAt(0) >= '1' && t.charAt(0) <= '9') {
                submit(t.charAt(0) - '0');
            }
        });
    }

    // ---------- Interacció ----------

    private void selectCell(int row, int col) {
        root.requestFocus();
        if (status[row][col] != EMPTY) return; // caselles bloquejades no es seleccionen
        int oldRow = selRow, oldCol = selCol;
        selRow = row;
        selCol = col;
        if (oldRow >= 0) renderCell(oldRow, oldCol);
        renderCell(row, col);
    }

    private void submit(int value) {
        if (selRow < 0 || status[selRow][selCol] != EMPTY) return;
        JSONObject msg = new JSONObject()
                .put("type", "move")
                .put("row", selRow)
                .put("col", selCol)
                .put("value", value);
        Main.wsClient.safeSend(msg.toString());
    }

    // ---------- Missatges del servidor ----------

    public void updateState(JSONObject o) {
        myName = o.getString("you");
        txtPlayer.setText("Juga: " + myName);

        JSONArray board = o.getJSONArray("board");
        JSONArray st = o.getJSONArray("status");
        for (int i = 0; i < 81; i++) {
            values[i / 9][i % 9] = board.getInt(i);
            status[i / 9][i % 9] = st.getInt(i);
        }

        // Si la casella seleccionada ja s'ha bloquejat (algú l'ha encertada), la deseleccionem
        if (selRow >= 0 && status[selRow][selCol] != EMPTY) {
            selRow = -1;
            selCol = -1;
        }
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) renderCell(r, c);
        }

        updatePlayers(o.getJSONArray("players"));
    }

    private void updatePlayers(JSONArray players) {
        listPlayers.getChildren().clear();
        for (int i = 0; i < players.length(); i++) {
            JSONObject p = players.getJSONObject(i);
            String name = p.getString("name");
            int score = p.getInt("score");
            boolean me = name.equals(myName);

            Label lblName = new Label((me ? "▶ " : "") + name);
            Label lblScore = new Label(String.valueOf(score));
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(lblName, spacer, lblScore);
            if (me) {
                row.setStyle("-fx-font-weight: bold; -fx-background-color: #dcecff; -fx-padding: 3 6 3 6;");
                // Feedback de puntuació pròpia
                if (lastScore != null && score - lastScore == 2) {
                    setInfo("✔ Correcte! +2 punts", "#2e7d32");
                }
                lastScore = score;
            } else {
                row.setStyle("-fx-padding: 3 6 3 6;");
            }
            listPlayers.getChildren().add(row);
        }
    }

    /** El servidor diu que el valor no era correcte: -1 punt. */
    public void wrongMove(int row, int col) {
        setInfo("✘ Incorrecte: -1 punt", "#c62828");
        cells[row][col].setStyle(style(row, col, "#ffb3b3", "#c62828", false));
        PauseTransition pause = new PauseTransition(Duration.millis(500));
        pause.setOnFinished(e -> renderCell(row, col));
        pause.play();
    }

    /** Mostra el nom i reinicia l'estat visual (quan comença una partida nova). */
    public void resetFeedback() {
        lastScore = null;
        selRow = -1;
        selCol = -1;
        setInfo("Selecciona una casella i prem un número de l'1 al 9", "#000000");
    }

    private void setInfo(String text, String color) {
        txtInfo.setText(text);
        txtInfo.setStyle("-fx-text-fill: " + color + ";");
    }

    // ---------- Dibuix ----------

    private void renderCell(int r, int c) {
        Label cell = cells[r][c];
        int v = values[r][c];
        cell.setText(v == 0 ? "" : String.valueOf(v));
        switch (status[r][c]) {
            case GIVEN -> cell.setStyle(style(r, c, "#e6e6e6", "#000000", true));
            case SOLVED -> cell.setStyle(style(r, c, "#9be09b", "#14532d", true));
            default -> {
                boolean selected = (r == selRow && c == selCol);
                cell.setStyle(style(r, c, selected ? "#cfe8ff" : "#ffffff", "#1a4fa0", false));
            }
        }
    }

    /** Estil d'una casella; les vores gruixudes marquen les 9 regions de 3x3. */
    private static String style(int r, int c, String bg, String fg, boolean bold) {
        double top = (r % 3 == 0) ? 2 : 0.5;
        double left = (c % 3 == 0) ? 2 : 0.5;
        double right = (c == 8) ? 2 : 0;
        double bottom = (r == 8) ? 2 : 0;
        return "-fx-background-color: " + bg + ";"
                + "-fx-text-fill: " + fg + ";"
                + "-fx-font-size: 22px;"
                + "-fx-font-weight: " + (bold ? "bold" : "normal") + ";"
                + "-fx-border-color: #222222;"
                + "-fx-border-width: " + top + " " + right + " " + bottom + " " + left + ";";
    }
}
