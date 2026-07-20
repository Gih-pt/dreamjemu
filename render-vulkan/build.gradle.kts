// Module: render-vulkan — the only supported production rendering backend. See /docs/ARCHITECTURE.md.
// OpenGL must never be added as a dependency here or anywhere else in the project.

val lwjglVersion = "3.3.4"
val lwjglNatives = "natives-windows" // TODO: resolve per-OS at build time (windows/linux/macos, x86_64/arm64)

dependencies {
    implementation(project(":common"))
    implementation(project(":core-gpu-pvr2"))

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-vulkan")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    // lwjgl-vulkan has no natives artifact: Vulkan is loaded via the system's Vulkan loader (ICD).
}
