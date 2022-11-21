package org.qbicc.graph.literal;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import org.qbicc.graph.Value;
import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.CompoundType;

/**
 *
 */
public final class CompoundLiteral extends Literal {

    private final CompoundType type;
    private final Map<CompoundType.Member, Literal> values;
    private final int hashCode;

    CompoundLiteral(final CompoundType type, final Map<CompoundType.Member, Literal> values) {
        this.type = type;
        this.values = values;
        hashCode = Objects.hash(type, values);
    }

    public CompoundType getType() {
        return type;
    }

    public Map<CompoundType.Member, Literal> getValues() {
        return values;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public Value extractMember(LiteralFactory lf, CompoundType.Member member) {
        return values.get(member);
    }

    public boolean isZero() {
        return false;
    }

    public boolean equals(final Literal other) {
        return other instanceof CompoundLiteral && equals((CompoundLiteral) other);
    }

    public boolean equals(final CompoundLiteral other) {
        return this == other || other != null && hashCode == other.hashCode && type.equals(other.type) && values.equals(other.values);
    }

    public int hashCode() {
        return hashCode;
    }

    public StringBuilder toString(StringBuilder builder) {
        builder.append('{');
        Iterator<Map.Entry<CompoundType.Member, Literal>> iterator = values.entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<CompoundType.Member, Literal> entry = iterator.next();
            builder.append(entry.getKey().getName());
            builder.append('=');
            builder.append(entry.getValue());
            while (iterator.hasNext()) {
                builder.append(',');
                entry = iterator.next();
                builder.append(entry.getKey().getName());
                builder.append('=');
                builder.append(entry.getValue());
            }
        }
        builder.append('}');
        return builder;
    }
}
