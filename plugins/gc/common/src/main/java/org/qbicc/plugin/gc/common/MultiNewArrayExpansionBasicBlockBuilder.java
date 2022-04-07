package org.qbicc.plugin.gc.common;

import java.util.Iterator;
import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.Descriptor;

/**
 * Expand a multiNewArray into a loop nest that allocates and
 * connects all of the constituent one dimensional arrays.
 */
public class MultiNewArrayExpansionBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public MultiNewArrayExpansionBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value multiNewArray(final ArrayTypeDescriptor desc, final List<Value> dimensions) {
        return multiNewArray(desc, dimensions.iterator());
    }

    private Value multiNewArray(final ArrayTypeDescriptor desc, final Iterator<Value> dimensions) {
        Value dimension = dimensions.next();
        Value newArray = newArray(desc, dimension);
        if (! dimensions.hasNext()) {
            return newArray;
        }
        Descriptor elementDesc = desc.getElementTypeDescriptor();
        if (!(elementDesc instanceof ArrayTypeDescriptor)) {
            ctxt.error(getLocation(), "Unexpected array descriptor: %s", elementDesc);
            throw new BlockEarlyTermination(unreachable());
        }
        // create a loop to create and fill each nested array
        BlockLabel loop = new BlockLabel();
        BasicBlock initial = goto_(loop);
        begin(loop);
        PhiValue phi = phi(dimension.getType(), loop);
        BlockLabel exit = new BlockLabel();
        BlockLabel resume = new BlockLabel();
        LiteralFactory lf = ctxt.getLiteralFactory();
        if_(isEq(phi, dimension), exit, resume);
        try {
            begin(resume);
            phi.setValueForBlock(ctxt, getCurrentElement(), initial, lf.literalOf(0));
            Value innerArray = multiNewArray((ArrayTypeDescriptor) elementDesc, dimensions);
            store(elementOf(referenceHandle(newArray), phi), innerArray);
            BasicBlock loopExit = goto_(loop);
            phi.setValueForBlock(ctxt, getCurrentElement(), loopExit, add(phi, lf.literalOf(1)));
        } catch (BlockEarlyTermination ignored) {
            // continue
        }
        begin(exit);
        return newArray;
    }

}
