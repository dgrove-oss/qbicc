package org.qbicc.graph;

import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * Represents the bitwise complement of the integer or boolean input.
 */
public final class Comp extends AbstractUnaryValue {
    Comp(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value v) {
        super(callSite, element, line, bci, v);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public WordType getType() {
        return (WordType) super.getType();
    }

    @Override
    public boolean isDefNe(Value other) {
        return other.isDefEq(input) || super.isDefNe(other);
    }

    @Override
    public Value getValueIfTrue(BasicBlockBuilder bbb, Value input) {
        return getInput().getValueIfFalse(bbb, input);
    }

    @Override
    public Value getValueIfFalse(BasicBlockBuilder bbb, Value input) {
        return getInput().getValueIfTrue(bbb, input);
    }

    @Override
    String getNodeName() {
        return "Comp";
    }
}
