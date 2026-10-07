package com.clientFX;

import org.json.JSONObject;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    public static UtilsWS wsClient;
    public static String playerNick = "Albert";

    public static CtrlConfig ctrlConfig;
    public static CtrlBoard ctrlBoard;
    public static CtrlRanking ctrlRanking;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        final int windowWidth = 700;
        final int windowHeight = 500;

        UtilsViews.parentContainer.setStyle("-fx-font: 14 arial;");
        UtilsViews.addView(getClass(), "ViewConfig", "/assets/viewConfig.fxml");
        UtilsViews.addView(getClass(), "ViewBoard", "/assets/viewBoard.fxml");
        UtilsViews.addView(getClass(), "ViewRanking", "/assets/viewRanking.fxml");

        ctrlConfig = (CtrlConfig) UtilsViews.getController("ViewConfig");
        ctrlBoard = (CtrlBoard) UtilsViews.getController("ViewBoard");
        ctrlRanking = (CtrlRanking) UtilsViews.getController("ViewRanking");

        Scene scene = new Scene(UtilsViews.parentContainer);

        stage.setScene(scene);
        stage.setTitle("Sudoku Multijugador JavaFX");
        stage.setMinWidth(windowWidth);
        stage.setMinHeight(windowHeight);
        stage.show();

        if (!System.getProperty("os.name").contains("Mac")) {
            try {
                Image icon = new Image("file:/icons/icon.png");
                stage.getIcons().add(icon);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void stop() {
        if (wsClient != null) {
            wsClient.forceExit();
        }
        System.exit(0);
    }

    public static void pauseDuring(long milliseconds, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(milliseconds));
        pause.setOnFinished(event -> Platform.runLater(action));
        pause.play();
    }

    public static void connectToServer() {
        ctrlConfig.txtMessage.setTextFill(Color.BLACK);
        ctrlConfig.txtMessage.setText("Connecting ...");

        pauseDuring(1000, () -> {
            String protocol = ctrlConfig.txtProtocol.getText();
            String host = ctrlConfig.txtHost.getText();
            String port = ctrlConfig.txtPort.getText();

            wsClient = UtilsWS.getSharedInstance(protocol + "://" + host + ":" + port);

            wsClient.onOpen((msg) -> {
                JSONObject joinMsg = new JSONObject();
                joinMsg.put("type", "join");
                joinMsg.put("nick", playerNick);
                wsClient.safeSend(joinMsg.toString());
            });

            wsClient.onMessage((response) -> Platform.runLater(() -> wsMessage(response)));
            wsClient.onError((response) -> Platform.runLater(() -> wsError(response)));
        });
    }

    private static void wsMessage(String response) {
        JSONObject msgObj = new JSONObject(response);
        String type = msgObj.getString("type");

        if ("game_state".equals(type)) {
            boolean finished = msgObj.getBoolean("gameFinished");

            if (!finished) {
                if (!"ViewBoard".equals(UtilsViews.getActiveView())) {
                    UtilsViews.setViewAnimating("ViewBoard");
                }
                ctrlBoard.updateGameState(msgObj);
            } else {
                if (!"ViewRanking".equals(UtilsViews.getActiveView())) {
                    UtilsViews.setViewAnimating("ViewRanking");
                }
                ctrlRanking.updateRanking(msgObj.getJSONArray("players"));
            }
        }
    }

    private static void wsError(String response) {
        if ("ViewConfig".equals(UtilsViews.getActiveView())) {
            ctrlConfig.txtMessage.setTextFill(Color.RED);
            ctrlConfig.txtMessage.setText("Error de connexió!");
        }
    }
}