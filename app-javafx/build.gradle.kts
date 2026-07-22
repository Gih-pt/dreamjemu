plugins {
    id("org.openjfx.javafxplugin") version "0.1.0"
    application
}

javafx {
    version = "21.0.2"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
}

application {
    // Points at Launcher, not Main directly — see Launcher's Javadoc for why
    // (avoids the "JavaFX runtime components are missing" launcher check).
    mainClass.set("org.dreamjemu.app.Launcher")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":core-system"))
    implementation(project(":core-cpu-sh4"))
    implementation(project(":core-gpu-pvr2"))
    implementation(project(":core-aica"))
    implementation(project(":core-maple"))
    implementation(project(":core-gdrom"))
    implementation(project(":render-vulkan"))
    // render-metal is early-stage/abstraction-only per docs/ROADMAP.md — not wired in yet.
}

/**
 * Builds a native, self-contained application image using jpackage: a
 * platform-native launcher (DreamJEmu.exe / DreamJEmu.app / DreamJEmu) with
 * a bundled Java runtime, so end users never need Java installed separately
 * — see README.md's "no original console files, no separate install steps"
 * spirit and docs/ROADMAP.md's Nightly/Stable release goals.
 *
 * This produces an "app-image" (a runnable folder), not a signed installer
 * (.msi/.dmg/.deb) — those need extra platform-specific tooling (WiX,
 * codesigning certs, dpkg-dev) that isn't set up yet. Producing an
 * installer from this image is a follow-up step, not implemented here.
 *
 * Requires a full JDK (with jmods) providing `jpackage` — the JAVA_HOME
 * used to run Gradle must point at one. Run with, e.g.:
 *   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app-javafx:jpackageImage
 */
tasks.register<Exec>("jpackageImage") {
    group = "distribution"
    description = "Builds a native, self-contained app image (bundled JRE) via jpackage."

    dependsOn("installDist")

    val installLibDir = layout.buildDirectory.dir("install/${project.name}/lib")
    val outputDir = layout.buildDirectory.dir("jpackage")
    val mainJarFileName = tasks.named<Jar>("jar").flatMap { it.archiveFileName }

    inputs.dir(installLibDir)
    outputs.dir(outputDir)

    doFirst {
        delete(outputDir)

        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
        val jpackageBin = if (isWindows) "$javaHome/bin/jpackage.exe" else "$javaHome/bin/jpackage"

        // jpackage requires a strictly numeric X[.Y[.Z]] version — strip any
        // "-bootstrap"/"-SNAPSHOT"-style suffix from the Gradle project version.
        val rawVersion = project.version.toString()
        val sanitizedVersion = Regex("^[0-9]+(\\.[0-9]+){0,2}").find(rawVersion)?.value ?: "0.0.1"

        commandLine(
            jpackageBin,
            "--type", "app-image",
            "--input", installLibDir.get().asFile.absolutePath,
            "--dest", outputDir.get().asFile.absolutePath,
            "--name", "DreamJEmu",
            "--main-jar", mainJarFileName.get(),
            "--main-class", "org.dreamjemu.app.Launcher",
            "--app-version", sanitizedVersion,
            "--vendor", "DreamJEmu contributors",
            "--description", "DreamJEmu - a free, open-source Dreamcast emulator (no BIOS required)"
        )
    }
}
