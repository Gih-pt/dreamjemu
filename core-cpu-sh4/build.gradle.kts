// Module: core-cpu-sh4 — see /docs/ARCHITECTURE.md for its responsibilities.

dependencies {
    // Only depends on core-system's Bus/MemoryRegion interfaces, not on any
    // specific memory map (SystemBus/DreamcastAddressMap) — the interpreter
    // should work against any Bus implementation. See docs/ARCHITECTURE.md.
    implementation(project(":core-system"))
    // For TRACE-level per-instruction logging — see Sh4Cpu's LOG field.
    implementation(project(":common"))
}
