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
package org.graalvm.vm.posix.api;

public class Stack implements Struct {
	public static final int SS_ONSTACK = 1;
	public static final int SS_DISABLE = 2;

	public static final int MINSIGSTKSZ = 4096;
	public static final int SIGSTKSZ = 8192;

	public long ss_sp;
	public int ss_flags;
	public long ss_size;

	@Override
	public PosixPointer read32(PosixPointer ptr) {
		PosixPointer p = ptr;
		ss_sp = p.getI32();
		p = p.add(4);
		ss_flags = p.getI32();
		p = p.add(4);
		ss_size = p.getI32();
		return p.add(4);
	}

	@Override
	public PosixPointer read64(PosixPointer ptr) {
		PosixPointer p = ptr;
		ss_sp = p.getI64();
		p = p.add(8);
		ss_flags = p.getI32();
		p = p.add(8);
		ss_size = p.getI64();
		return p.add(8);
	}

	@Override
	public PosixPointer write32(PosixPointer ptr) {
		PosixPointer p = ptr;
		p.setI32((int) ss_sp);
		p = p.add(4);
		p.setI32(ss_flags);
		p = p.add(4);
		p.setI32((int) ss_size);
		return p.add(4);
	}

	@Override
	public PosixPointer write64(PosixPointer ptr) {
		PosixPointer p = ptr;
		p.setI64(ss_sp);
		p = p.add(8);
		p.setI32(ss_flags);
		p = p.add(8);
		p.setI64(ss_size);
		return p.add(8);
	}

	@Override
	public String toString() {
		return String.format("{ss_sp=0x%x, ss_flags=%d, ss_size=%d}", ss_sp, ss_flags, ss_size);
	}
}
