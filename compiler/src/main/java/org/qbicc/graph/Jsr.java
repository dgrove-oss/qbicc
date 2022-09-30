package org.qbicc.graph;

import java.util.Map;
import java.util.Objects;

import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class Jsr extends AbstractTerminator implements Resume, Terminator {
    private final Node dependency;
    private final BlockLabel jsrTargetLabel;
    private final BlockLiteral returnAddress;
    private final BasicBlock terminatedBlock;

    Jsr(final Node callSite, final ExecutableElement element, final int line, final int bci, final BlockEntry blockEntry, final Node dependency, final BlockLabel jsrTargetLabel, final BlockLiteral returnAddress, Map<Slot, BlockParameter> parameters, Map<Slot, Value> targetArguments) {
        super(callSite, element, line, bci, targetArguments);
        terminatedBlock = new BasicBlock(blockEntry, this, parameters);
        this.dependency = dependency;
        this.jsrTargetLabel = jsrTargetLabel;
        this.returnAddress = returnAddress;
    }

    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    public BlockLabel getJsrTargetLabel() {
        return jsrTargetLabel;
    }

    public BasicBlock getJsrTarget() {
        return BlockLabel.getTargetOf(jsrTargetLabel);
    }

    public BlockLabel getResumeTargetLabel() {
        return returnAddress.getBlockLabel();
    }

    public Value getReturnAddressValue() {
        return returnAddress;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public int getSuccessorCount() {
        return 2;
    }

    public BasicBlock getSuccessor(final int index) {
        return index == 0 ? getJsrTarget() : index == 1 ? getResumeTarget() : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(dependency, jsrTargetLabel, returnAddress);
    }

    @Override
    String getNodeName() {
        return "Jsr";
    }

    public boolean equals(final Object other) {
        return other instanceof Jsr && equals((Jsr) other);
    }

    public boolean equals(final Jsr other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && jsrTargetLabel.equals(other.jsrTargetLabel)
            && returnAddress.equals(other.returnAddress);
    }
}
