package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.CompoundType;
import org.qbicc.type.PointerType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A handle for a structure member.  The input handle must have compound type.
 */
public final class MemberOf extends AbstractValue {
    private final Value structurePointer;
    private final PointerType pointerType;
    private final CompoundType.Member member;

    MemberOf(Node callSite, ExecutableElement element, int line, int bci, Value structurePointer, CompoundType.Member member) {
        super(callSite, element, line, bci);
        this.structurePointer = structurePointer;
        this.member = member;
        pointerType = member.getType().getPointer().withQualifiersFrom(structurePointer.getType(PointerType.class));
    }

    public CompoundType getStructType() {
        return getStructurePointer().getType(PointerType.class).getPointeeType(CompoundType.class);
    }

    @Override
    public PointerType getType() {
        return pointerType;
    }

    @Override
    public boolean isConstant() {
        return structurePointer.isConstant();
    }

    @Override
    public boolean isPointeeConstant() {
        return structurePointer.isPointeeConstant();
    }

    @Override
    public AccessMode getDetectedMode() {
        return structurePointer.getDetectedMode();
    }

    public CompoundType.Member getMember() {
        return member;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> structurePointer;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    public Value getStructurePointer() {
        return structurePointer;
    }

    int calcHashCode() {
        return Objects.hash(structurePointer, member);
    }

    @Override
    String getNodeName() {
        return "MemberOf";
    }

    public boolean equals(final Object other) {
        return other instanceof MemberOf && equals((MemberOf) other);
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        b.append("member pointer ");
        structurePointer.toReferenceString(b);
        b.append('.');
        member.toString(b);
        return b;
    }

    public boolean equals(final MemberOf other) {
        return this == other || other != null && structurePointer.equals(other.structurePointer) && member.equals(other.member);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
