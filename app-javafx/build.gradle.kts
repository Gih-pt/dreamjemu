plugins {
    id("org.openjfx.javafxplugin") version "0.1.0"
    application
}

javafx {
    version = "21.0.2"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
}

application {
    mainClass.set("org.dreamjemu.app.Main")
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
