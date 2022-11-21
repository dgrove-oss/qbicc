package org.qbicc.graph.literal;

import org.qbicc.type.WordType;

public class ValueConvertLiteral extends Literal {
    final Literal value;
    final WordType toType;

    ValueConvertLiteral(final Literal value, final WordType toType) {
        this.value = value;
        this.toType = toType;
    }

    public WordType getType() {
        return toType;
    }

    public Literal getValue() { return value; }

    public boolean isZero() {
        return value.isZero();
    }

    public boolean equals(final Literal other) {
        return other instanceof ValueConvertLiteral && equals((ValueConvertLiteral) other);
    }

    public boolean equals(final ValueConvertLiteral other) {
        return other == this || other != null && toType.equals(other.toType) && value.equals(other.value);
    }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public int hashCode() { return value.hashCode() * 19 + toType.hashCode(); }

    @Override
    public boolean isNullable() {
        return value.isNullable();
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append("convert").append("(");
        value.toString(b);
        b.append(" to ");
        toType.toString(b);
        b.append(')');
        return b;
    }
}
