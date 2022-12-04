package org.qbicc.machine.llvm;

import org.qbicc.machine.llvm.impl.LLVM;

/**
 *
 */
public final class FunctionAttributes {
    private FunctionAttributes() {}

    public static final LLValue alwaysinline = LLVM.flagAttribute("alwaysinline");
    public static final LLValue gcLeafFunction = LLVM.flagAttribute("\"gc-leaf-function\"");
    public static final LLValue uwtable = LLVM.flagAttribute("uwtable");
    public static final LLValue noreturn = LLVM.flagAttribute("noreturn");
    public static final LLValue nounwind = LLVM.flagAttribute("nounwind");
    public static final LLValue readnone = LLVM.flagAttribute("readnone");

    public static LLValue framePointer(String val) {
        return LLVM.valueAttribute("\"frame-pointer\"", LLVM.quoteString(val));
    }

    public static LLValue statepointId(int id) {
        return LLVM.valueAttribute("\"statepoint-id\"", LLVM.quoteString(String.valueOf(id)));
    }
}

