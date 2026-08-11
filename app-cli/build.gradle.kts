// Module: app-cli — a plain command-line tool for detecting, loading, and
// inspecting Dreamcast disc images without needing app-javafx's GUI.
// Intended as a quick manual-testing aid while the GUI is still early —
// see /docs/STATUS.md and /docs/ROADMAP.md.

plugins {
    application
}

application {
    mainClass.set("org.dreamjemu.cli.Main")
}

dependencies {
    implementation(project(":core-gdrom"))
    // Added for the boot-load-and-jump step: HleBootLoader (core-system)
    // writes the located boot file into a real SystemBus, and Sh4Cpu
    // (core-cpu-sh4) is what actually gets pointed at the result.
    implementation(project(":core-system"))
    implementation(project(":core-cpu-sh4"))
    // For wiring PvrRegisters (SPG_STATUS) into SystemBus — see Main's
    // startCpu/attemptMinimalBoot for the SystemBus.mapRegion call.
    implementation(project(":core-gpu-pvr2"))
    // For the --log-level flag — see Main's usage text and LogConfig.
    implementation(project(":common"))
}
