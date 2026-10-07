package com.clientFX;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class UtilsViews {

    public static StackPane parentContainer = new StackPane();
    private static Map<String, Parent> views = new HashMap<>();
    private static Map<String, Object> controllers = new HashMap<>();
    private static String activeView = "";

    public static void addView(Class<?> clazz, String name, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(clazz.getResource(fxmlPath));
            Parent view = loader.load();
            views.put(name, view);
            controllers.put(name, loader.getController());

            if (parentContainer.getChildren().isEmpty()) {
                parentContainer.getChildren().add(view);
                activeView = name;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getActiveView() {
        return activeView;
    }

    public static Object getController(String name) {
        return controllers.get(name);
    }

    public static void setView(String name) {
        if (!views.containsKey(name)) return;
        parentContainer.getChildren().clear();
        parentContainer.getChildren().add(views.get(name));
        activeView = name;
    }

    public static void setViewAnimating(String name) {
        if (!views.containsKey(name) || activeView.equals(name)) return;

        Parent nextView = views.get(name);
        Parent currentView = views.get(activeView);

        nextView.setTranslateX(parentContainer.getWidth());
        parentContainer.getChildren().add(nextView);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), nextView);
        slideIn.setToX(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), currentView);
        slideOut.setToX(-parentContainer.getWidth());

        slideIn.play();
        slideOut.play();

        slideIn.setOnFinished(e -> {
            parentContainer.getChildren().remove(currentView);
            currentView.setTranslateX(0);
            activeView = name;
        });
    }
}