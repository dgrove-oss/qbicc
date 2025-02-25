package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.PointerType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InstanceMethodElement;

/**
 *
 */
public abstract class AbstractMethodLookup extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final Value reference;
    private final InstanceMethodElement method;

    public AbstractMethodLookup(final Node callSite, final ExecutableElement element, final int line, final int bci, Node dependency, final Value reference, final InstanceMethodElement method) {
        super(callSite, element, line, bci);
        // only bind to control flow to avoid bypassing null checks
        while (dependency instanceof OrderedNode on) {
            dependency = on.getDependency();
        }
        this.dependency = dependency;
        this.reference = reference;
        this.method = method;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(getClass(), dependency, reference, method);
    }

    public Node getDependency() {
        return dependency;
    }

    public Value getReference() {
        return reference;
    }

    public InstanceMethodElement getMethod() {
        return method;
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        return reference.toReferenceString(b.append("lookup ").append(getLabel()).append(' ').append(method).append(" for "));
    }

    abstract String getLabel();

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> reference;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof AbstractMethodLookup aml && equals(aml);
    }

    public boolean equals(AbstractMethodLookup other) {
        return other == this || other != null && dependency.equals(other.dependency) && reference.equals(other.reference) && method.equals(other.method);
    }

    @Override
    public PointerType getType() {
        return method.getType().getPointer();
    }
}
