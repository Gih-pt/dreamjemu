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

    static int mulL(int n, int m) {
        return 0x0007 | (n << 8) | (m << 4); // MUL.L Rm,Rn
    }

    static int mulsW(int n, int m) {
        return 0x200F | (n << 8) | (m << 4); // MULS.W Rm,Rn
    }

    static int muluW(int n, int m) {
        return 0x200E | (n << 8) | (m << 4); // MULU.W Rm,Rn
    }

    static int dmulsL(int n, int m) {
        return 0x300D | (n << 8) | (m << 4); // DMULS.L Rm,Rn
    }

    static int dmuluL(int n, int m) {
        return 0x3005 | (n << 8) | (m << 4); // DMULU.L Rm,Rn
    }

    static int div0u() {
        return 0x0019;
    }

    static int div0s(int n, int m) {
        return 0x2007 | (n << 8) | (m << 4); // DIV0S Rm,Rn
    }

    static int div1(int n, int m) {
        return 0x3004 | (n << 8) | (m << 4); // DIV1 Rm,Rn
    }

    static int rotcl(int n) {
        return 0x4024 | (n << 8);
    }

    static int movBLoadPostInc(int n, int m) {
        return 0x6004 | (n << 8) | (m << 4); // MOV.B @Rm+,Rn
    }

    static int movWLoadPostInc(int n, int m) {
        return 0x6005 | (n << 8) | (m << 4); // MOV.W @Rm+,Rn
    }

    static int movLLoadPostInc(int n, int m) {
        return 0x6006 | (n << 8) | (m << 4); // MOV.L @Rm+,Rn
    }

    static int movBStorePreDec(int n, int m) {
        return 0x2004 | (n << 8) | (m << 4); // MOV.B Rm,@-Rn
    }

    static int movWStorePreDec(int n, int m) {
        return 0x2005 | (n << 8) | (m << 4); // MOV.W Rm,@-Rn
    }

    static int movLStorePreDec(int n, int m) {
        return 0x2006 | (n << 8) | (m << 4); // MOV.L Rm,@-Rn
    }

    static int movBLoadIndexed(int n, int m) {
        return 0x000C | (n << 8) | (m << 4); // MOV.B @(R0,Rm),Rn
    }

    static int movWLoadIndexed(int n, int m) {
        return 0x000D | (n << 8) | (m << 4); // MOV.W @(R0,Rm),Rn
    }

    static int movLLoadIndexed(int n, int m) {
        return 0x000E | (n << 8) | (m << 4); // MOV.L @(R0,Rm),Rn
    }

    static int movBStoreIndexed(int n, int m) {
        return 0x0004 | (n << 8) | (m << 4); // MOV.B Rm,@(R0,Rn)
    }

    static int movWStoreIndexed(int n, int m) {
        return 0x0005 | (n << 8) | (m << 4); // MOV.W Rm,@(R0,Rn)
    }

    static int movLStoreIndexed(int n, int m) {
        return 0x0006 | (n << 8) | (m << 4); // MOV.L Rm,@(R0,Rn)
    }

    static int movBLoadDisp4(int m, int disp4) {
        return 0x8400 | (m << 4) | (disp4 & 0xF); // MOV.B @(disp,Rm),R0
    }

    static int movWLoadDisp4(int m, int disp4) {
        return 0x8500 | (m << 4) | (disp4 & 0xF); // MOV.W @(disp,Rm),R0
    }

    static int movLLoadDisp4(int n, int m, int disp4) {
        return 0x5000 | (n << 8) | (m << 4) | (disp4 & 0xF); // MOV.L @(disp,Rm),Rn
    }

    static int movBStoreDisp4(int n, int disp4) {
        return 0x8000 | (n << 4) | (disp4 & 0xF); // MOV.B R0,@(disp,Rn)
    }

    static int movWStoreDisp4(int n, int disp4) {
        return 0x8100 | (n << 4) | (disp4 & 0xF); // MOV.W R0,@(disp,Rn)
    }

    static int movLStoreDisp4(int n, int m, int disp4) {
        return 0x1000 | (n << 8) | (m << 4) | (disp4 & 0xF); // MOV.L Rm,@(disp,Rn)
    }

    static int movWLoadPcRel(int n, int disp8) {
        return 0x9000 | (n << 8) | (disp8 & 0xFF); // MOV.W @(disp,PC),Rn
    }

    static int movLLoadPcRel(int n, int disp8) {
        return 0xD000 | (n << 8) | (disp8 & 0xFF); // MOV.L @(disp,PC),Rn
    }

    static int mova(int disp8) {
        return 0xC700 | (disp8 & 0xFF); // MOVA @(disp,PC),R0
    }

    static int rte() {
        return 0x002B;
    }

    static int cmpHs(int n, int m) {
        return 0x3002 | (n << 8) | (m << 4);
    }

    static int cmpGe(int n, int m) {
        return 0x3003 | (n << 8) | (m << 4);
    }

    static int cmpHi(int n, int m) {
        return 0x3006 | (n << 8) | (m << 4);
    }

    static int cmpGt(int n, int m) {
        return 0x3007 | (n << 8) | (m << 4);
    }

    static int cmpPl(int n) {
        return 0x4015 | (n << 8);
    }

    static int cmpPz(int n) {
        return 0x4011 | (n << 8);
    }

    static int cmpStr(int n, int m) {
        return 0x200C | (n << 8) | (m << 4);
    }

    static int tstReg(int n, int m) {
        return 0x2008 | (n << 8) | (m << 4);
    }

    static int tstImm(int imm8) {
        return 0xC800 | (imm8 & 0xFF);
    }

    static int dt(int n) {
        return 0x4010 | (n << 8);
    }

    static int extsB(int n, int m) {
        return 0x600E | (n << 8) | (m << 4);
    }

    static int extsW(int n, int m) {
        return 0x600F | (n << 8) | (m << 4);
    }

    static int extuB(int n, int m) {
        return 0x600C | (n << 8) | (m << 4);
    }

    static int extuW(int n, int m) {
        return 0x600D | (n << 8) | (m << 4);
    }

    static int swapB(int n, int m) {
        return 0x6008 | (n << 8) | (m << 4);
    }

    static int swapW(int n, int m) {
        return 0x6009 | (n << 8) | (m << 4);
    }

    static int xtrct(int n, int m) {
        return 0x200D | (n << 8) | (m << 4);
    }

    static int addc(int n, int m) {
        return 0x300E | (n << 8) | (m << 4);
    }

    static int subc(int n, int m) {
        return 0x300A | (n << 8) | (m << 4);
    }

    static int negc(int n, int m) {
        return 0x600A | (n << 8) | (m << 4);
    }

    static int addv(int n, int m) {
        return 0x300F | (n << 8) | (m << 4);
    }

    static int subv(int n, int m) {
        return 0x300B | (n << 8) | (m << 4);
    }

    static int rotl(int n) {
        return 0x4004 | (n << 8);
    }

    static int rotr(int n) {
        return 0x4005 | (n << 8);
    }

    static int rotcr(int n) {
        return 0x4025 | (n << 8);
    }

    static int shad(int n, int m) {
        return 0x400C | (n << 8) | (m << 4);
    }

    static int shld(int n, int m) {
        return 0x400D | (n << 8) | (m << 4);
    }

    static int tasB(int n) {
        return 0x401B | (n << 8);
    }
}
