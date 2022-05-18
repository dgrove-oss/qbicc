package org.qbicc.type.generic;

import static org.qbicc.type.generic.Signature.*;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.qbicc.context.ClassContext;

/**
 *
 */
public final class BoundTypeArgument extends TypeArgument {
    private final Variance variance;
    private final ReferenceTypeSignature type;

    BoundTypeArgument(final Variance variance, final ReferenceTypeSignature type) {
        super(Objects.hash(BoundTypeArgument.class, variance, type));
        this.variance = variance;
        this.type = type;
    }

    public Variance getVariance() {
        return variance;
    }

    public ReferenceTypeSignature getBound() {
        return type;
    }

    public StringBuilder toString(final StringBuilder b) {
        if (variance == Variance.CONTRAVARIANT) {
            b.append("-");
        } else if (variance == Variance.COVARIANT) {
            b.append("+");
        }
        type.toString(b);
        return b;
    }

    static BoundTypeArgument parse(ClassContext classContext, ByteBuffer buf) {
        Variance variance;
        int i = peek(buf);
        if (i == '+') {
            variance = Variance.COVARIANT;
            buf.get(); // consume '+'
        } else if (i == '-') {
            variance = Variance.CONTRAVARIANT;
            buf.get(); // consume '-'
        } else {
            variance = Variance.INVARIANT;
        }
        return Cache.get(classContext).getBoundTypeArgument(variance, ReferenceTypeSignature.parse(classContext, buf));
    }

    public static BoundTypeArgument synthesize(ClassContext classContext, Variance variance, ReferenceTypeSignature bound) {
        return Cache.get(classContext).getBoundTypeArgument(variance, bound);
    }
}
