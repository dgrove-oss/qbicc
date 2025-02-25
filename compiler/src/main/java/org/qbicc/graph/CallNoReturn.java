package org.qbicc.graph;

import java.util.List;
import java.util.Objects;

import org.qbicc.type.InvokableType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A plain method or function call which never returns normally.
 * Exceptions thrown by the target are not caught; instead, they are propagated out of the caller's frame.
 * This node terminates its block.
 *
 * @see BasicBlockBuilder#callNoReturn(Value, Value, List)
 */
public final class CallNoReturn extends AbstractTerminator implements InvocationNode {
    private final Node dependency;
    private final BasicBlock terminatedBlock;
    private final Value target;
    private final Value receiver;
    private final List<Value> arguments;
    private final InvokableType calleeType;

    CallNoReturn(Node callSite, ExecutableElement element, int line, int bci, final BlockEntry blockEntry, Node dependency, Value target, Value receiver, List<Value> arguments) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.terminatedBlock = new BasicBlock(blockEntry, this);
        this.target = target;
        this.receiver = receiver;
        this.arguments = arguments;
        calleeType = (InvokableType) target.getPointeeType();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(CallNoReturn.class, dependency, target, receiver, arguments);
    }

    @Override
    String getNodeName() {
        return "CallNoReturn";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CallNoReturn && equals((CallNoReturn) other);
    }

    public boolean equals(CallNoReturn other) {
        return this == other || other != null && dependency.equals(other.dependency) && target.equals(other.target) && receiver.equals(other.receiver) && arguments.equals(other.arguments);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return InvocationNode.toRValueString(this, "call", b).append(" no-return");
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    @Override
    public boolean maySafePoint() {
        return ! target.isNoSafePoints();
    }

    public InvokableType getCalleeType() {
        return calleeType;
    }

    public List<Value> getArguments() {
        return arguments;
    }

    @Override
    public Value getTarget() {
        return target;
    }

    @Override
    public Value getReceiver() {
        return receiver;
    }

    @Override
    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    @Override
    public <T, R> R accept(TerminatorVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
