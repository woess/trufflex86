package org.graalvm.vm.x86;

import java.util.Collections;
import java.util.NavigableMap;

import org.graalvm.vm.memory.VirtualMemory;
import org.graalvm.vm.x86.isa.CpuState;
import org.graalvm.vm.x86.node.debug.trace.ExecutionTraceWriter;
import org.graalvm.vm.x86.node.flow.TraceRegistry;
import org.graalvm.vm.x86.posix.PosixEnvironment;

import com.everyware.posix.api.Stack;
import com.everyware.posix.elf.Symbol;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

public class AMD64Context {
    private static final String ARCH_NAME = "x86_64";
    private static final String[] REGISTER_NAMES = {"rax", "rcx", "rdx", "rbx", "rsp", "rbp", "rsi", "rdi", "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15"};

    private final VirtualMemory memory;
    private final PosixEnvironment posix;
    private final String[] args;

    private final TruffleLanguage<AMD64Context> language;
    private final FrameDescriptor frameDescriptor;
    private final FrameSlot[] gpr;
    private final FrameSlot[] zmm;
    private final FrameSlot[] xmm;
    private final FrameSlot[] xmmF32;
    private final FrameSlot[] xmmF64;
    private final FrameSlot[] xmmType;
    private final FrameSlot fs;
    private final FrameSlot gs;
    private final FrameSlot pc;
    private final FrameSlot cf;
    private final FrameSlot pf;
    private final FrameSlot af;
    private final FrameSlot zf;
    private final FrameSlot sf;
    private final FrameSlot df;
    private final FrameSlot of;
    private final FrameSlot ac;
    private final FrameSlot id;

    private final FrameSlot instructionCount;

    private final ArchitecturalState state;

    private NavigableMap<Long, Symbol> symbols;

    private TraceRegistry traces;

    private CpuState snapshot;
    private long returnAddress;
    private long scratchMemory;

    private ExecutionTraceWriter traceWriter;

    public AMD64Context(TruffleLanguage<AMD64Context> language, Env env, FrameDescriptor fd) {
        this(language, env, fd, null);
    }

    public AMD64Context(TruffleLanguage<AMD64Context> language, Env env, FrameDescriptor fd, ExecutionTraceWriter traceWriter) {
        this.language = language;
        this.traceWriter = traceWriter;
        frameDescriptor = fd;
        memory = VirtualMemory.create();
        posix = new PosixEnvironment(memory, ARCH_NAME);
        posix.setStandardIn(env.in());
        posix.setStandardOut(env.out());
        posix.setStandardErr(env.err());
        args = env.getApplicationArguments();
        assert REGISTER_NAMES.length == 16;
        gpr = new FrameSlot[REGISTER_NAMES.length];
        for (int i = 0; i < REGISTER_NAMES.length; i++) {
            gpr[i] = frameDescriptor.addFrameSlot(REGISTER_NAMES[i], FrameSlotKind.Long);
        }
        zmm = new FrameSlot[32];
        xmm = new FrameSlot[32];
        xmmF32 = new FrameSlot[32];
        xmmF64 = new FrameSlot[32];
        xmmType = new FrameSlot[32];
        for (int i = 0; i < zmm.length; i++) {
            zmm[i] = frameDescriptor.addFrameSlot("zmm" + i, FrameSlotKind.Object);
            xmm[i] = frameDescriptor.addFrameSlot("xmm" + i, FrameSlotKind.Object);
            xmmF32[i] = frameDescriptor.addFrameSlot("xmm" + i + "F32", FrameSlotKind.Float);
            xmmF64[i] = frameDescriptor.addFrameSlot("xmm" + i + "F64", FrameSlotKind.Double);
            xmmType[i] = frameDescriptor.addFrameSlot("xmm" + i + "Type", FrameSlotKind.Int);
        }
        fs = frameDescriptor.addFrameSlot("fs", FrameSlotKind.Long);
        gs = frameDescriptor.addFrameSlot("gs", FrameSlotKind.Long);
        pc = frameDescriptor.addFrameSlot("rip", FrameSlotKind.Long);
        cf = frameDescriptor.addFrameSlot("cf", FrameSlotKind.Boolean);
        pf = frameDescriptor.addFrameSlot("pf", FrameSlotKind.Boolean);
        af = frameDescriptor.addFrameSlot("af", FrameSlotKind.Boolean);
        zf = frameDescriptor.addFrameSlot("zf", FrameSlotKind.Boolean);
        sf = frameDescriptor.addFrameSlot("sf", FrameSlotKind.Boolean);
        df = frameDescriptor.addFrameSlot("df", FrameSlotKind.Boolean);
        of = frameDescriptor.addFrameSlot("of", FrameSlotKind.Boolean);
        ac = frameDescriptor.addFrameSlot("ac", FrameSlotKind.Boolean);
        id = frameDescriptor.addFrameSlot("id", FrameSlotKind.Boolean);
        instructionCount = frameDescriptor.addFrameSlot("instructionCount", FrameSlotKind.Long);
        traces = new TraceRegistry(language, frameDescriptor);
        state = new ArchitecturalState(this);
        symbols = Collections.emptyNavigableMap();
    }

    public TruffleLanguage<AMD64Context> getLanguage() {
        return language;
    }

    public FrameDescriptor getFrameDescriptor() {
        return frameDescriptor;
    }

    public VirtualMemory getMemory() {
        return memory;
    }

    public PosixEnvironment getPosixEnvironment() {
        return posix;
    }

    public void setSymbols(NavigableMap<Long, Symbol> symbols) {
        this.symbols = symbols;
    }

    public NavigableMap<Long, Symbol> getSymbols() {
        return symbols;
    }

    public String[] getArguments() {
        return args;
    }

    public FrameSlot getGPR(int i) {
        return gpr[i];
    }

    public FrameSlot getZMM(int i) {
        return zmm[i];
    }

    public FrameSlot getFS() {
        return fs;
    }

    public FrameSlot getGS() {
        return gs;
    }

    public FrameSlot getPC() {
        return pc;
    }

    public FrameSlot[] getGPRs() {
        return gpr;
    }

    public FrameSlot[] getZMMs() {
        return zmm;
    }

    public FrameSlot[] getXMMs() {
        return xmm;
    }

    public FrameSlot[] getXMMF32() {
        return xmmF32;
    }

    public FrameSlot[] getXMMF64() {
        return xmmF64;
    }

    public FrameSlot[] getXMMType() {
        return xmmType;
    }

    public FrameSlot getCF() {
        return cf;
    }

    public FrameSlot getPF() {
        return pf;
    }

    public FrameSlot getAF() {
        return af;
    }

    public FrameSlot getZF() {
        return zf;
    }

    public FrameSlot getSF() {
        return sf;
    }

    public FrameSlot getDF() {
        return df;
    }

    public FrameSlot getOF() {
        return of;
    }

    public FrameSlot getAC() {
        return ac;
    }

    public FrameSlot getID() {
        return id;
    }

    public FrameSlot getInstructionCount() {
        return instructionCount;
    }

    public ArchitecturalState getState() {
        return state;
    }

    public SymbolResolver getSymbolResolver() {
        return new SymbolResolver(symbols);
    }

    public TraceRegistry getTraceRegistry() {
        return traces;
    }

    public long getSigaltstack() {
        Stack stack = posix.getSigaltstack();
        if (stack == null) {
            return 0;
        } else {
            return stack.ss_sp + stack.ss_size;
        }
    }

    public void setStateSnapshot(CpuState state) {
        snapshot = state;
    }

    public CpuState getStateSnapshot() {
        return snapshot;
    }

    public void setReturnAddress(long address) {
        returnAddress = address;
    }

    public long getReturnAddress() {
        return returnAddress;
    }

    public void setScratchMemory(long address) {
        scratchMemory = address;
    }

    public long getScratchMemory() {
        return scratchMemory;
    }

    public ExecutionTraceWriter getTraceWriter() {
        return traceWriter;
    }
}
