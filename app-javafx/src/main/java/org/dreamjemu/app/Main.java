package org.dreamjemu.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * DreamJEmu desktop shell entry point.
 *
 * This is intentionally a placeholder window for the bootstrap phase of the project
 * (see /docs/STATUS.md). It exists so the Gradle build produces a runnable artifact
 * from day one, before the emulation core, library management, settings screens,
 * and debugging tools described in /docs/ARCHITECTURE.md are implemented.
 *
 * Do not add emulation logic here — this module talks to core-system's public API,
 * it never touches CPU/GPU/AICA/Maple/GD-ROM internals directly.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Label placeholder = new Label(
                "DreamJEmu — bootstrap window.\n" +
                "No emulation core is implemented yet — see docs/STATUS.md and docs/ROADMAP.md."
        );
        Scene scene = new Scene(new StackPane(placeholder), 800, 600);
        stage.setTitle("DreamJEmu");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
