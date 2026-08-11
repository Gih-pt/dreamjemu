// Module: core-gpu-pvr2 — see /docs/ARCHITECTURE.md for its responsibilities.

dependencies {
    // Only depends on core-system's MemoryRegion interface, for PvrRegisters
    // to plug into SystemBus.mapRegion — see core-cpu-sh4's build.gradle.kts
    // for the same "depend only on the interface, not the concrete
    // SystemBus" reasoning.
    implementation(project(":core-system"))
}
