package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmDoubleArrayImpl extends VmArrayImpl {

    VmDoubleArrayImpl(VmImpl vm, int size) {
        super(vm.doubleArrayClass, size);
    }

    VmDoubleArrayImpl(final VmDoubleArrayImpl original) {
        super(original);
    }

    @Override
    public long getArrayElementOffset(int index) {
        return getVmClass().getVm().doubleArrayContentOffset + ((long) index << 3);
    }

    @Override
    protected VmDoubleArrayImpl clone() {
        return new VmDoubleArrayImpl(this);
    }
}
