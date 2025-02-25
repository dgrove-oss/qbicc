package org.qbicc.plugin.native_;

import static org.qbicc.runtime.CNative.*;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.FunctionType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.DescriptorTypeResolver;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.AnyTypeArgument;
import org.qbicc.type.generic.BoundTypeArgument;
import org.qbicc.type.generic.ClassTypeSignature;
import org.qbicc.type.generic.ReferenceTypeSignature;
import org.qbicc.type.generic.TypeArgument;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;
import org.qbicc.type.generic.Variance;

/**
 * A type resolver which resolves function types.
 */
public class FunctionTypeResolver implements DescriptorTypeResolver.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DescriptorTypeResolver delegate;

    public FunctionTypeResolver(final ClassContext classCtxt, final DescriptorTypeResolver delegate) {
        this.classCtxt = classCtxt;
        ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    public DescriptorTypeResolver getDelegate() {
        return delegate;
    }

    public ValueType resolveTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature) {
        out: if (descriptor instanceof ClassTypeDescriptor) {
            ClassTypeDescriptor ctd = (ClassTypeDescriptor) descriptor;
            if (ctd.getPackageName().equals(Native.NATIVE_PKG)) {
                if (ctd.getClassName().equals(Native.FUNCTION)) {
                    FunctionType functionType;
                    // check the signature
                    TypeSystem ts = ctxt.getTypeSystem();
                    if (signature instanceof ClassTypeSignature sig) {
                        if (! sig.getIdentifier().equals(Native.FUNCTION)) {
                            ctxt.warning("Incorrect generic signature (expected a %s but got \"%s\")", function.class, sig);
                            break out;
                        }
                        List<TypeArgument> args = sig.getTypeArguments();
                        if (args.size() != 1) {
                            ctxt.warning("Incorrect number of generic signature arguments (expected a %s but got \"%s\")", function.class, sig);
                            break out;
                        }
                        TypeArgument typeArgument = args.get(0);
                        if (typeArgument instanceof AnyTypeArgument) {
                            ctxt.error("Function types cannot be open-ended");
                            return ts.getFunctionType(ts.getVoidType(), List.of());
                        } else {
                            assert typeArgument instanceof BoundTypeArgument;
                            BoundTypeArgument bound = (BoundTypeArgument) typeArgument;
                            if (bound.getVariance() != Variance.INVARIANT) {
                                ctxt.error("Function types cannot be open-ended");
                                return ts.getFunctionType(ts.getVoidType(), List.of());
                            }
                            // looks right, get the proper pointee type
                            ReferenceTypeSignature pointeeSig = bound.getBound();
                            if (! (pointeeSig instanceof ClassTypeSignature ctsBound)) {
                                ctxt.error("Function pointee must be a functional interface");
                                return ts.getFunctionType(ts.getVoidType(), List.of());
                            }
                            // todo: use context to resolve type variable bounds
                            ClassTypeDescriptor classDesc = ctsBound.asDescriptor(classCtxt);
                            final String name;
                            if (classDesc.getPackageName().isEmpty()) {
                                name = classDesc.getClassName();
                            } else {
                                name = classDesc.getPackageName() + '/' + classDesc.getClassName();
                            }
                            if (name.equals("java/lang/Object")) {
                                // special case: it's really an "any" pointer with extra front-end guards on it
                                ctxt.error("Function types cannot be open-ended");
                                return ts.getFunctionType(ts.getVoidType(), List.of());
                            }
                            DefinedTypeDefinition definedType = classCtxt.findDefinedType(name);
                            if (definedType == null) {
                                ctxt.error("Function interface type \"%s\" is missing", name);
                                return ts.getFunctionType(ts.getVoidType(), List.of());
                            } else {
                                if (definedType.isInterface()) {
                                    return NativeInfo.get(ctxt).getTypeOfFunctionalInterface(definedType, ctsBound);
                                } else {
                                    ctxt.error("Function interface type \"%s\" is not an interface", name);
                                    return ts.getFunctionType(ts.getVoidType(), List.of());
                                }
                            }
                        }
                    } else {
                        // todo: how can we cleanly get the location from here?
                        ctxt.warning("Generic signature type mismatch (expected a %s but got a %s)", ClassTypeSignature.class, signature.getClass());
                        functionType = ts.getFunctionType(ts.getVoidType(), List.of());
                    }
                    return functionType;
                }
            }
        }
        return getDelegate().resolveTypeFromDescriptor(descriptor, paramCtxt, signature);
    }
}
