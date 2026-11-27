package com.clientFX;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
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

    public static int port = 3000;
    public static String protocol = "http";
    public static String host = "localhost";
    public static String protocolWS = "ws";

    public static CtrlConfig ctrlConfig;
    public static CtrlGame ctrlGame;
    public static CtrlRanking ctrlRanking;

    /** Nom amb què el jugador entra a la partida. */
    public static String playerName = "";

    /** Mentre esperem que el servidor comenci una partida nova, ignorem estats "finished". */
    private static boolean waitingNewGame = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        final int windowWidth = 820;
        final int windowHeight = 640;

        UtilsViews.parentContainer.setStyle("-fx-font: 14 arial;");
        UtilsViews.addView(getClass(), "ViewConfig", "/assets/viewConfig.fxml");
        UtilsViews.addView(getClass(), "ViewGame", "/assets/viewGame.fxml");
        UtilsViews.addView(getClass(), "ViewRanking", "/assets/viewRanking.fxml");

        ctrlConfig = (CtrlConfig) UtilsViews.getController("ViewConfig");
        ctrlGame = (CtrlGame) UtilsViews.getController("ViewGame");
        ctrlRanking = (CtrlRanking) UtilsViews.getController("ViewRanking");

        Scene scene = new Scene(UtilsViews.parentContainer, windowWidth, windowHeight);

        stage.setScene(scene);
        stage.setTitle("Sudoku multijugador");
        stage.setMinWidth(windowWidth);
        stage.setMinHeight(windowHeight);
        stage.show();

        // Add icon only if not Mac
        if (!System.getProperty("os.name").contains("Mac")) {
            Image icon = new Image("file:/icons/icon.png");
            stage.getIcons().add(icon);
        }
    }

    @Override
    public void stop() {
        if (wsClient != null) {
            wsClient.forceExit();
        }
        System.exit(0); // Kill all executor services
    }

    public static void pauseDuring(long milliseconds, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(milliseconds));
        pause.setOnFinished(event -> Platform.runLater(action));
        pause.play();
    }

    public static <T> List<T> jsonArrayToList(JSONArray array, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(clazz.cast(array.get(i)));
        }
        return list;
    }

    // ---------- Connexió ----------

    public static void connectToServer() {

        String name = ctrlConfig.txtName.getText().trim();
        if (name.isEmpty()) {
            ctrlConfig.txtMessage.setTextFill(Color.RED);
            ctrlConfig.txtMessage.setText("Escriu el teu nom");
            return;
        }
        playerName = name;

        ctrlConfig.txtMessage.setTextFill(Color.BLACK);
        ctrlConfig.txtMessage.setText("Connecting ...");

        pauseDuring(1000, () -> { // Give time to show connecting message ...

            String protocol = ctrlConfig.txtProtocol.getText();
            String host = ctrlConfig.txtHost.getText();
            String port = ctrlConfig.txtPort.getText();
            wsClient = UtilsWS.getSharedInstance(protocol + "://" + host + ":" + port);

            // Platform.runLater assegura que el codi s'executi
            // al fil de la UI, per evitar problemes de concurrència amb JavaFX
            wsClient.onOpen((response) -> sendJoin());
            wsClient.onMessage((response) -> { Platform.runLater(() -> { wsMessage(response); }); });
            wsClient.onError((response) -> { Platform.runLater(() -> { wsError(response); }); });

            // Si la connexió ja s'havia obert abans de registrar el callback
            if (wsClient.isOpen()) {
                sendJoin();
            }
        });
    }

    private static void sendJoin() {
        JSONObject join = new JSONObject().put("type", "join").put("name", playerName);
        wsClient.safeSend(join.toString());
    }

    public static void requestPlayAgain() {
        waitingNewGame = true;
        ctrlGame.resetFeedback();
        wsClient.safeSend(new JSONObject().put("type", "playAgain").toString());
        UtilsViews.setViewAnimating("ViewGame");
    }

    // ---------- Missatges del servidor ----------

    private static void wsMessage(String response) {
        JSONObject msg = new JSONObject(response);

        switch (msg.getString("type")) {
            case "state" -> handleState(msg);
            case "wrong" -> ctrlGame.wrongMove(msg.getInt("row"), msg.getInt("col"));
            case "error" -> {
                ctrlConfig.txtMessage.setTextFill(Color.RED);
                ctrlConfig.txtMessage.setText(msg.optString("message", "Error"));
            }
            default -> System.out.println("Missatge desconegut: " + response);
        }
    }

    private static void handleState(JSONObject state) {
        boolean finished = state.getBoolean("finished");
        if (!finished) {
            waitingNewGame = false;
        }

        ctrlGame.updateState(state);
        ctrlRanking.update(state);

        String active = UtilsViews.getActiveView();
        if ("ViewConfig".equals(active)) {
            UtilsViews.setViewAnimating(finished ? "ViewRanking" : "ViewGame");
        } else if ("ViewGame".equals(active) && finished && !waitingNewGame) {
            UtilsViews.setViewAnimating("ViewRanking");
        }
    }

    private static void wsError(String response) {

        String connectionRefused = "Connection refused";
        if (response.indexOf(connectionRefused) != -1) {
            ctrlConfig.txtMessage.setTextFill(Color.RED);
            ctrlConfig.txtMessage.setText(connectionRefused);
            pauseDuring(1500, () -> {
                ctrlConfig.txtMessage.setText("");
            });
        }
    }
}
