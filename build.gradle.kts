plugins {
    java
}

allprojects {
    group = "org.dreamjemu"
    version = "0.0.1-bootstrap"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.2")
    }
}

// NOTE: LWJGL (Vulkan bindings) and JavaFX plugin/dependency wiring belong in
// render-vulkan/build.gradle.kts and app-javafx/build.gradle.kts respectively,
// once those modules have real code to compile. Keeping the root build file
// minimal on purpose during bootstrap.
