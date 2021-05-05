package org.qbicc.plugin.lowering;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.object.Function;
import org.qbicc.type.FunctionType;

public class ThrowLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ThrowLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public BasicBlock throw_(final Value value) {
        ThrowExceptionHelper teh = ThrowExceptionHelper.get(ctxt);

        // TODO Is this safe? Can the java/lang/Thread object be moved while this pointer is still in use?
        Value ptr = bitCast(addressOf(instanceFieldOf(referenceHandle(currentThread()), teh.getUnwindExceptionField())), teh.getUnwindExceptionField().getType().getPointer());

        String functionName = "_Unwind_RaiseException";
        FunctionType functionType = teh.getRaiseExceptionMethod().getType();
        ctxt.getImplicitSection(getCurrentElement()).declareFunction(teh.getRaiseExceptionMethod(), functionName, functionType);
        callFunction(ctxt.getLiteralFactory().literalOfSymbol(functionName, functionType), List.of(ptr), Function.FN_NO_RETURN);
        return unreachable();
    }
}
