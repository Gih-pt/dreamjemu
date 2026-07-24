package org.dreamjemu.cpu.sh4;

/**
 * Shared SH-4 instruction encoders used across this module's tests,
 * mirroring exactly the instruction formats implemented in {@link Sh4Cpu}.
 * Kept in one place so Sh4CpuTest and Sh4CpuSystemBusIntegrationTest can't
 * silently drift out of sync with each other or with the interpreter.
 */
final class Sh4Asm {

    private Sh4Asm() {
    }

    static int nop() {
        return 0x0009;
    }

    static int movImm(int n, int imm8) {
        return 0xE000 | (n << 8) | (imm8 & 0xFF);
    }

    static int movReg(int n, int m) {
        return 0x6003 | (n << 8) | (m << 4);
    }

    static int addImm(int n, int imm8) {
        return 0x7000 | (n << 8) | (imm8 & 0xFF);
    }

    static int addReg(int n, int m) {
        return 0x300C | (n << 8) | (m << 4);
    }

    static int subReg(int n, int m) {
        return 0x3008 | (n << 8) | (m << 4);
    }

    static int cmpEqReg(int n, int m) {
        return 0x3000 | (n << 8) | (m << 4);
    }

    static int cmpEqImmR0(int imm8) {
        return 0x8800 | (imm8 & 0xFF);
    }

    static int bt(int disp8) {
        return 0x8900 | (disp8 & 0xFF);
    }

    static int bf(int disp8) {
        return 0x8B00 | (disp8 & 0xFF);
    }

    static int bra(int disp12) {
        return 0xA000 | (disp12 & 0x0FFF);
    }

    static int movLStore(int n, int m) {
        return 0x2002 | (n << 8) | (m << 4); // MOV.L Rm,@Rn
    }

    static int movLLoad(int n, int m) {
        return 0x6002 | (n << 8) | (m << 4); // MOV.L @Rm,Rn
    }

    static int andReg(int n, int m) {
        return 0x2009 | (n << 8) | (m << 4);
    }

    static int orReg(int n, int m) {
        return 0x200B | (n << 8) | (m << 4);
    }

    static int xorReg(int n, int m) {
        return 0x200A | (n << 8) | (m << 4);
    }

    static int andImmR0(int imm8) {
        return 0xC900 | (imm8 & 0xFF);
    }

    static int orImmR0(int imm8) {
        return 0xCB00 | (imm8 & 0xFF);
    }

    static int xorImmR0(int imm8) {
        return 0xCA00 | (imm8 & 0xFF);
    }

    static int shll(int n) {
        return 0x4000 | (n << 8);
    }

    static int shlr(int n) {
        return 0x4001 | (n << 8);
    }

    static int shal(int n) {
        return 0x4020 | (n << 8);
    }

    static int shar(int n) {
        return 0x4021 | (n << 8);
    }

    static int bsr(int disp12) {
        return 0xB000 | (disp12 & 0x0FFF);
    }

    static int jsr(int n) {
        return 0x400B | (n << 8);
    }

    static int rts() {
        return 0x000B;
    }

    static int jmp(int n) {
        return 0x402B | (n << 8);
    }

    static int movBStore(int n, int m) {
        return 0x2000 | (n << 8) | (m << 4); // MOV.B Rm,@Rn
    }

    static int movBLoad(int n, int m) {
        return 0x6000 | (n << 8) | (m << 4); // MOV.B @Rm,Rn
    }

    static int movWStore(int n, int m) {
        return 0x2001 | (n << 8) | (m << 4); // MOV.W Rm,@Rn
    }

    static int movWLoad(int n, int m) {
        return 0x6001 | (n << 8) | (m << 4); // MOV.W @Rm,Rn
    }

    static int notReg(int n, int m) {
        return 0x6007 | (n << 8) | (m << 4); // NOT Rm,Rn
    }

    static int negReg(int n, int m) {
        return 0x600B | (n << 8) | (m << 4); // NEG Rm,Rn
    }
}
