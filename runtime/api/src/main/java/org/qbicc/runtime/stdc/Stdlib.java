package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stddef.*;

import org.qbicc.runtime.NoReturn;
import org.qbicc.runtime.Build.Target.IsGLibC;

/**
 *
 */
@include("<stdlib.h>")
@define(value = "_DEFAULT_SOURCE", when = IsGLibC.class)
public final class Stdlib {

    // heap

    public static native <P extends ptr<?>> P malloc(size_t size);

    public static native <P extends ptr<?>> P calloc(size_t nMembers, size_t size);

    public static native <P extends ptr<?>> P realloc(ptr<?> ptr, size_t size);

    /**
     * Free the memory referenced by the given pointer.
     *
     * @param ptr a pointer containing the address of memory to free
     */
    public static native void free(ptr<?> ptr);

    // process exit

    @NoReturn
    public static native void abort();

    @NoReturn
    public static native void exit(c_int exitCode);

    public static native c_int atexit(function_ptr<Runnable> function);

    // environment - thread unsafe wrt other env-related operations

    public static native char_ptr getenv(const_char_ptr name);

    public static native c_int setenv(const_char_ptr name, const_char_ptr value, c_int overwrite);

    public static native c_int unsetenv(const_char_ptr name);

    public static native c_int clearenv();

    public static final c_int EXIT_SUCCESS = constant();
    public static final c_int EXIT_FAILURE = constant();

    public static native c_long strtol(const_char_ptr str, char_ptr_ptr entPtr, c_int base);
    public static native long_long strtoll(const_char_ptr str, char_ptr_ptr entPtr, c_int base);
}
