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
package org.graalvm.vm.x86.nfi;

import java.io.File;
import java.io.IOException;

import org.graalvm.vm.x86.AMD64Context;
import org.graalvm.vm.x86.AMD64Language;
import org.graalvm.vm.x86.ArchitecturalState;
import org.graalvm.vm.x86.InteropFunctionPointers;
import org.graalvm.vm.x86.node.AMD64Node;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;

public class InterpreterStartNode extends AMD64Node {
    @Child private InterpreterRootNode interpreter;

    @TruffleBoundary
    private static String getLibnfiPath() {
        String path = System.getProperty("vmx86.libnfi");
        if (path != null) {
            return path;
        } else {
            String defaultPath = AMD64Language.getHome() + File.separator + "libnfi.so";
            File f = new File(defaultPath);
            if (f.exists()) {
                try {
                    return f.getCanonicalPath();
                } catch (IOException e) {
                    return null;
                }
            } else {
                f = new File("build/libnfi.so");
                if (f.exists()) {
                    try {
                        return f.getCanonicalPath();
                    } catch (IOException e) {
                        return null;
                    }
                } else {
                    f = new File("../../build/libnfi.so");
                    if (f.exists()) {
                        try {
                            return f.getCanonicalPath();
                        } catch (IOException e) {
                            return null;
                        }
                    } else {
                        // get exception for default path
                        return defaultPath;
                    }
                }
            }
        }
    }

    public InteropFunctionPointers execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        TruffleLanguage<AMD64Context> language = AMD64NFILanguage.getCurrentLanguage();
        ArchitecturalState state = language.getContextReference().get().getState();
        interpreter = insert(new InterpreterRootNode(state, getLibnfiPath()));
        return interpreter.executeInit(frame);
    }
}
