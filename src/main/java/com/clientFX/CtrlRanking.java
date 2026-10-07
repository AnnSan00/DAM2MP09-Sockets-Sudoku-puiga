package com.clientFX;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class CtrlRanking {

    @FXML private ListView<String> listRanking;

    public void updateRanking(JSONArray players) {
        listRanking.getItems().clear();
        for (int i = 0; i < players.length(); i++) {
            JSONObject p = players.getJSONObject(i);
            listRanking.getItems().add((i + 1) + ". " + p.getString("nick") + " - " + p.getInt("score") + " pts");
        }
    }

    @FXML
    private void playAgain() {
        JSONObject restartMsg = new JSONObject();
        restartMsg.put("type", "restart");
        Main.wsClient.safeSend(restartMsg.toString());
        UtilsViews.setView("ViewBoard");
    }
}