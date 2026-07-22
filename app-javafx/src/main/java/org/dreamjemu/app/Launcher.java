package org.dreamjemu.app;

/**
 * Indirect entry point used by the packaged application (jpackage app-image)
 * and by the JAR manifest's Main-Class.
 *
 * The Java launcher applies a special check when a JAR's main class directly
 * extends {@code javafx.application.Application}: if the JavaFX modules
 * aren't on the module path — which they aren't in jpackage's flat,
 * non-modular classpath layout — it refuses to start at all, printing
 * "Error: JavaFX runtime components are missing, and are required to run
 * this application", even though the JavaFX jars ARE present on the
 * classpath and everything would otherwise work fine.
 *
 * The standard workaround is this indirection: a plain class (not itself
 * extending Application) as the actual entry point, which simply forwards
 * to {@link Main}. The Java launcher's special-case check only looks at the
 * literal main class it was told to run, so it never sees Main directly and
 * doesn't trigger the module-path requirement.
 *
 * Always point the "run the app" configuration (Gradle's application
 * mainClass, and jpackage's --main-class) at this class, not at {@link Main}
 * directly.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
