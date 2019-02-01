/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.graalvm.vm.x86.isa.instruction;

import org.graalvm.vm.memory.vector.Vector128;
import org.graalvm.vm.x86.ArchitecturalState;
import org.graalvm.vm.x86.isa.AMD64Instruction;
import org.graalvm.vm.x86.isa.ImmediateOperand;
import org.graalvm.vm.x86.isa.Operand;
import org.graalvm.vm.x86.isa.OperandDecoder;
import org.graalvm.vm.x86.node.ReadNode;
import org.graalvm.vm.x86.node.WriteNode;

import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class Psrl extends AMD64Instruction {
    private final Operand operand1;
    private final Operand operand2;
    private final String name;

    @Child protected ReadNode readSrc;
    @Child protected ReadNode readShamt;
    @Child protected WriteNode writeDst;

    protected Psrl(long pc, byte[] instruction, Operand operand1, Operand operand2, String name) {
        super(pc, instruction);
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.name = name;

        setGPRReadOperands(operand1, operand2);
        setGPRWriteOperands(operand1);
    }

    @Override
    protected void createChildNodes() {
        ArchitecturalState state = getState();
        readSrc = operand1.createRead(state, next());
        readShamt = operand2.createRead(state, next());
        writeDst = operand1.createWrite(state, next());
    }

    public static class Psrlw extends Psrl {
        public Psrlw(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getAVXOperand2(128), operands.getAVXOperand1(128), "psrlw");
        }

        public Psrlw(long pc, byte[] instruction, OperandDecoder operands, int shamt) {
            super(pc, instruction, operands.getAVXOperand1(128), new ImmediateOperand(shamt), "psrlw");
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            Vector128 vec = readSrc.executeI128(frame);
            long shamt = readShamt.executeI64(frame);
            Vector128 result;
            if (shamt > 15) {
                result = Vector128.ZERO;
            } else {
                result = vec.shrPackedI16((int) shamt);
            }
            writeDst.executeI128(frame, result);
            return next();
        }
    }

    public static class Psrld extends Psrl {
        public Psrld(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getAVXOperand2(128), operands.getAVXOperand1(128), "psrld");
        }

        public Psrld(long pc, byte[] instruction, OperandDecoder operands, int shamt) {
            super(pc, instruction, operands.getAVXOperand1(128), new ImmediateOperand(shamt), "psrld");
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            Vector128 vec = readSrc.executeI128(frame);
            long shamt = readShamt.executeI64(frame);
            Vector128 result;
            if (shamt > 31) {
                result = Vector128.ZERO;
            } else {
                result = vec.shrPackedI32((int) shamt);
            }
            writeDst.executeI128(frame, result);
            return next();
        }
    }

    public static class Psrlq extends Psrl {
        public Psrlq(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getAVXOperand2(128), operands.getAVXOperand1(128), "psrlq");
        }

        public Psrlq(long pc, byte[] instruction, OperandDecoder operands, int shamt) {
            super(pc, instruction, operands.getAVXOperand1(128), new ImmediateOperand(shamt), "psrlq");
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            Vector128 vec = readSrc.executeI128(frame);
            long shamt = readShamt.executeI64(frame);
            Vector128 result;
            if (shamt > 63) {
                result = Vector128.ZERO;
            } else {
                result = vec.shrPackedI64((int) shamt);
            }
            writeDst.executeI128(frame, result);
            return next();
        }
    }

    @Override
    protected String[] disassemble() {
        return new String[]{name, operand1.toString(), operand2.toString()};
    }
}
