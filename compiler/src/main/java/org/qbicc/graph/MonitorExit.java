package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public class MonitorExit extends AbstractNode implements Action, InstanceOperation, OrderedNode {
    private final Node dependency;
    private final Value instance;

    MonitorExit(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final Value instance) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.instance = instance;
    }

    public Value getInstance() {
        return instance;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(MonitorExit.class, dependency, instance);
    }

    @Override
    String getNodeName() {
        return "MonitorExit";
    }

    public boolean equals(final Object other) {
        return other instanceof MonitorExit && equals((MonitorExit) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        instance.toReferenceString(b);
        b.append(')');
        return b;
    }

    public boolean equals(final MonitorExit other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && instance.equals(other.instance);
    }
}
