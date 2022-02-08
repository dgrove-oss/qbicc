package org.qbicc.interpreter.impl;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;
import org.qbicc.type.ValueType;

/**
 *
 */
abstract class MemoryImpl implements Memory {
    private static final byte[] NO_DATA = new byte[0];
    private static final Object[] NO_THINGS = new Object[0];

    private static final VarHandle ht = MethodHandles.arrayElementVarHandle(Object[].class);
    private static final VarHandle h8 = MethodHandles.arrayElementVarHandle(byte[].class);

    // Data are items that can be represented directly as bytes and have a minimum alignment of 1
    final byte[] data;
    // Things are items that must be represented as references and have a minimum alignment of 2
    final Object[] things;

    MemoryImpl(int dataSize) {
        // round up to hold one whole Thing
        int thingSize = (dataSize + 1) >> 1;
        dataSize = thingSize << 1;
        data = dataSize == 0 ? NO_DATA : new byte[dataSize];
        things = dataSize == 0 ? NO_THINGS : new Object[thingSize];
    }

    MemoryImpl(MemoryImpl original) {
        data = original.data.clone();
        things = original.things.clone();
    }

    static int alignGap(final int align, final int offset) {
        int mask = align - 1;
        return align - offset & mask;
    }

    static void checkAlign(int offs, int align) {
        int mask = align - 1;
        if ((offs & mask) != 0) {
            throw new IllegalArgumentException("Invalid unaligned access: 0x" + Integer.toHexString(offs));
        }
    }

    @Override
    public final int load8(int index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return (int) h8.get(data, index);
        } else if (SingleOpaque.includes(mode)) {
            return (int) h8.getOpaque(data, index);
        } else if (GlobalAcquire.includes(mode)) {
            return (int) h8.getAcquire(data, index);
        } else {
            return (int) h8.getVolatile(data, index);
        }
    }

    @Override
    public abstract int load16(int index, ReadAccessMode mode);

    @Override
    public abstract int load32(int index, ReadAccessMode mode);

    @Override
    public abstract long load64(int index, ReadAccessMode mode);

    @Override
    public VmObject loadRef(int index, ReadAccessMode mode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(mode)) {
            return (VmObject) ht.get(things, index >> 1);
        } else if (SingleOpaque.includes(mode)) {
            return (VmObject) ht.getOpaque(things, index >> 1);
        } else if (GlobalAcquire.includes(mode)) {
            return (VmObject) ht.getAcquire(things, index >> 1);
        } else {
            return (VmObject) ht.getVolatile(things, index >> 1);
        }
    }

    @Override
    public ValueType loadType(int index, ReadAccessMode mode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(mode)) {
            return (ValueType) ht.get(things, index >> 1);
        } else if (SingleOpaque.includes(mode)) {
            return (ValueType) ht.getOpaque(things, index >> 1);
        } else if (GlobalAcquire.includes(mode)) {
            return (ValueType) ht.getAcquire(things, index >> 1);
        } else {
            return (ValueType) ht.getVolatile(things, index >> 1);
        }
    }

    @Override
    public final void store8(int index, int value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            h8.set(data, index, (byte) value);
        } else if (SingleOpaque.includes(mode)) {
            h8.setOpaque(data, index, (byte) value);
        } else if (GlobalRelease.includes(mode)) {
            h8.setRelease(data, index, (byte) value);
        } else {
            h8.setVolatile(data, index, (byte) value);
        }
    }

    @Override
    public abstract void store16(int index, int value, WriteAccessMode mode);

    @Override
    public abstract void store32(int index, int value, WriteAccessMode mode);

    @Override
    public abstract void store64(int index, long value, WriteAccessMode mode);

    @Override
    public void storeRef(int index, VmObject value, WriteAccessMode mode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(mode)) {
            ht.set(things, index >> 1, value);
        } else if (SingleOpaque.includes(mode)) {
            ht.setOpaque(things, index >> 1, value);
        } else if (GlobalRelease.includes(mode)) {
            ht.setRelease(things, index >> 1, value);
        } else {
            ht.setVolatile(things, index >> 1, value);
        }
    }

    @Override
    public void storeType(int index, ValueType value, WriteAccessMode mode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(mode)) {
            ht.set(things, index >> 1, value);
        } else if (SingleOpaque.includes(mode)) {
            ht.setOpaque(things, index >> 1, value);
        } else if (GlobalRelease.includes(mode)) {
            ht.setRelease(things, index >> 1, value);
        } else {
            ht.setVolatile(things, index >> 1, value);
        }
    }

    @Override
    public final int compareAndExchange8(int index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load8(index, readMode) & 0xff;
            if (val == (expect & 0xff)) {
                store8(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h8.compareAndExchangeAcquire(data, index, (byte) expect, (byte) update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h8.compareAndExchangeRelease(data, index, (byte) expect, (byte) update);
        } else {
            return (int) h8.compareAndExchange(data, index, (byte) expect, (byte) update);
        }
    }

    @Override
    public abstract int compareAndExchange16(int index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public abstract int compareAndExchange32(int index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public abstract long compareAndExchange64(int index, long expect, long update, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public VmObject compareAndExchangeRef(int index, VmObject expect, VmObject update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            VmObject val = loadRef(index, readMode);
            if (val == expect) {
                storeRef(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (VmObject) ht.compareAndExchangeAcquire(things, index >> 1, expect, update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (VmObject) ht.compareAndExchangeRelease(things, index >> 1, expect, update);
        } else {
            return (VmObject) ht.compareAndExchange(things, index >> 1, expect, update);
        }
    }

    @Override
    public ValueType compareAndExchangeType(int index, ValueType expect, ValueType update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        checkAlign(index, 2);
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            ValueType val = loadType(index, readMode);
            if (val == expect) {
                storeType(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (ValueType) ht.compareAndExchangeAcquire(things, index >> 1, expect, update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (ValueType) ht.compareAndExchangeRelease(things, index >> 1, expect, update);
        } else {
            return (ValueType) ht.compareAndExchange(things, index >> 1, expect, update);
        }
    }

    @Override
    public final int getAndSet8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load8(index, readMode);
            store8(index, value, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h8.getAndSetAcquire(data, index, (byte) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h8.getAndSetRelease(data, index, (byte) value);
        } else {
            return (int) h8.getAndSet(data, index, (byte) value);
        }
    }

    @Override
    public abstract int getAndSet16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public abstract int getAndSet32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public abstract long getAndSet64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public VmObject getAndSetRef(int index, VmObject value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            VmObject val = loadRef(index, readMode);
            storeRef(index, value, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (VmObject) ht.getAndSetAcquire(things, index >> 1, value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (VmObject) ht.getAndSetRelease(things, index >> 1, value);
        } else {
            return (VmObject) ht.getAndSet(things, index >> 1, value);
        }
    }

    @Override
    public ValueType getAndSetType(int index, ValueType value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            ValueType val = loadType(index, readMode);
            storeType(index, value, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (ValueType) ht.getAndSetAcquire(things, index >> 1, value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (ValueType) ht.getAndSetRelease(things, index >> 1, value);
        } else {
            return (ValueType) ht.getAndSet(things, index >> 1, value);
        }
    }

    @Override
    public final int getAndAdd8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load8(index, readMode);
            store8(index, value + val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h8.getAndAddAcquire(data, index, (byte) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h8.getAndAddRelease(data, index, (byte) value);
        } else {
            return (int) h8.getAndAdd(data, index, (byte) value);
        }
    }

    @Override
    public abstract int getAndAdd16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public abstract int getAndAdd32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public abstract long getAndAdd64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Override
    public int getAndBitwiseAnd8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load8(index, readMode);
            store8(index, value & val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h8.getAndBitwiseAndAcquire(data, index, (byte) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h8.getAndBitwiseAndRelease(data, index, (byte) value);
        } else {
            return (int) h8.getAndBitwiseAnd(data, index, (byte) value);
        }
    }

    @Override
    public int getAndBitwiseOr8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load8(index, readMode);
            store8(index, value | val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h8.getAndBitwiseOrAcquire(data, index, (byte) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h8.getAndBitwiseOrRelease(data, index, (byte) value);
        } else {
            return (int) h8.getAndBitwiseOr(data, index, (byte) value);
        }
    }

    @Override
    public int getAndBitwiseXor8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load8(index, readMode);
            store8(index, value ^ val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h8.getAndBitwiseXorAcquire(data, index, (byte) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h8.getAndBitwiseXorRelease(data, index, (byte) value);
        } else {
            return (int) h8.getAndBitwiseXor(data, index, (byte) value);
        }
    }

    @Override
    public void storeMemory(int destIndex, Memory src, int srcIndex, int size) {
        if (size > 0) {
            MemoryImpl srcImpl = (MemoryImpl) src;
            System.arraycopy(srcImpl.data, srcIndex, data, destIndex, size);
            // misaligned copies of things will get weird results
            System.arraycopy(srcImpl.things, (srcIndex + 1) >> 1, things, (destIndex + 1) >> 1, (size + 1) >> 1);
        }
    }

    @Override
    public void storeMemory(int destIndex, byte[] src, int srcIndex, int size) {
        if (size > 0) {
            // just data
            System.arraycopy(src, srcIndex, data, destIndex, size);
            // clear corresponding things
            Arrays.fill(things, destIndex >> 1, (destIndex + size + 1) >> 1, null);
        }
    }

    @Override
    public abstract MemoryImpl copy(int newSize);

    byte[] getArray() {
        return data;
    }

    @Override
    protected abstract MemoryImpl clone();
}
