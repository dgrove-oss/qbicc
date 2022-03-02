package org.qbicc.type.generic;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;

import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Iterables;
import org.qbicc.context.ClassContext;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 *
 */
public final class BaseTypeSignature extends TypeSignature {
    public static final BaseTypeSignature B = new BaseTypeSignature("B", "byte");
    public static final BaseTypeSignature C = new BaseTypeSignature("C", "char");
    public static final BaseTypeSignature D = new BaseTypeSignature("D", "double");
    public static final BaseTypeSignature F = new BaseTypeSignature("F", "float");
    public static final BaseTypeSignature I = new BaseTypeSignature("I", "int");
    public static final BaseTypeSignature J = new BaseTypeSignature("J", "long");
    public static final BaseTypeSignature S = new BaseTypeSignature("S", "short");
    public static final BaseTypeSignature Z = new BaseTypeSignature("Z", "long");

    public static final BaseTypeSignature V = new BaseTypeSignature("V", "void");

    private final String shortName;
    private final String fullName;

    private BaseTypeSignature(final String shortName, final String fullName) {
        this(shortName, fullName, Iterables.iMap());
    }

    private BaseTypeSignature(final String shortName, final String fullName, final ImmutableMap<ClassTypeDescriptor, Annotation> annotations) {
        super(Objects.hash(BaseTypeSignature.class, shortName, fullName), annotations);
        this.shortName = shortName;
        this.fullName = fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public String getFullName() {
        return fullName;
    }

    public int getCodePoint() {
        return shortName.charAt(0);
    }

    public boolean equals(final TypeSignature other) {
        return other instanceof BaseTypeSignature && equals((BaseTypeSignature) other);
    }

    public BaseTypeDescriptor asDescriptor(final ClassContext classContext) {
        return (BaseTypeDescriptor) super.asDescriptor(classContext);
    }

    BaseTypeDescriptor makeDescriptor(final ClassContext classContext) {
        return BaseTypeDescriptor.forChar(shortName.charAt(0));
    }

    public boolean equals(final BaseTypeSignature other) {
        return this == other;
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append(shortName);
    }

    @Override
    public BaseTypeSignature withAnnotation(Annotation annotation) {
        return (BaseTypeSignature) super.withAnnotation(annotation);
    }

    @Override
    public BaseTypeSignature withAnnotations(Set<Annotation> set) {
        return (BaseTypeSignature) super.withAnnotations(set);
    }

    @Override
    public BaseTypeSignature withOnlyAnnotations(Set<Annotation> set) {
        return (BaseTypeSignature) super.withOnlyAnnotations(set);
    }

    @Override
    public BaseTypeSignature withoutAnnotation(Annotation annotation) {
        return (BaseTypeSignature) super.withoutAnnotation(annotation);
    }

    @Override
    public BaseTypeSignature withoutAnnotation(ClassTypeDescriptor descriptor) {
        return (BaseTypeSignature) super.withoutAnnotation(descriptor);
    }

    @Override
    BaseTypeSignature replacingAnnotationMap(ImmutableMap<ClassTypeDescriptor, Annotation> newMap) {
        return new BaseTypeSignature(shortName, fullName, newMap);
    }

    static BaseTypeSignature parse(ByteBuffer buf) {
        return forChar(next(buf));
    }

    static BaseTypeSignature forChar(final int i) {
        return switch (i) {
            case 'B' -> B;
            case 'C' -> C;
            case 'D' -> D;
            case 'F' -> F;
            case 'I' -> I;
            case 'J' -> J;
            case 'S' -> S;
            case 'V' -> V;
            case 'Z' -> Z;
            default -> throw parseError();
        };
    }
}
