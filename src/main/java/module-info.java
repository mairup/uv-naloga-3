module com.uv.naloge.tretjaNaloga {
    requires transitive javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.bootstrapicons;

    opens com.uv.naloge.naloga3 to javafx.fxml;

    exports com.uv.naloge.naloga3;
}