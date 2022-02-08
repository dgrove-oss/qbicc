package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmCharArrayImpl extends VmArrayImpl {

    VmCharArrayImpl(VmImpl vm, int size) {
        super(vm.charArrayClass, size);
    }

    VmCharArrayImpl(final VmCharArrayImpl original) {
        super(original);
    }

    @Override
    public long getArrayElementOffset(int index) {
        return getVmClass().getVm().charArrayContentOffset + ((long) index << 1);
    }

    @Override
    protected VmCharArrayImpl clone() {
        return new VmCharArrayImpl(this);
    }
}
