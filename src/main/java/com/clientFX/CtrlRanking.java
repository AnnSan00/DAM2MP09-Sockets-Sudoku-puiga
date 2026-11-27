package com.clientFX;

import java.net.URL;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class CtrlRanking implements Initializable {

    @FXML private Label txtWinner;
    @FXML private VBox listRanking;

    @Override
    public void initialize(URL url, ResourceBundle rb) { }

    /** Refresca la llista (ja ve ordenada per punts des del servidor). */
    public void update(JSONObject state) {
        String me = state.getString("you");
        JSONArray players = state.getJSONArray("players");

        listRanking.getChildren().clear();
        for (int i = 0; i < players.length(); i++) {
            JSONObject p = players.getJSONObject(i);
            String name = p.getString("name");

            Label lblPos = new Label((i + 1) + ".");
            lblPos.setMinWidth(30);
            Label lblName = new Label(name);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label lblScore = new Label(p.getInt("score") + " punts");

            HBox row = new HBox(8, lblPos, lblName, spacer, lblScore);
            String base = "-fx-font-size: 18px; -fx-padding: 6 10 6 10;";
            if (name.equals(me)) {
                base += "-fx-background-color: #dcecff; -fx-font-weight: bold;";
            }
            row.setStyle(base);
            listRanking.getChildren().add(row);
        }

        if (players.length() > 0) {
            txtWinner.setText("Guanyador: " + players.getJSONObject(0).getString("name"));
        }
    }

    @FXML
    private void playAgain() {
        Main.requestPlayAgain();
    }
}
