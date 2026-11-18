package com.example.tryoutranking;

import com.example.tryoutranking.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/main.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 760);
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        scene.getStylesheets().add(Main.class.getResource("/css/application.css").toExternalForm());

        MainController controller = loader.getController();
        controller.setStage(stage);

        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(620);
        stage.setOnCloseRequest(controller::handleCloseRequest);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
