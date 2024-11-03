module com.morrison.ip_scanner {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens com.morrison.ip_scanner to javafx.fxml;
    exports com.morrison.ip_scanner;
}