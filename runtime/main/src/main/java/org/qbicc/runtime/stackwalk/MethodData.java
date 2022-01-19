package org.qbicc.runtime.stackwalk;

import org.qbicc.runtime.AutoQueued;
import org.qbicc.runtime.CNative;
import org.qbicc.runtime.main.CompilerIntrinsics;

import static org.qbicc.runtime.CNative.*;

public final class MethodData {

    public static native String getFileName(int minfoIndex);
    public static native String getMethodName(int minfoIndex);
    public static native String getMethodDesc(int minfoIndex);
    public static native int getTypeId(int minfoIndex);
    public static native int getModifiers(int minfoIndex);

    public static String getClassName(int minfoIndex) {
        return getClass(minfoIndex).getName();
    }

    public static Class<?> getClass(int minfoIndex) {
        type_id typeId = word(getTypeId(minfoIndex));
        return CompilerIntrinsics.getClassFromTypeId(typeId, word(0));
    }

    public static boolean hasAllModifiersOf(int minfoIndex, int mask) {
        int modifiers = getModifiers(minfoIndex);
        return (modifiers & mask) == mask;
    }

    public static boolean hasNoModifiersOf(int minfoIndex, int mask) {
        int modifiers = getModifiers(minfoIndex);
        return (modifiers & mask) == 0;
    }

    public static native int getMethodInfoIndex(int scIndex);
    public static native int getLineNumber(int scIndex);
    public static native int getBytecodeIndex(int scIndex);
    public static native int getInlinedAtIndex(int scIndex);

    public static native int getSourceCodeInfoIndex(int index);
    public static native long getInstructionAddress(int index);
    public static native int getInstructionListSize();

    static int findInstructionIndex(long ip) {
        // do a binary search in instruction table
        int upper = MethodData.getInstructionListSize();
        int lower = 0;
        while (upper >= lower) {
            int mid = ( upper + lower ) >>> 1;
            long addr = MethodData.getInstructionAddress(mid);
            if (ip == addr) {
                return mid;
            } else if (ip > addr) {
                lower = mid+1;
            } else {
                upper = mid-1;
            }
        }
        return -1;
    }

    private static native void fillStackTraceElement(StackTraceElement element, int scIndex);

    @CNative.extern
    public static native int putchar(int arg);

    // helper to print a string
    private static void printString(String string) {
        char[] contents = string.toCharArray();
        for (char ch: contents) {
            putchar((byte)ch);
        }
        putchar('\n');
    }

    // helper to print a stack frame info
    private static void printFrame(int scIndex) {
        int minfoIndex = getMethodInfoIndex(scIndex);
        String className = getClassName(minfoIndex);
        String fileName = getFileName(minfoIndex);
        String methodName = getMethodName(minfoIndex);
        printString(className + "#" + methodName + "(" + fileName + ")");
    }

    @AutoQueued
    public static void fillStackTraceElements(StackTraceElement[] steArray, Object backtrace, int depth) {
        int[] sourceCodeIndexList = (int[]) backtrace;
        for (int i = 0; i < depth; i++) {
            //printFrame(sourceCodeIndexList[i]);
            fillStackTraceElement(steArray[i], sourceCodeIndexList[i]);
        }
    }
}

