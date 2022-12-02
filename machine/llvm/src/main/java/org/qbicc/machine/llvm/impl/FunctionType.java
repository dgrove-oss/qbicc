package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.qbicc.machine.llvm.LLValue;

final class FunctionType extends AbstractValue {
    final AbstractValue returnType;
    private final List<LLValue> argTypes;
    private final boolean variadic;

    FunctionType(final LLValue returnType, final List<LLValue> argTypes, boolean variadic) {
        this.returnType = (AbstractValue) returnType;
        this.argTypes = argTypes;
        this.variadic = variadic;
    }

    boolean isVariadic() {
        return variadic;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        ((AbstractValue) returnType).appendTo(target);
        target.append(' ');
        target.append('(');
        final Iterator<LLValue> iterator = argTypes.iterator();
        if (iterator.hasNext()) {
            ((AbstractValue) iterator.next()).appendTo(target);
            while (iterator.hasNext()) {
                target.append(',');
                target.append(' ');
                ((AbstractValue) iterator.next()).appendTo(target);
            }
            if (variadic) {
                target.append(',');
                target.append(' ');
            }
        }
        if (variadic) {
            target.append("...");
        }
        target.append(')');
        return target;
    }
}
