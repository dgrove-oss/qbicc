package cc.quarkus.qcc.plugin.opt;

import java.util.Iterator;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.NodeVisitor;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Value;

/**
 * A copying visitor which removes redundant {@link PhiValue} nodes.
 */
public class PhiOptimizerVisitor implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock> {
    private final CompilationContext context;
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate;

    public PhiOptimizerVisitor(final CompilationContext context, final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    public NodeVisitor<Node.Copier, Value, Node, BasicBlock> getDelegateNodeVisitor() {
        return delegate;
    }

    public Value visit(final Node.Copier param, final PhiValue node) {
        // see if there is more than one input
        Iterator<BasicBlock> iterator = node.incomingBlocks().iterator();
        while (iterator.hasNext()) {
            BasicBlock b1 = iterator.next();
            if (b1.isReachable()) {
                Value v1 = node.getValueForBlock(b1);
                if (v1 != null && ! v1.equals(node)) {
                    while (iterator.hasNext()) {
                        BasicBlock b2 = iterator.next();
                        Value v2 = node.getValueForBlock(b2);
                        if (v2 != null && ! v2.equals(v1) && ! v2.equals(node)) {
                            // multiple values; process as phi node
                            return (PhiValue) NodeVisitor.Delegating.super.visit(param, node);
                        }
                    }
                    // one value; process as specific value
                    return param.copyValue(v1);
                }
            }
        }
        // *no* inputs; should be impossible!
        context.error("internal: phi block with no inputs");
        return context.getLiteralFactory().literalOfNull();
    }
}
