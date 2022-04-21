package org.qbicc.interpreter.impl;

import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmPrimitiveClass;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ObjectType;
import org.qbicc.type.Primitive;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;
import static org.qbicc.graph.atomic.AccessModes.SingleRelease;

/**
 *
 */
class VmPrimitiveClassImpl extends VmClassImpl implements VmPrimitiveClass {
    private final LoadedTypeDefinition arrayTypeDefinition;
    private final BaseTypeDescriptor descriptor;
    private final Primitive primitive;

    VmPrimitiveClassImpl(VmImpl vmImpl, VmClassClassImpl classClass, Primitive primitive, LoadedTypeDefinition typeDefinition, LoadedTypeDefinition arrayTypeDefinition, BaseTypeDescriptor descriptor) {
        super(vmImpl, classClass, typeDefinition, 0);
        this.arrayTypeDefinition = arrayTypeDefinition;
        this.descriptor = descriptor;
        this.primitive = primitive;
    }

    @Override
    VmObjectImpl newInstance() {
        throw new UnsupportedOperationException("Cannot construct a primitive instance");
    }

    @Override
    StringBuilder toString(StringBuilder target) {
        return target.append(getName());
    }

    @Override
    public BaseTypeDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public boolean isAssignableFrom(VmClass other) {
        return this == other;
    }

    @Override
    public String getSimpleName() {
        return primitive.getName();
    }

    @Override
    public String getName() {
        return primitive.getName();
    }

    @Override
    public VmArrayClassImpl getArrayClass() {
        return (VmArrayClassImpl) arrayTypeDefinition.getVmClass();
    }

    @Override
    void postConstruct(VmImpl vm) {
        postConstruct(primitive.getName(), vm);
        FieldElement instanceTypeIdField = CoreClasses.get(vm.getCompilationContext()).getClassTypeIdField();
        memory.storeType(indexOf(instanceTypeIdField), primitive.getType(), SinglePlain);
    }

    void setArrayClass(CompilationContext ctxt, VmArrayClassImpl arrayClazz) {
        // post-construct array type def (break bootstrapping circularity)
        int acfIdx = indexOf(CoreClasses.get(ctxt).getArrayClassField());
        getMemory().storeRef(acfIdx, arrayClazz, SingleRelease);
    }

    Primitive getPrimitive() {
        return primitive;
    }

    public ObjectType getInstanceObjectType() {
        throw new UnsupportedOperationException("No instance object type for primitive classes");
    }
}
