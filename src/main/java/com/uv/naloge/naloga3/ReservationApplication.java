package com.uv.naloge.naloga3;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;
import java.util.Objects;

public class ReservationApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(ReservationApplication.class.getResource("reservation-view.fxml"));
        Scene scene = new Scene(loader.load(), 520, 770);
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        scene.getStylesheets().add(Objects.requireNonNull(
                ReservationApplication.class.getResource("styles.css")).toExternalForm());

        stage.setTitle("Rezervacija počitnic");
        stage.setMinWidth(425);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
